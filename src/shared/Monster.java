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
        this.name = (name != null && !name.trim().isEmpty()) ? name : "Monster " + id;
        this.hp = Math.max(1, hp); // 최소 1
        this.maxHp = Math.max(1, hp);
        this.attack = Math.max(1, attack); // 최소 1
        this.speed = Math.max(1, speed); // 최소 1
    }
    
    /**
     * 체력 설정 (검증 포함)
     */
    public void setHp(int hp) {
        if (hp < 0) hp = 0;
        if (hp > maxHp) hp = maxHp;
        this.hp = hp;
        if (hp <= 0) this.isDead = true;
    }
    
    /**
     * 데미지 받기
     */
    public void takeDamage(int damage) {
        if (damage < 0) damage = 0;
        setHp(this.hp - damage);
    }
}