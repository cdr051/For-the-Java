package shared;
import java.io.Serializable;
import java.awt.Color;

public class Player implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public int id;
    public String name;
    public int x, y;
    public int hp;
    public Color color;
    public String jobClass;
    public boolean isHost;
    public boolean isReady;
    public int movePoints = 0; 
    
    // ⭐ [추가] 이번 턴에 주사위를 굴렸는지 체크하는 변수
    public boolean hasRolled = false;

    public Player(int id, String name, Color color) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.hp = 100;
        this.x = 0; 
        this.y = 0;
        this.jobClass = "기사";
        this.isHost = false;
        this.isReady = false;
        this.movePoints = 0;
        this.hasRolled = false; // 초기화
    }
}