package zh1.liang.tiny.netty.util.internal;
/**
 * @author: zhe.liang
 * @create: 2023-10-18 16:29
 * 该接口的具体作用实际上只是记录了我们创建的定时任务在任务队列中的下标。
 *
 * 具体实现可以看默认的实现类。这里再多说一句，netty的作者之所以搞这个接口，是为了减少寻找定时任务时，遍历队列的消耗。
 */
public interface PriorityQueueNode {

    //如果一个任务不再队列中，把下标值设为-1
    int INDEX_NOT_IN_QUEUE = -1;

    //获取在传入队列中的下标地址
    int priorityQueueIndex(DefaultPriorityQueue<?> queue);

    //设置当前的任务在队列中的下标位置
    void priorityQueueIndex(DefaultPriorityQueue<?> queue, int i);
}
