package zh1.liang.tiny.netty.bootstrap;

import lombok.extern.slf4j.Slf4j;
import zh1.liang.tiny.netty.channel.*;
import zh1.liang.tiny.netty.channel.ChannelOption;
import zh1.liang.tiny.netty.util.AttributeKey;
import zh1.liang.tiny.netty.util.concurrent.EventExecutor;
import zh1.liang.tiny.netty.util.internal.ObjectUtil;
import zh1.liang.tiny.netty.util.internal.SocketUtils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * @author: zhe.liang
 * @create: 2023-08-14 20:47
 */
@Slf4j
public abstract class AbstractBootstrap<B extends AbstractBootstrap<B, C>, C extends Channel> {

    volatile EventLoopGroup group;

    private volatile ChannelFactory<? extends C> channelFactory;

    private volatile SocketAddress localAddress;

    private final Map<ChannelOption<?>, Object> options = new LinkedHashMap<>();

    private final Map<AttributeKey<?>, Object> attrs = new LinkedHashMap<>();

    private volatile ChannelHandler handler;

    AbstractBootstrap() {

    }

    AbstractBootstrap(AbstractBootstrap<B,C> bootstrap){
        group = bootstrap.group;
        channelFactory = bootstrap.channelFactory;
        localAddress = bootstrap.localAddress;
        synchronized (bootstrap.options){
            options.putAll(bootstrap.options);
        }
        synchronized (bootstrap.attrs){
            attrs.putAll(bootstrap.attrs);
        }

    }

    public B group(EventLoopGroup group) {
        ObjectUtil.checkNotNull(group, "group");
        if (this.group != null) {
            throw new IllegalStateException("group set already");
        }
        this.group = group;
        return self();
    }

    public B channel(Class<? extends C> channelClass) {
        return channelFactory(new ReflectiveChannelFactory<C>(
                ObjectUtil.checkNotNull(channelClass, "channelClass")
        ));
    }

    @Deprecated
    public B channelFactory(ChannelFactory<? extends C> channelFactory) {
        ObjectUtil.checkNotNull(channelFactory, "channelFactory");
        if (this.channelFactory != null) {
            throw new IllegalStateException("channelFactory set already");
        }
        this.channelFactory = channelFactory;
        return self();
    }

    public B localAddress(SocketAddress localAddress) {
        this.localAddress = localAddress;
        return self();
    }


    public B localAddress(int inetPort) {
        return localAddress(new InetSocketAddress(inetPort));
    }


    public B localAddress(String inetHost, int inetPort) {
        return localAddress(SocketUtils.socketAddress(inetHost, inetPort));
    }


    public B localAddress(InetAddress inetHost, int inetPort) {
        return localAddress(new InetSocketAddress(inetHost, inetPort));
    }


    public <T> B option(ChannelOption<T> option, T value) {
        ObjectUtil.checkNotNull(option, "option");
        if (value == null) {
            synchronized (options) {
                options.remove(option);
            }
        } else {
            synchronized (options) {
                options.put(option, value);
            }
        }
        return self();
    }

    public <T> B attr(AttributeKey<T> key, T value) {
        ObjectUtil.checkNotNull(key, "key");
        if (value == null) {
            synchronized (attrs) {
                attrs.remove(key);
            }
        } else {
            synchronized (attrs) {
                attrs.put(key, value);
            }
        }
        return self();
    }

    public B validate() {
        if (group == null) {
            throw new IllegalStateException("group not set");
        }
        if (channelFactory == null) {
            throw new IllegalStateException("channel or channelFactory not set");
        }
        return self();
    }


    public ChannelFuture register(){
        validate();
        return initAndRegister();
    }


    public ChannelFuture bind(){
        validate();
        if(localAddress == null){
            throw new IllegalStateException("localAddress not set");
        }
        return doBind(localAddress);
    }

    public ChannelFuture bind(int netPort){
        return bind(new InetSocketAddress(netPort));
    }

    public ChannelFuture bind(String inetHost, int inetPort) {
        return bind(SocketUtils.socketAddress(inetHost, inetPort));
    }


    public ChannelFuture bind(InetAddress inetHost, int inetPort) {
        return bind(new InetSocketAddress(inetHost, inetPort));
    }


    public ChannelFuture bind(SocketAddress localAddress) {
        validate();
        return doBind(ObjectUtil.checkNotNull(localAddress, "localAddress"));
    }

    public final EventLoopGroup group() {
        return group;
    }

    final Map<ChannelOption<?>, Object> options0() {
        return options;
    }

    final Map<AttributeKey<?>, Object> attrs0() {
        return attrs;
    }

    final SocketAddress localAddress() {
        return localAddress;
    }

