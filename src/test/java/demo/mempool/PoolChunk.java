package demo.mempool;

public class PoolChunk<T> {

    final T memory;

    //当前Chunk还可以分配的字节数量
    private int freeBytes;

    //Chunk块的大小，也就是事先申请的一大块内存的大小，为16M
    private static final int CHUNK_SIZE = 1024 * 1024 * 16;

    //该Chunk属于哪个PoolChunkList
    PoolChunkList<T> parent;

    //该Chunk内存块在PoolChunkList中的前驱节点
    PoolChunk<T> prev;

    //该Chunk内存块在PoolChunkList中的下一个节点
    PoolChunk<T> next;


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

        //构造方法
    PoolChunk(T memory,int chunkSize){
        this.memory = memory;
        //该chunk内存块还可以分配的内存值，初始值为16MB
        freeBytes = chunkSize;
    }

    //该方法就是Poolchunk分配内存的方法，在该方法中，会进行内存的分配
    //分配成功了就返回true，失败则返回false
    boolean allocate(long capacity) {
        //暂且不实现
        return true;
    }

}
