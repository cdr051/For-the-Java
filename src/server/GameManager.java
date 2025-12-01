package server;

import shared.*;
import shared.Message.BattleRequest;
import java.util.*;

public class GameManager {
    private GameState gameState = new GameState();

    // 전투가 발생한 맵 좌표 저장
    private int battleTileX = -1;
    private int battleTileY = -1;

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

    // 주사위 굴리기
    public synchronized void rollDice(int playerId) {
        if (gameState.isBattleMode) return;
        if (gameState.currentTurnPlayerId != playerId) return;
        Player p = gameState.players.get(playerId);
        if (p.hasRolled || p.movePoints > 0) return; 

        p.movePoints = new Random().nextInt(6) + 1;
        p.hasRolled = true;
        gameState.logMessage = String.format("🎲 %s 주사위 결과: %d", p.name, p.movePoints);
    }

    // 플레이어 이동
    public synchronized void movePlayer(int playerId, int dx, int dy) {
        if (gameState.isBattleMode) return;
        if (gameState.currentTurnPlayerId != playerId) return;
        
        Player p = gameState.players.get(playerId);
        if (p.movePoints <= 0) { gameState.logMessage = "🚫 이동력이 부족합니다!"; return; }

        int newX = p.x + dx;
        int newY = p.y + dy;
        if (newX < 0 || newX >= 10 || newY < 0 || newY >= 10) return;
        if (gameState.map[newY][newX] == 1) { gameState.logMessage = "🌊 물 불가"; return; }

        p.x = newX; p.y = newY; p.movePoints--;

        if (gameState.map[newY][newX] == 2) {
            initiateBattle(p, newX, newY);
        }
    }

    // 로그 쌓기
    private void addBattleLog(String msg) {
        gameState.battleLog.add(msg);
        gameState.logMessage = msg; 
    }

    // ⚔️ 전투 시작 (⭐ 몬스터 스케일링 적용됨)
    private void initiateBattle(Player triggerPlayer, int x, int y) {
        this.battleTileX = x;
        this.battleTileY = y;
        
        gameState.isBattleMode = true;
        gameState.battleMemberIds.clear();
        gameState.monsters.clear();
        gameState.battleOrder.clear();
        gameState.battleLog.clear(); 

        // 1. 참여자 선정
        List<Player> participants = new ArrayList<>();
        participants.add(triggerPlayer);
        
        for (Player other : gameState.players) {
            if (other.id == triggerPlayer.id) continue;
            int dist = Math.max(Math.abs(triggerPlayer.x - other.x), Math.abs(triggerPlayer.y - other.y));
            if (dist <= 2) participants.add(other);
        }

        // 2. 플레이어 등록
        for (Player p : participants) {
            gameState.battleMemberIds.add(p.id);
            p.updateStatsByJob(); 
            gameState.battleOrder.add(new BattleUnit(false, p.id, p.name, p.getTotalSpeed()));
        }

        // 3. ⭐ [핵심] 몬스터 생성 및 스케일링 (라운드 비례 강해짐)
        int r = gameState.roundNumber; // 현재 라운드
        
        // 고블린: 기본 체력 30 + (라운드당 10), 공격력 5 + (라운드당 2)
        int gobHp = 30 + (r * 10);
        int gobAtk = 5 + (r * 2);
        Monster m1 = new Monster(0, "고블린 (Lv."+r+")", gobHp, gobAtk, 12);

        // 오크: 기본 체력 50 + (라운드당 15), 공격력 15 + (라운드당 3)
        int orcHp = 50 + (r * 15);
        int orcAtk = 15 + (r * 3);
        Monster m2 = new Monster(1, "오크 (Lv."+r+")", orcHp, orcAtk, 3);

        gameState.monsters.add(m1);
        gameState.monsters.add(m2);

        gameState.battleOrder.add(new BattleUnit(true, 0, m1.name, m1.speed));
        gameState.battleOrder.add(new BattleUnit(true, 1, m2.name, m2.speed));
        Collections.sort(gameState.battleOrder);

        addBattleLog("⚔️ 전투 개시! (현재 라운드: " + r + ")");
        addBattleLog(String.format("⚠️ 몬스터가 강해졌습니다! (HP 증가, 공격력 증가)"));

        gameState.battleTurnIndex = -1;
        processNextBattleTurn();
    }

