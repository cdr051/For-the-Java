package server;

import shared.*;
import shared.Message.BattleRequest;

public class GameManager {
    private GameState gameState = new GameState();
    
    // 리팩토링
    private MapManager mapManager;
    private BattleManager battleManager;

    public GameManager() {
        // 매니저 초기화 및 의존성 주입
        this.mapManager = new MapManager(gameState);
        this.battleManager = new BattleManager(gameState, this);
    }

    public synchronized GameState getGameState() { return gameState; }

    public synchronized void setPlayerName(int id, String name) {
        if (id >= 0 && id < gameState.players.size()) {
            gameState.players.get(id).name = name;
        }
    }

    // 주사위 요청 -> MapManager에게 위임
    public synchronized void rollDice(int playerId) {
        if (gameState.isBattleMode) return;
        if (gameState.currentTurnPlayerId != playerId) return;
        
        mapManager.rollDice(playerId);
    }

    // 이동 요청 -> MapManager 위임 후 전투 체크
    public synchronized void movePlayer(int playerId, int dx, int dy) {
        if (gameState.isBattleMode) return;
        if (gameState.currentTurnPlayerId != playerId) return;
        
        // 이동 시도
        boolean moved = mapManager.movePlayer(playerId, dx, dy);
        
        if (moved) {
            Player p = gameState.players.get(playerId);
            // 몬스터 타일(2)인지 체크 -> 전투 시작
            if (gameState.map[p.y][p.x] == 2) {
                battleManager.initiateBattle(p, p.x, p.y);
            }
        }
    }

    // 전투 요청 -> BattleManager에게 위임
    public synchronized void processBattleAction(int playerId, BattleRequest req) {
        if (!gameState.isBattleMode) return;
        if (gameState.currentTurnPlayerId != playerId) return;
        
        battleManager.processBattleAction(playerId, req);
    }

    // 턴 넘기기 (중앙 관리 유지)
    public synchronized void passTurn(int playerId) {
        if (gameState.currentTurnPlayerId != playerId) return;
        
        Player currentP = gameState.players.get(playerId);
        currentP.movePoints = 0; 
        currentP.hasRolled = false;

        if (gameState.isBattleMode) {
            // 전투 중 턴 순환
            int currentIndexInList = gameState.battleMemberIds.indexOf(playerId);
            if (currentIndexInList == -1) {
                gameState.currentTurnPlayerId = gameState.battleMemberIds.get(0);
            } else {
                int nextIndexInList = (currentIndexInList + 1) % gameState.battleMemberIds.size();
                gameState.currentTurnPlayerId = gameState.battleMemberIds.get(nextIndexInList);
            }
            
            Player nextP = gameState.players.get(gameState.currentTurnPlayerId);
            gameState.logMessage = String.format("⚔️ [전투] %s님의 차례입니다.", nextP.name);
        } 
        else {
            // 맵 이동 중 턴 순환
            int nextId = (gameState.currentTurnPlayerId + 1) % gameState.players.size();
            gameState.currentTurnPlayerId = nextId;
            
            if (nextId == 0) {
                gameState.roundNumber++;
                gameState.logMessage = String.format("🔔 [라운드 %d] 시작!", gameState.roundNumber);
            } else {
                Player nextP = gameState.players.get(nextId);
                gameState.logMessage = String.format("📢 %s님의 턴입니다.", nextP.name);
            }
        }
    }
}