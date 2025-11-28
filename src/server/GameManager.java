package server;

import shared.*;
import shared.Message.BattleRequest;
import java.util.*;

public class GameManager {
    private GameState gameState = new GameState();

    private int battleTileX = -1;
    private int battleTileY = -1;

    public synchronized GameState getGameState() { return gameState; }

    public synchronized void setPlayerName(int id, String name) {
        if (id >= 0 && id < gameState.players.size()) {
            gameState.players.get(id).name = name;
        }
    }

    public synchronized void rollDice(int playerId) {
        if (gameState.isBattleMode) return;
        if (gameState.currentTurnPlayerId != playerId) return;
        
        Player p = gameState.players.get(playerId);
        
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

    public synchronized void movePlayer(int playerId, int dx, int dy) {
        if (gameState.isBattleMode) return;
        if (gameState.currentTurnPlayerId != playerId) return;
        
        Player p = gameState.players.get(playerId);

        if (p.movePoints <= 0) {
            gameState.logMessage = "🚫 이동력이 부족합니다!";
            return;
        }

        int newX = p.x + dx;
        int newY = p.y + dy;

        // ⭐ [수정] 맵 범위 체크를 GameState 상수로 변경 (12x8 대응)
        if (newX < 0 || newX >= GameState.MAP_WIDTH || newY < 0 || newY >= GameState.MAP_HEIGHT) return;
        
        if (gameState.map[newY][newX] == 1) {
            gameState.logMessage = "🌊 물에는 들어갈 수 없습니다.";
            return; 
        }

        p.x = newX;
        p.y = newY;
        p.movePoints--;

        if (gameState.map[newY][newX] == 2) {
            initiateBattle(p, newX, newY);
        }
    }

    private void initiateBattle(Player triggerPlayer, int x, int y) {
        this.battleTileX = x;
        this.battleTileY = y;
        
        gameState.isBattleMode = true;
        gameState.battleMemberIds.clear();
        gameState.monsters.clear();

        List<String> partyNames = new ArrayList<>();
        
        gameState.battleMemberIds.add(triggerPlayer.id);
        partyNames.add(triggerPlayer.name);

        for (Player other : gameState.players) {
            if (other.id == triggerPlayer.id) continue;
            
            int dist = Math.max(Math.abs(triggerPlayer.x - other.x), Math.abs(triggerPlayer.y - other.y));
            if (dist <= 2) {
                gameState.battleMemberIds.add(other.id);
                partyNames.add(other.name);
            }
        }

        gameState.monsters.add(new Monster(0, "고블린", 50));
        gameState.monsters.add(new Monster(1, "오크", 80));

        gameState.logMessage = String.format("⚔️ 몬스터 발견! 파티: %s", String.join(", ", partyNames));
    }

    public synchronized void processBattleAction(int playerId, BattleRequest req) {
        if (!gameState.isBattleMode) return;
        if (gameState.currentTurnPlayerId != playerId) return;

        if (!gameState.battleMemberIds.contains(playerId)) {
            return; 
        }

        Player p = gameState.players.get(playerId);
        
        if ("FLEE".equals(req.action)) {
            if (Math.random() < 0.5) { 
                endBattle(true);
                gameState.logMessage = "💨 " + p.name + " 파티가 도망에 성공했습니다!";
                passTurn(playerId); 
                return;
            } else {
                gameState.logMessage = "🚫 도망 실패! 몬스터에게 잡혔습니다.";
            }
        }
        else {
            int damage = 0;
            boolean isAoE = false;
            String skillName = "공격";

            if ("ATTACK".equals(req.action)) damage = 15;
            else if ("SKILL1".equals(req.action)) { damage = 25; skillName = "강타"; }
            else if ("SKILL2".equals(req.action)) { damage = 10; isAoE = true; skillName = "광역기"; }

            if (isAoE) {
                for(Monster m : gameState.monsters) { if(!m.isDead) m.hp -= damage; }
                gameState.logMessage = String.format("💥 [%s] %s! (광역 %d 피해)", p.name, skillName, damage);
            } else {
                if (req.targetIndex >= 0 && req.targetIndex < gameState.monsters.size()) {
                    Monster target = gameState.monsters.get(req.targetIndex);
                    if (!target.isDead) {
                        target.hp -= damage;
                        gameState.logMessage = String.format("⚔️ [%s] %s -> %s (%d 피해)", p.name, skillName, target.name, damage);
                    }
                }
            }
        }

        checkMonsterDeath();
        
        if (gameState.monsters.stream().allMatch(m -> m.isDead)) {
            endBattle(true);
            return;
        }

        monsterCounterAttack();
        passTurn(playerId);
    }

    private void monsterCounterAttack() {
        for (Monster m : gameState.monsters) {
            if (m.isDead) continue;
            if (!gameState.battleMemberIds.isEmpty()) {
                int targetId = gameState.battleMemberIds.get(new Random().nextInt(gameState.battleMemberIds.size()));
                Player target = gameState.players.get(targetId);
                
                int dmg = 5 + new Random().nextInt(6); 
                target.hp -= dmg;
                gameState.logMessage += String.format(" / 👹 %s 반격 -> %s (%d)", m.name, target.name, dmg);
            }
        }
    }
    
    private void checkMonsterDeath() {
        for(Monster m : gameState.monsters) {
            if (!m.isDead && m.hp <= 0) {
                m.isDead = true; m.hp = 0;
            }
        }
    }

    private void endBattle(boolean win) {
        gameState.isBattleMode = false;
        
        if (win && battleTileX != -1 && battleTileY != -1) {
            gameState.map[battleTileY][battleTileX] = 0;
            gameState.logMessage = "🎉 전투 승리! 몬스터가 사라졌습니다.";
        }
        
        battleTileX = -1;
        battleTileY = -1;
        
        passTurn(gameState.currentTurnPlayerId);
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