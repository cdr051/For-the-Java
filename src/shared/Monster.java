package shared;

import java.io.Serializable;

public class Monster implements Serializable {
    private static final long serialVersionUID = 4L;
    
    private String name;
    private int health;
    private int physicalAttack;
    private int magicalAttack;
    private int physicalDefense;
    private int magicalDefense;
    private boolean isDead = false;

    public Monster(String name, int pa, int ma, int pd, int md, int hp) {
        this.name = name;
        this.physicalAttack = pa;
        this.magicalAttack = ma;
        this.physicalDefense = pd;
        this.magicalDefense = md;
        this.health = hp;
    }

    public void scaleStats(int turnCount) {
        this.physicalAttack += 1;
        this.magicalAttack += 1;
    }
    
    public boolean takeDamage(int amount) {
        if (isDead) return true;
        this.health -= amount;
        if (this.health <= 0) {
            this.health = 0;
            this.isDead = true;
        }
        return this.isDead;
    }

    public String getName() { return name; }
    public int getHealth() { return health; }
    public int getPhysicalAttack() { return physicalAttack; }
    public int getMagicalAttack() { return magicalAttack; }
    public int getPhysicalDefense() { return physicalDefense; }
    public int getMagicalDefense() { return magicalDefense; }
    public boolean isDead() { return isDead; }
}