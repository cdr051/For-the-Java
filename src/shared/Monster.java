package shared;
import java.io.Serializable;

public class Monster implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public int id;
    public String name;
    public int hp;
    public int maxHp;
    public boolean isDead = false;

    public Monster(int id, String name, int hp) {
        this.id = id;
        this.name = name;
        this.hp = hp;
        this.maxHp = hp;
    }
}