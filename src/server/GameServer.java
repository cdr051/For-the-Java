package server;

import client.Message;  // shared로 옮겨도 됨
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class GameServer {

    private static final int PORT = 5000;
    private List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) {
        new GameServer().startServer();
    }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[SERVER] 대기중... 포트: " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("[SERVER] 새 클라이언트 접속");

                ClientHandler handler = new ClientHandler(socket, this);
                clients.add(handler);
                handler.start();
            }
        } catch (IOException e) {
            System.out.println("[SERVER] 오류 발생");
        }
    }

    public void broadcast(Message msg) {
        synchronized (clients) {
            for (ClientHandler c : clients) {
                c.send(msg);
            }
        }
    }

    public void broadcastPlayerList() {
        List<String> names = getPlayerNames();
        broadcast(new Message(Message.Type.PLAYER_LIST, names));
    }

    public void sendPlayerListToOne(ClientHandler handler) {
        handler.send(new Message(Message.Type.PLAYER_LIST, getPlayerNames()));
    }

    public void removeClient(ClientHandler handler) {
        clients.remove(handler);
    }

    private List<String> getPlayerNames() {
        List<String> list = new ArrayList<>();
        synchronized (clients) {
            for (ClientHandler c : clients) {
                list.add(c.getPlayerName());
            }
        }
        return list;
    }
}
