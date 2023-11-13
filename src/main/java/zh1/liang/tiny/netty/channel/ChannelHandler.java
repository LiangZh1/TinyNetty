package zh1.liang.tiny.netty.channel;

import java.lang.annotation.*;

/**
 * @author: zhe.liang
 * @create: 2023-09-18 20:53
 */
public interface ChannelHandler {

    void handlerAdded(ChannelHandlerContext ctx) throws Exception;


    void handlerRemoved(ChannelHandlerContext ctx) throws Exception;


    @Deprecated
    void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception;


    /**
     * @Description:ChannelHandler是否可以公用的注解，这要考虑到并发问题。
     */
    @Inherited
    @Documented
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Sharable {

    }
}