    final ChannelFactory<? extends C> channelFactory() {
        return channelFactory;
    }


    /**
     * @Author: PP-jessica
     * @Description:这个是传入一个options的map集合
     */
    static void setChannelOptions(Channel channel, Map<ChannelOption<?>, Object> options) {
        for (Map.Entry<ChannelOption<?>, Object> e: options.entrySet()) {
            setChannelOption(channel, e.getKey(), e.getValue());
        }
    }


    static void setChannelOptions(
            Channel channel, Map.Entry<ChannelOption<?>, Object>[] options) {
        for (Map.Entry<ChannelOption<?>, Object> e: options) {
            setChannelOption(channel, e.getKey(), e.getValue());
        }
    }


    protected abstract void init(Channel channel) throws Exception;

    public abstract AbstractBootstrapConfig<B, C> config();


    protected final ChannelFuture initAndRegister(){
        Channel channel = null;
        try {
            channel = channelFactory.newChannel();
            init(channel);
        } catch (Throwable t) {
            if (channel != null) {
                channel.unsafe().closeForcibly();
                return new DefaultChannelPromise(channel,channel.eventLoop()).setFailure(t);
            }
        }

        //注册到boss线程组的执行器上
        ChannelFuture regFuture = config().group().register(channel);

        if(regFuture.cause() != null){
            if(channel.isRegistered()){
                channel.close();
            }else {
                channel.unsafe().closeForcibly();
            }
        }

        return regFuture;

    }

    private ChannelFuture doBind(final SocketAddress localAddress){
        //channel注册到selector，但是没有设置关心的事件
        ChannelFuture regFuture = initAndRegister();
        final Channel channel = regFuture.channel();

        //注册失败
        if (regFuture.cause() != null) {
            return regFuture;
        }

        //判断init是否完成
        if (regFuture.isDone()) { //完成，则进行bind
            DefaultChannelPromise promise = new DefaultChannelPromise(channel);
            doBind0(regFuture,channel,localAddress,promise);
            return promise;
        }else { //否则等待init完成
            //走到这里，说明上面的initAndRegister方法中，服务端的channel还没有完全注册到单线程执行器的selector上
            //此时可以直接则向regFuture添加回调函数，这里有个专门的静态内部类，用来协助判断服务端channel是否注册成功
            //该回调函数会在regFuture完成的状态下被调用，在回调函数中进行服务端的绑定
            PendingRegistrationPromise bindPromise = new PendingRegistrationPromise(channel);

            regFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    Throwable cause = future.cause();
                    if(cause != null){ //init失败，bind直接失败
                        bindPromise.setFailure(cause);
                    }else {
                        bindPromise.registered();
                        doBind0(regFuture,channel,localAddress,bindPromise);
                    }
                }
            });

            return bindPromise;
        }


    }

    private static void doBind0(
            ChannelFuture regFuture,
            final Channel channel,
            final SocketAddress localAddress,
            final ChannelPromise bindPromise) {
        //仍然是异步执行，其实只要记住这个异步执行就可以
        channel.eventLoop().execute(new Runnable() {
            @Override
            public void run() {
                if (regFuture.isSuccess()) {
                    //服务端channel绑定端口号
                    channel.bind(localAddress,bindPromise).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
                }else {
                    bindPromise.setFailure(regFuture.cause());
                }
            }
        });

    }

    final ChannelHandler handler() {
        return handler;
    }

    public B handler(ChannelHandler handler) {
        this.handler = ObjectUtil.checkNotNull(handler, "handler");
        return self();
    }


    private B self() {
        return (B) this;
    }

    private static void setChannelOption(Channel channel, ChannelOption<?> option, Object value) {
        try {
            if (!channel.config().setOption((ChannelOption<Object>) option, value)) {
                log.warn("Unknown channel option '{}' for channel '{}'", option, channel);
            }
        } catch (Throwable t) {
            log.warn("Failed to set channel option '{}' with value '{}' for channel '{}'", option, value, channel, t);
        }
    }

    static final class PendingRegistrationPromise extends DefaultChannelPromise {

        private volatile boolean registered;

        PendingRegistrationPromise(Channel channel) {
            super(channel);
        }
        //该方法是该静态类独有的，该方法被调用的时候，registered赋值为true
        void registered() {
            registered = true;
        }


        /**
         * @Author: PP-jessica
         * @Description:该方法简化一下， 全局的执行器不是必须引入的
         */
        @Override
        protected EventExecutor executor() {
            return super.executor();
//            if (registered) {
//                return super.executor();
//            }
//            这里返回一个全局的执行器，但我们没必要引入
//            return GlobalEventExecutor.INSTANCE;
        }
    }


}
