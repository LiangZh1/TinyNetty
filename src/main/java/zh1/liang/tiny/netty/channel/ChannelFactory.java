package zh1.liang.tiny.netty.channel;



public interface ChannelFactory<T extends Channel> {
    T newChannel();
}
