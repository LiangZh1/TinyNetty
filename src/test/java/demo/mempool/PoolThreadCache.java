package demo.mempool;

import java.nio.ByteBuffer;

/**
 * @author: zhe.liang
 * @create: 2023-11-16 16:28
 **/
public class PoolThreadCache {

    //持有的使用次数最少的heapArena
    PoolArena<byte[]> heapArena;

    //持有的使用次数最少的directArena
    PoolArena<ByteBuffer> directArena;

    public PoolThreadCache(PoolArena<byte[]> heapArena, PoolArena<ByteBuffer> directArena) {
        if (heapArena != null) {
            this.heapArena = heapArena;
            //被持有的引用计数加1
            heapArena.numThreadCaches.getAndIncrement();
        }

        if (directArena != null) {
            this.directArena = directArena;
            //被持有的引用计数加1
            directArena.numThreadCaches.getAndIncrement();
        }
    }
}
