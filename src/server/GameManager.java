package server;

import shared.*;
import shared.Message.BattleRequest;
import java.util.*;

public class GameManager {
    private GameState gameState = new GameState();

    private int battleTileX = -1;
    private int battleTileY = -1;
    
    private static final int COST_ATK = 50;
    private static final int COST_MAXHP = 50; 
    private static final int COST_HEAL = 30;  

    public synchronized GameState getGameState() { return gameState; }

    public synchronized void setPlayerName(int id, String name) {
        if (id >= 0 && id < gameState.players.size()) {
            gameState.players.get(id).name = name;
        }
    }

    public synchronized void changeJob(int playerId, String jobName) {
        if (playerId < gameState.players.size()) {
            Player p = gameState.players.get(playerId);
            p.jobClass = jobName;
            p.updateStatsByJob(); 
        }
    }

    public synchronized void rollDice(int playerId) {
        if (gameState.isBattleMode || gameState.isShopMode) return;
        if (gameState.currentTurnPlayerId != playerId) return;
        
        Player p = gameState.players.get(playerId);
        if (p.hasRolled || p.movePoints > 0) return; 

        p.movePoints = new Random().nextInt(6) + 1;
        p.hasRolled = true;
        gameState.logMessage = String.format("🎲 %s 주사위 결과: %d", p.name, p.movePoints);
    }

    public synchronized void movePlayer(int playerId, int dx, int dy) {
        if (gameState.isBattleMode || gameState.isShopMode) return;
        if (gameState.currentTurnPlayerId != playerId) return;
        
        Player p = gameState.players.get(playerId);
        if (p.movePoints <= 0) { gameState.logMessage = "🚫 이동력이 부족합니다!"; return; }

        int newX = p.x + dx;
        int newY = p.y + dy;
        
        if (newX < 0 || newX >= 12 || newY < 0 || newY >= 8) return;
        
        if (gameState.map[newY][newX] == 1) { gameState.logMessage = "🌊 물에는 들어갈 수 없습니다."; return; }

        p.x = newX; p.y = newY; p.movePoints--;

        int tileType = gameState.map[newY][newX];
        if (tileType == 2) {
            initiateBattle(p, newX, newY);
        } else if (tileType == 3) {
            openShop(p);
        } else if (tileType == 4) {
            initiateBossBattle(p, newX, newY);
        }
    }

    public void openShop(Player p) {
        gameState.isShopMode = true;
        gameState.shopWarning = "";
        gameState.logMessage = String.format("🏪 %s님이 상점에 입장했습니다.", p.name);
    }

    public void exitShop(int playerId) {
        if (!gameState.isShopMode) return;
        if (gameState.currentTurnPlayerId != playerId) return;
        
        gameState.isShopMode = false;
        gameState.shopWarning = "";
        gameState.logMessage = "상점에서 나왔습니다.";
    }

    public void buyItem(int playerId, String itemCode) {
        if (!gameState.isShopMode || gameState.currentTurnPlayerId != playerId) return;

        Player p = gameState.players.get(playerId);
        gameState.shopWarning = ""; 

        if ("ATK".equals(itemCode)) {
            if (gameState.teamGold >= COST_ATK) {
                gameState.teamGold -= COST_ATK;
                p.bonusAttack += 5; 
                gameState.logMessage = String.format("⚔️ %s 공격력 강화 성공!", p.name);
            } else {
                gameState.shopWarning = "골드가 부족합니다!";
            }
        } 
        else if ("MAXHP".equals(itemCode)) {
            if (gameState.teamGold >= COST_MAXHP) {
                gameState.teamGold -= COST_MAXHP;
                p.bonusMaxHp += 20; 
                p.hp += 20; 
                gameState.logMessage = String.format("💗 %s 최대 체력 증가!", p.name);
            } else {
                gameState.shopWarning = "골드가 부족합니다!";
            }
        }
        else if ("HEAL".equals(itemCode)) {
            if (p.hp >= p.getTotalMaxHp()) {
                gameState.shopWarning = "현재 체력이 최대입니다!"; 
                return; 
            }

            if (gameState.teamGold >= COST_HEAL) {
                gameState.teamGold -= COST_HEAL;
                p.hp = Math.min(p.getTotalMaxHp(), p.hp + 30);
                gameState.logMessage = String.format("🧪 %s 체력 회복!", p.name);
            } else {
                gameState.shopWarning = "골드가 부족합니다!"; 
            }
        }
    }

    private void initiateBattle(Player triggerPlayer, int x, int y) {
        setupBattle(triggerPlayer, x, y, false);
    }

    private void initiateBossBattle(Player triggerPlayer, int x, int y) {
        setupBattle(triggerPlayer, x, y, true);
    }

