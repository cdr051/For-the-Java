package shared;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;

    public int[][] map;
    public List<Player> players = new ArrayList<>();
    public int currentTurnPlayerId = 0;
    
    // ⭐ [핵심 수정] 초기값을 0으로 변경 (0: 대기실, 1~: 게임시작)
    public int roundNumber = 0; 
    
    public String logMessage = "대기실 입장";
    public int teamLives = 3; 

    public boolean isBattleMode = false;
    public boolean isGameOver = false;
    
    public List<Monster> monsters = new ArrayList<>();
    public List<Integer> battleMemberIds = new ArrayList<>();
    public List<BattleUnit> battleOrder = new ArrayList<>();
    public int battleTurnIndex = 0;
    public List<String> battleLog = new ArrayList<>();

    public boolean isShopMode = false;
    public int teamGold = 100;
    public String shopWarning = "";

    public GameState() {
        map = new int[10][10];
        while (true) {
            generateRandomMap();
            if (isValidMap()) break;
        }
    }

    private void generateRandomMap() {
        for(int i=0; i<10; i++) {
            for(int j=0; j<10; j++) {
                double r = Math.random();
                if(r < 0.25) map[i][j] = 1; 
                else if(r < 0.35) map[i][j] = 2; 
                else if(r < 0.40) map[i][j] = 3; 
                else map[i][j] = 0; 
            }
        }
        map[0][0] = 0; map[0][1] = 0; map[1][0] = 0; 
        if(map[1][1] == 1) map[1][1] = 0; 
    }

    private boolean isValidMap() {
        boolean[][] visited = new boolean[10][10];
        Queue<Point> queue = new LinkedList<>();
        queue.add(new Point(0, 0));
        visited[0][0] = true;
        int count = 0;

        while (!queue.isEmpty()) {
            Point p = queue.poll();
            count++;
            int[] dx = {0, 0, -1, 1};
            int[] dy = {-1, 1, 0, 0};
            for (int i = 0; i < 4; i++) {
                int nx = p.x + dx[i];
                int ny = p.y + dy[i];
                if (nx >= 0 && nx < 10 && ny >= 0 && ny < 10) {
                    if (!visited[ny][nx] && map[ny][nx] != 1) {
                        visited[ny][nx] = true;
                        queue.add(new Point(nx, ny));
                    }
                }
            }
        }
        if (count < 20) return false;
        for(int i=0; i<10; i++) {
            for(int j=0; j<10; j++) {
                if ((map[i][j] == 2 || map[i][j] == 3) && !visited[i][j]) return false;
            }
        }
        return true;
    }

    private static class Point {
        int x, y;
        Point(int x, int y) { this.x = x; this.y = y; }
    }
}