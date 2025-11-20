package shared;

import java.awt.Point;
import java.io.Serializable;

public class GameProtocol implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String C_MSG_READY = "C_MSG_READY";
    public static final String C_MSG_ROLL_DICE = "C_MSG_ROLL_DICE";
    public static final String C_MSG_MOVE = "C_MSG_MOVE";
    public static final String C_MSG_ATTACK = "C_MSG_ATTACK"; // 공격 명령

    public static final String S_MSG_UPDATE_LOG = "S_MSG_UPDATE_LOG";
    public static final String S_MSG_GAME_START = "S_MSG_GAME_START";
    public static final String S_MSG_DICE_RESULT = "S_MSG_DICE_RESULT";
    public static final String S_MSG_UPDATE_STATE = "S_MSG_UPDATE_STATE";
    public static final String S_MSG_INVALID_MOVE = "S_MSG_INVALID_MOVE";
    public static final String S_MSG_YOUR_TURN = "S_MSG_YOUR_TURN";
    public static final String S_MSG_BATTLE_START = "S_MSG_BATTLE_START"; // 전투 시작
    public static final String S_MSG_BATTLE_UPDATE = "S_MSG_BATTLE_UPDATE"; // 전투 상태 갱신
    public static final String S_MSG_BATTLE_END = "S_MSG_BATTLE_END";   // 전투 종료

    private String messageType;
    private Object payload;

    public GameProtocol(String messageType, Object payload) {
        this.messageType = messageType;
        this.payload = payload;
    }

    public String getMessageType() { return messageType; }
    public Object getPayload() { return payload; }
}