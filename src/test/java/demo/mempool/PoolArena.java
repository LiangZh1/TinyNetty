package demo.mempool;

import demo.mempool.bytebuf.ByteBuf;
import demo.mempool.bytebuf.PooledByteBuf;
import demo.mempool.bytebuf.PooledDirectByteBuf;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author: zhe.liang
 * @create: 2023-11-15 13:15
 **/
public class PoolArena<T> {

    //定义内存规格的枚举类
    enum SizeClass {
        //Tiny的大小为16B到496B，按照16B的大小递增
        Tiny,
        //Small的大小为512B到4KB，按照乘以2的大小递增
        Small,
        //Normal的大小为8KB到16MB，按照乘以2的大小递增，大于16MB的为Huge，不会被缓存，这里的意思是不会被申请为Chunk
        //实际上，在Netty的内存分配中，是先从内存中申请了一大块大内，然后再从这一大块内存中逐渐分配给各个线程使用的
        //这一大块内存就为Chunk，值为16MB
        Normal
    }

    //Chunk块的大小，也就是事先申请的一大块内存的大小，为16M
    private static final int CHUNK_SIZE = 1024 * 1024 * 16;

    //这个原子类记录的就是该PoolArena正被多少单线程执行器引用
    final AtomicInteger numThreadCaches = new AtomicInteger();


    //内存使用率为0到25%的Chunk集合
    private final PoolChunkList<T> qInit;

    //内存使用率为1%到50%%的Chunk集合
    private final PoolChunkList<T> q000;

    //内存使用率为25%到75%的Chunk集合
    private final PoolChunkList<T> q025;

    //内存使用率为50%到100%的Chunk集合
    private final PoolChunkList<T> q050;

    //内存使用率为75%到100%的Chunk集合
    private final PoolChunkList<T> q075;

    //内存使用率为100%的Chunk集合
    private final PoolChunkList<T> q100;

    //构造方法，在构造方法中把定义的链表初始化，并且连接起来了
    protected PoolArena(){
        //开始连接这些链表对象，可以发现，现在qInit对象成了头节点
        q100 = new PoolChunkList<T>( null, 100, Integer.MAX_VALUE, CHUNK_SIZE);
        q075 = new PoolChunkList<T>( q100, 75, 100, CHUNK_SIZE);
        q050 = new PoolChunkList<T>( q075, 50, 100, CHUNK_SIZE);
        q025 = new PoolChunkList<T>( q050, 25, 75, CHUNK_SIZE);
        q000 = new PoolChunkList<T>( q025, 1, 50, CHUNK_SIZE);
        qInit = new PoolChunkList<T>(, q000, 0, 25, CHUNK_SIZE);
        //通过上面的连接方式，这5个PoolChunkList对象的连接顺序就成了下面这样
        //qInit ——> q000 ——> q025 ——> q050 ——> q075 ——> q100
        //但是PoolChunkList对象构成的新链表是一个双向链表，因此，还要把前节点的指针补上
        q100.prevList(q075);
        q075.prevList(q050);
        q050.prevList(q025);
        q025.prevList(q000);
        //这里可以看到，q000没有前置节点，这意味着当q000中的PoolChunk的内存使用率过低
        //整个PoolChunk就会被释放了，不会再存在于链表中
        q000.prevList(null);
        //这里可以看到qInit的前置节点是自己，这意味着当qInit中的PoolChunk的内存使用率低于临界值
        //并不会被释放。其实也不会低于临界值了，因为qInit的最低内存使用率是0，PoolChunk的最低内存
        //使用率也为0，就是相等的情况，既然相等，肯定就不会释放了，这个在下一章会在代码层面实现
        //只有进入了q000集合中的Chunk才会被释放
        qInit.prevList(qInit);
    }

    /**
     * @Description:分配内存的方法，现在还是非常简化的方法，返回值并不正确
     */
    public ByteBuf allocate(int reqCapacity, int maxCapacity) {
        PooledByteBuf<T> buf = newByteBuf(maxCapacity);
        allocate(buf, reqCapacity);
        return buf;
    }


    //该方法也是新添加的，就是用来得到一个PooledDirectByteBuf对象
    //并且是从PooledDirectByteBuf的对象池中得到的

    protected PooledByteBuf<T> newByteBuf(int maxCapacity) {
        return PooledDirectByteBuf.newInstance(maxCapacity);
    }


