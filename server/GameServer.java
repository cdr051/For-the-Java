package server;

import shared.*;
import shared.Message.BattleRequest; // ⭐ [핵심] 이 줄이 있어야 에러가 안 납니다!
import java.io.*;
import java.net.*;
import java.util.*;

public class GameServer {
    private static final int PORT = 9999;
    private static List<ObjectOutputStream> clients = new ArrayList<>();
    
    // GameManager 인스턴스 생성
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

    // 모든 클라이언트에게 메시지 전송
    public static synchronized void broadcast(Message msg) {
        for (ObjectOutputStream out : clients) {
            try {
                out.reset();
                out.writeObject(msg);
                out.flush();
            } catch (IOException e) { 
                // 연결 끊긴 클라이언트 처리 (간단히 무시)
            }
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

                // 1. 입장 및 로그인 처리
                GameState currentState = gameManager.getGameState();
                synchronized (currentState) {
                    int id = currentState.players.size();
                    Player p = new Player(id, "Player " + (id + 1), java.awt.Color.BLUE);
                    if (id == 0) {
                        p.isHost = true;
                        p.isReady = true; 
                    }
                    currentState.players.add(p);
                    myId = id;
                    
                    out.writeObject(new Message(Message.Type.LOGIN, myId));
                }
                
                // 로비 갱신 알림 (리스트 복사해서 전송)
                broadcast(new Message(Message.Type.LOBBY_UPDATE, new ArrayList<>(currentState.players)));
                broadcast(new Message(Message.Type.CHAT, "[시스템] Player " + (myId+1) + "님이 입장했습니다."));

                // 2. 메시지 수신 루프
                while (true) {
                    Message msg = (Message) in.readObject();
                    
                    // --- 로비 로직 ---
                    if (msg.type == Message.Type.SET_NAME) { 
                        gameManager.setPlayerName(myId, (String) msg.payload);
                        broadcast(new Message(Message.Type.LOBBY_UPDATE, new ArrayList<>(gameManager.getGameState().players)));
                    }
                    else if (msg.type == Message.Type.CHAT) {
                        broadcast(msg);
                    } 
                    else if (msg.type == Message.Type.CHANGE_JOB) {
                        gameManager.getGameState().players.get(myId).jobClass = (String) msg.payload;
                        broadcast(new Message(Message.Type.LOBBY_UPDATE, new ArrayList<>(gameManager.getGameState().players)));
                    }
                    else if (msg.type == Message.Type.READY) {
                        boolean ready = (boolean) msg.payload;
                        gameManager.getGameState().players.get(myId).isReady = ready;
                        broadcast(new Message(Message.Type.LOBBY_UPDATE, new ArrayList<>(gameManager.getGameState().players)));
                    }
                    else if (msg.type == Message.Type.START_GAME) {
                        broadcast(new Message(Message.Type.START_GAME, gameManager.getGameState()));
                    }
                    
                    // --- 게임 플레이 로직 ---
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
                    
                    // --- ⭐ 전투 로직 추가됨 ---
                    else if (msg.type == Message.Type.BATTLE_ACTION) {
                        // 이제 import shared.Message.BattleRequest; 가 있어서 에러 안 남
                        BattleRequest req = (BattleRequest) msg.payload;
                        gameManager.processBattleAction(myId, req);
                        broadcast(new Message(Message.Type.STATE_UPDATE, gameManager.getGameState()));
                    }
                }
            } catch (Exception e) {
                System.out.println("Player " + myId + " 연결 종료");
                clients.remove(out);
            }
        }
    }
}