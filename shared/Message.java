package shared;
import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        LOGIN, SET_NAME, LOBBY_UPDATE, CHAT, CHANGE_JOB, READY, START_GAME,
        STATE_UPDATE, ROLL_DICE, MOVE_REQ, TURN_PASS,
        
        // ⭐ [추가] 전투 관련 메시지
        BATTLE_ACTION // 공격, 스킬, 도망 등
    }
    
    // 전투 액션 상세 정보 (어떤 행동을 했는지)
    public static class BattleRequest implements Serializable {
        public String action; // "ATTACK", "SKILL1", "SKILL2", "FLEE"
        public int targetIndex; // 몬스터 인덱스 (단일 공격용)
        
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