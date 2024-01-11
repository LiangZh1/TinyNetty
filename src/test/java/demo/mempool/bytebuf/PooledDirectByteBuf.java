package demo.mempool.bytebuf;

import java.nio.ByteBuffer;

/**
 * @author: zhe.liang
 * @create: 2023-11-16 14:29
 **/
public class PooledDirectByteBuf extends PooledByteBuf<ByteBuffer>{

    public static PooledDirectByteBuf newInstance(int maxCapacity){
        return new PooledDirectByteBuf();
    }
}
