package zh1.liang.tiny.netty.bootstrap;

import lombok.extern.slf4j.Slf4j;
import zh1.liang.tiny.netty.channel.*;
import zh1.liang.tiny.netty.channel.ChannelOption;
import zh1.liang.tiny.netty.util.AttributeKey;
import zh1.liang.tiny.netty.util.internal.ObjectUtil;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;

/**
 * @author: zhe.liang
 * @create: 2023-09-16 17:33
 */
@Slf4j
public class Bootstrap extends AbstractBootstrap<Bootstrap, Channel>{

    private final BootstrapConfig config = new BootstrapConfig(this);

    /**
     * 远程地址
     */
    private volatile SocketAddress remoteAddress;

    public Bootstrap(){

    }

    public Bootstrap remoteAddress(SocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
        return this;
    }

    private Bootstrap(Bootstrap bootstrap) {
        super(bootstrap);
        remoteAddress = bootstrap.remoteAddress;
    }

    public ChannelFuture connect(String inetHost, int inetPort) {
        return connect(new InetSocketAddress(inetHost, inetPort));
    }

    public ChannelFuture connect(SocketAddress remoteAddress) {
        ObjectUtil.checkNotNull(remoteAddress, "remoteAddress");
        return doResolveAndConnect(remoteAddress, null);
    }

    private ChannelFuture doResolveAndConnect(final SocketAddress remoteAddress, final SocketAddress localAddress){
        final ChannelFuture regFuture = initAndRegister();
        //获取要注册的channel
        Channel channel = regFuture.channel();

        //注册完成了，可以执行后续的操作
        if (regFuture.isDone()) {
            if (!regFuture.isSuccess()) {
                return regFuture;
            }
            //注册成功，开始执行绑定端口号
            ChannelPromise connectPromise = new DefaultChannelPromise(channel);
            return doResolveAndConnect0(channel, remoteAddress, localAddress, connectPromise);

        }else {
            final PendingRegistrationPromise connectPromise = new PendingRegistrationPromise(channel);

            regFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    Throwable cause = future.cause();
                    if(cause != null){
                        connectPromise.setFailure(cause);
                    }else {
                        connectPromise.registered();
                        doResolveAndConnect0(channel,remoteAddress,localAddress,connectPromise);
                    }
                }
            });

            return connectPromise;
        }



    }

    private ChannelFuture doResolveAndConnect0(Channel channel, SocketAddress remoteAddress, SocketAddress localAddress, final ChannelPromise connectPromise){
        try{
            //todo 解析远程地址
            doConnect(remoteAddress,localAddress,connectPromise);
        }catch (Throwable throwable){
            connectPromise.tryFailure(throwable);
        }
        return connectPromise;
    }

    private void doConnect(SocketAddress remoteAddress,SocketAddress localAddress,final ChannelPromise connectPromise){
        Channel channel = connectPromise.channel();

        channel.eventLoop().execute(new Runnable() {
            @Override
            public void run() {
                if (localAddress == null) {
                    channel.connect(remoteAddress,null,connectPromise);
                }
                connectPromise.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);

            }
        });
    }

    @Override
    protected void init(Channel channel) throws Exception {
        final Map<ChannelOption<?>, Object> options = options0();

        synchronized (options) {
            setChannelOptions(channel, options);
        }

        final Map<AttributeKey<?>, Object> attrs = attrs0();

        synchronized (attrs) {
            for (Map.Entry<AttributeKey<?>, Object> e: attrs.entrySet()) {
                channel.attr((AttributeKey<Object>) e.getKey()).set(e.getValue());
            }
        }
    }

    @Override
    public Bootstrap validate() {
        super.validate();
//        if (config.handler() == null) {
//            throw new IllegalStateException("handler not set");
//        }
        return this;
    }

    @Override
    public final BootstrapConfig config() {
        return config;
    }

    final SocketAddress remoteAddress() {
        return remoteAddress;
    }
}
