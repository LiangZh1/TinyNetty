package zh1.liang.tiny.netty.channel;
/**
 * @author: zhe.liang
 * @create: 2023-09-18 20:55
 */
public interface ChannelInboundHandler extends ChannelHandler{

    //只要成功注册到 Selector 上之后，channelRegistered 方法就会被回调
    void channelRegistered(ChannelHandlerContext ctx) throws Exception;

    void channelUnregistered(ChannelHandlerContext ctx) throws Exception;

    //在 bind 方法执行成功之后开始回调，确切的说，该方法会在 NioServerSocketChannel 绑定端口号成功之后被回调
    void channelActive(ChannelHandlerContext ctx) throws Exception;

    void channelInactive(ChannelHandlerContext ctx) throws Exception;


    void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception;

    void channelReadComplete(ChannelHandlerContext ctx) throws Exception;

    void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception;


    void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception;


    @Override
    @SuppressWarnings("deprecation")
    void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception;

}
