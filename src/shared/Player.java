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
    public int movePoints = 0; 
    public boolean hasRolled = false;

    // 능력치 변수 추가
    public int hp;
    public int maxHp;  // 최대 체력
    public int attack; // 공격력

    public Player(int id, String name, Color color) {
        this.id = id;
        this.name = name;
        this.color = color;
        
        // 초기 능력치 설정
        this.maxHp = 100;
        this.hp = 100;
        this.attack = 10; // 기본 공격력 10
        
        this.x = 0; 
        this.y = 0;
        this.jobClass = "기사";
        this.isHost = false;
        this.isReady = false;
        this.movePoints = 0;
        this.hasRolled = false;
    }
}