package zh1.liang.tiny.netty.channel;

import lombok.extern.slf4j.Slf4j;
import zh1.liang.tiny.netty.util.concurrent.RejectedExecutionHandler;
import zh1.liang.tiny.netty.util.concurrent.SingleThreadEventExecutor;
import zh1.liang.tiny.netty.util.internal.ObjectUtil;

import java.util.Queue;
import java.util.concurrent.Executor;

/**
 *
 * @author: zhe.liang
 *
 * @create: 2023-07-31 13:55
 **/
@Slf4j
public abstract class SingleThreadEventLoop extends SingleThreadEventExecutor implements EventLoop{

    protected static final int DEFAULT_MAX_PENDING_TASKS = Integer.MAX_VALUE;


    protected SingleThreadEventLoop(EventLoopGroup parent, Executor executor,
                                    boolean addTaskWakesUp, Queue<Runnable> taskQueue, Queue<Runnable> tailTaskQueue,
                                    RejectedExecutionHandler rejectedExecutionHandler) {
        super(parent, executor, addTaskWakesUp, taskQueue, rejectedExecutionHandler);
    }

    @Override
    public EventLoop next() {
        return this;
    }

    @Override
    public EventLoopGroup parent() {
        return null;
    }

    protected boolean hasTasks() {
        return super.hasTask();
    }

    @Override
    public ChannelFuture register(Channel channel) {
        //在这里可以发现在执行任务的时候，channel和promise也是绑定的
        return register(new DefaultChannelPromise(channel, this));
    }

    /**
     * @Author: PP-jessica
     * @Description:因为还没有引入unsafe类，所以该方法暂时先简化实现
     */
    @Override
    public ChannelFuture register(final ChannelPromise promise) {
        ObjectUtil.checkNotNull(promise, "promise");
        promise.channel().unsafe().register(this, promise);
        return promise;
    }



}
