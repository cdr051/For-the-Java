package shared;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GameState implements Serializable {
    private static final long serialVersionUID = 3L;

    private TileType[][] mapData;
    private List<Player> players;
    private int currentPlayerTurn; // 0=P1, 1=P2
    private boolean isBattleActive = false; // 전투 중인지 여부
    private BattleState currentBattle; // 현재 전투 상태

    public GameState() {
        this.players = new ArrayList<>();
        this.currentPlayerTurn = 0;
    }
    
    public void setMap(TileType[][] mapData) { this.mapData = mapData; }
    public void addPlayer(Player player) { this.players.add(player); }
    public void nextTurn() {
        this.currentPlayerTurn = (this.currentPlayerTurn + 1) % players.size();
    }
    
    // 전투 상태 관리
    public void setBattle(BattleState battle) {
        this.isBattleActive = (battle != null);
        this.currentBattle = battle;
    }
    
    // Getters
    public TileType[][] getMap() { return this.mapData; }
    public List<Player> getPlayers() { return this.players; }
    public int getCurrentPlayerTurn() { return this.currentPlayerTurn; }
    public Player getCurrentPlayer() { return this.players.get(currentPlayerTurn); }
    public TileType getTileAt(int x, int y) { return mapData[y][x]; }
    public boolean isBattleActive() { return isBattleActive; }
    public BattleState getCurrentBattle() { return currentBattle; }

    // Setters
    public void setTileAt(int x, int y, TileType type) { mapData[y][x] = type; }
}