    // 전투 턴 진행
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
        if (unit.isMonster) return gameState.monsters.get(unit.id).isDead;
        else return gameState.players.get(unit.id).hp <= 0;
    }

    // 몬스터 공격
    private void monsterAttackLogic(int monsterIdx) {
        Monster m = gameState.monsters.get(monsterIdx);
        if (m.isDead) return;

        List<Player> livePlayers = new ArrayList<>();
        for (int pid : gameState.battleMemberIds) {
            Player p = gameState.players.get(pid);
            if (p.hp > 0) livePlayers.add(p);
        }

        if (!livePlayers.isEmpty()) {
            Player target = livePlayers.get(new Random().nextInt(livePlayers.size()));
            target.hp -= m.attack;
            addBattleLog(String.format("👹 %s의 공격! -> %s(%s) [%d 피해]", 
                    m.name, target.name, target.jobClass, m.attack));
        }
    }

    // 플레이어 행동 처리
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
        } else {
            int finalAttack = p.getTotalAttack();
            int damage = finalAttack;
            String skillName = "기본 공격";

            if ("SKILL1".equals(req.action)) { damage = (int)(finalAttack * 1.5); skillName = "강타"; }
            else if ("SKILL2".equals(req.action)) { damage = (int)(finalAttack * 0.8); skillName = "광역기"; }

            if ("SKILL2".equals(req.action)) {
                for(Monster m : gameState.monsters) { if(!m.isDead) m.hp -= damage; }
                addBattleLog(String.format("💥 %s(%s)의 %s! (적 전체 %d 피해)", p.name, p.jobClass, skillName, damage));
            } else {
                if (req.targetIndex >= 0 && req.targetIndex < gameState.monsters.size()) {
                    Monster target = gameState.monsters.get(req.targetIndex);
                    if (!target.isDead) {
                        target.hp -= damage;
                        addBattleLog(String.format("⚔️ %s(%s)의 %s! -> %s [%d 피해]", p.name, p.jobClass, skillName, target.name, damage));
                    }
                }
            }
        }

        checkMonsterDeath();
        if (gameState.monsters.stream().allMatch(m -> m.isDead)) {
            endBattle(true);
            return;
        }
        processNextBattleTurn();
    }

    private void checkMonsterDeath() {
        for(Monster m : gameState.monsters) {
            if (!m.isDead && m.hp <= 0) {
                m.isDead = true; m.hp = 0;
                addBattleLog(String.format("☠️ %s 처치!", m.name));
            }
        }
    }

    private void endBattle(boolean win) {
        gameState.isBattleMode = false;
        if (win && battleTileX != -1) {
            for(int pid : gameState.battleMemberIds) {
                Player p = gameState.players.get(pid);
                p.gold += 30;
            }
            gameState.map[battleTileY][battleTileX] = 0;
            gameState.logMessage = "🎉 전투 승리! (30골드 획득)";
        }
        battleTileX = -1; battleTileY = -1;
        
        // 전투 끝나면 맵 턴 넘기기 호출
        passTurn(gameState.currentTurnPlayerId);
    }

    // ⭐ [핵심] 턴 넘기기 로직 수정됨
    public synchronized void passTurn(int playerId) {
        if (gameState.currentTurnPlayerId != playerId) return;
        
        Player currentP = gameState.players.get(playerId);
        currentP.movePoints = 0;
        currentP.hasRolled = false;

        // [CASE 1] 전투 중: 라운드 절대 증가 안 함
        if (gameState.isBattleMode) {
            // 전투 로그에만 집중하므로 별도 로직 없음 (processNextBattleTurn에서 관리)
            // 다만 예외 상황을 대비해 코드는 남겨둠
            return; 
        } 
        
        // [CASE 2] 맵 이동 중
        else {
            int nextId = (gameState.currentTurnPlayerId + 1) % gameState.players.size();
            gameState.currentTurnPlayerId = nextId;
            
            // ⭐ [중요] 한 바퀴 돌아서 0번 플레이어가 될 때만 라운드 증가
            if (nextId == 0) {
                gameState.roundNumber++;
                gameState.logMessage = String.format("🔔 [라운드 %d] 시작! 몬스터가 더 강해집니다.", gameState.roundNumber);
            } else {
                Player nextP = gameState.players.get(nextId);
                gameState.logMessage = String.format("📢 %s님의 턴입니다.", nextP.name);
            }
        }
    }
}