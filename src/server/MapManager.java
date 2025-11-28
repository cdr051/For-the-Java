package server;

import shared.*;
import java.util.Random;

public class MapManager {
    private GameState gameState;

    public MapManager(GameState gameState) {
        this.gameState = gameState;
    }

    // 🎲 주사위 굴리기
    public void rollDice(int playerId) {
        Player p = gameState.players.get(playerId);
        
        // 검증 로직
        if (p.hasRolled) {
            gameState.logMessage = "🚫 이미 주사위를 굴렸습니다. 이동하거나 턴을 넘기세요.";
            return;
        }
        if (p.movePoints > 0) return; 

        Random rand = new Random();
        int dice = rand.nextInt(6) + 1; 
        
        p.movePoints = dice;
        p.hasRolled = true; 
        
        gameState.logMessage = String.format("🎲 %s 주사위 결과: %d", p.name, dice);
    }

    // 🏃 플레이어 이동 (성공 여부 반환)
    public boolean movePlayer(int playerId, int dx, int dy) {
        Player p = gameState.players.get(playerId);

        // 1. 이동력 체크
        if (p.movePoints <= 0) {
            gameState.logMessage = "🚫 이동력이 부족합니다!";
            return false;
        }

        int newX = p.x + dx;
        int newY = p.y + dy;

        // 2. 맵 범위 체크
        if (newX < 0 || newX >= GameState.MAP_WIDTH || newY < 0 || newY >= GameState.MAP_HEIGHT) return false;
        
        // 3. 지형 체크 (물)
        if (gameState.map[newY][newX] == 1) {
            gameState.logMessage = "🌊 물에는 들어갈 수 없습니다.";
            return false; 
        }

        // 4. 이동 수행
        p.x = newX;
        p.y = newY;
        p.movePoints--;
        
        return true; // 이동 성공
    }
}