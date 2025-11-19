package server;

import client.Message; // shared 패키지로 빼도 됨

import java.io.*;
import java.net.Socket;

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
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            // 첫 수신은 플레이어 이름
            Object first = in.readObject();
            if (first instanceof String name) {
                this.playerName = name;
                System.out.println("[SERVER] 플레이어 접속: " + name);

                // 서버 전체에 접속 알림
                server.broadcast(
                        new Message(Message.Type.PLAYER_JOIN, name)
                );

                // 모든 클라이언트에게 현재 참가자 목록 전달
                server.broadcastPlayerList();
            }

            // 메시지 처리 루프
            while (true) {
                Object msg = in.readObject();

                // 문자열이면 채팅
                if (msg instanceof String s) {
                    server.broadcast(
                            new Message(Message.Type.CHAT, playerName + ": " + s)
                    );
                }

                // Message 객체면 타입 분석
                else if (msg instanceof Message m) {
                    handleMessage(m);
                }
            }

        } catch (Exception e) {
            System.out.println("[SERVER] " + playerName + " 연결 종료");
        } finally {
            server.removeClient(this);
            server.broadcastPlayerList();
        }
    }

    private void handleMessage(Message msg) {
        switch (msg.getType()) {

            case REQUEST_PLAYER_LIST ->
                    server.sendPlayerListToOne(this);

            default ->
                    System.out.println("[SERVER] Unknown message: " + msg.getType());
        }
    }

    public void send(Message msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            System.out.println("[SERVER] 전송 오류");
        }
    }

    public String getPlayerName() {
        return playerName;
    }
}
