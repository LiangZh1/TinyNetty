package zh1.liang.tiny.netty.util.concurrent;


public interface RejectedExecutionHandler {

    void rejected(Runnable task, SingleThreadEventExecutor executor);
}
