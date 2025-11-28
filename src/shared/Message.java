package shared;
import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        LOGIN, SET_NAME, LOBBY_UPDATE, CHAT, CHANGE_JOB, READY, START_GAME,
        STATE_UPDATE, ROLL_DICE, MOVE_REQ, TURN_PASS,
        BATTLE_ACTION,
        
        SHOP_EXIT,SHOP_BUY 
    }
    
    public static class BattleRequest implements Serializable {
        public String action; 
        public int targetIndex; 
        
        public BattleRequest(String action, int targetIndex) {
            this.action = action;
            this.targetIndex = targetIndex;
        }
    }

    public Type type;
    public Object payload; // SHOP_BUY일 때 "ATK" 또는 "HP" 문자열 전송

    public Message(Type type, Object payload) {
        this.type = type;
        this.payload = payload;
    }
}