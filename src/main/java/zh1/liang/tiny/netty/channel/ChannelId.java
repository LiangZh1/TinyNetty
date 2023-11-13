package zh1.liang.tiny.netty.channel;

import java.io.Serializable;

/**
 * @author: zhe.liang
 * @create: 2023-08-06 20:17
 */
public interface ChannelId extends Serializable,Comparable<ChannelId> {


    String asShortText();


    String asLongText();
}
