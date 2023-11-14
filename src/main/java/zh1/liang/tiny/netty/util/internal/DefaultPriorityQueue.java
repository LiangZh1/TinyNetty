package zh1.liang.tiny.netty.util.internal;

import java.util.*;

/**
 * @author: zhe.liang
 * @create: 2023-10-18 16:32
 */
public class DefaultPriorityQueue<T extends PriorityQueueNode> extends AbstractQueue<T> implements PriorityQueue<T> {

    // 该PriorityQueueNode类型实际上就ScheduledFutureTask，因为ScheduledFutureTask
    //  就实现了PriorityQueueNode接口
    private static final PriorityQueueNode[] EMPTY_ARRAY = new PriorityQueueNode[0];

    //任务比较器，比较哪个任务排在队列前面
    private final Comparator<T> comparator;

    //队列中真正存储定时任务的数组
    private T[] queue;

    //队列中存储的定时任务的个数。
    private int size;

    public DefaultPriorityQueue(Comparator<T> comparator, int initialSize) {
        this.comparator = ObjectUtil.checkNotNull(comparator, "comparator");
        queue = (T[]) (initialSize != 0 ? new PriorityQueueNode[initialSize] : EMPTY_ARRAY);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean contains(Object o) {
        //如果不是PriorityQueueNode类型直接返回false
        if (!(o instanceof PriorityQueueNode)) {
            return false;
        }
        //转换成PriorityQueueNode类型
        PriorityQueueNode node = (PriorityQueueNode) o;
        //node.priorityQueueIndex(this)方法取出该对象在队列数组中的下标
        return contains(node, node.priorityQueueIndex(this));
    }

    /**
     * @Author: PP-jessica
     * @Description:真正判断队列是否包含该定时任务
     */
    private boolean contains(PriorityQueueNode node, int i) {
        return i >= 0 && i < size && node.equals(queue[i]);
    }

    /**
     * @Author: PP-jessica
     * @Description:队列是否包含该对象
     */
    @Override
    public boolean containsTyped(T node) {
        return contains(node, node.priorityQueueIndex(this));
    }

    @Override
    public void clear() {
        for (int i = 0; i < size; ++i) {
            //取出每一个定时任务
            T node = queue[i];
            if (node != null) {
                //不为null就把元素内部的index设置为INDEX_NOT_IN_QUEU，这就代表着该定时任务不再这个队列中了
                node.priorityQueueIndex(this, PriorityQueueNode.INDEX_NOT_IN_QUEUE);
                //把数组的每一个元素置为null
                queue[i] = null;
            }
        }
        size = 0;
    }

    /**
     * @Description:只把size置为0，但是数组中的定时任务还未被删除，相当于逻辑删除
     */
    @Override
    public void clearIgnoringIndexes() {
        size = 0;
    }


    @Override
    public boolean offer(T e) {
        if (e.priorityQueueIndex(this) != PriorityQueueNode.INDEX_NOT_IN_QUEUE){
            throw new IllegalArgumentException("e.priorityQueueIndex(): " + e.priorityQueueIndex(this) +
                    " (expected: " + PriorityQueueNode.INDEX_NOT_IN_QUEUE + ") + e: " + e);
        }

        //这里不加锁？
        if (size >= queue.length) {
            queue = Arrays.copyOf(queue, queue.length + ((queue.length < 64) ?
                    (queue.length + 2) :
                    (queue.length >>> 1)));
        }

        //把定时任务添加到任务队列中，这里会先取值，然后再size加1
        bubbleUp(size++, e);
        return true;
    }


    @Override
    public T poll() {
        if(size == 0){
            return null;
        }
        T element = queue[0];
        //改变该定时任务的下标，意味着该定时任务要从任务队列中取出了
        element.priorityQueueIndex(this, PriorityQueueNode.INDEX_NOT_IN_QUEUE);

        T last = queue[--size];
        queue[size] = null;

        if (size != 0) {
            //如果size不为0，就移动元素
            //把后面的元素插入到合适位置，这里用到了小顶堆的数据结构。其实这个任务优先队列，用到的就是这个数据结构。
            bubbleDown(0, last);
        }
        return element;
    }

    @Override
    public T peek() {
        return (size == 0) ? null : queue[0];
    }

    @Override
    public boolean remove(Object o) {
        final T node;
        try {
            node = (T) o;
        } catch (ClassCastException e) {
            return false;
        }
        return removeTyped(node);
    }

    @Override
    public boolean removeTyped(T node) {
        int i = node.priorityQueueIndex(this);
        if (!contains(node, i)) {
            return false;
        }
        node.priorityQueueIndex(this, PriorityQueueNode.INDEX_NOT_IN_QUEUE);
        if (--size == 0 || size == i) {
            queue[i] = null;
            return true;
        }
        T moved = queue[i] = queue[size];
        queue[size] = null;
        if (comparator.compare(node, moved) < 0) {
            bubbleDown(i, moved);
        } else {
            bubbleUp(i, moved);
        }
        return true;
    }

    @Override
    public void priorityChanged(T node) {
        int i = node.priorityQueueIndex(this);
        if (!contains(node, i)) {
            return;
        }
        if (i == 0) {
            bubbleDown(i, node);
        } else {
            int iParent = (i - 1) >>> 1;
            T parent = queue[iParent];
            if (comparator.compare(node, parent) < 0) {
                bubbleUp(i, node);
            } else {
                bubbleDown(i, node);
            }
        }
    }

    @Override
    public Object[] toArray() {
        return Arrays.copyOf(queue, size);
    }

    @Override
    public <X> X[] toArray(X[] a) {
        if (a.length < size) {
            return (X[]) Arrays.copyOf(queue, size, a.getClass());
        }
        System.arraycopy(queue, 0, a, 0, size);
        if (a.length > size) {
            a[size] = null;
        }
        return a;
    }

    @Override
    public Iterator<T> iterator() {
        return new PriorityQueueIterator();
    }






    /**
     * @Description:移动，也就是重新排列任务队列中的定时任务的优先级，这里面用到的就是二分法
     *
     */
    private void bubbleDown(int k, T node) {
        //看到这就会想到二分法，右移一位就是除以2
        final int half = size >>> 1;
        while (k < half) {
            //在这里会循环取一半，二分
            int iChild = (k << 1) + 1;
            T child = queue[iChild];
            int rightChild = iChild + 1;
            if (rightChild < size && comparator.compare(child, queue[rightChild]) > 0) {
                child = queue[iChild = rightChild];
            }
            if (comparator.compare(node, child) <= 0) {
                break;
            }
            queue[k] = child;
            //在这里就重新设置了定时任务队列中数据的下标
            child.priorityQueueIndex(this, k);
            k = iChild;
        }
        queue[k] = node;
        //在这里就重新设置了定时任务队列中数据的下标
        node.priorityQueueIndex(this, k);
    }


    /**
     * @Author: PP-jessica
     * @Description:添加定时任务到任务队列中的方法，用到了二分法
     */
    private void bubbleUp(int k, T node) {
        while (k > 0) {
            int iParent = (k - 1) >>> 1;
            T parent = queue[iParent];
            if (comparator.compare(node, parent) >= 0) {
                break;
            }
            queue[k] = parent;
            //在这里就设置了定时任务队列中数据的下标
            parent.priorityQueueIndex(this, k);
            k = iParent;
        }
        queue[k] = node;
        //在这里就设置了定时任务队列中数据的下标
        node.priorityQueueIndex(this, k);
    }







    private final class PriorityQueueIterator implements Iterator<T> {
        private int index;

        @Override
        public boolean hasNext() {
            return index < size;
        }

        @Override
        public T next() {
            if (index >= size) {
                throw new NoSuchElementException();
            }

            return queue[index++];
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove");
        }
    }
}
