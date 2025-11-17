package client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ClientApp {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int PORT = 5000;

    public static void main(String[] args) {
        new ClientApp().startClient();
    }

    public void startClient() {
        try (Socket socket = new Socket(SERVER_IP, PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
             Scanner scanner = new Scanner(System.in)) {

            System.out.print("닉네임을 입력하세요: ");
            String name = scanner.nextLine();
            out.writeObject(name);
            out.flush();

            // 서버 메시지 수신 스레드
            Thread receiveThread = new Thread(() -> {
                try {
                    while (true) {
                        Object msg = in.readObject();
                        if (msg instanceof String) {
                            System.out.println(msg);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("[클라이언트] 서버 연결 종료");
                }
            });
            receiveThread.start();

            // 채팅 입력 루프
            while (true) {
                String input = scanner.nextLine();
                out.writeObject(input);
                out.flush();
            }

        } catch (IOException e) {
            System.out.println("[클라이언트] 서버에 연결할 수 없습니다.");
        }
    }
}