    boolean allocate(PooledByteBuf<T> buf, int reqCapacity) {
        int normCapacity = normalizeCapacity(reqCapacity);
        //锁Arena对象
        synchronized (this){
            allocate(buf, reqCapacity, normCapacity);
        }
    }



    /**
     * @Author: PP-jessica
     * @Description:分配内存的方法，现在还是非常简化的方法，返回值并不正确
     */
    boolean allocate(PooledByteBuf<T> buf, int reqCapacity,int normCapacity) {
        //可以看到，PoolChunList的allocate方法也重构了
        if (q050.allocate(buf, reqCapacity) || q025.allocate(buf, reqCapacity) || q000.allocate(buf, reqCapacity) || qInit.allocate(buf, reqCapacity) ||
                q075.allocate(buf, reqCapacity)) {
            //分配成功就返回true
            return true;
        }
        //程序第一次申请内存时，肯定还没有创建ChunkPool，这时候要创建一个chunk内存块
        //如果上面都没有分配成功，那就意味着所有的chunk中剩余可分配内存都不够了，这时候就要创建
        //新的PoolChunk，让操作系统帮助申请16MB内存供程序内部使用，这里的CHUNK_SIZE就是该类的成员变量
        //只不过没有列出来，是16MB
        PoolChunk<T> c = new PoolChunk(new DirectByteBuffer(CHUNK_SIZE),CHUNK_SIZE);
        //分配内存
        boolean success = c.allocate(buf,reqCapacity,normCapacity);
        //接着把这个chunk内存块加入到init链表中
        qInit.add(c);
        return success;
    }

    //新增加的方法，和numThreadCaches成员变量有关，就是返回该变量的值
    public int numThreadCaches() {
        return numThreadCaches.get();
    }

    int normalizeCapacity(int reqCapacity) {
        //判断要申请的内存必须是大于0的
        checkPositiveOrZero(reqCapacity, "reqCapacity");
        //这个是针对大于16MB的内存的的规整，我们暂时还不需要关注这里
        //其实这里就是如果用户申请的是大于16MB，就会直接申请这个内存
        if (reqCapacity >= chunkSize) {
            return directMemoryCacheAlignment == 0 ? reqCapacity : alignCapacity(reqCapacity);
        }
        //判断申请的内存是否为Tiny大小的，不是Tiny就意味着是另外两种，而另外两种
        //分配内存都是按照乘以2递增的，所以使用相同的算法
        if (!isTiny(reqCapacity)) {
            //这里进入分支，说明要申请的内存不是tiny类型的，得到要申请的内存
            int normalizedCapacity = reqCapacity;
            normalizedCapacity --;
            normalizedCapacity |= normalizedCapacity >>>  1;
            normalizedCapacity |= normalizedCapacity >>>  2;
            normalizedCapacity |= normalizedCapacity >>>  4;
            normalizedCapacity |= normalizedCapacity >>>  8;
            normalizedCapacity |= normalizedCapacity >>> 16;
            normalizedCapacity ++;
            //判断normalizedCapacity是否溢出了，溢出就右移
            if (normalizedCapacity < 0) {
                normalizedCapacity >>>= 1;
            }
            return normalizedCapacity;
        }
        //这里的逻辑就简单一些了，走到这里就意味着要申请的内存是tiny大小的
        //tiny是按16B递增的，所以判断能够被16整除，可以整除就能直接返回
        if ((reqCapacity & 15) == 0) {
            return reqCapacity;
        }
        //这里也涉及到一个位运算，首先我们要弄清楚15取反的值
        //～15的二进制  1111 1111 1111 1111 1111 1111 1111 0000
        //如果reqCapacity小于16，那这个数的二进制的低五位肯定不是1，所以做与运算结果肯定为0
        //所以，这里的意思就是如果分配的内存小于16B，那就直接返回16B
        //而如果申请的内存是大于十六的，做了与运算后得到的就是个16的倍数，再加上16仍然是16的倍数，直接返回即可
        return (reqCapacity & ~15) + 16;
    }


    /**
     * @Author: PP-jessica
     * @Description:判断要申请的内存是否小于512字节
     */
    static boolean isTiny(int normCapacity) {
        //0xFFFFFE00是个十六进制的数，换算成二进制为 1111 1111 1111 1111 1111 1110 0000 0000
        //496的二进制为                          0000 0000 0000 0000 0000 0001 1111 0000
        //512的二进制为                          0000 0000 0000 0000 0000 0010 0000 0000
        //这两个数做&运算的值
        return (normCapacity & 0xFFFFFE00) == 0;
    }

}
