package demo.IdleHandler;

import zh1.liang.tiny.netty.channel.*;

import java.net.SocketAddress;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author: zhe.liang
 * @create: 2023-10-16 17:53
 */
public class IdleStateHandler extends ChannelInboundHandlerAdapter implements ChannelOutboundHandler {

    //设定的读空闲时间
    private final long readerIdleTimeNanos;

    //设定的写空闲时间
    private final long writerIdleTimeNanos;

    // 新定义一个最后一次接收消息的时间
    private long lastReadTime;

    //最后一次的写时间
    private long lastWriteTime;

    private long allIdleTimeNanos;

    // 定义的检测读空闲的定时任务
    private ScheduledFuture<?> readerIdleTimeout;

    // 定义的检测写空闲的定时任务
    private ScheduledFuture<?> writerIdleTimeout;

    //要添加到Promise中的监听器
    private final ChannelFutureListener writeListener = new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            //该监听器会在发送消息成功后被回调，回调的时候会把最后一次发送消息的时间重置为当前时间
            lastWriteTime = ticksInNanos();
        }
    };

    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception {

    }

    public IdleStateHandler(long readerIdleTime, long writerIdleTime) {
        //判断用户设置的读空闲时间是否小于0
        if (readerIdleTime <= 0) {
            readerIdleTimeNanos = 0;
        } else {
            readerIdleTimeNanos = TimeUnit.SECONDS.toNanos(readerIdleTime);
        }
        //判断用户传进来的写空闲时间是否小于0
        if (writerIdleTime <= 0) {
            writerIdleTimeNanos = 0;
        } else {
            writerIdleTimeNanos = TimeUnit.SECONDS.toNanos(writerIdleTime);
        }
    }


    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        //在该方法中初始化两个每隔3秒就执行一次的定时任务
        //分别检测读空闲和写空闲事件
        initialize(ctx);
    }


    //该方法就是把空闲事件向ChannelPipeline链表上传递，并且被相应节点的UserEventTriggered方法处理
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt){
        //这里就会回调每一个入站处理器的userEventTriggered方法
        ctx.fireUserEventTriggered(evt);
    }

    // 得到当前时间
    long ticksInNanos() {
        return System.nanoTime();
    }

    // 设置定时任务
    ScheduledFuture<?> schedule(
            ChannelHandlerContext ctx, Runnable task, long delay, TimeUnit unit) {
        return ctx.executor().schedule(task, delay, unit);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 如果readerIdleTimeNanos大于0，说明设定了读空闲检测
        if (readerIdleTimeNanos > 0) {
            // 更新最后一次接收到消息的时间，并且每接收到一次消息就重制一次时间
            lastReadTime = ticksInNanos();
        }
        // 继续向链表后面的节点传递入站事件
        ctx.fireChannelRead(msg);
    }



    //当发送数据的时候，经过该处理器，会在该方法内给Promise添加一个发送事件成功后的监听器
    //因为是发送数据，所以自然是要把最后一次发送数据的事件置为当前时间，并且该监听器会在
    //消息发送成功后被回调
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (writerIdleTimeNanos > 0 || allIdleTimeNanos > 0) {
            ctx.write(msg, promise.unvoid()).addListener(writeListener);
        } else {
            ctx.write(msg, promise);
        }
    }

    // 这个方法就是创建监测3秒心跳的定时任务
    private void initialize(ChannelHandlerContext ctx) {
        //用当前时间给这个最后一次接收消息和最后一次发送消息的时间赋值
        lastReadTime = lastWriteTime = ticksInNanos();
        if (readerIdleTimeNanos > 0) {
            readerIdleTimeout = schedule(ctx, new ReaderIdleTimeoutTask(ctx),
                    readerIdleTimeNanos, TimeUnit.NANOSECONDS);
        }
        //设置写超时定时任务
        if (writerIdleTimeNanos > 0) {
            writerIdleTimeout = schedule(ctx, new WriterIdleTimeoutTask(ctx),
                    writerIdleTimeNanos, TimeUnit.NANOSECONDS);
        }
    }



    private abstract static class AbstractIdleTask implements Runnable {
        private final ChannelHandlerContext ctx;

        AbstractIdleTask(ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public void run() {
            if (!ctx.channel().isOpen()) {
                return;
            }
            // 这里调用的就是下面的那个抽象方法，而抽象方法要被子类实现
            // 模版方法模式
            run(ctx);
        }

        protected abstract void run(ChannelHandlerContext ctx);
    }

    private final class ReaderIdleTimeoutTask extends AbstractIdleTask {
        // 构造方法
        ReaderIdleTimeoutTask(ChannelHandlerContext ctx) {
            super(ctx);
        }

        @Override
        protected void run(ChannelHandlerContext ctx) {
            // 读空闲时间赋值，这里就是把3秒的读空闲时间赋值给这个局部变量了
            long nextDelay = readerIdleTimeNanos;

            // 接下来就让当前时间减去最后一次读的时间，得到一个时间差，这个时间差就是距离上一次
            // 接收到消息后过去的时间，然后再让读空闲时间减去该时间差
            nextDelay -= ticksInNanos() - lastReadTime;

            // 如果结果小于0，说明时间差大于3秒了，这意味着已经超时了
            if (nextDelay <= 0) {
                // 既然是循环检测读空闲，所以就要刷新定时任务，再过readerIdleTimeNanos时间执行定时任务
                // 在这里把定时任务本身，也就是this再次提交给定时任务队列，供3秒之后再次执行，这个知识上一章已经讲了
                readerIdleTimeout = schedule(ctx, this, readerIdleTimeNanos, TimeUnit.NANOSECONDS);
                IdleStateEvent event = new IdleStateEvent(IdleState.READER_IDLE, false);
                channelIdle(ctx, event);

            } else {
                // 走到这里说明时间差还小于3秒，说明还没有过去3秒呢，读空闲的时间为3秒
                // 但现在还没过3秒，意味着需要继续等一会，等到过去3秒是再检测
                // 所以要提交新的定时任务，而定时任务的时间为剩余的读空闲时间
                // 其实出现这种情况也很好理解，比如说客户端是在第1秒启动的，在第3秒接收到了
                // 消息，那么最后一次接收到消息的时间就是第3秒，如果检测读空闲的定时任务是在第2秒
                // 才启动，3秒后执行，那就是第5秒执行，第5秒的时候，当前时间减去最后一次接收到
                // 消息的时间为2秒，显然还没到3秒。如果要想检测，定时任务是不是在等待1秒检测，就可以判断
                // 是不是会触发读空闲事件了？这里的逻辑就是这个意思，大家仔细品味一下吧
                readerIdleTimeout = schedule(ctx, this, nextDelay, TimeUnit.NANOSECONDS);
            }
        }
    }


    private final class WriterIdleTimeoutTask extends AbstractIdleTask {

        WriterIdleTimeoutTask(ChannelHandlerContext ctx) {
            super(ctx);
        }

        //下面代码的逻辑和检测读空闲定时任务的逻辑几乎一样，就不再重复注释了
        @Override
        protected void run(ChannelHandlerContext ctx) {
            long lastWriteTime = IdleStateHandler.this.lastWriteTime;
            long nextDelay = writerIdleTimeNanos - (ticksInNanos() - lastWriteTime);
            if (nextDelay <= 0) {
                writerIdleTimeout = schedule(ctx, this, writerIdleTimeNanos, TimeUnit.NANOSECONDS);
                //创建写空闲事件
                IdleStateEvent event = new IdleStateEvent(IdleState.WRITER_IDLE, false);
                channelIdle(ctx, event);
            } else {
                writerIdleTimeout = schedule(ctx, this, nextDelay, TimeUnit.NANOSECONDS);
            }
        }
    }


    @Override
    public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception {

    }

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {

    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {

    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {

    }

    @Override
    public void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {

    }

    @Override
    public void read(ChannelHandlerContext ctx) throws Exception {

    }
}
