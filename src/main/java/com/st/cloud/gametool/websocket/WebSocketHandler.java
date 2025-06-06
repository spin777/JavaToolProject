package com.st.cloud.gametool.websocket;

import com.st.cloud.gametool.controller.AppController;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author dev03
 */
public class WebSocketHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private static final ExecutorService VIRTUAL_THREAD_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private ByteBuf tempByteBuf;

    private final AppController app;

    public WebSocketHandler(AppController app) {
        this.app = app;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) {
        switch (frame) {
            case BinaryWebSocketFrame msg -> {
                ByteBuf in = msg.content();
                if (!msg.isFinalFragment()) {
                    if (tempByteBuf == null) {
                        tempByteBuf = ctx.alloc().heapBuffer();
                    }
                    tempByteBuf.writeBytes(in);
                } else {
                    handleMessage(in);
                }
            }
            case ContinuationWebSocketFrame msg -> {
                tempByteBuf.writeBytes(msg.content());
                if (msg.isFinalFragment()) {
                    handleMessage(tempByteBuf);
                    tempByteBuf.clear();
                }
            }
            case null, default -> {
                if (frame != null) {
                    ctx.fireChannelRead(frame.retain());
                }
            }
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt == WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_COMPLETE) {
            System.out.println("WebSocket 握手成功！");
        }
        super.userEventTriggered(ctx, evt);
        javafx.application.Platform.runLater(() -> {
            app.getLinkGame().setText("断开游戏");
            app.getLinkState().set(true);
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.err.println("连接异常中断：" + cause.getMessage());
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("WebSocket 连接已关闭");
        super.channelInactive(ctx);
        javafx.application.Platform.runLater(() -> {
            app.getLinkGame().setText("链接游戏");
            app.getLinkState().set(false);
            app.endRunGame();
        });
    }

    void handleMessage(ByteBuf byteBuf) {
        int totalLength = byteBuf.readableBytes();
        short code = byteBuf.readShort();
        int dataSize = totalLength - 2;
        byte[] bytes = new byte[dataSize];
        byteBuf.readBytes(bytes);
        CompletableFuture.runAsync(() -> app.onMessage(code, bytes), VIRTUAL_THREAD_EXECUTOR);
    }
}
