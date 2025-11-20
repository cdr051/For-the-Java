package shared;

import java.io.Serializable;
import java.util.List;

public class BattleState implements Serializable {
    private static final long serialVersionUID = 5L;
    
    private Player playerInTurn;
    private List<Monster> monsters;

    public BattleState(Player player, List<Monster> monsters) {
        this.playerInTurn = player;
        this.monsters = monsters;
    }

    public Player getPlayerInTurn() { return playerInTurn; }
    public List<Monster> getMonsters() { return monsters; }
}