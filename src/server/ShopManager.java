package server;

import shared.*;

public class ShopManager {
    private GameState gameState;
    
    private static final int COST_ATK = 50;
    private static final int COST_MAXHP = 50; 
    private static final int COST_HEAL = 30;  

    public ShopManager(GameState gameState) {
        this.gameState = gameState;
    }

    public void openShop(Player p) {
        gameState.isShopMode = true;
        gameState.shopWarning = ""; // 입장 시 경고 메시지 초기화
        gameState.logMessage = String.format("🏪 %s님이 상점에 입장했습니다.", p.name);
    }

    public void exitShop(int playerId) {
        if (gameState.currentTurnPlayerId != playerId) return;
        gameState.isShopMode = false;
        gameState.shopWarning = ""; // 퇴장 시 초기화
        gameState.logMessage = "상점에서 나왔습니다.";
    }

    public void buyItem(int playerId, String itemCode) {
        if (gameState.currentTurnPlayerId != playerId) return;

        Player p = gameState.players.get(playerId);
        
        // 구매 시도 시 일단 경고 메시지 초기화
        gameState.shopWarning = "";

        // 공격력 강화
        if ("ATK".equals(itemCode)) {
            if (gameState.teamGold >= COST_ATK) {
                gameState.teamGold -= COST_ATK;
                p.attack += 5; 
                gameState.logMessage = String.format("⚔️ %s 공격력 강화 성공!", p.name);
            } else {
                gameState.shopWarning = "골드가 부족합니다!";
            }
        } 
        // 최대 체력 증가
        else if ("MAXHP".equals(itemCode)) {
            if (gameState.teamGold >= COST_MAXHP) {
                gameState.teamGold -= COST_MAXHP;
                p.maxHp += 20; 
                gameState.logMessage = String.format("💗 %s 최대 체력 증가!", p.name);
            } else {
                gameState.shopWarning = "골드가 부족합니다!";
            }
        }
        // 체력 회복
        else if ("HEAL".equals(itemCode)) {
            // 이미 풀피인지 체크
            if (p.hp >= p.maxHp) {
                gameState.shopWarning = "현재 체력이 최대입니다!"; 
                return; // 돈 안 깎고 리턴
            }

            if (gameState.teamGold >= COST_HEAL) {
                gameState.teamGold -= COST_HEAL;
                int healAmount = 30;
                p.hp = Math.min(p.maxHp, p.hp + healAmount);
                gameState.logMessage = String.format("🧪 %s 체력 회복!", p.name);
            } else {
                gameState.shopWarning = "골드가 부족합니다!"; 
            }
        }
    }
}