package zh1.liang.tiny.netty.channel;

import zh1.liang.tiny.netty.util.concurrent.GenericFutureListener;

/**
 * @author: zhe.liang
 * @create: 2023-08-06 20:16
 */
public interface ChannelFutureListener extends GenericFutureListener<ChannelFuture> {

    ChannelFutureListener CLOSE = new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) {
            future.channel().close();
        }
    };

    ChannelFutureListener CLOSE_ON_FAILURE = new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) {
            if (!future.isSuccess()) {
                future.channel().close();
            }
        }
    };
}
