package zh1.liang.tiny.netty.channel;

import zh1.liang.tiny.netty.util.IntSupplier;

/**
 * @author: zhe.liang
 * @create: 2023-08-04 20:22
 **/
public class DefaultSelectStrategy implements SelectStrategy{

    static final SelectStrategy INSTANCE = new DefaultSelectStrategy();

    private DefaultSelectStrategy() { }

    @Override
    public int calculateStrategy(IntSupplier selectSupplier, boolean hasTasks) throws Exception {
        return hasTasks ? selectSupplier.get() : SelectStrategy.SELECT;
    }
}
