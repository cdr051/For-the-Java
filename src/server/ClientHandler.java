package server;

import shared.GameProtocol;
import shared.Player;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler extends Thread {
    private Socket socket;
    private GameManager gameManager;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private int playerIndex;
    private Player player;
    private boolean isReady = false;

    public ClientHandler(Socket socket, GameManager gameManager, int playerNum) {
        this.socket = socket;
        this.gameManager = gameManager;
        this.playerIndex = playerNum - 1;
        
        try {
            this.oos = new ObjectOutputStream(socket.getOutputStream());
            this.ois = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {}
    }

    @Override
    public void run() {
        try {
            while (true) {
                GameProtocol msg = (GameProtocol) ois.readObject();
                gameManager.handleMessage(this, msg);
            }
        } catch (Exception e) {
            gameManager.removeClient(this);
        }
    }

    public void sendMessage(GameProtocol msg) {
        try {
            oos.writeObject(msg);
            oos.flush();
            oos.reset(); 
        } catch (IOException e) {}
    }
    
    public int getPlayerIndex() { return playerIndex; }
    public boolean isReady() { return isReady; }
    public void setReady(boolean ready) { isReady = ready; }
    public void setPlayer(Player player) { this.player = player; }
    public Player getPlayer() { return this.player; }
}