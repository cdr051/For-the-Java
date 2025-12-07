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
        System.out.println("🔥 For The King Server Started...");
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
                broadcast(new Message(Message.Type.CHAT, "[시스템] Player " + (myId+1) + " 입장."));

                while (true) {
                    Message msg = (Message) in.readObject();
                    
                    if (msg.type == Message.Type.SET_NAME) { 
                        gameManager.setPlayerName(myId, (String) msg.payload);
                        broadcast(new Message(Message.Type.LOBBY_UPDATE, new ArrayList<>(gameManager.getGameState().players)));
                    }
                    else if (msg.type == Message.Type.CHAT) { broadcast(msg); } 
                    else if (msg.type == Message.Type.CHANGE_JOB) {
                        gameManager.changeJob(myId, (String) msg.payload);
                        broadcast(new Message(Message.Type.LOBBY_UPDATE, new ArrayList<>(gameManager.getGameState().players)));
                    }
                    else if (msg.type == Message.Type.READY) {
                        boolean ready = (boolean) msg.payload;
                        gameManager.getGameState().players.get(myId).isReady = ready;
                        broadcast(new Message(Message.Type.LOBBY_UPDATE, new ArrayList<>(gameManager.getGameState().players)));
                    }
                    else if (msg.type == Message.Type.START_GAME) {
                        // ⭐ [핵심 수정] 여기서 startGame()을 호출해야 라운드가 1이 됨
                        gameManager.startGame(); 
                        broadcast(new Message(Message.Type.START_GAME, gameManager.getGameState()));
                    }
                    else if (msg.type == Message.Type.ROLL_DICE) {
                        gameManager.rollDice(myId);
                        broadcast(new Message(Message.Type.STATE_UPDATE, gameManager.getGameState()));
                    }
                    else if (msg.type == Message.Type.MOVE_REQ) {
                        int[] move = (int[]) msg.payload;
                        gameManager.movePlayer(myId, move[0], move[1]);
                        broadcast(new Message(Message.Type.STATE_UPDATE, gameManager.getGameState()));
                    }
                    else if (msg.type == Message.Type.TURN_PASS) {
                        gameManager.passTurn(myId);
                        broadcast(new Message(Message.Type.STATE_UPDATE, gameManager.getGameState()));
                    }
                    else if (msg.type == Message.Type.BATTLE_ACTION) {
                        BattleRequest req = (BattleRequest) msg.payload;
                        gameManager.processBattleAction(myId, req);
                        broadcast(new Message(Message.Type.STATE_UPDATE, gameManager.getGameState()));
                    }
                    else if (msg.type == Message.Type.SHOP_BUY) {
                        String itemCode = (String) msg.payload;
                        gameManager.processShopBuy(myId, itemCode);
                        broadcast(new Message(Message.Type.STATE_UPDATE, gameManager.getGameState()));
                    }
                    else if (msg.type == Message.Type.SHOP_EXIT) {
                        gameManager.exitShop(myId);
                        broadcast(new Message(Message.Type.STATE_UPDATE, gameManager.getGameState()));
                    }
                    else if (msg.type == Message.Type.RESET_REQ) {
                        gameManager.resetGame();
                        broadcast(new Message(Message.Type.LOBBY_UPDATE, new ArrayList<>(gameManager.getGameState().players)));
                        broadcast(new Message(Message.Type.STATE_UPDATE, gameManager.getGameState()));
                    }
                }
            } catch (Exception e) {
                clients.remove(out);
            }
        }
    }
}