    private void setupBattle(Player triggerPlayer, int x, int y, boolean isBoss) {
        this.battleTileX = x;
        this.battleTileY = y;
        
        gameState.isBattleMode = true;
        gameState.battleMemberIds.clear();
        gameState.monsters.clear();
        gameState.battleOrder.clear(); 
        gameState.battleLog.clear(); 

        List<Player> participants = new ArrayList<>();
        participants.add(triggerPlayer);
        
        for (Player other : gameState.players) {
            if (other.id == triggerPlayer.id) continue;
            int dist = Math.max(Math.abs(triggerPlayer.x - other.x), Math.abs(triggerPlayer.y - other.y));
            // 살아있는 사람만 전투 참가 (죽은 자는 제외)
            if (dist <= 2 && other.hp > 0) {
                participants.add(other);
            }
        }

        for (Player p : participants) {
            gameState.battleMemberIds.add(p.id);
            p.updateStatsByJob();
            gameState.battleOrder.add(new BattleUnit(false, p.id, p.name, p.getTotalSpeed()));
        }

        int r = gameState.roundNumber; 
        
        if (isBoss) {
            Monster boss = new Monster(99, "🔥 드래곤 (BOSS)", 500, 30 + (r*5), 8);
            gameState.monsters.add(boss);
            gameState.battleOrder.add(new BattleUnit(true, 99, boss.name, boss.speed));
            gameState.logMessage = "🔥 보스 출현! 드래곤과의 결전!";
        } else {
            Monster m1 = new Monster(0, "고블린 (Lv."+r+")", 30 + (r * 10), 5 + (r * 2), 12);
            Monster m2 = new Monster(1, "오크 (Lv."+r+")", 50 + (r * 15), 15 + (r * 3), 3);
            gameState.monsters.add(m1);
            gameState.monsters.add(m2);

            gameState.battleOrder.add(new BattleUnit(true, 0, m1.name, m1.speed));
            gameState.battleOrder.add(new BattleUnit(true, 1, m2.name, m2.speed));
            gameState.logMessage = "⚔️ 몬스터 무리와 마주쳤습니다!";
        }

        Collections.sort(gameState.battleOrder);
        gameState.battleTurnIndex = -1;
        processNextBattleTurn();
    }

    private void processNextBattleTurn() {
        if (!gameState.isBattleMode) return;

        gameState.battleTurnIndex = (gameState.battleTurnIndex + 1) % gameState.battleOrder.size();
        BattleUnit currentUnit = gameState.battleOrder.get(gameState.battleTurnIndex);

        if (isUnitDead(currentUnit)) {
            processNextBattleTurn();
            return;
        }

        if (currentUnit.isMonster) {
            monsterAttackLogic(currentUnit.id);
            if (gameState.isBattleMode) processNextBattleTurn(); 
        } else {
            gameState.currentTurnPlayerId = currentUnit.id;
        }
    }

    private boolean isUnitDead(BattleUnit unit) {
        if (unit.isMonster) {
            for(Monster m : gameState.monsters) {
                if(m.id == unit.id) return m.isDead;
            }
            return true;
        }
        else {
            return gameState.players.get(unit.id).hp <= 0;
        }
    }

    private void monsterAttackLogic(int monsterId) {
        Monster m = null;
        for(Monster mon : gameState.monsters) {
            if(mon.id == monsterId) { m = mon; break; }
        }
        
        if (m == null || m.isDead) return;

        // [핵심] 타겟팅: 살아있는(hp > 0) 플레이어만 공격 대상
        List<Player> targets = new ArrayList<>();
        for (int pid : gameState.battleMemberIds) {
            Player p = gameState.players.get(pid);
            if (p.hp > 0) targets.add(p);
        }

        if (!targets.isEmpty()) {
            Player target = targets.get(new Random().nextInt(targets.size()));
            int dmg = m.attack;
            target.hp = Math.max(0, target.hp - dmg); 
            
            gameState.battleLog.add(String.format("👹 %s의 공격! -> %s [%d 피해]", m.name, target.name, dmg));

            if (target.hp == 0) {
                gameState.battleLog.add("☠️ " + target.name + "님이 쓰러졌습니다!");
            }
        }

        // [핵심] 이번 전투 참가자가 모두 죽었는지 확인
        boolean allParticipantsDead = true;
        for (int pid : gameState.battleMemberIds) {
            if (gameState.players.get(pid).hp > 0) {
                allParticipantsDead = false;
                break;
            }
        }

        if (allParticipantsDead) {
            // 이번 전투 패배
            endBattle(false); 
        }
    }

