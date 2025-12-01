package shared;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList; // BFS용
import java.util.List;
import java.util.Queue;      // BFS용

public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;

    public int[][] map;
    public List<Player> players = new ArrayList<>();
    public int currentTurnPlayerId = 0;
    public int roundNumber = 1;
    public String logMessage = "게임 시작!";

    // 전투 관련
    public boolean isBattleMode = false;
    public List<Monster> monsters = new ArrayList<>();
    public List<Integer> battleMemberIds = new ArrayList<>();
    public List<BattleUnit> battleOrder = new ArrayList<>();
    public int battleTurnIndex = 0;
    public List<String> battleLog = new ArrayList<>();

    public GameState() {
        map = new int[10][10];
        
        // ⭐ [핵심] 유효한 맵이 나올 때까지 계속 다시 만듦
        while (true) {
            generateRandomMap();
            if (isValidMap()) {
                break; // 맵이 괜찮으면 확정
            }
            // 아니면 루프 돌면서 다시 생성
        }
    }

    // 1. 랜덤 맵 생성 (기존 로직 + 안전 구역)
    private void generateRandomMap() {
        for(int i=0; i<10; i++) {
            for(int j=0; j<10; j++) {
                double r = Math.random();
                if(r < 0.25) map[i][j] = 1;      // 물 (확률 25%)
                else if(r < 0.35) map[i][j] = 2; // 몬스터 (확률 10%)
                else map[i][j] = 0;              // 평지 (나머지)
            }
        }

        // ⭐ 시작 지점 안전 구역 확보 (플레이어 갇힘 방지)
        map[0][0] = 0; // 시작점
        map[0][1] = 0; // 우측
        map[1][0] = 0; // 하단
        // (1,1)은 선택사항이지만 열어두면 좋음
        if(map[1][1] == 1) map[1][1] = 0; 
    }

    // 2. 맵 검증 로직 (BFS 알고리즘 사용)
    private boolean isValidMap() {
        boolean[][] visited = new boolean[10][10];
        Queue<Point> queue = new LinkedList<>();
        
        // 시작점(0,0)에서 출발
        queue.add(new Point(0, 0));
        visited[0][0] = true;

        int reachableLandCount = 0;

        // BFS 탐색: 갈 수 있는 땅을 모두 방문
        while (!queue.isEmpty()) {
            Point p = queue.poll();
            reachableLandCount++;

            // 상하좌우 확인
            int[] dx = {0, 0, -1, 1};
            int[] dy = {-1, 1, 0, 0};

            for (int i = 0; i < 4; i++) {
                int nx = p.x + dx[i];
                int ny = p.y + dy[i];

                // 맵 범위 안이고
                if (nx >= 0 && nx < 10 && ny >= 0 && ny < 10) {
                    // 방문 안 했고, 물(1)이 아니면 이동 가능
                    if (!visited[ny][nx] && map[ny][nx] != 1) {
                        visited[ny][nx] = true;
                        queue.add(new Point(nx, ny));
                    }
                }
            }
        }

        // 검증 1: 맵이 너무 좁으면(갈 수 있는 땅이 20칸 미만) 실패
        if (reachableLandCount < 20) return false;

        // 검증 2: 모든 몬스터(2) 타일에 도달할 수 있는가?
        for(int i=0; i<10; i++) {
            for(int j=0; j<10; j++) {
                if (map[i][j] == 2) {
                    // 몬스터가 있는데 방문을 못 했다면 -> 고립된 몬스터임
                    if (!visited[i][j]) return false;
                }
            }
        }

        return true; // 모든 조건 통과
    }

    // BFS용 간단한 좌표 클래스 (내부 클래스)
    private static class Point {
        int x, y;
        Point(int x, int y) { this.x = x; this.y = y; }
    }
}