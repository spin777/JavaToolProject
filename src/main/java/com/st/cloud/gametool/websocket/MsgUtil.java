package com.st.cloud.gametool.websocket;

import com.google.protobuf.MessageLite;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * @author dev03
 */
public class MsgUtil {

    public static byte[] messageToBytes(short code, MessageLite.Builder builder) {
        byte[] bytes = null;
        int length = 2;
        if (Objects.nonNull(builder)) {
            bytes = builder.build().toByteArray();
            length += bytes.length;
        }
        ByteBuffer bb = ByteBuffer.allocate(length);
        bb.putShort(code);
        if (Objects.nonNull(bytes)) {
            bb.put(bytes);
        }
        return bb.array();
    }

    /**
     * 转换二进制消息
     *
     * @param code    消息协议号
     * @param builder Protobuf 消息对象，需要实现 Message 接口
     */
    public static ByteBuf formatMessage(short code, MessageLite.Builder builder) {
        byte[] array = messageToBytes(code, builder);
        return Unpooled.wrappedBuffer(array);
    }


    /**
     * 发送二进制消息
     *
     * @param channel 客户端的 Netty Channel，用于写回消息
     * @param builder Protobuf 消息对象，需要实现 Message 接口
     */
    public static void sendBinaryMsg(Channel channel, short code, MessageLite.Builder builder) {
        if (Objects.isNull(channel)) {
            return;
        }
        ByteBuf buffer = formatMessage(code, builder);
        BinaryWebSocketFrame frame = new BinaryWebSocketFrame(buffer);
        channel.writeAndFlush(frame);
    }
}
