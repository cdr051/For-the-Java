package server;

import shared.*;
import shared.Message.BattleRequest;

public class GameManager {
    private GameState gameState = new GameState();
    
    // 매니저들
    private MapManager mapManager;
    private BattleManager battleManager;
    private ShopManager shopManager; // 상점 매니저

    public GameManager() {
        this.mapManager = new MapManager(gameState);
        this.battleManager = new BattleManager(gameState, this);
        this.shopManager = new ShopManager(gameState); // 매니저 초기화
    }

    public synchronized GameState getGameState() { return gameState; }

    public synchronized void setPlayerName(int id, String name) {
        if (id >= 0 && id < gameState.players.size()) {
            gameState.players.get(id).name = name;
        }
    }

    public synchronized void rollDice(int playerId) {
        if (gameState.isBattleMode || gameState.isShopMode) return;
        if (gameState.currentTurnPlayerId != playerId) return;
        
        mapManager.rollDice(playerId);
    }

    public synchronized void movePlayer(int playerId, int dx, int dy) {
        if (gameState.isBattleMode || gameState.isShopMode) return;
        if (gameState.currentTurnPlayerId != playerId) return;
        
        boolean moved = mapManager.movePlayer(playerId, dx, dy);
        
        if (moved) {
            Player p = gameState.players.get(playerId);
            int tileType = gameState.map[p.y][p.x];
            
            if (tileType == 2) { // 몬스터
                battleManager.initiateBattle(p, p.x, p.y);
            } 
            else if (tileType == 3) { // 상점
                shopManager.openShop(p);
            }
        }
    }

    // 상점 나가기
    public synchronized void exitShop(int playerId) {
        if (!gameState.isShopMode) return;
        shopManager.exitShop(playerId);
    }

    // ⭐ [6번 내용] 구매 요청 처리 (여기에 추가됨)
    public synchronized void buyItem(int playerId, String itemCode) {
        if (!gameState.isShopMode) return;
        shopManager.buyItem(playerId, itemCode);
    }

    public synchronized void processBattleAction(int playerId, BattleRequest req) {
        if (!gameState.isBattleMode) return;
        if (gameState.currentTurnPlayerId != playerId) return;
        
        battleManager.processBattleAction(playerId, req);
    }

    public synchronized void passTurn(int playerId) {
        if (gameState.currentTurnPlayerId != playerId) return;
        
        Player currentP = gameState.players.get(playerId);
        currentP.movePoints = 0; 
        currentP.hasRolled = false;

        if (gameState.isBattleMode) {
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