package server;

import java.io.*;
import java.net.*;
import java.util.*;

public class GameServer {
    private static final int PORT = 5000;
    private ServerSocket serverSocket;
    private List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) {
        new GameServer().startServer();
    }

    public void startServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("[SERVER] 게임 서버가 " + PORT + "번 포트에서 대기 중입니다...");

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("[SERVER] 새 클라이언트 접속: " + socket.getInetAddress());

                ClientHandler handler = new ClientHandler(socket, this);
                clients.add(handler);
                handler.start(); // 개별 스레드로 처리
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 서버 전체 브로드캐스트 (채팅 등 공용 메시지)
    public void broadcast(String message) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.sendMessage(message);
            }
        }
    }

    // 클라이언트 연결 해제 시 제거
    public void removeClient(ClientHandler client) {
        clients.remove(client);
        System.out.println("[SERVER] 클라이언트 연결 해제됨");
    }
}
