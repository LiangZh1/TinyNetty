package zh1.liang.tiny.netty.util.concurrent.promise;

import java.util.EventListener;

/**
 * @author: zhe.liang
 * @create: 2023-08-05 22:43
 */
public interface GenericFutureListener<F extends Future<?>> extends EventListener {
    void operationComplete(F future) throws Exception;
}
