package zh1.liang.tiny.netty.channel.nio;

import zh1.liang.tiny.netty.channel.EventLoop;
import zh1.liang.tiny.netty.channel.MultiThreadEventLoopGroup;
import zh1.liang.tiny.netty.channel.EventLoopTaskQueueFactory;
import zh1.liang.tiny.netty.channel.DefaultSelectStrategyFactory;
import zh1.liang.tiny.netty.channel.SelectStrategyFactory;

import zh1.liang.tiny.netty.util.concurrent.RejectedExecutionHandler;
import zh1.liang.tiny.netty.util.concurrent.RejectedExecutionHandlers;

import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.Executor;

/**
 * @author: zhe.liang
 * @create: 2023-08-02 13:27
 **/
public class NioEventLoopGroup extends MultiThreadEventLoopGroup {


    public NioEventLoopGroup(int nThreads) {
        this(nThreads, (Executor) null);
    }

    public NioEventLoopGroup(int nThreads, Executor executor) {
        this(nThreads, executor, SelectorProvider.provider());
    }

    public NioEventLoopGroup(int nThreads, Executor executor, final SelectorProvider selectorProvider) {
        this(nThreads, executor, selectorProvider, DefaultSelectStrategyFactory.INSTANCE);
    }

    public NioEventLoopGroup(int nThreads, Executor executor, final SelectorProvider selectorProvider,
                             final SelectStrategyFactory selectStrategyFactory) {
        super(nThreads, executor, selectorProvider, selectStrategyFactory, RejectedExecutionHandlers.reject());
    }

    //在这里，如果返回的这个EventLoop接口继承了EventExecutor接口，
    // 就可以调用EventExecutor接口中的方法了。
    @Override
    protected EventLoop newChild(Executor executor, Object... args) throws Exception {
        EventLoopTaskQueueFactory queueFactory = args.length == 4 ? (EventLoopTaskQueueFactory) args[3] : null;
        return new NioEventLoop(this, executor, (SelectorProvider) args[0],
                ((SelectStrategyFactory) args[1]).newSelectStrategy(), (RejectedExecutionHandler) args[2], queueFactory);
    }
}
