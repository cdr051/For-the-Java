package shared;

import java.io.Serializable;

public class Player implements Serializable {
    private static final long serialVersionUID = 2L;
    
    private String playerName;
    private String className;

    private int physicalAttack, magicalAttack, physicalDefense, magicalDefense, health;
    private int x, y;
    private int lastDiceRoll = 0;
    private boolean isDead = false;

    public Player(String name, String className, int pa, int ma, int pd, int md, int hp) {
        this.playerName = name;
        this.className = className;
        this.physicalAttack = pa;
        this.magicalAttack = ma;
        this.physicalDefense = pd;
        this.magicalDefense = md;
        this.health = hp;
        this.x = 0; 
        this.y = 5; 
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
    public String getPlayerName() { return playerName; }
    public int getPhysicalAttack() { return physicalAttack; }
    public int getMagicalAttack() { return magicalAttack; }
    public int getPhysicalDefense() { return physicalDefense; }
    public int getMagicalDefense() { return magicalDefense; }
    public int getHealth() { return health; }
    public boolean isDead() { return isDead; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getLastDiceRoll() { return lastDiceRoll; }

    public void setPosition(int x, int y) { this.x = x; this.y = y; }
    public void setLastDiceRoll(int roll) { this.lastDiceRoll = roll; }
}