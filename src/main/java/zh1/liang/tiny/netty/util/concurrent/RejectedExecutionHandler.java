package zh1.liang.tiny.netty.util.concurrent;


import zh1.liang.tiny.netty.util.concurrent.SingleThreadEventExecutor;

public interface RejectedExecutionHandler {

    void rejected(Runnable task, SingleThreadEventExecutor executor);
}
