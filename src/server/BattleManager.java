package server;

import shared.*;
import shared.Message.BattleRequest;
import java.util.*;

public class BattleManager {
    private GameState gameState;
    private GameManager gameManager; // 턴 넘기기 호출용

    // 전투가 발생한 타일 좌표 저장
    private int battleTileX = -1;
    private int battleTileY = -1;

    public BattleManager(GameState gameState, GameManager gameManager) {
        this.gameState = gameState;
        this.gameManager = gameManager;
    }

    // ⚔️ 전투 시작
    public void initiateBattle(Player triggerPlayer, int x, int y) {
        this.battleTileX = x;
        this.battleTileY = y;
        
        gameState.isBattleMode = true;
        gameState.battleMemberIds.clear();
        gameState.monsters.clear();

        List<String> partyNames = new ArrayList<>();
        
        // 전투 멤버 결성 (트리거 플레이어 + 거리 2칸 이내 동료)
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

        // 몬스터 생성
        gameState.monsters.add(new Monster(0, "고블린", 50));
        gameState.monsters.add(new Monster(1, "오크", 80));

        gameState.logMessage = String.format("⚔️ 몬스터 발견! 파티: %s", String.join(", ", partyNames));
    }

    // 👊 전투 행동 처리
    public void processBattleAction(int playerId, BattleRequest req) {
        if (!gameState.battleMemberIds.contains(playerId)) return;

        Player p = gameState.players.get(playerId);
        
        // 1. 도망가기
        if ("FLEE".equals(req.action)) {
            if (Math.random() < 0.5) { 
                endBattle(false); // 실패 판정 (골드 X)
                gameState.logMessage = "💨 " + p.name + " 파티가 도망에 성공했습니다! (골드 획득 없음)";
                return;
            } else {
                gameState.logMessage = "🚫 도망 실패! 몬스터에게 잡혔습니다.";
            }
        }
        // 2. 공격/스킬
        else {
            processAttack(p, req);
        }

        checkMonsterDeath();
        
        // 승리 체크
        if (gameState.monsters.stream().allMatch(m -> m.isDead)) {
            endBattle(true); // 승리 판정 (골드 O)
            return;
        }

        monsterCounterAttack();
        gameManager.passTurn(playerId);
    }

    private void processAttack(Player p, BattleRequest req) {
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
            gameState.map[battleTileY][battleTileX] = 0; // 맵에서 몬스터 제거
            
            // 골드 지급
            int rewardGold = 50;
            gameState.teamGold += rewardGold;
            gameState.logMessage = String.format("🎉 승리! %d골드 획득! (현재: %d G)", rewardGold, gameState.teamGold);
        }
        
        battleTileX = -1;
        battleTileY = -1;
        
        gameManager.passTurn(gameState.currentTurnPlayerId);
    }
}