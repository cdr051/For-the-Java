package shared;
import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        LOGIN, SET_NAME, LOBBY_UPDATE, CHAT, CHANGE_JOB, READY, START_GAME,
        STATE_UPDATE, ROLL_DICE, MOVE_REQ, TURN_PASS,
        BATTLE_ACTION, RESET_REQ,
        
        // ⭐ [추가] 상점 관련
        SHOP_BUY,  // 구매 요청
        SHOP_EXIT  // 나가기 요청
    }
    
    // ... (BattleRequest 클래스 및 나머지 기존 코드 유지) ...
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