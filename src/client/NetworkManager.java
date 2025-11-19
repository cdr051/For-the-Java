package client;

import java.io.*;
import java.net.*;
import java.util.function.Consumer;

public class NetworkManager {

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    // 수신 이벤트 리스너 (GUI가 설정해서 사용)
    private Consumer<Message> onMessageReceived;

    public void setOnMessageReceived(Consumer<Message> listener) {
        this.onMessageReceived = listener;
    }

    /**
     * 서버 연결
     */
    public boolean connect(String ip, int port, String playerName) {
        try {
            socket = new Socket(ip, port);

            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush(); // deadlock 방지
            in = new ObjectInputStream(socket.getInputStream());

            // 첫 메시지로 플레이어 이름 전달
            out.writeObject(playerName);
            out.flush();

            // 메시지 수신 스레드 시작
            startReceiveThread();

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 메시지 수신 스레드
     */
    private void startReceiveThread() {
        new Thread(() -> {
            try {
                while (true) {
                    Object obj = in.readObject();

                    if (obj instanceof Message msg) {
                        if (onMessageReceived != null) {
                            onMessageReceived.accept(msg); // GUI에 이벤트 전달
                        }
                    } 
                    else if (obj instanceof String chatMsg) {
                        // 문자열이면 채팅 메시지라고 간주
                        if (onMessageReceived != null) {
                            onMessageReceived.accept(
                                    new Message(Message.Type.CHAT, chatMsg)
                            );
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("[클라이언트] 서버 연결 종료");
            }
        }).start();
    }

    /**
     * 메시지 송신
     */
    public void send(Message message) {
        try {
            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            System.out.println("메시지 전송 실패");
        }
    }

    /**
     * 문자열 채팅 전송 (테스트용)
     */
    public void sendChat(String text) {
        try {
            out.writeObject(text);
            out.flush();
        } catch (IOException e) {
            System.out.println("채팅 전송 실패");
        }
    }
}
