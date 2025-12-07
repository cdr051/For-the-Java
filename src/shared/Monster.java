package shared;
import java.io.Serializable;

public class Monster implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public int id;
    public String name;
    public int hp;
    public int maxHp;
    public boolean isDead = false;
    
    public int attack;
    public int speed;
    
    // ⭐ [신규] 디버프 상태
    public int atkDebuffTurns = 0; // 도적 '연막탄' 효과 남은 턴

    public Monster(int id, String name, int hp, int attack, int speed) {
        this.id = id;
        this.name = name;
        this.hp = hp;
        this.maxHp = hp;
        this.attack = attack;
        this.speed = speed;
    }
}