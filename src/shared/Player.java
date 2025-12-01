package shared;
import java.io.Serializable;
import java.awt.Color;

public class Player implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public int id;
    public String name;
    public int x, y;
    public Color color;
    public String jobClass;
    public boolean isHost;
    public boolean isReady;
    
    // 이동 관련
    public int movePoints = 0; 
    public boolean hasRolled = false;
    public int hp; 

    // 기본 스탯
    public int baseMaxHp;
    public int baseAttack;
    public int baseSpeed;

    // 보너스 스탯
    public int bonusMaxHp = 0;
    public int bonusAttack = 0;
    public int bonusSpeed = 0;

    public Player(int id, String name, Color color) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.jobClass = "기사"; 
        
        updateStatsByJob(); 
        this.hp = getTotalMaxHp(); 
    }

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
        if (this.hp > getTotalMaxHp()) this.hp = getTotalMaxHp();
    }

    public int getTotalMaxHp() { return baseMaxHp + bonusMaxHp; }
    public int getTotalAttack() { return baseAttack + bonusAttack; }
    public int getTotalSpeed() { return baseSpeed + bonusSpeed; }
}