package zh1.liang.tiny.netty.testHandler;

import zh1.liang.tiny.netty.channel.ChannelHandlerContext;
import zh1.liang.tiny.netty.channel.ChannelInboundHandlerAdapter;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
*
* @author: zhe.liang
*
* @create: 2023-10-16 17:53
**/
public class IdleStateHandler extends ChannelInboundHandlerAdapter {

    private final long readerIdleTimeNanos;

    //新定义一个最后一次接收消息的时间
    private long lastReadTime;

    //定义的检测读空闲的定时任务
    private ScheduledFuture<?> readerIdleTimeout;

    public IdleStateHandler(long seconds) {
        //判断用户设置的读空闲时间是否小于0
        if (seconds <= 0) {
            readerIdleTimeNanos = 0;
        } else {
            readerIdleTimeNanos = TimeUnit.SECONDS.toNanos(seconds);
        }
    }

    //得到当前时间
    long ticksInNanos() {
        return System.nanoTime();
    }

    //设置定时任务
    ScheduledFuture<?> schedule(ChannelHandlerContext ctx, Runnable task, long delay, TimeUnit unit) {
        return ctx.executor().schedule(task, delay, unit);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //如果readerIdleTimeNanos大于0，说明设定了读空闲检测
        if (readerIdleTimeNanos > 0) {
            //更新最后一次接收到消息的时间，并且每接收到一次消息就重制一次时间
            lastReadTime = ticksInNanos();
        }
        //继续向链表后面的节点传递入站事件
        ctx.fireChannelRead(msg);
    }

    //这个方法就是创建监测3秒心跳的定时任务
    private void initialize(ChannelHandlerContext ctx) {
        //最后一次读写时间设置成当前时间，这里就是初始化最后一次接收消息时间
        //用当前时间给这个最后一次接收消息的时间赋值
        lastReadTime = ticksInNanos();
        //设置读超时定时任务，这里面的重点在ReaderIdleTimeoutTask
        //这个ReaderIdleTimeoutTask就是监测读空闲的定时任务，下面马上就会讲到
        if (readerIdleTimeNanos > 0) {
            //设定的读空闲时间是3秒，所以就在这里创建一个3秒后执行的定时任务
            readerIdleTimeout = schedule(ctx, new ReaderIdleTimeoutTask(ctx),
                    readerIdleTimeNanos, TimeUnit.NANOSECONDS);
        }
    }


    private abstract static class AbstractIdleTask implements Runnable{
        private final ChannelHandlerContext ctx;

        AbstractIdleTask(ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public void run() {
            if (!ctx.channel().isOpen()) {
                return;
            }
            //这里调用的就是下面的那个抽象方法，而抽象方法要被子类实现
            //模版方法模式
            run(ctx);
        }

        protected abstract void run(ChannelHandlerContext ctx);
    }


    private final class ReaderIdleTimeoutTask extends AbstractIdleTask{
        //构造方法
        ReaderIdleTimeoutTask(ChannelHandlerContext ctx) {
            super(ctx);
        }

        @Override
        protected void run(ChannelHandlerContext ctx) {
            //读空闲时间赋值，这里就是把3秒的读空闲时间赋值给这个局部变量了
            long nextDelay = readerIdleTimeNanos;

            //接下来就让当前时间减去最后一次读的时间，得到一个时间差，这个时间差就是距离上一次
            //接收到消息后过去的时间，然后再让读空闲时间减去该时间差
            nextDelay -= ticksInNanos() - lastReadTime;

            //如果结果小于0，说明时间差大于3秒了，这意味着已经超时了
            if (nextDelay <= 0) {
                //既然是循环检测读空闲，所以就要刷新定时任务，再过readerIdleTimeNanos时间执行定时任务
                //在这里把定时任务本身，也就是this再次提交给定时任务队列，供3秒之后再次执行，这个知识上一章已经讲了
                readerIdleTimeout = schedule(ctx, this, readerIdleTimeNanos, TimeUnit.NANOSECONDS);
            } else {
                //走到这里说明时间差还小于3秒，说明还没有过去3秒呢，读空闲的时间为3秒
                //但现在还没过3秒，意味着需要继续等一会，等到过去3秒是再检测
                //所以要提交新的定时任务，而定时任务的时间为剩余的读空闲时间
                //其实出现这种情况也很好理解，比如说客户端是在第1秒启动的，在第3秒接收到了
                //消息，那么最后一次接收到消息的时间就是第3秒，如果检测读空闲的定时任务是在第2秒
                //才启动，3秒后执行，那就是第5秒执行，第5秒的时候，当前时间减去最后一次接收到
                //消息的时间为2秒，显然还没到3秒。如果要想检测，定时任务是不是在等待1秒检测，就可以判断
                //是不是会触发读空闲事件了？这里的逻辑就是这个意思，大家仔细品味一下吧
                readerIdleTimeout = schedule(ctx, this, nextDelay, TimeUnit.NANOSECONDS);
            }
        }
    }
}
