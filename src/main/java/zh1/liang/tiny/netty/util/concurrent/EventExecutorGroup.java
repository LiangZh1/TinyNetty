package zh1.liang.tiny.netty.util.concurrent;

import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author: zhe.liang
 * @create: 2023-08-02 13:37
 **/
public interface EventExecutorGroup extends ScheduledExecutorService, Iterable<EventExecutor>{
    
    EventExecutor next();

    void shutdownGracefully();

    boolean isTerminated();

    void awaitTermination(Integer integer, TimeUnit timeUnit) throws InterruptedException;


    @Override
    Iterator<EventExecutor> iterator();


    @Override
    Future<?> submit(Runnable task);

    @Override
    <T> Future<T> submit(Runnable task, T result);

    @Override
    <T> Future<T> submit(Callable<T> task);


    @Override
    ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit);

    @Override
    <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit);

    @Override
    ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit);

    @Override
    ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit);


}
