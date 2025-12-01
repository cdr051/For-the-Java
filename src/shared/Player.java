package shared;
import java.io.Serializable;
import java.awt.Color;

public class Player implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public int id;
    public String name;
    public int x, y;
    public int hp; // 현재 체력
    public Color color;
    public String jobClass;
    public boolean isHost;
    public boolean isReady;
    public int movePoints = 0; 
    public boolean hasRolled = false;

    // ⭐ [추가] 골드 (상점용)
    public int gold = 100; // 시작 자금

    // ⭐ [수정] 기본 스탯 (직업에 따라 변함)
    public int baseMaxHp;
    public int baseAttack;
    public int baseSpeed;

    // ⭐ [수정] 보너스 스탯 (상점/아이템으로 영구 증가, 직업 바꿔도 유지됨)
    public int bonusMaxHp = 0;
    public int bonusAttack = 0;
    public int bonusSpeed = 0;

    public Player(int id, String name, Color color) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.jobClass = "기사";
        this.isHost = false;
        this.isReady = false;
        
        // 스탯 초기화
        updateStatsByJob(); 
        this.hp = getTotalMaxHp(); // 체력 꽉 채우기
    }

    // 직업별 기본 스탯 설정
    public void updateStatsByJob() {
        switch (jobClass) {
            case "기사":
                this.baseMaxHp = 100; this.baseAttack = 10; this.baseSpeed = 5;
                break;
            case "마법사":
                this.baseMaxHp = 60; this.baseAttack = 40; this.baseSpeed = 10;
                break;
            case "궁수":
                this.baseMaxHp = 80; this.baseAttack = 20; this.baseSpeed = 15;
                break;
            case "도적":
                this.baseMaxHp = 70; this.baseAttack = 30; this.baseSpeed = 20;
                break;
            default: 
                this.baseMaxHp = 100; this.baseAttack = 10; this.baseSpeed = 5;
        }
        
        // 직업 변경 시 현재 체력이 새 최대 체력을 넘지 않게 조정
        if (this.hp > getTotalMaxHp()) {
            this.hp = getTotalMaxHp();
        }
    }

    // ⭐ [핵심] 외부에서 사용할 최종 스탯 (기본 + 보너스)
    public int getTotalMaxHp() { return baseMaxHp + bonusMaxHp; }
    public int getTotalAttack() { return baseAttack + bonusAttack; }
    public int getTotalSpeed() { return baseSpeed + bonusSpeed; }
}