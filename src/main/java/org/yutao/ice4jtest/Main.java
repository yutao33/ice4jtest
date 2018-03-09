package org.yutao.ice4jtest;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;


public class Main {

    public static void main(String[] args) throws Throwable {
        try {
            ICEClient client = new ICEClient(2020, "audio");
            client.init();
            client.exchangeSdpWithPeer();
            client.startConnect();
            final DatagramSocket socket = client.getDatagramSocket();
            final SocketAddress remoteAddress = client
                    .getRemotePeerSocketAddress();
            System.out.println(socket.toString());
            new Thread(new Runnable() {

                public void run() {
                    while (true) {
                        try {
                            byte[] buf = new byte[1024];
                            DatagramPacket packet = new DatagramPacket(buf,
                                    buf.length);
                            socket.receive(packet);
                            System.out.println("receive:"
                                    + new String(packet.getData(), 0, packet
                                    .getLength()));
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                    }
                }
            }).start();

            new Thread(new Runnable() {

                public void run() {
                    int count = 1;
                    while (true) {
                        try {
                            byte[] buf = ("send msg " + count++ + "").getBytes();
                            DatagramPacket packet = new DatagramPacket(buf,
                                    buf.length);

                            packet.setSocketAddress(remoteAddress);
                            socket.send(packet);
                            System.out.println("send msg");
                            TimeUnit.SECONDS.sleep(10);
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                    }
                }
            }).start();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}
