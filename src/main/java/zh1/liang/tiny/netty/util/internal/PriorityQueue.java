package zh1.liang.tiny.netty.util.internal;

import java.util.Queue;

/**
 * @author: zhe.liang
 * @create: 2023-10-18 16:27
 */
public interface PriorityQueue<T> extends Queue<T> {

    boolean removeTyped(T node);

    boolean containsTyped(T node);

    void priorityChanged(T node);

    void clearIgnoringIndexes();
}
