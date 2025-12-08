package server;

import shared.*;
import shared.Message.BattleRequest;
import java.util.*;

public class GameManager {
    private GameState gameState = new GameState();
    private int battleTileX = -1;
    private int battleTileY = -1;
    private int returnSafeX = 0;
    private int returnSafeY = 0;

    public synchronized GameState getGameState() {
        return gameState;
    }

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

    public synchronized void startGame() {
        gameState.roundNumber = 1;
        gameState.logMessage = "게임이 시작되었습니다! (Round 1)";
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
        if (p.movePoints <= 0) {
            gameState.logMessage = "🚫 이동력이 부족합니다!";
            return;
        }

        int newX = p.x + dx;
        int newY = p.y + dy;

        // ⭐ [수정] 12x8 맵 경계 체크
        if (newX < 0 || newX >= 12 || newY < 0 || newY >= 8) return;

        if (gameState.map[newY][newX] == 1) {
            gameState.logMessage = "🌊 물에는 들어갈 수 없습니다.";
            return;
        }

        int prevX = p.x;
        int prevY = p.y;
        p.x = newX;
        p.y = newY;
        p.movePoints--;

        int tileType = gameState.map[newY][newX];
        if (tileType == 2) {
            initiateBattle(p, newX, newY, prevX, prevY);
        } else if (tileType == 3) {
            enterShop(p);
        }
    }

    private void enterShop(Player p) {
        gameState.isShopMode = true;
        gameState.shopWarning = "환영합니다!";
        gameState.logMessage = String.format("💰 %s님이 상점에 입장했습니다.", p.name);
    }

    public synchronized void exitShop(int playerId) {
        if (!gameState.isShopMode || gameState.currentTurnPlayerId != playerId) return;
        gameState.isShopMode = false;
        gameState.shopWarning = "";
        gameState.logMessage = "상점을 나왔습니다.";
    }

    public synchronized void processShopBuy(int playerId, String itemCode) {
        if (!gameState.isShopMode || gameState.currentTurnPlayerId != playerId) return;
        
        Player p = gameState.players.get(playerId);
        int cost = 0;
        
        if ("ATK".equals(itemCode)) cost = 50;
        else if ("MAXHP".equals(itemCode)) cost = 50;
        else if ("HEAL".equals(itemCode)) cost = 30;

        if (gameState.teamGold < cost) {
            gameState.shopWarning = "골드가 부족합니다!";
            return;
        }

        gameState.teamGold -= cost;

        if ("ATK".equals(itemCode)) {
            p.bonusAttack += 5;
            gameState.shopWarning = "구매 성공! (공격력 +5)";
        } else if ("MAXHP".equals(itemCode)) {
            p.bonusMaxHp += 20;
            p.hp += 20;
            gameState.shopWarning = "구매 성공! (최대체력 +20)";
        } else if ("HEAL".equals(itemCode)) {
            p.hp = Math.min(p.hp + 30, p.getTotalMaxHp());
            gameState.shopWarning = "구매 성공! (체력 회복)";
        }
    }

    private void addBattleLog(String msg) {
        gameState.battleLog.add(msg);
        gameState.logMessage = msg;
    }

    private void initiateBattle(Player triggerPlayer, int x, int y, int prevX, int prevY) {
        this.battleTileX = x;
        this.battleTileY = y;
        this.returnSafeX = prevX;
        this.returnSafeY = prevY;

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
            if (dist <= 2) participants.add(other);
        }

        for (Player p : participants) {
            gameState.battleMemberIds.add(p.id);
            p.updateStatsByJob();
            p.shieldStacks = 0;
            p.atkBuffTurns = 0;
            gameState.battleOrder.add(new BattleUnit(false, p.id, p.name, p.getTotalSpeed()));
        }

        int r = gameState.roundNumber;
        List<Integer> mobTypes = new ArrayList<>(Arrays.asList(0, 1, 2, 3));
        Collections.shuffle(mobTypes);

        for (int i = 0; i < 2; i++) {
            int type = mobTypes.get(i);
            Monster m = createMonsterByType(i, type, r);
            gameState.monsters.add(m);
            gameState.battleOrder.add(new BattleUnit(true, i, m.name, m.speed));
        }

