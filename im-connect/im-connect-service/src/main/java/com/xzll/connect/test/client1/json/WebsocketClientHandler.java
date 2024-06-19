
package com.xzll.connect.test.client1.json;

import com.alibaba.fastjson.JSON;
import com.xzll.common.constant.ImConstant;
import com.xzll.common.pojo.response.ClientGetBatchMsgIdVO;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @Author: hzz
 * @Date: 2022/1/14 10:39:23
 * @Description:
 */
public class WebsocketClientHandler extends SimpleChannelInboundHandler<Object> {
    private final WebSocketClientHandshaker handshaker;
    private ChannelPromise handshakeFuture;

    public static List<String> msgIds = new ArrayList<>();

    public WebsocketClientHandler(WebSocketClientHandshaker handshaker) {
        this.handshaker = handshaker;
    }

    public ChannelFuture handshakeFuture() {
        return handshakeFuture;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        handshakeFuture = ctx.newPromise();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("客户端连接建立,当前uid:" + ctx.channel().attr(ImConstant.USER_ID_KEY).get());
        // 在通道连接成功后发送握手连接
        handshaker.handshake(ctx.channel());
        super.channelActive(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
//      if (event.state() == IdleState.WRITER_IDLE) {
            // 发送心跳消息
            sendHeartbeat(ctx);
//      }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    private void sendHeartbeat(ChannelHandlerContext ctx) {
        // 构建心跳消息并发送
        PingWebSocketFrame pingWebSocketFrame = new PingWebSocketFrame();
        ctx.writeAndFlush(pingWebSocketFrame);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel ch = ctx.channel();

        // 这里是第一次使用http连接成功的时候
        if (!handshaker.isHandshakeComplete()) {
            handshaker.finishHandshake(ch, (FullHttpResponse) msg);
            System.out.println("WebSocket Client connected!");
            handshakeFuture.setSuccess();
            return;
        }

        // 这里是第一次使用http连接失败的时候
        if (msg instanceof FullHttpResponse) {
            FullHttpResponse response = (FullHttpResponse) msg;
            throw new IllegalStateException(
                    "Unexpected FullHttpResponse (getStatus=" + response.getStatus() + ", content=" + response.content()
                            .toString(CharsetUtil.UTF_8) + ')');
        }

        // 这里是服务器与客户端进行通讯的
        WebSocketFrame frame = (WebSocketFrame) msg;
        if (frame instanceof TextWebSocketFrame) {
            TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
            System.out.println("客户端：接收到TextWebSocketFrame消息，消息内容是-- " + textFrame.text());
            ClientGetBatchMsgIdVO rsp = JSON.parseObject(textFrame.text(), ClientGetBatchMsgIdVO.class);
            if (Objects.nonNull(rsp) && !CollectionUtils.isEmpty(rsp.getMsgIds())) {
                msgIds.addAll(rsp.getMsgIds());
                WebsocketClient.getMsgFlag = false;
            }
        } else if (frame instanceof BinaryWebSocketFrame) {
            System.out.println("客户端：接收到BinaryWebSocketFrame消息，消息内容是-- ");
            ByteBuf content = frame.content();
            byte[] result = new byte[content.readableBytes()];
            content.readBytes(result);
            for (byte b : result) {
                System.out.print(b);
                System.out.print(",");
            }
            System.out.println();
        } else if (frame instanceof PongWebSocketFrame) {
            System.out.println("WebSocket Client received pong");
        } else if (frame instanceof CloseWebSocketFrame) {
            System.out.println("WebSocket Client received closing");
            ch.close();
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable arg1) {
        System.out.println("异常发生");
        arg1.printStackTrace();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("客户端连接断开");
        super.channelInactive(ctx);
    }
}
