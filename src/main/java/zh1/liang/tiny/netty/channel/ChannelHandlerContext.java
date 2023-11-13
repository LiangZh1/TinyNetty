package zh1.liang.tiny.netty.channel;

import zh1.liang.tiny.netty.util.AttributeMap;
import zh1.liang.tiny.netty.util.concurrent.EventExecutor;

/**
 * @author: zhe.liang
 * @create: 2023-09-18 20:57
 *
 * 需要支持ctx往前、往后的调用，所以这个接口需要继承invoker
 */
public interface ChannelHandlerContext extends AttributeMap,ChannelInboundInvoker,ChannelOutboundInvoker{

    /**
     * 包装的Channel
     * @return
     */
    Channel channel();

    ChannelHandler handler();

    String name();

    EventExecutor executor();

    ChannelPipeline pipeline();

    boolean isRemoved();


    @Override
    ChannelHandlerContext fireChannelRegistered();

    @Override
    ChannelHandlerContext fireChannelUnregistered();

    @Override
    ChannelHandlerContext fireChannelActive();

    @Override
    ChannelHandlerContext fireChannelInactive();

    @Override
    ChannelHandlerContext fireExceptionCaught(Throwable cause);

    @Override
    ChannelHandlerContext fireUserEventTriggered(Object evt);

    @Override
    ChannelHandlerContext fireChannelRead(Object msg);

    @Override
    ChannelHandlerContext fireChannelReadComplete();

    @Override
    ChannelHandlerContext fireChannelWritabilityChanged();

    @Override
    ChannelHandlerContext read();

    @Override
    ChannelHandlerContext flush();


}
