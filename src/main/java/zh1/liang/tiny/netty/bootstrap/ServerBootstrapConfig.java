package zh1.liang.tiny.netty.bootstrap;


import zh1.liang.tiny.netty.channel.Channel;
import zh1.liang.tiny.netty.channel.EventLoopGroup;
import zh1.liang.tiny.netty.util.internal.StringUtil;

public final class ServerBootstrapConfig extends AbstractBootstrapConfig<ServerBootStrap, Channel> {

    ServerBootstrapConfig(ServerBootStrap bootstrap) {
        super(bootstrap);
    }

    @SuppressWarnings("deprecation")
    public EventLoopGroup childGroup() {
        return bootstrap.childGroup();
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(super.toString());
        buf.setLength(buf.length() - 1);
        buf.append(", ");
        EventLoopGroup childGroup = childGroup();
        if (childGroup != null) {
            buf.append("childGroup: ");
            buf.append(StringUtil.simpleClassName(childGroup));
            buf.append(", ");
        }
        if (buf.charAt(buf.length() - 1) == '(') {
            buf.append(')');
        } else {
            buf.setCharAt(buf.length() - 2, ')');
            buf.setLength(buf.length() - 1);
        }

        return buf.toString();
    }
}
