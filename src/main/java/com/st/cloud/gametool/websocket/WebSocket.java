package com.st.cloud.gametool.websocket;

import com.google.protobuf.MessageLite;
import com.st.cloud.gametool.controller.AppController;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.IdleStateHandler;

import javax.net.ssl.SSLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

/**
 * @author dev03
 */
public class WebSocket {
    Channel channel;
    final URI uri;
    final boolean ssl;
    final String token;
    final int gameId;
    final int roomId;
    EventLoopGroup group;
    int port;

    private final AppController app;

    public WebSocket(String uri, String token, int gameId, int roomId, AppController app) {
        if (uri.startsWith("http")) {
            uri = "ws" + uri.substring(4);
        } else if (uri.startsWith("https")) {
            uri = "wss" + uri.substring(5);
        }
        uri += "/gateway/webSocket";
        try {
            this.uri = new URI(uri);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        port = this.uri.getPort();
        if (port == -1) {
            port = "wss".equals(this.uri.getScheme()) ? 443 : 80;
        }
        this.ssl = "wss".equalsIgnoreCase(this.uri.getScheme());
        this.token = token;
        this.gameId = gameId;
        this.roomId = roomId;
        this.app = app;
        this.group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
    }

    public void connect() {
        DefaultHttpHeaders headers = new DefaultHttpHeaders();
        headers.add("Authorization", token);
        headers.add("gameId", gameId);
        headers.add("roomId", roomId);
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws SSLException {
                            ChannelPipeline pipeline = ch.pipeline();
                            if (ssl) {
                                SslContext sslCtx = SslContextBuilder.forClient()
                                        .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
                                pipeline.addLast(sslCtx.newHandler(ch.alloc(), uri.getHost(), uri.getPort()));
                            }
                            pipeline.addLast(new HttpClientCodec());
                            pipeline.addLast(new HttpObjectAggregator(65536));
                            pipeline.addLast(new WebSocketClientProtocolHandler(WebSocketClientHandshakerFactory.newHandshaker(
                                    uri, WebSocketVersion.V13, null, false, headers)));
                            pipeline.addLast(new IdleStateHandler(0, 1, 0, TimeUnit.SECONDS));
                            pipeline.addLast(new WebSocketHandler(app));
                        }
                    });
            ChannelFuture future = bootstrap.connect(uri.getHost(), this.port);
            // 设置连接成功/失败回调
            future.addListener((ChannelFutureListener) future1 -> {
                if (future1.isSuccess()) {
                    channel = future1.channel();
                } else {
                    group.shutdownGracefully().syncUninterruptibly();
                }
            });
        } catch (Exception e) {
            close();
        }
    }

    public void close() {
        if (channel != null && channel.isActive()) {
            channel.close().syncUninterruptibly();
        }
        if (group != null && !group.isShutdown()) {
            group.shutdownGracefully().syncUninterruptibly();
            group = null;
        }
    }

    /**
     * 发送消息
     */
    public void sendMessage(short code, MessageLite.Builder builder) {
        MsgUtil.sendBinaryMsg(channel, code, builder);
    }

}