        Collections.sort(gameState.battleOrder);
        addBattleLog("⚔️ 전투 개시! (현재 라운드: " + r + ")");
        gameState.battleTurnIndex = -1;
        processNextBattleTurn();
    }

    private Monster createMonsterByType(int id, int type, int r) {
        String name;
        int hp, atk, spd;

        switch (type) {
            case 0:
                name = "슬라임 (Lv." + r + ")";
                hp = 20 + (r * 8);
                atk = 5 + (r * 2);
                spd = 8;
                break;
            case 1:
                name = "스켈레톤 (Lv." + r + ")";
                hp = 35 + (r * 10);
                atk = 12 + (r * 3);
                spd = 10;
                break;
            case 2:
                name = "오크 (Lv." + r + ")";
                hp = 60 + (r * 15);
                atk = 10 + (r * 3);
                spd = 4;
                break;
            case 3:
                name = "늑대인간 (Lv." + r + ")";
                hp = 50 + (r * 12);
                atk = 15 + (r * 4);
                spd = 16;
                break;
            default:
                name = "고블린 (Lv." + r + ")";
                hp = 30 + (r * 10);
                atk = 8 + (r * 2);
                spd = 6;
        }
        return new Monster(id, name, hp, atk, spd);
    }

    private void processNextBattleTurn() {
        if (!gameState.isBattleMode) return;
        if (gameState.isGameOver) return;

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
            Player p = gameState.players.get(currentUnit.id);
            if (p.atkBuffTurns > 0) {
                p.atkBuffTurns--;
                if (p.atkBuffTurns == 0) addBattleLog(String.format("📉 %s의 버프가 끝났습니다.", p.name));
            }
            gameState.currentTurnPlayerId = currentUnit.id;
        }
    }

    private boolean isUnitDead(BattleUnit unit) {
        if (unit.isMonster) return gameState.monsters.get(unit.id).isDead;
        else return gameState.players.get(unit.id).hp <= 0;
    }

    private void monsterAttackLogic(int monsterIdx) {
        Monster m = gameState.monsters.get(monsterIdx);
        if (m.isDead) return;

        int finalAtk = m.attack;
        if (m.atkDebuffTurns > 0) {
            finalAtk /= 2;
            m.atkDebuffTurns--;
        }

        List<Player> livePlayers = new ArrayList<>();
        for (int pid : gameState.battleMemberIds) {
            Player p = gameState.players.get(pid);
            if (p.hp > 0) livePlayers.add(p);
        }

        if (!livePlayers.isEmpty()) {
            Player target = livePlayers.get(new Random().nextInt(livePlayers.size()));
            if (target.shieldStacks > 0) {
                target.shieldStacks--;
                addBattleLog(String.format("🛡️ %s의 방패 방어! (0 피해)", target.name));
            } else {
                target.hp -= finalAtk;
                addBattleLog(String.format("👹 %s의 공격! -> %s [%d 피해]", m.name, target.name, finalAtk));
                if (target.hp <= 0) {
                    target.hp = 0;
                    addBattleLog(String.format("💀 %s님이 쓰러졌습니다!", target.name));
                }
            }
        }
        checkPartyStatus();
    }

    private void checkPartyStatus() {
        boolean allDead = true;
        for (int pid : gameState.battleMemberIds) {
            if (gameState.players.get(pid).hp > 0) {
                allDead = false;
                break;
            }
        }

        if (allDead) {
            gameState.teamLives--;
            if (gameState.teamLives > 0) {
                addBattleLog("❌ 전멸! 목숨 1개 소모하여 부활합니다. (남은 목숨: " + gameState.teamLives + ")");
                revivePartyAndRetreat();
            } else {
                gameState.isGameOver = true;
                addBattleLog("❌ [GAME OVER] 모든 목숨 소진...");
                addBattleLog("👉 '로비로 돌아가기'를 눌러주세요.");
            }
        }
    }

    private void revivePartyAndRetreat() {
        gameState.isBattleMode = false;
        for (int pid : gameState.battleMemberIds) {
            Player p = gameState.players.get(pid);
            p.hp = p.getTotalMaxHp();
            if (p.id == gameState.currentTurnPlayerId) {
                p.x = returnSafeX;
                p.y = returnSafeY;
                gameState.logMessage = String.format("🚑 전멸하여 %s로 후퇴했습니다.", p.name);
            }
        }
        battleTileX = -1;
        battleTileY = -1;
        passTurn(gameState.currentTurnPlayerId);
    }

    public synchronized void processBattleAction(int playerId, BattleRequest req) {
        if (!gameState.isBattleMode) return;

        BattleUnit currentUnit = gameState.battleOrder.get(gameState.battleTurnIndex);
        if (currentUnit.isMonster || currentUnit.id != playerId) return;

        Player p = gameState.players.get(playerId);

        if ("FLEE".equals(req.action)) {
            if (Math.random() < 0.5) {
                endBattle(true);
                gameState.logMessage = String.format("💨 %s 파티 도망 성공!", p.name);
                passTurn(playerId);
                return;
            } else {
                addBattleLog(String.format("🚫 %s 도망 실패!", p.name));
            }
        } else if ("ATTACK".equals(req.action)) {
            int damage = p.getTotalAttack();
            attackSingleTarget(p, req.targetIndex, damage, "기본 공격");
        } else {
            processClassSkill(p, req.action, req.targetIndex);
        }

        checkMonsterDeath();
        if (gameState.monsters.stream().allMatch(m -> m.isDead)) {
            endBattle(true);
            return;
        }

        processNextBattleTurn();
    }

    private void processClassSkill(Player p, String action, int targetIdx) {
        int baseAtk = p.getTotalAttack();
        switch (p.jobClass) {
            case "기사":
                if ("SKILL1".equals(action)) {
                    p.shieldStacks = 1;
                    addBattleLog(String.format("🛡️ %s: 방패 들기!", p.name));
                } else if ("SKILL2".equals(action)) {
                    int dmg = (int) (baseAtk * 1.5);
                    attackSingleTarget(p, targetIdx, dmg, "강타");
                }
                break;
            case "마법사":
                if ("SKILL1".equals(action)) {
                    int dmg = (int) (baseAtk * 0.8);
                    for (Monster m : gameState.monsters)
                        if (!m.isDead) m.hp -= dmg;
                    addBattleLog(String.format("⚡ %s: 라이트닝 체인! (전체 %d 피해)", p.name, dmg));
                } else if ("SKILL2".equals(action)) {
                    int dmg = (int) (baseAtk * 2.0);
                    attackSingleTarget(p, targetIdx, dmg, "파이어볼");
                }
                break;
            case "궁수":
                if ("SKILL1".equals(action)) {
                    addBattleLog(String.format("🏹 %s: 속사 발동!", p.name));
                    int dmg = (int) (baseAtk * 0.5);
                    for (int i = 0; i < 3; i++) {
                        Monster rt = getRandomLivingMonster();
                        if (rt != null) {
                            rt.hp -= dmg;
                            addBattleLog(String.format(" -> %s에게 %d 피해!", rt.name, dmg));
                        }
                    }
                } else if ("SKILL2".equals(action)) {
                    p.atkBuffTurns = 2;
                    addBattleLog(String.format("👁️ %s: 매의 눈! (공격력 2배)", p.name));
                }
                break;
            case "도적":
                if ("SKILL1".equals(action)) {
                    double mult = 1.0 + (new Random().nextDouble() * 2.0);
                    int dmg = (int) (baseAtk * mult);
                    attackSingleTarget(p, targetIdx, dmg, "급소 찌르기");
                } else if ("SKILL2".equals(action)) {
                    for (Monster m : gameState.monsters)
                        if (!m.isDead) m.atkDebuffTurns = 2;
                    addBattleLog(String.format("💨 %s: 연막탄! (적 공격력 감소)", p.name));
                }
                break;
        }
    }

    private void attackSingleTarget(Player p, int targetIdx, int damage, String skillName) {
        if (targetIdx >= 0 && targetIdx < gameState.monsters.size()) {
            Monster target = gameState.monsters.get(targetIdx);
            if (!target.isDead) {
                target.hp -= damage;
                addBattleLog(String.format("⚔️ %s의 %s! -> %s [%d 피해]", p.name, skillName, target.name, damage));
            } else {
                addBattleLog("🚫 이미 죽은 적입니다.");
            }
        }
    }

    private Monster getRandomLivingMonster() {
        List<Monster> living = new ArrayList<>();
        for (Monster m : gameState.monsters)
            if (!m.isDead) living.add(m);
        if (living.isEmpty()) return null;
        return living.get(new Random().nextInt(living.size()));
    }

    private void checkMonsterDeath() {
        for (Monster m : gameState.monsters) {
            if (!m.isDead && m.hp <= 0) {
                m.isDead = true;
                m.hp = 0;
                addBattleLog(String.format("☠️ %s 처치!", m.name));
            }
        }
    }

    private void endBattle(boolean win) {
        gameState.isBattleMode = false;
        if (win && battleTileX != -1) {
            int reward = gameState.battleMemberIds.size() * 30;
            gameState.teamGold += reward;
            gameState.map[battleTileY][battleTileX] = 0;
            gameState.logMessage = String.format("🎉 전투 승리! (팀 자금 +%d G)", reward);
        }
        battleTileX = -1;
        battleTileY = -1;
        passTurn(gameState.currentTurnPlayerId);
    }

    public synchronized void passTurn(int playerId) {
        if (gameState.isShopMode) return;
        if (gameState.currentTurnPlayerId != playerId) return;

        Player currentP = gameState.players.get(playerId);
        currentP.movePoints = 0;
        currentP.hasRolled = false;

        if (gameState.isBattleMode) {
            // 배틀 중일 땐 턴 패스 로직 없음
        } else {
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

    public synchronized void resetGame() {
        gameState.isBattleMode = false;
        gameState.isGameOver = false;
        gameState.isShopMode = false;
        gameState.battleLog.clear();
        gameState.roundNumber = 0;
        gameState.teamGold = 100;
        gameState.teamLives = 3;
        gameState.logMessage = "대기실로 돌아왔습니다.";

        for (Player p : gameState.players) {
            p.hp = p.getTotalMaxHp();
            p.x = 0;
            p.y = 0;
            p.movePoints = 0;
            p.hasRolled = false;
            p.isReady = false;
            p.shieldStacks = 0;
            p.atkBuffTurns = 0;
            p.bonusAttack = 0;
            p.bonusMaxHp = 0;
        }

        if (!gameState.players.isEmpty())
            gameState.players.get(0).isReady = true;
    }
}