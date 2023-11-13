package zh1.liang.tiny.netty.channel;

import zh1.liang.tiny.netty.util.concurrent.EventExecutor;

/**
 * @author: zhe.liang
 * @create: 2023-10-11 13:35
 *
 *  * 消息处理器是个接口，只实现消息就行，不允许存在成员变量，否则，用户使用起来会比较麻烦
 */
public class DefaultChannelHandlerContext extends AbstractChannelHandlerContext {

    private final ChannelHandler handler;

    DefaultChannelHandlerContext(
            DefaultChannelPipeline pipeline, EventExecutor executor, String name, ChannelHandler handler) {
        //用反射判断该消息处理器实现了哪个方法
        super(pipeline, executor, name, handler.getClass());
        this.handler = handler;
    }



    @Override
    public ChannelHandler handler() {
        return handler;
    }


}
