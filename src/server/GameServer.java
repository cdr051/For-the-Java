package server;

import client.Message;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class GameServer {
    private static final int PORT = 5000;
    private List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    private Queue<ClientHandler> waitingQueue = new ConcurrentLinkedQueue<>();

    public static void main(String[] args) { new GameServer().startServer(); }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("서버 시작 (Port: " + PORT + ")");
            while (true) {
                Socket s = serverSocket.accept();
                new ClientHandler(s, this).start();
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    public synchronized void addClient(ClientHandler h) {
        clients.add(h);
        waitingQueue.add(h);
        checkMatching();
    }
    public synchronized void removeClient(ClientHandler h) {
        clients.remove(h);
        waitingQueue.remove(h);
    }

    private void checkMatching() {
        if (waitingQueue.size() >= 2) {
            ClientHandler p1 = waitingQueue.poll();
            ClientHandler p2 = waitingQueue.poll();
            GameSession s = new GameSession(p1, p2);
            p1.setGameSession(s);
            p2.setGameSession(s);
            s.start();
        }
    }

    public void broadcast(Message msg) {
        synchronized (clients) { for (ClientHandler c : clients) c.send(msg); }
    }
    public void sendPlayerListToOne(ClientHandler h) { /* 생략 */ }
}