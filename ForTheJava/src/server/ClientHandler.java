package server;

import java.io.*;
import java.net.*;

public class ClientHandler extends Thread {
    private Socket socket;
    private GameServer server;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String playerName;

    public ClientHandler(Socket socket, GameServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            // 첫 번째 메시지: 클라이언트가 이름 전송
            playerName = (String) in.readObject();
            System.out.println("[SERVER] 플레이어 접속: " + playerName);

            server.broadcast("📢 " + playerName + " 님이 접속했습니다!");

            // 간단한 대기 루프
            while (true) {
                Object msg = in.readObject();
                if (msg instanceof String) {
                    System.out.println("[" + playerName + "]: " + msg);
                }
            }

        } catch (Exception e) {
            System.out.println("[SERVER] " + playerName + " 연결 종료");
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {}
            server.removeClient(this);
        }
    }

    public void sendMessage(String message) {
        try {
            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            System.out.println("[SERVER] 메시지 전송 실패: " + message);
        }
    }
}
