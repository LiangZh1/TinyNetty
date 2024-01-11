package demo.mempool;

import demo.mempool.bytebuf.PooledByteBuf;

public class PoolChunk<T> {

    public final T memory;

    //当前Chunk还可以分配的字节数量
    private int freeBytes;

    //Chunk块的大小，也就是事先申请的一大块内存的大小，为16M
    private static final int CHUNK_SIZE = 1024 * 1024 * 16;

    //该Chunk属于哪个PoolChunkList
    public PoolChunkList<T> parent;

    //该Chunk内存块在PoolChunkList中的前驱节点
    public PoolChunk<T> prev;

    //该Chunk内存块在PoolChunkList中的下一个节点
    public PoolChunk<T> next;


    //构造方法
    PoolChunk(T memory,int chunkSize){
        this.memory = memory;
        //该chunk内存块还可以分配的内存值，初始值为16MB
        freeBytes = chunkSize;
    }

    public int usage() {
        final int freeBytes = this.freeBytes;
        return usage(freeBytes);
    }

    private int usage(int freeBytes) {
        if (freeBytes == 0) {
            //走到这里说明利用率已经100%了
            return 100;
        }
        int freePercentage = (int) (freeBytes / CHUNK_SIZE);

        if (freePercentage == 0) {
            return 99;
        }
        //减去剩下的百分比，就是使用的百分比
        return 100 - freePercentage;
    }

    //该方法就是Poolchunk分配内存的方法，在该方法中，会进行内存的分配
    //分配成功了就返回true，失败则返回false
    boolean allocate(PooledByteBuf<T> buf,long capacity) {
        //暂且不实现
        System.out.println("分配一块byte数组");
        initBuf(buf);
        return true;
    }

    /**
     * @Author: PP-jessica
     * @Description:初始化buf的方法，其实就是把分配好的堆外内存交给ByteBuf对象来管理
     */
    void initBuf(PooledByteBuf<T> buf, T memory, int reqCapacity) {
        //调用PooledByteBuf对象的init方法就行了，这里就和上一章的内容串联起来了
        //在上一章讲解PooledByteBuf类时，我在里面定义了一个init方法，跟大家说这个方法
        //会在PoolChunk类中被调用。就是在这里调用，内存分配成功后，把内存交给ByteBuf对象来包装
        buf.init(this,分配好的内存,reqCapacity);
    }

}
