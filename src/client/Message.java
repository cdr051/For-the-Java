package client;

import java.io.Serializable;

public class Message implements Serializable {

    public enum Type {
        // 로비 관련
        PLAYER_JOIN,
        PLAYER_LIST,
        REQUEST_PLAYER_LIST,

        // 채팅
        CHAT,

        // 게임 관련 기본 구조
        PLAYER_MOVE,
        TURN_CHANGE,
        BATTLE_RESULT,
        GAME_STATE
    }

    private Type type;
    private Object data;

    public Message(Type type, Object data) {
        this.type = type;
        this.data = data;
    }

    public Type getType() {
        return type;
    }

    public Object getData() {
        return data;
    }
}
