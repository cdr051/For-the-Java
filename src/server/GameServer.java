package server;

import java.net.ServerSocket;
import java.net.Socket;

public class GameServer {
    private GameManager gameManager;

    public GameServer() {
        gameManager = new GameManager();
        
        try (ServerSocket serverSocket = new ServerSocket(9999)) {
            System.out.println("서버가 9999 포트에서 시작되었습니다.");
            int playerNum = 0;
            while (playerNum < 2) { // 2명까지만 받음
                Socket socket = serverSocket.accept();
                playerNum++;
                
                ClientHandler handler = new ClientHandler(socket, gameManager, playerNum);
                gameManager.addClient(handler);
                handler.start();
                System.out.println("Player " + playerNum + " 접속.");
            }
            System.out.println("2명 접속 완료. 로비 대기 중...");
        } catch (Exception e) {
            System.out.println("서버 오류: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        new GameServer();
    }
}