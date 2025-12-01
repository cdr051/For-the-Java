package shared;
import java.io.Serializable;

public class Monster implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public int id; // 0, 1 ...
    public String name;
    public int hp;
    public int maxHp;
    public boolean isDead = false;
    
    // ⭐ [추가] 몬스터 스탯
    public int attack;
    public int speed;

    public Monster(int id, String name, int hp, int attack, int speed) {
        this.id = id;
        this.name = name;
        this.hp = hp;
        this.maxHp = hp;
        this.attack = attack;
        this.speed = speed;
    }
}