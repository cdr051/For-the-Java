package shared;
import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        LOGIN, SET_NAME, LOBBY_UPDATE, CHAT, CHANGE_JOB, READY, START_GAME,
        STATE_UPDATE, ROLL_DICE, MOVE_REQ, TURN_PASS,
        
        // 전투 관련
        BATTLE_ACTION, 
        SHOP_BUY, SHOP_EXIT,
        GAME_OVER
    }
    
    // 전투 액션 상세 정보
    public static class BattleRequest implements Serializable {
        public String action; 
        public int targetIndex; 
        
        public BattleRequest(String action, int targetIndex) {
            this.action = action;
            this.targetIndex = targetIndex;
        }
    }

    public Type type;
    public Object payload;

    public Message(Type type, Object payload) {
        this.type = type;
        this.payload = payload;
    }
}