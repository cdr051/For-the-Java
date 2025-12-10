package server;

import shared.*;
import shared.Message.BattleRequest;
import java.io.*;
import java.net.*;
import java.util.*;

public class GameServer {
    private static final int PORT = GameConfig.SERVER_PORT;
    private static List<ObjectOutputStream> clients = new ArrayList<>();
    private static GameManager gameManager = new GameManager(); 

    public static void main(String[] args) {
        System.out.println("🔥 For The King Server Started on port " + PORT + "...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("✅ Server is ready to accept connections.");
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    System.out.println("📥 New client connection: " + socket.getRemoteSocketAddress());
                    new ClientHandler(socket).start();
                } catch (IOException e) {
                    System.err.println("❌ Error accepting client connection: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            System.err.println("❌ Failed to start server on port " + PORT + ": " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static synchronized void broadcast(Message msg) {
        if (msg == null) return;
        
        List<ObjectOutputStream> toRemove = new ArrayList<>();
        for (ObjectOutputStream out : clients) {
            try {
                out.reset();
                out.writeObject(msg);
                out.flush();
            } catch (IOException e) {
                System.err.println("⚠️ Error broadcasting to client: " + e.getMessage());
                toRemove.add(out);
            }
        }
        // 연결이 끊어진 클라이언트 제거
        clients.removeAll(toRemove);
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
                    try {
                        Message msg = (Message) in.readObject();
                        if (msg == null || msg.type == null) continue;
                        
                        handleMessage(msg);
                    } catch (ClassNotFoundException e) {
                        System.err.println("❌ Invalid message format from client " + myId + ": " + e.getMessage());
                        break;
                    } catch (ClassCastException e) {
                        System.err.println("❌ Invalid message payload from client " + myId + ": " + e.getMessage());
                        continue;
                    }
                }
            } catch (IOException e) {
                System.out.println("🔌 Client " + myId + " disconnected: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("❌ Unexpected error with client " + myId + ": " + e.getMessage());
                e.printStackTrace();
            } finally {
                cleanupClient();
            }
        }
        
        private void handleMessage(Message msg) {
            try {
                switch (msg.type) {
                    case SET_NAME:
                        if (msg.payload instanceof String) {
                            gameManager.setPlayerName(myId, (String) msg.payload);
                            broadcast(new Message(Message.Type.LOBBY_UPDATE, new ArrayList<>(gameManager.getGameState().players)));
                        }
                        break;
                    case CHAT:
                        if (msg.payload instanceof String) {
                            broadcast(msg);
                        }
                        break;
                    case CHANGE_JOB:
                        if (msg.payload instanceof String) {
                            gameManager.changeJob(myId, (String) msg.payload);
                            broadcast(new Message(Message.Type.LOBBY_UPDATE, new ArrayList<>(gameManager.getGameState().players)));
                        }
                        break;
                    case READY:
                        if (msg.payload instanceof Boolean) {
                            GameState state = gameManager.getGameState();
                            if (myId >= 0 && myId < state.players.size()) {
                                state.players.get(myId).isReady = (Boolean) msg.payload;
                                broadcast(new Message(Message.Type.LOBBY_UPDATE, new ArrayList<>(state.players)));
                            }
                        }
                        break;
                    case START_GAME:
                        gameManager.startGame();
                        broadcast(new Message(Message.Type.START_GAME, gameManager.getGameState()));
                        break;
                    case ROLL_DICE:
                        gameManager.rollDice(myId);
                        broadcast(new Message(Message.Type.STATE_UPDATE, gameManager.getGameState()));
                        break;
                    case MOVE_REQ:
                        if (msg.payload instanceof int[] && ((int[]) msg.payload).length >= 2) {
                            int[] move = (int[]) msg.payload;
                            gameManager.movePlayer(myId, move[0], move[1]);
                            broadcast(new Message(Message.Type.STATE_UPDATE, gameManager.getGameState()));
                        }
                        break;
                    case TURN_PASS:
                        gameManager.passTurn(myId);
                        broadcast(new Message(Message.Type.STATE_UPDATE, gameManager.getGameState()));
                        break;
                    case BATTLE_ACTION:
                        if (msg.payload instanceof BattleRequest) {
                            BattleRequest req = (BattleRequest) msg.payload;
                            gameManager.processBattleAction(myId, req);
                            broadcast(new Message(Message.Type.STATE_UPDATE, gameManager.getGameState()));
                        }
                        break;
                    case SHOP_BUY:
                        if (msg.payload instanceof String) {
                            String itemCode = (String) msg.payload;
                            gameManager.processShopBuy(myId, itemCode);
                            broadcast(new Message(Message.Type.STATE_UPDATE, gameManager.getGameState()));
                        }
                        break;
                    case SHOP_EXIT:
                        gameManager.exitShop(myId);
                        broadcast(new Message(Message.Type.STATE_UPDATE, gameManager.getGameState()));
                        break;
                    case RESET_REQ:
                        gameManager.resetGame();
                        broadcast(new Message(Message.Type.LOBBY_UPDATE, new ArrayList<>(gameManager.getGameState().players)));
                        broadcast(new Message(Message.Type.STATE_UPDATE, gameManager.getGameState()));
                        break;
                    default:
                        System.out.println("⚠️ Unknown message type: " + msg.type);
                }
            } catch (Exception e) {
                System.err.println("❌ Error handling message " + msg.type + " from client " + myId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        private void cleanupClient() {
            synchronized (clients) {
                clients.remove(out);
            }
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {
                System.err.println("⚠️ Error closing client resources: " + e.getMessage());
            }
            System.out.println("👋 Client " + myId + " cleaned up.");
        }
    }
}