package zh1.liang.tiny.netty.util.concurrent;


/**
 * @author: zhe.liang
 * @create: 2023-08-03 11:56
 **/
public interface Promise<V> extends Future<V> {

    Promise<V> setSuccess(V result);

    boolean trySuccess(V result);

    Promise<V> setFailure(Throwable cause);

    boolean tryFailure(Throwable cause);

    boolean setUncancellable();

}
