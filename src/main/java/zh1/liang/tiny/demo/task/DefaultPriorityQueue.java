package zh1.liang.tiny.demo.task;

import zh1.liang.tiny.netty.util.internal.ObjectUtil;

import java.util.Arrays;
import java.util.Comparator;

/**
 * @author: zhe.liang
 * @create: 2023-10-12 13:17
 */
public class DefaultPriorityQueue<T> {

    //先初始化一个定时任务的数组，数组的长度为0
    private static final ScheduledFutureTask[] EMPTY_ARRAY = new ScheduledFutureTask[0];

    //任务比较器，比较哪个任务排在队列前面
    private final Comparator<T> comparator;


    //队列中真正存储定时任务的数组
    private T[] queue;


    //队列中存储的定时任务的个数。
    private int size;


    public DefaultPriorityQueue(Comparator<T> comparator, int initialSize) {
        this.comparator = ObjectUtil.checkNotNull(comparator, "comparator");


        queue = (T[]) (initialSize != 0 ? new ScheduledFutureTask[initialSize] : EMPTY_ARRAY);
    }

    public boolean add(T e){
        //如果队列存储的定时任务个数已经大于或者等于队列的长度了，就开始扩容
        if (size >= queue.length) {
            //这里判断一下，如果队列本身的长度是否超过64，如果没超过，长度就增加queue.length + 2
            //如果超过，长度就增加queue.length >>> 1
            queue = Arrays.copyOf(queue, queue.length + ((queue.length < 64) ?
                    (queue.length + 2) :
                    (queue.length >>> 1)));
        }

        //把定时任务真正添加到任务队列中，这里会先取值，然后再size加1
        //也就意味着是任务真正添加队列成功后，队列的容量才加1
        bubbleUp(size++, e);
        return true;
    }


    //添加定时任务到任务队列中的核心方法，k是数组存储元素的个数
    //就是刚才传进来的size，node就是要添加的定时任务
    private void bubbleUp(int k, T needToAdd) {
        while (k > 0) {
            //在数组模拟的二叉树中，(k - 1) >>> 1这个计算式得到的就是将要添加子节点的父节点
            //在数组中的索引位置
            //比如说现在有一个小顶堆二叉树，就像下面这样
            //                        2
            //    5   3
            //  6   9
            //(k - 1) >>> 1计算出来的就是3这个数值在数组索引中的位置
            //在这里，我们就直接称它为父节点位置吧

            //就是先拿到顶，也就是最小的
            int parentIndex = (k - 1) >>> 1;
            T parent = queue[parentIndex];

            //这里得到的值如果大于0，说明要添加到队列的定时任务的执行时间大于父节点
            //的时间，肯定不用调整父节点的位置，所以直接退出循环就行了
            if (comparator.compare(needToAdd, parent) >= 0) {
                break;
            }

            //如果要添加的定时任务的执行时间小于父节点的时间，就意味着父节点要和定时任务交换
            //位置，下面就是把父节点放到了定时任务本该放到的位置
            queue[k] = parent;
            //然后用parentIndex给k赋值，以便寻找父节点的父节点，然后和定时任务的执行时间做对比
            k = parentIndex;
        }
        queue[k] = needToAdd;
    }


    //获取队列头部的定时任务,其实每取走一个定时任务，优先级队列中的定时任务都要重新排序
    //这里就不展开讲解了，手写代码中都有
    public T peek() {
        return (size == 0) ? null : queue[0];
    }
}
