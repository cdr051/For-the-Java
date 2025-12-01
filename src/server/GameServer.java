package server;

import shared.*;
import shared.Message.BattleRequest;
import java.io.*;
import java.net.*;
import java.util.*;

public class GameServer {
    private static final int PORT = 9999;
    private static List<ObjectOutputStream> clients = new ArrayList<>();
    private static GameManager gameManager = new GameManager(); 

    public static void main(String[] args) {
        System.out.println("🔥 For The King Server (Integrated) Started...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                new ClientHandler(socket).start();
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    public static synchronized void broadcast(Message msg) {
        for (ObjectOutputStream out : clients) {
            try {
                out.reset();
                out.writeObject(msg);
                out.flush();
            } catch (IOException e) {}
        }
    }

    static class ClientHandler extends Thread {
        private Socket socket;
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private int myId;

        public ClientHandler(Socket socket) { this.socket = socket; }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(socket.getInputStream());
                
                synchronized (clients) { clients.add(out); }

                GameState currentState = gameManager.getGameState();
                synchronized (currentState) {
                    int id = currentState.players.size();
                    Player p = new Player(id, "Player " + (id + 1), java.awt.Color.BLUE);
                    if (id == 0) { p.isHost = true; p.isReady = true; }
                    currentState.players.add(p);
                    myId = id;
                    out.writeObject(new Message(Message.Type.LOGIN, myId));
                }
                broadcast(new Message(Message.Type.LOBBY_UPDATE, new ArrayList<>(currentState.players)));

                while (true) {
                    Message msg = (Message) in.readObject();
                    
                    switch (msg.type) {
                        case SET_NAME:
                            gameManager.setPlayerName(myId, (String) msg.payload);
                            broadcast(new Message(Message.Type.LOBBY_UPDATE, new ArrayList<>(gameManager.getGameState().players)));
                            break;
                        case CHAT:
                            broadcast(msg);
                            break;
                        case CHANGE_JOB:
                            gameManager.changeJob(myId, (String) msg.payload);
                            broadcast(new Message(Message.Type.LOBBY_UPDATE, new ArrayList<>(gameManager.getGameState().players)));
                            break;
                        case READY:
                            gameManager.getGameState().players.get(myId).isReady = (boolean) msg.payload;
                            broadcast(new Message(Message.Type.LOBBY_UPDATE, new ArrayList<>(gameManager.getGameState().players)));
                            break;
                        case START_GAME:
                            broadcast(new Message(Message.Type.START_GAME, gameManager.getGameState()));
                            break;
                        // --- 게임 플레이 ---
                        case ROLL_DICE:
                            gameManager.rollDice(myId);
                            broadcast(new Message(Message.Type.STATE_UPDATE, gameManager.getGameState()));
                            break;
                        case MOVE_REQ:
                            int[] move = (int[]) msg.payload;
                            gameManager.movePlayer(myId, move[0], move[1]);
                            broadcast(new Message(Message.Type.STATE_UPDATE, gameManager.getGameState()));
                            break;
                        case TURN_PASS:
                            gameManager.passTurn(myId);
                            broadcast(new Message(Message.Type.STATE_UPDATE, gameManager.getGameState()));
                            break;
                        // --- 전투 ---
                        case BATTLE_ACTION:
                            gameManager.processBattleAction(myId, (BattleRequest) msg.payload);
                            broadcast(new Message(Message.Type.STATE_UPDATE, gameManager.getGameState()));
                            break;
                        // 상점
                        case SHOP_BUY:
                            gameManager.buyItem(myId, (String) msg.payload);
                            broadcast(new Message(Message.Type.STATE_UPDATE, gameManager.getGameState()));
                            break;
                        case SHOP_EXIT:
                            gameManager.exitShop(myId);
                            broadcast(new Message(Message.Type.STATE_UPDATE, gameManager.getGameState()));
                            break;
                    }
                }
            } catch (Exception e) {
                clients.remove(out);
            }
        }
    }
}