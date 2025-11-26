package client;

import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        PLAYER_JOIN,
        PLAYER_LIST,
        REQUEST_PLAYER_LIST,
        CHAT,
        GAME_START,
        YOUR_TURN,
        SUBMIT_TILE, // 타일 제출
        OPPONENT_SUBMITTED,
        UPDATE_OPP_COUNTS, //상대 남은 패 갯수

        ROUND_RESULT,
        GAME_OVER
    }

    private Type type;
    private Object data;

    public Message(Type type, Object data) {
        this.type = type;
        this.data = data;
    }

    public Type getType() { return type; }
    public Object getData() { return data; }
}