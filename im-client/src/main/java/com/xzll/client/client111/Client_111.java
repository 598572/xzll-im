package com.xzll.client.client111;



import java.net.URISyntaxException;

/**
 * @Author: hzz
 * @Date: 2022/1/14 10:29:25
 * @Description:
 */
public class Client_111 {

    private static final String XUNIJI="172.30.128.65";
    private static final String XUNIJI_家="192.168.1.103";
    public static final String LOCAL = "127.0.0.1";

    public static void start(String[] args) {
        WebsocketClient111 websocketClient111 = new WebsocketClient111(LOCAL, 10001);
        try {
            websocketClient111.run();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        start(new String[0]);
    }

}
