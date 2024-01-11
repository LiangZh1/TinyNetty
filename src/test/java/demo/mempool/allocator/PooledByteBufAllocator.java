package demo.mempool.allocator;

import demo.mempool.PoolThreadCache;
import demo.mempool.bytebuf.ByteBuf;
import demo.mempool.PoolArena;
import demo.threadlocal.FastThreadLocal;
import zh1.liang.tiny.netty.util.NettyRuntime;
import zh1.liang.tiny.netty.util.internal.SystemPropertyUtil;

import java.nio.ByteBuffer;

/**
 * @author: zhe.liang
 * @create: 2023-11-15 13:46
 **/
public class PooledByteBufAllocator extends AbstractByteBufAllocator{


    //直接内存类型，也就是direc类型的ARENA的数量，其实就是directArenas数组的长度
    //要创建的PoolArena对象的数量，通常情况下默认初始值是CPU核数乘以2
    private static final int DEFAULT_NUM_DIRECT_ARENA;

    //是否为所有线程都创建自己的私有Map，默认为true，在静态代码块中被赋值
    private static final boolean DEFAULT_USE_CACHE_FOR_ALL_THREADS;

    public static final PooledByteBufAllocator DEFAULT =
            new PooledByteBufAllocator();


    static {
        final int defaultMinNumArena = NettyRuntime.availableProcessors() * 2;
        DEFAULT_NUM_DIRECT_ARENA = Math.max(0,
                SystemPropertyUtil.getInt(
                        "io.netty.allocator.numDirectArenas", defaultMinNumArena));
        DEFAULT_USE_CACHE_FOR_ALL_THREADS = SystemPropertyUtil.getBoolean(
                "io.netty.allocator.useCacheForAllThreads", true);
    }

    //这里有两个PoolArena的数组，一个数组的泛型为字节数组，一个数组的泛型为ByteBuffer
    //显然，为Byte数组就意味着PoolChunk包装的堆内存，而使用的是ByteBuffer的，就意味着
    //PoolChunk管理的是操作系统帮忙申请的堆外内存
    private final PoolArena<byte[]>[] heapArenas;

    private final PoolArena<ByteBuffer>[] directArenas;


    private final PoolThreadLocalCache threadCache;

    public PooledByteBufAllocator(boolean preferDirect,int nDirectArena,boolean useCacheForAllThreads){
        super(preferDirect);

        //在这里创建了一个FastTreadLocal，其实就可以把这个属性当成得到每个线程私有Map的入口
        threadCache = new PoolThreadLocalCache(useCacheForAllThreads);

        directArenas = newArenaArray(nDirectArena);
        for (int i = 0; i < directArenas.length; i ++) {
            PoolArena arena = new PoolArena();
            directArenas[i] = arena;
        }
    }

    @Override
    protected ByteBuf newHeapBuffer(int initialCapacity, int maxCapacity) {
        //我们的文章并不涉及堆内存的分配，所以这个方法就做一个空实现好了
        return null;
    }

    @Override
    protected ByteBuf newDirectBuffer(int initialCapacity, int maxCapacity) {
        PoolThreadCache cache = threadCache.get();

        PoolArena<ByteBuffer> directArena = null;
        ByteBuf buf = directArena.allocate(initialCapacity, maxCapacity);

        return buf;
    }

    final class PoolThreadLocalCache extends FastThreadLocal<PoolThreadCache>{

        private final boolean useCacheForAllThreads;

        public PoolThreadLocalCache(boolean useCacheForAllThreads) {
            this.useCacheForAllThreads = useCacheForAllThreads;
        }





    }
}
