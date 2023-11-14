package demo.mempool;

public class PoolChunkList<T> {

    //该链表中的头节点
    private PoolChunk<T> head;

    //PoolChunkList按照使用率从低到高再组成一个List
    //PoolChunkList也会和其他的PoolChunkList构成链表，所以这里得到下一个PoolChunkList的指针
    private final PoolChunkList<T> nextList;

    //PoolChunkList也会和其他的PoolChunkList构成链表，所以这里得到前一个PoolChunkList的指针
    private PoolChunkList<T> prevList;


    //该list内每一个Chunk的最小内存利用率
    private final int minUsage;

    //该list内每一个Chunk的最大内存利用率
    private final int maxUsage;

    //该链表内每一个Chunk可以分配的最大内存值，其实就是最小利用率乘以16MB，就得到最少分配
    //出去的内存，然后让16MB减去这个值就得到剩下的可以分配的最大内存
    private final int maxCapacity;


    PoolChunkList(PoolChunkList<T> nextList, int minUsage, int maxUsage, int chunkSize) {
        //断言最小利用率要小于最大利用率
        assert minUsage <= maxUsage;
        this.nextList = nextList;
        this.minUsage = minUsage;
        this.maxUsage = maxUsage;
        //在这里计算可分配的每个Chunk块可分配的最大内存
        maxCapacity = calculateMaxCapacity(minUsage, chunkSize);
    }



//     * @Description:该方法可以得到每一个Chunk可以分配的最大内存值
    private static int calculateMaxCapacity(int minUsage, int chunkSize) {
        //最小内存使用率不能低于1
        minUsage = minUsage0(minUsage);
        if (minUsage == 100) {
            //如果等于100就不能分配任何内存了
            return 0;
        }
        //这个就是计算百分比的数学运算了 //就是100-去最小利用值然后除以100，让chunkSize乘以这个百分数就行了
        return  (int) (chunkSize / (100L - minUsage) / 100L);
    }

    private static int minUsage0(int value) {
        return Math.max(1, value);
    }



    /**
     * @Description:分配内存的方法
     * 多了一个参数reqCapacity，就是要分配的内存大小
     */
    boolean allocate(int reqCapacity) {
        //如果要申请的内存超过了一个Chunk可分配的最大内存值
        if (normCapacity > maxCapacity) {
            //分配不了就直接退出
            return false;
        }
        //便遍历该链表中的Chunk
        for (PoolChunk<T> cur = head; cur != null; cur = cur.next) {
            //从Chunk中分配经过规整的内存，具体的方法都在PoolChunk中，这里我们知识粗讲逻辑
            //核心会在PoolChunk中讲到
            if (cur.allocate(reqCapacity)) {
                //这里就会判断当前分配完内存的Chunk的内存利用率是否超过了它的最大内存利用率
                if (cur.usage() >= maxUsage) {
                    //超过了就从当前链表中移除该Chunk
                    remove(cur);
                    //把该Chunk添加到链表的下一个节点中
                    //注意，这里的下一个节点是PoolChunkList组成的链表的下一个节点
                    nextList.add(cur);
                }
                return true;
            }
        }
        return false;
    }


    /**
     * @Author: PP-jessica
     * @Description:该方法就是用来把一个Chunk内存块添加到PoolChunkList中的方法
     */
    void add(PoolChunk<T> chunk) {
        //这里先判断一下内存的利用率，如果内存利用率超过了该PoolChunkList的最大内存利用率
        if (chunk.usage() >= maxUsage) {
            //就寻找链表中的下一个节点，然后把该Chunk尝试着放到下一个节点中
            //其实就是递归调用该方法
            nextList.add(chunk);
            return;
        }
        //这里意味着内存利用率符合要求，直接放到该链表中即可
        add0(chunk);
    }


    /**
     * @Author: PP-jessica
     * @Description:把Chunk加入到PoolChunkList中的方法
     * 这里是以是否有头节点为区分，逻辑很简单。
     */
    void add0(PoolChunk<T> chunk) {
        //Poolchunk对象被加入到这个链表中了，就应该把PoolChunk所属的链表赋值
        chunk.parent = this;
        //如果该类的头节点为null，说明链表还没开始构建呢
        if (head == null) {
            //直接把当前加入的Chunk块赋值为头节点
            head = chunk;
            //头节点的前后节点都为null
            chunk.prev = null;
            chunk.next = null;
        } else {
            //走到这里说明已经有头节点了，意味着链表已经构建了
            chunk.prev = null;
            //把当前加入的Chunk块放到头节点前面，成为新的头节点
            //其实就是头插法
            chunk.next = head;
            //头节点的的前一个节点为当前Chunk对象
            head.prev = chunk;
            //新的头节点就是当前加入的Chunk了
            head = chunk;
        }
    }

    /**
     * @Author: PP-jessica
     * @Description:移动Chunk内存块到PoolChunkList链表的上一个节点中的方法
     */
    private boolean move(PoolChunk<T> chunk) {
        //断言该Chunk的内存利用率小于该List对象的利用率最大值
        assert chunk.usage() < maxUsage;
        //如果该Chunk的内存利用率小于该List对象的利用率最小值
        if (chunk.usage() < minUsage) {
            //递归调用move0方法，继续向前驱节点移动
            return move0(chunk);
        }
        //把Chunk内存块添加到该List对象中
        add0(chunk);
        return true;
    }


    /**
     * @Author: PP-jessica
     * @Description:真正把Chunk内存块移动到上一个PoolChunkList节点中的方法
     */
    private boolean move0(PoolChunk<T> chunk) {
        //判断是否有前驱节点
        if (prevList == null) {
            //这两句注释要结合下面讲的内容来理解，大家先混个眼熟
            //等下面讲解了名称为q000的PoolChunkList对象后，就知道是什么意思了
            //如果没有前驱节点，大家可以想想，哪个PoolChunkList对象没有前驱节点？是q000对象
            assert chunk.usage() == 0;
            //这里直接返回说明如果这个Chunk内存块本身就在q000对象中了，没有前驱节点可以移动，它就没必要移动，等待被释放即可
            return false;
        }
        //走到这里说明有前驱节点，那就移动带前驱节点中
        return prevList.move(chunk);
    }


    /**
     * @Author: PP-jessica
     * @Description:从链表中删除该Chunk节点，操作数据结构的方法，大家自己看看逻辑就行，我就不详细注释了
     * 这里只有一个是否头节点的区分，逻辑很简单
     */
    private void remove(PoolChunk<T> cur) {
        //判断当前节点是不是头节点
        if (cur == head) {
            //如果是，就先把当前节点的下一个节点置为头节点
            //因为当前节点要从链表中删除了
            head = cur.next;
            if (head != null) {
                head.prev = null;
            }
        } else {
            //走到这里说明当前节点不是头节点，正常删除即可
            //得到当前节点的下一个节点
            PoolChunk<T> next = cur.next;
            //把当前节点前一个节点的下一个节点指向刚才得到的下一个节点
            cur.prev.next = next;
            if (next != null) {
                next.prev = cur.prev;
            }
        }
    }
}
