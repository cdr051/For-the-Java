package shared;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;

    public int[][] map;
    public List<Player> players = new ArrayList<>();
    public int currentTurnPlayerId = 0;
    public int roundNumber = 1; 
    public String logMessage = "게임 시작!";

    // ⭐ [추가] 전투 관련 필드
    public boolean isBattleMode = false;       // 현재 전투 중인가?
    public List<Monster> monsters = new ArrayList<>(); // 현재 전투 중인 몬스터들
    public List<Integer> battleMemberIds = new ArrayList<>(); // 전투에 참여한 플레이어 ID 목록

    public GameState() {
        map = new int[10][10];
        // 맵 생성 (몬스터 타일: 2)
        for(int i=0; i<10; i++) {
            for(int j=0; j<10; j++) {
                if(Math.random() < 0.2) map[i][j] = 1; 
                else if(Math.random() < 0.1) map[i][j] = 2; // 🔴 몬스터 타일
                else map[i][j] = 0; 
            }
        }
    }
}