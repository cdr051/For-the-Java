package shared;
import java.io.Serializable;

public class BattleUnit implements Serializable, Comparable<BattleUnit> {
    private static final long serialVersionUID = 1L;

    public boolean isMonster;
    public int id;
    public String name;
    public int speed;

    public BattleUnit(boolean isMonster, int id, String name, int speed) {
        this.isMonster = isMonster;
        this.id = id;
        this.name = name;
        this.speed = speed;
    }

    @Override
    public int compareTo(BattleUnit o) {
        // 속도 내림차순 정렬 (속도 빠른 순서대로)
        return o.speed - this.speed; 
    }
}