    public synchronized void processBattleAction(int playerId, BattleRequest req) {
        if (!gameState.isBattleMode) return;
        BattleUnit currentUnit = gameState.battleOrder.get(gameState.battleTurnIndex);
        if (currentUnit.isMonster || currentUnit.id != playerId) return; 

        Player p = gameState.players.get(playerId);
        
        if ("FLEE".equals(req.action)) {
            if (Math.random() < 0.5) { 
                endBattle(true); // 도망 성공 시 맵으로 복귀
                gameState.logMessage = "💨 도망 성공!";
                passTurn(playerId); 
                return;
            } else {
                gameState.battleLog.add("🚫 도망 실패!");
            }
        }
        else {
            int dmg = p.getTotalAttack();
            boolean isAoE = false;
            String skillName = "공격";

            if ("ATTACK".equals(req.action)) { }
            else if ("SKILL1".equals(req.action)) { 
                dmg = (int)(dmg * 1.5); 
                skillName = "강타"; 
            }
            else if ("SKILL2".equals(req.action)) { 
                dmg = (int)(dmg * 0.8); 
                isAoE = true; 
                skillName = "광역기"; 
            }

            if (isAoE) {
                for(Monster m : gameState.monsters) { if(!m.isDead) m.hp -= dmg; }
                gameState.battleLog.add(String.format("💥 [%s] %s! (광역 %d 피해)", p.name, skillName, dmg));
            } else {
                if (req.targetIndex >= 0 && req.targetIndex < gameState.monsters.size()) {
                    Monster target = gameState.monsters.get(req.targetIndex);
                    if (!target.isDead) {
                        target.hp -= dmg;
                        gameState.battleLog.add(String.format("⚔️ [%s] %s -> %s (%d 피해)", p.name, skillName, target.name, dmg));
                    }
                }
            }
        }

        checkMonsterDeath();
        
        if (gameState.monsters.stream().allMatch(m -> m.isDead)) {
            endBattle(true); // 승리
            return;
        }

        processNextBattleTurn();
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
        
        if (win) {
            // 승리
            if (battleTileX != -1 && gameState.map[battleTileY][battleTileX] == 4) {
                gameState.teamGold += 500;
                gameState.logMessage = "🎉🎉 드래곤 처치! 게임 클리어! (+500G) 🎉🎉";
            } else {
                gameState.teamGold += 50;
                gameState.logMessage = "🎉 승리! (팀 자금 +50G)";
            }
            if(battleTileX != -1) gameState.map[battleTileY][battleTileX] = 0; 
            // 승리 시에는 현재 턴 플레이어 다음으로 턴을 넘김
            passTurn(gameState.currentTurnPlayerId);
        } else {
            // 패배 (전투 참가자 전멸)
            gameState.logMessage = "💀 전투 패배... 사망자는 행동할 수 없습니다.";
            
            // 모든 플레이어가 죽었는지 확인 (진짜 게임 오버)
            boolean globalWipe = true;
            for(Player p : gameState.players) {
                if(p.hp > 0) { globalWipe = false; break; }
            }

            if(globalWipe) {
                gameState.logMessage = "☠️ [GAME OVER] 모든 플레이어가 사망했습니다.";
                // 여기서 게임을 멈추거나 리셋 로직을 넣을 수 있음 (현재는 멈춤 상태 유지)
            } else {
                // 아직 살아있는 동료가 있다면, 그 사람에게 턴을 넘김
                passTurn(gameState.currentTurnPlayerId);
            }
        }
        
        battleTileX = -1;
        battleTileY = -1;
    }

    // 턴 넘기기
    public synchronized void passTurn(int playerId) {
        if (gameState.isBattleMode) return; 
        
        // 현재 플레이어 상태 초기화
        Player currentP = gameState.players.get(playerId);
        currentP.movePoints = 0;
        currentP.hasRolled = false;

        int nextId = (gameState.currentTurnPlayerId + 1) % gameState.players.size();
        
        // 살아있는 플레이어 찾기
        int loopCount = 0;
        while (gameState.players.get(nextId).hp <= 0) {
            nextId = (nextId + 1) % gameState.players.size();
            loopCount++;
            
            // 한 바퀴 다 돌았는데 전원 사망이면 루프 탈출 (무한루프 방지)
            if (loopCount >= gameState.players.size()) {
                gameState.logMessage = "☠️ 모든 플레이어가 사망했습니다.";
                return;
            }
        }

        // 살아있는 다음 플레이어에게 턴 부여
        gameState.currentTurnPlayerId = nextId;

        // ID 0번(방장) 차례가 돌아오거나, 라운드 넘김 처리가 필요하면 여기서 체크
        // (단순화를 위해 누군가의 턴이 돌아오면 라운드 처리 로직은 생략하거나
        //  살아있는 사람 중 가장 ID가 낮은 사람일 때 라운드를 올리는 식으로 보정 가능.
        //  여기서는 일단 단순히 0번이 걸릴 때 라운드 증가 유지)
        if (nextId == 0) {
            gameState.roundNumber++;
            gameState.logMessage = String.format("🔔 라운드 %d 시작!", gameState.roundNumber);
        } else {
            gameState.logMessage = String.format("📢 %s님의 턴입니다.", gameState.players.get(nextId).name);
        }
    }
}