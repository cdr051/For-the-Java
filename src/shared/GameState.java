package shared;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GameState implements Serializable {
    //충돌방지용 버전번호
    private static final long serialVersionUID = 3L;

    public static final int MAP_WIDTH = 12;
    public static final int MAP_HEIGHT = 8;

    public int[][] map;
    public List<Player> players = new ArrayList<>();
    public int currentTurnPlayerId = 0;
    public int roundNumber = 1; 
    public String logMessage = "게임 시작!";

    // 상점 경고메세지
    public String shopWarning = ""; 

    public int teamGold = 100;
    public boolean isBattleMode = false;
    public boolean isShopMode = false; 

    public List<Monster> monsters = new ArrayList<>();
    public List<Integer> battleMemberIds = new ArrayList<>();

    public GameState() {
        map = new int[MAP_HEIGHT][MAP_WIDTH];
        for(int y=0; y<MAP_HEIGHT; y++) {
            for(int x=0; x<MAP_WIDTH; x++) {
                double r = Math.random();
                if(r < 0.2) map[y][x] = 1; 
                else if(r < 0.3) map[y][x] = 2; 
                else if(r < 0.35) map[y][x] = 3; 
                else map[y][x] = 0; 
            }
        }
    }
}