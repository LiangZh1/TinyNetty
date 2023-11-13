package zh1.liang.tiny.netty.channel;

import zh1.liang.tiny.netty.util.NettyRuntime;
import zh1.liang.tiny.netty.util.concurrent.DefaultThreadFactory;
import zh1.liang.tiny.netty.util.concurrent.MultiThreadEventExecutorGroup;
import zh1.liang.tiny.netty.util.internal.SystemPropertyUtil;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * @author: zhe.liang
 * @create: 2023-08-02 14:25
 **/
public abstract class MultiThreadEventLoopGroup extends MultiThreadEventExecutorGroup implements EventLoopGroup{

    private static final int DEFAULT_EVENT_LOOP_THREADS;

    //如果用户没有设定线程数量，则线程数默认使用这里的cpu核数乘2
    static {
        DEFAULT_EVENT_LOOP_THREADS = Math.max(1, SystemPropertyUtil.getInt(
                "io.netty.eventLoopThreads", NettyRuntime.availableProcessors() * 2));

    }


    //通常情况下，这个构造器会从子类被调用
    protected MultiThreadEventLoopGroup(int nThreads, Executor executor, Object... args) {
        super(nThreads == 0 ? DEFAULT_EVENT_LOOP_THREADS : nThreads, executor, args);
    }

    @Override
    protected ThreadFactory newDefaultThreadFactory() {
        return new DefaultThreadFactory(getClass(), Thread.MAX_PRIORITY);
    }

    @Override
    public EventLoop next() {
        return (EventLoop) super.next();
    }

    @Override
    protected abstract EventLoop newChild(Executor executor, Object... args) throws Exception;

    @Override
    public ChannelFuture register(Channel channel) {
        return next().register(channel);
    }

    @Override
    public ChannelFuture register(ChannelPromise promise) {
        return next().register(promise);
    }


}
