package shared;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;

    public final int MAP_COLS = GameConfig.MAP_COLS; // 가로
    public final int MAP_ROWS = GameConfig.MAP_ROWS; // 세로
    public int[][] map;
    
    public List<Player> players = new ArrayList<>();
    public int currentTurnPlayerId = 0;
    public int roundNumber = 0; 
    public String logMessage = "대기실 입장";
    public int teamLives = GameConfig.INITIAL_TEAM_LIVES;

    public boolean isBattleMode = false;
    public boolean isGameOver = false;
    
    public List<Monster> monsters = new ArrayList<>();
    public List<Integer> battleMemberIds = new ArrayList<>();
    public List<BattleUnit> battleOrder = new ArrayList<>();
    public int battleTurnIndex = 0;
    public List<String> battleLog = new ArrayList<>();

    public boolean isShopMode = false;
    public int teamGold = GameConfig.INITIAL_TEAM_GOLD;
    public String shopWarning = "";

    public GameState() {
        map = new int[MAP_ROWS][MAP_COLS];
        int attempts = 0;
        int maxAttempts = 100; // 무한 루프 방지
        while (attempts < maxAttempts) {
            generateRandomMap();
            if (isValidMap()) break;
            attempts++;
        }
        if (attempts >= maxAttempts) {
            System.err.println("⚠️ 유효한 맵 생성 실패, 기본 맵 사용");
            generateDefaultMap();
        }
    }
    
    private void generateDefaultMap() {
        // 기본 맵 생성 (모두 잔디)
        for(int i=0; i<MAP_ROWS; i++) {
            for(int j=0; j<MAP_COLS; j++) {
                map[i][j] = GameConfig.TILE_GRASS;
            }
        }
    }
    
    /**
     * 플레이어 ID로 플레이어 찾기
     */
    public Player getPlayer(int id) {
        if (id < 0 || id >= players.size()) return null;
        return players.get(id);
    }

    private void generateRandomMap() {
        for(int i=0; i<MAP_ROWS; i++) {
            for(int j=0; j<MAP_COLS; j++) {
                double r = Math.random();
                if(r < GameConfig.MAP_WATER_PROBABILITY) {
                    map[i][j] = GameConfig.TILE_WATER; 
                } else if(r < GameConfig.MAP_MONSTER_PROBABILITY) {
                    map[i][j] = GameConfig.TILE_MONSTER; 
                } else if(r < GameConfig.MAP_SHOP_PROBABILITY) {
                    map[i][j] = GameConfig.TILE_SHOP; 
                } else {
                    map[i][j] = GameConfig.TILE_GRASS; 
                }
            }
        }
        // 시작 위치는 항상 잔디
        map[0][0] = GameConfig.TILE_GRASS;
        map[0][1] = GameConfig.TILE_GRASS;
        map[1][0] = GameConfig.TILE_GRASS;
        if(map[1][1] == GameConfig.TILE_WATER) {
            map[1][1] = GameConfig.TILE_GRASS;
        }
    }

    private boolean isValidMap() {
        boolean[][] visited = new boolean[MAP_ROWS][MAP_COLS];
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
                if (nx >= 0 && nx < MAP_COLS && ny >= 0 && ny < MAP_ROWS) {
                    if (!visited[ny][nx] && map[ny][nx] != GameConfig.TILE_WATER) {
                        visited[ny][nx] = true;
                        queue.add(new Point(nx, ny));
                    }
                }
            }
        }
        // 최소 접근 가능한 타일 수 체크
        if (count < GameConfig.MAP_MIN_ACCESSIBLE_TILES) return false;
        
        // 몬스터나 상점이 갈 수 없는 곳에 있으면 안됨
        for(int i=0; i<MAP_ROWS; i++) {
            for(int j=0; j<MAP_COLS; j++) {
                if ((map[i][j] == GameConfig.TILE_MONSTER || 
                     map[i][j] == GameConfig.TILE_SHOP) && !visited[i][j]) {
                    return false;
                }
            }
        }
        return true;
    }

    private static class Point {
        int x, y;
        Point(int x, int y) { this.x = x; this.y = y; }
    }
}