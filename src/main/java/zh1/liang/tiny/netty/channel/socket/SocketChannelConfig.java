package zh1.liang.tiny.netty.channel.socket;

import zh1.liang.tiny.netty.channel.ChannelConfig;

/**
 * @author: zhe.liang
 * @create: 2023-08-14 13:22
 */
public interface SocketChannelConfig extends ChannelConfig {
    boolean isTcpNoDelay();


    SocketChannelConfig setTcpNoDelay(boolean tcpNoDelay);


    int getSoLinger();


    SocketChannelConfig setSoLinger(int soLinger);


    int getSendBufferSize();


    SocketChannelConfig setSendBufferSize(int sendBufferSize);


    int getReceiveBufferSize();


    SocketChannelConfig setReceiveBufferSize(int receiveBufferSize);


    boolean isKeepAlive();


    SocketChannelConfig setKeepAlive(boolean keepAlive);


    int getTrafficClass();


    SocketChannelConfig setTrafficClass(int trafficClass);

    boolean isReuseAddress();

    SocketChannelConfig setReuseAddress(boolean reuseAddress);

    SocketChannelConfig setPerformancePreferences(int connectionTime, int latency, int bandwidth);


    boolean isAllowHalfClosure();

    SocketChannelConfig setAllowHalfClosure(boolean allowHalfClosure);

    @Override
    SocketChannelConfig setConnectTimeoutMillis(int connectTimeoutMillis);

    @Override
    SocketChannelConfig setWriteSpinCount(int writeSpinCount);

    @Override
    SocketChannelConfig setAutoRead(boolean autoRead);

    @Override
    SocketChannelConfig setAutoClose(boolean autoClose);
}
