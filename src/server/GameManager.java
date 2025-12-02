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
        
        if (p.hp <= 0) {
            gameState.logMessage = "☠️ 사망자는 행동할 수 없습니다.";
            return;
        }

        if (p.hasRolled || p.movePoints > 0) return; 

        p.movePoints = new Random().nextInt(6) + 1;
        p.hasRolled = true;
        gameState.logMessage = String.format("🎲 %s 주사위 결과: %d", p.name, p.movePoints);
    }

    public synchronized void movePlayer(int playerId, int dx, int dy) {
        if (gameState.isBattleMode || gameState.isShopMode) return;
        if (gameState.currentTurnPlayerId != playerId) return;
        
        Player p = gameState.players.get(playerId);

        if (p.hp <= 0) return;

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
            
            // 거리가 2칸 이내이고, 살아있으면 참가
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
            Monster boss = new Monster(99, "🔥 드래곤 (BOSS)", 350, 25 + (r*5), 8);
            gameState.monsters.add(boss);
            gameState.battleOrder.add(new BattleUnit(true, 99, boss.name, boss.speed));
            gameState.logMessage = "🔥 보스 출현! " + participants.size() + "명이 함께 싸웁니다!";
        } else {
            Monster m1 = new Monster(0, "고블린 (Lv."+r+")", 30 + (r * 10), 5 + (r * 2), 12);
            Monster m2 = new Monster(1, "오크 (Lv."+r+")", 50 + (r * 15), 15 + (r * 3), 3);
            gameState.monsters.add(m1);
            gameState.monsters.add(m2);

            gameState.battleOrder.add(new BattleUnit(true, 0, m1.name, m1.speed));
            gameState.battleOrder.add(new BattleUnit(true, 1, m2.name, m2.speed));
            gameState.logMessage = "⚔️ 몬스터 출현! " + participants.size() + "명이 난입했습니다!";
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

        List<Player> targets = new ArrayList<>();
        for (int pid : gameState.battleMemberIds) {
            Player p = gameState.players.get(pid);
            if (p.hp > 0) targets.add(p);
        }

        if (targets.isEmpty()) return;

        if (m.id == 99) {
            if (Math.random() < 0.3) {
                gameState.battleLog.add("🔥🔥 드래곤 화염 브레스!");
                for (Player p : targets) {
                    int dmg = (int)(m.attack * 0.8); 
                    p.hp = Math.max(0, p.hp - dmg);
                    gameState.battleLog.add(String.format("   -> %s 불탐! [%d 피해]", p.name, dmg));
                    if (p.hp == 0) gameState.battleLog.add("☠️ " + p.name + "님이 재가 되었습니다.");
                }
            } 
            else {
                Player target = targets.get(new Random().nextInt(targets.size()));
                int dmg = (int)(m.attack * 1.2);
                gameState.battleLog.add("🐲 드래곤이 물어뜯습니다!");
                target.hp = Math.max(0, target.hp - dmg);
                gameState.battleLog.add(String.format("   -> %s [%d 피해]", target.name, dmg));
                if (target.hp == 0) gameState.battleLog.add("☠️ " + target.name + "님이 쓰러졌습니다!");
            }
        } 
        else {
            Player target = targets.get(new Random().nextInt(targets.size()));
            int dmg = m.attack;
            target.hp = Math.max(0, target.hp - dmg); 
            gameState.battleLog.add(String.format("⚔️ %s의 공격 -> %s [%d 피해]", m.name, target.name, dmg));
            if (target.hp == 0) gameState.battleLog.add("☠️ " + target.name + "님이 쓰러졌습니다!");
        }

        // 전투 종료(전멸) 체크
        boolean allParticipantsDead = true;
        for (int pid : gameState.battleMemberIds) {
            if (gameState.players.get(pid).hp > 0) {
                allParticipantsDead = false;
                break;
            }
        }

        if (allParticipantsDead) {
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
                endBattle(true); 
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
            if (battleTileX != -1 && gameState.map[battleTileY][battleTileX] == 4) {
                gameState.teamGold += 500;
                gameState.logMessage = "🎉🎉 드래곤 처치! 게임 클리어! (+500G) 🎉🎉";
            } else {
                gameState.teamGold += 50;
                gameState.logMessage = "🎉 승리! (팀 자금 +50G)";
            }
            if(battleTileX != -1) gameState.map[battleTileY][battleTileX] = 0; 
            passTurn(gameState.currentTurnPlayerId);
        } else {
            gameState.logMessage = "💀 전투 패배... 사망자는 행동할 수 없습니다.";
            passTurn(gameState.currentTurnPlayerId);
        }
        
        battleTileX = -1;
        battleTileY = -1;
    }

    private void resetGame() {
        GameState newState = new GameState();
        gameState.map = newState.map;
        gameState.roundNumber = 1;
        gameState.teamGold = 100;
        gameState.logMessage = "🔄 전멸하여 게임이 초기화되었습니다.";
        gameState.isBattleMode = false;
        gameState.isShopMode = false;
        gameState.monsters.clear();

        // 2. 플레이어 상태 초기화
        for (Player p : gameState.players) {
            p.hp = p.getTotalMaxHp();
            p.x = 0; p.y = 0; // 시작 지점으로 이동
            p.movePoints = 0;
            p.hasRolled = false;
            p.isReady = false; // 준비 상태 해제
        }
        gameState.currentTurnPlayerId = 0;

        // 3. 클라이언트들에게 GAME_OVER 메시지 전송 (로비로 이동하라고 명령)
        GameServer.broadcast(new Message(Message.Type.GAME_OVER, null));
        GameServer.broadcast(new Message(Message.Type.LOBBY_UPDATE, new ArrayList<>(gameState.players)));
    }

    public synchronized void passTurn(int playerId) {
        if (gameState.isBattleMode) return; 
        
        Player currentP = gameState.players.get(playerId);
        currentP.movePoints = 0;
        currentP.hasRolled = false;

        boolean allDead = true;
        for(Player p : gameState.players) {
            if (p.hp > 0) {
                allDead = false;
                break;
            }
        }

        if (allDead) {
            resetGame();
            return;
        }

        int nextId = (gameState.currentTurnPlayerId + 1) % gameState.players.size();
        
        while (gameState.players.get(nextId).hp <= 0) {
            nextId = (nextId + 1) % gameState.players.size();
        }

        gameState.currentTurnPlayerId = nextId;

        if (nextId == 0) {
            gameState.roundNumber++;
            gameState.logMessage = String.format("🔔 라운드 %d 시작!", gameState.roundNumber);
        } else {
            gameState.logMessage = String.format("📢 %s님의 턴입니다.", gameState.players.get(nextId).name);
        }
    }
}