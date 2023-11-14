package demo.IdleHandler;

import lombok.extern.slf4j.Slf4j;
import zh1.liang.tiny.netty.util.internal.ObjectUtil;

/**
 * @author: zhe.liang
 * @create: 2023-10-16 18:22
 */
@Slf4j
public class IdleStateEvent {
    public static final IdleStateEvent READER_IDLE_STATE_EVENT =
            new IdleStateEvent(IdleState.READER_IDLE, true);

    public static final IdleStateEvent WRITER_IDLE_STATE_EVENT =
            new IdleStateEvent(IdleState.WRITER_IDLE, true);

    // 空闲事件类型
    private final IdleState state;
    // 是不是第一次触发空闲事件
    private final boolean first;

    public IdleStateEvent(IdleState state, boolean first) {
        this.state = ObjectUtil.checkNotNull(state, "state");
        this.first = first;
    }

    public IdleState state() {
        return state;
    }

    public boolean isFirst() {
        return first;
    }
}
