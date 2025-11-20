package shared;

import java.io.Serializable;

public class AttackInfo implements Serializable {
    private static final long serialVersionUID = 6L;
    
    private AttackType attackType;
    private int targetIndex;

    public AttackInfo(AttackType attackType, int targetIndex) {
        this.attackType = attackType;
        this.targetIndex = targetIndex;
    }

    public AttackType getAttackType() { return attackType; }
    public int getTargetIndex() { return targetIndex; }
}