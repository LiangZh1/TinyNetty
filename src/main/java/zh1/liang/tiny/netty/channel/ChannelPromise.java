package zh1.liang.tiny.netty.channel;

import zh1.liang.tiny.netty.util.concurrent.Future;
import zh1.liang.tiny.netty.util.concurrent.GenericFutureListener;
import zh1.liang.tiny.netty.util.concurrent.Promise;

/**
 * @author: zhe.liang
 * @create: 2023-08-06 20:27
 */
public interface ChannelPromise extends ChannelFuture, Promise<Void> {
    @Override
    Channel channel();

    @Override
    ChannelPromise setSuccess(Void result);

    /**
     * @Author: PP-jessica
     * @Description:这个方法和下面的方法是本接口中定义的
     */
    ChannelPromise setSuccess();

    boolean trySuccess();

    @Override
    ChannelPromise setFailure(Throwable cause);

    @Override
    ChannelPromise addListener(GenericFutureListener<? extends Future<? super Void>> listener);

    @Override
    ChannelPromise addListeners(GenericFutureListener<? extends Future<? super Void>>... listeners);

    @Override
    ChannelPromise removeListener(GenericFutureListener<? extends Future<? super Void>> listener);

    @Override
    ChannelPromise removeListeners(GenericFutureListener<? extends Future<? super Void>>... listeners);

    @Override
    ChannelPromise sync() throws InterruptedException;

    @Override
    ChannelPromise syncUninterruptibly();

    @Override
    ChannelPromise await() throws InterruptedException;

    @Override
    ChannelPromise awaitUninterruptibly();

    ChannelPromise unvoid();
}
