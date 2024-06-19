
package com.xzll.connect.test.client111;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xzll.common.constant.ImConstant;
import com.xzll.common.constant.MsgStatusEnum;
import com.xzll.common.constant.MsgTypeEnum;
import com.xzll.common.pojo.base.ImBaseResponse;
import com.xzll.common.pojo.request.ClientReceivedMsgAckAO;
import com.xzll.common.pojo.base.ImBaseRequest;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: hzz
 * @Date: 2022/1/14 10:39:23
 * @Description:
 */
public class WebsocketClientHandler111 extends SimpleChannelInboundHandler<Object> {
    private final WebSocketClientHandshaker handshaker;
    private ChannelPromise handshakeFuture;

    public static List<String> msgIds = new ArrayList<>();

    public WebsocketClientHandler111(WebSocketClientHandshaker handshaker) {
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

            System.out.println("用户：（ " + WebsocketClient111.VALUE + " ），接收到TextWebSocketFrame消息，消息内容是: " + textFrame.text());
            ImBaseResponse imBaseResponse = JSON.parseObject(textFrame.text(), ImBaseResponse.class);
            ImBaseResponse.MsgType msgType = imBaseResponse.getMsgType();

            //模拟接收到单聊消息后 receive client 返回已读 or 未读
            int firstLevelMsgType = msgType.getFirstLevelMsgType();
            int secondLevelMsgType = msgType.getSecondLevelMsgType();

            /**
             * 给发送者回复未读/已读消息
             */
            if (firstLevelMsgType == MsgTypeEnum.FirstLevelMsgType.CHAT_MSG.getCode()
                    && secondLevelMsgType == MsgTypeEnum.SecondLevelMsgType.C2C.getCode()) {

                JSONObject jsonObject = JSON.parseObject(textFrame.text());
                String msgId = jsonObject.getObject("msgId", String.class);
                String toUserId = jsonObject.getObject("toUserId", String.class);
                String fromUserId = jsonObject.getObject("fromUserId", String.class);

                ClientReceivedMsgAckAO clientReceivedMsgAckAO = new ClientReceivedMsgAckAO();
                clientReceivedMsgAckAO.setMsgId(msgId);
                clientReceivedMsgAckAO.setFromUserId(toUserId);
                clientReceivedMsgAckAO.setToUserId(fromUserId);
                //模拟接收方已读 发送成功ack
                clientReceivedMsgAckAO.setMsgStatus(MsgStatusEnum.MsgStatus.UN_READ.getCode());


                ImBaseRequest.MsgType request = new ImBaseRequest.MsgType();
                request.setFirstLevelMsgType(MsgTypeEnum.FirstLevelMsgType.ACK_MSG.getCode());
                request.setSecondLevelMsgType(MsgTypeEnum.SecondLevelMsgType.UN_READ.getCode());

                ImBaseRequest imBaseRequest = new ImBaseRequest<>();

                imBaseRequest.setMsgType(request);
                imBaseRequest.setBody(clientReceivedMsgAckAO);

                //模拟未读
                ctx.channel().writeAndFlush(new TextWebSocketFrame(JSONUtil.toJsonStr(imBaseRequest)));
                System.out.println("发送未读完成，data: " + JSONUtil.toJsonStr(imBaseRequest));

                //模拟已读
                request.setSecondLevelMsgType(MsgTypeEnum.SecondLevelMsgType.READ.getCode());
                clientReceivedMsgAckAO.setMsgStatus(MsgStatusEnum.MsgStatus.READED.getCode());
                imBaseRequest.setMsgType(request);
                imBaseRequest.setBody(clientReceivedMsgAckAO);
                ctx.channel().writeAndFlush(new TextWebSocketFrame(JSONUtil.toJsonStr(imBaseRequest)));
                System.out.println("发送已读完成，data: " + JSONUtil.toJsonStr(imBaseRequest));
            }
            /**
             * 处理单聊消息
             */
            if (MsgTypeEnum.FirstLevelMsgType.CHAT_MSG.getCode() == firstLevelMsgType) {
                if (MsgTypeEnum.SecondLevelMsgType.C2C.getCode() == secondLevelMsgType) {
                    JSONObject jsonObject = JSON.parseObject(textFrame.text());
                    String msgContent = jsonObject.getObject("msgContent", String.class);
                    String fromUserId = jsonObject.getObject("fromUserId", String.class);
                    System.out.println("【" + fromUserId + "】: " + msgContent);
                }
            }
            /**
             * 处理获取消息id的消息
             */
            if (MsgTypeEnum.FirstLevelMsgType.GET_DATA_MSG.getCode() == firstLevelMsgType) {
                if (MsgTypeEnum.SecondLevelMsgType.GET_MSG_IDS.getCode() == secondLevelMsgType) {
                    JSONObject jsonObject = JSON.parseObject(textFrame.text());
                    List<String> msgIds = jsonObject.getObject("msgIds", List.class);
                    System.out.println("获取到一批消息id,长度:" + msgIds.size());
                    if (!CollectionUtils.isEmpty(msgIds)) {
                        WebsocketClientHandler111.msgIds.addAll(msgIds);
                        WebsocketClient111.getMsgFlag = false;
                    }
                }
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
