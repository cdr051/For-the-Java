package client;

import shared.GameProtocol;
import shared.GameState;
import shared.BattleState;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class NetworkManager {
    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private ClientApp clientApp;

    public NetworkManager(ClientApp clientApp) {
        this.clientApp = clientApp;
    }

    public void connect(String ip, int port) {
        try {
            socket = new Socket(ip, port);
            oos = new ObjectOutputStream(socket.getOutputStream());
            ois = new ObjectInputStream(socket.getInputStream());
            clientApp.updateLog("서버에 성공적으로 연결되었습니다.");
            new Thread(this::receiveMessages).start();
        } catch (IOException e) {
            clientApp.updateLog("서버 연결 실패: " + e.getMessage());
        }
    }

    private void receiveMessages() {
        try {
            while (true) {
                GameProtocol msg = (GameProtocol) ois.readObject();
                String type = msg.getMessageType();
                
                if (type.equals(GameProtocol.S_MSG_UPDATE_LOG)) {
                    clientApp.updateLog(msg.getPayload().toString());
                } else if (type.equals(GameProtocol.S_MSG_GAME_START)) {
                    clientApp.showGameMap((GameState) msg.getPayload());
                } else if (type.equals(GameProtocol.S_MSG_UPDATE_STATE)) {
                    clientApp.updateGameState((GameState) msg.getPayload());
                } else if (type.equals(GameProtocol.S_MSG_DICE_RESULT)) {
                    clientApp.onDiceRolled((int) msg.getPayload());
                } else if (type.equals(GameProtocol.S_MSG_YOUR_TURN)) {
                    clientApp.onTurnStarted(msg.getPayload().toString());
                } else if (type.equals(GameProtocol.S_MSG_INVALID_MOVE)) {
                    clientApp.updateLog(msg.getPayload().toString());
                } else if (type.equals(GameProtocol.S_MSG_BATTLE_START)) {
                    clientApp.onBattleStart((BattleState) msg.getPayload());
                } else if (type.equals(GameProtocol.S_MSG_BATTLE_UPDATE)) {
                    clientApp.onBattleUpdate((BattleState) msg.getPayload());
                } else if (type.equals(GameProtocol.S_MSG_BATTLE_END)) {
                    clientApp.onBattleEnd(msg.getPayload().toString());
                }
            }
        } catch (Exception e) {
            clientApp.updateLog("서버와의 연결이 끊겼습니다.");
        }
    }

    public void sendMessage(GameProtocol msg) {
        if (oos == null) {
            clientApp.updateLog("오류: 서버에 연결되어 있지 않습니다.");
            return;
        }

        try {
            oos.writeObject(msg);
            oos.flush();
            oos.reset();
        } catch (IOException e) {
            clientApp.updateLog("메시지 전송 실패: " + e.getMessage());
        }
    }
}