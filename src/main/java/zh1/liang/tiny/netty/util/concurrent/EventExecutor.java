package zh1.liang.tiny.netty.util.concurrent;

/**
 * @author: zhe.liang
 * @create: 2023-08-01 14:23
 *
 *  eventloop的接口，这个接口也继承了EventExecutorGroup，这样在eventloopgroup中
 *  调用方法，eventloop中就可以直接调用同名方法。
 **/
public interface EventExecutor extends EventExecutorGroup {

    @Override
    EventExecutor next();

    EventExecutorGroup parent();

    boolean inEventLoop(Thread thread);

    <V> Promise<V> newPromise();

    <V> ProgressivePromise<V> newProgressivePromise();

    <V> Future<V> newSucceededFuture(V result);

    <V> Future<V> newFailedFuture(Throwable cause);

}
