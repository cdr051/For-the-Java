package server;

import client.Message;
import java.io.*;
import java.net.Socket;

public class ClientHandler extends Thread {
    private Socket socket;
    private GameServer server;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String playerName;
    private GameSession currentSession;

    public ClientHandler(Socket socket, GameServer server) {
        this.socket = socket;
        this.server = server;
    }

    public void setGameSession(GameSession session) { this.currentSession = session; }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            Object first = in.readObject();
            if (first instanceof String) {
                this.playerName = (String) first;
                System.out.println("[SERVER] 접속: " + playerName);
                server.broadcast(new Message(Message.Type.PLAYER_JOIN, playerName));
                server.addClient(this);
            }

            while (true) {
                Object obj = in.readObject();
                if (obj instanceof Message msg) {
                    if (msg.getType() == Message.Type.SUBMIT_TILE && currentSession != null) {
                        currentSession.handleSubmission(this, (int) msg.getData());
                    } else if (msg.getType() == Message.Type.REQUEST_PLAYER_LIST) {
                        server.sendPlayerListToOne(this);
                    }
                } else if (obj instanceof String) {
                    server.broadcast(new Message(Message.Type.CHAT, playerName + ": " + obj));
                }
            }
        } catch (Exception e) {
            System.out.println("[SERVER] 종료: " + playerName);
        } finally {
            server.removeClient(this);
        }
    }

    public void send(Message msg) {
        try { out.writeObject(msg); out.flush(); } catch (IOException e) {}
    }
    public String getPlayerName() { return playerName; }
}