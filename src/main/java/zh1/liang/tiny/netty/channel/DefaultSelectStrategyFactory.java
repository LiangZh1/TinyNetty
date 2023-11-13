package zh1.liang.tiny.netty.channel;

/**
 * @author: zhe.liang
 * @create: 2023-08-04 20:24
 **/
public class DefaultSelectStrategyFactory implements SelectStrategyFactory{

    public static final SelectStrategyFactory INSTANCE = new DefaultSelectStrategyFactory();

    private DefaultSelectStrategyFactory() { }

    @Override
    public SelectStrategy newSelectStrategy() {
        return DefaultSelectStrategy.INSTANCE;
    }
}
