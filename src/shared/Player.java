package shared;
import java.io.Serializable;
import java.awt.Color;

public class Player implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // 기본 정보
    public int id;
    public String name;
    public Color color;
    
    // 위치 및 상태 (기존 코드 호환성을 위해 public 유지, 검증은 별도 메서드로)
    public int x, y;
    public int hp; 
    public String jobClass;
    public boolean isHost;
    public boolean isReady;
    public int movePoints = 0; 
    public boolean hasRolled = false;
    public int gold = 100;

    // 기본 스탯
    public int baseMaxHp;
    public int baseAttack;
    public int baseSpeed;

    // 보너스 스탯
    public int bonusMaxHp = 0;
    public int bonusAttack = 0;
    public int bonusSpeed = 0;

    // 전투 상태 변수
    public int shieldStacks = 0;   // 기사: 방어 횟수
    public int atkBuffTurns = 0;   // 궁수: 공격력 버프 남은 턴
    
    // 검증이 포함된 setter 메서드들 (선택적 사용)
    public void setHpWithValidation(int hp) {
        if (hp < 0) hp = 0;
        int maxHp = getTotalMaxHp();
        if (hp > maxHp) hp = maxHp;
        this.hp = hp;
    }
    
    public void setMovePointsWithValidation(int movePoints) {
        if (movePoints < 0) movePoints = 0;
        this.movePoints = movePoints;
    }
    
    public void setPositionWithValidation(int x, int y) {
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        this.x = x;
        this.y = y;
    }

    public Player(int id, String name, Color color) {
        this.id = id;
        this.name = (name != null && !name.trim().isEmpty()) ? name : "Player " + (id + 1);
        this.color = color != null ? color : Color.BLUE;
        this.jobClass = GameConfig.JOB_KNIGHT;
        this.isHost = false;
        this.isReady = false;
        this.gold = GameConfig.INITIAL_PLAYER_GOLD;
        
        updateStatsByJob(); 
        this.hp = getTotalMaxHp(); 
    }

    public void updateStatsByJob() {
        switch (jobClass) {
            case GameConfig.JOB_KNIGHT:
                this.baseMaxHp = GameConfig.KNIGHT_BASE_HP;
                this.baseAttack = GameConfig.KNIGHT_BASE_ATK;
                this.baseSpeed = GameConfig.KNIGHT_BASE_SPD;
                break;
            case GameConfig.JOB_MAGE:
                this.baseMaxHp = GameConfig.MAGE_BASE_HP;
                this.baseAttack = GameConfig.MAGE_BASE_ATK;
                this.baseSpeed = GameConfig.MAGE_BASE_SPD;
                break;
            case GameConfig.JOB_ARCHER:
                this.baseMaxHp = GameConfig.ARCHER_BASE_HP;
                this.baseAttack = GameConfig.ARCHER_BASE_ATK;
                this.baseSpeed = GameConfig.ARCHER_BASE_SPD;
                break;
            case GameConfig.JOB_ROGUE:
                this.baseMaxHp = GameConfig.ROGUE_BASE_HP;
                this.baseAttack = GameConfig.ROGUE_BASE_ATK;
                this.baseSpeed = GameConfig.ROGUE_BASE_SPD;
                break;
            default: 
                // 기본값은 기사 스탯
                this.baseMaxHp = GameConfig.KNIGHT_BASE_HP;
                this.baseAttack = GameConfig.KNIGHT_BASE_ATK;
                this.baseSpeed = GameConfig.KNIGHT_BASE_SPD;
        }
        if (this.hp > getTotalMaxHp()) this.hp = getTotalMaxHp();
    }

    public int getTotalMaxHp() { return baseMaxHp + bonusMaxHp; }
    
    public int getTotalSpeed() { return baseSpeed + bonusSpeed; }
    
    // 공격력 계산 시 버프 적용
    public int getTotalAttack() { 
        int total = baseAttack + bonusAttack;
        if (atkBuffTurns > 0) {
            total *= GameConfig.PLAYER_ATK_BUFF_MULTIPLIER;
        }
        return total;
    }
}