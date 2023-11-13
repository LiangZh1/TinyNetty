package zh1.liang.tiny.netty.channel;

import zh1.liang.tiny.netty.util.IntSupplier;

/**
 * @author: zhe.liang
 * @create: 2023-08-04 13:07
 **/
public interface SelectStrategy {

    int SELECT = -1;

    int CONTINUE = -2;

    int BUSY_WAIT = -3;

    int calculateStrategy(IntSupplier selectSupplier, boolean hasTasks) throws Exception;

}
