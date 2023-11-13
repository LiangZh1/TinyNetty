package zh1.liang.tiny.netty.util;
/**
 * @author: zhe.liang
 * @create: 2023-08-09 16:29
 */
public interface Constant<T extends Constant<T>> extends Comparable<T> {
    int id();

    String name();
}
