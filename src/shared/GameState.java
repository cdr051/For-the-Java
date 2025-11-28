package shared;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;

    // ⭐ [수정] 맵 크기 상수 정의 (가로 12, 세로 8)
    public static final int MAP_WIDTH = 12;
    public static final int MAP_HEIGHT = 8;

    public int[][] map;
    public List<Player> players = new ArrayList<>();
    public int currentTurnPlayerId = 0;
    public int roundNumber = 1; 
    public String logMessage = "게임 시작!";

    public boolean isBattleMode = false;
    public List<Monster> monsters = new ArrayList<>();
    public List<Integer> battleMemberIds = new ArrayList<>();

    public GameState() {
        // ⭐ [수정] 배열 크기 변경 [세로][가로]
        map = new int[MAP_HEIGHT][MAP_WIDTH];
        
        // 맵 생성
        for(int y=0; y<MAP_HEIGHT; y++) {
            for(int x=0; x<MAP_WIDTH; x++) {
                if(Math.random() < 0.2) map[y][x] = 1; // 물
                else if(Math.random() < 0.1) map[y][x] = 2; // 몬스터
                else map[y][x] = 0; // 평지
            }
        }
    }
}