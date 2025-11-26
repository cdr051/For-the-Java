package server;

import shared.*;
import shared.Message.BattleRequest; // BattleRequest 사용을 위해 import
import java.util.*;

public class GameManager {
    private GameState gameState = new GameState();

    // 전투가 발생한 맵 좌표를 기억하기 위한 변수
    private int battleTileX = -1;
    private int battleTileY = -1;

    public synchronized GameState getGameState() { return gameState; }

    // 닉네임 설정
    public synchronized void setPlayerName(int id, String name) {
        if (id >= 0 && id < gameState.players.size()) {
            gameState.players.get(id).name = name;
        }
    }

    // 🎲 주사위 굴리기 (턴당 1회 제한 적용)
    public synchronized void rollDice(int playerId) {
        // 전투 중에는 주사위 금지
        if (gameState.isBattleMode) return;
        // 내 턴인지 확인
        if (gameState.currentTurnPlayerId != playerId) return;
        
        Player p = gameState.players.get(playerId);
        
        // ⭐ [핵심] 이번 턴에 이미 굴렸다면 거절
        if (p.hasRolled) {
            gameState.logMessage = "🚫 이미 주사위를 굴렸습니다. 이동하거나 턴을 넘기세요.";
            return;
        }

        // 혹시 이동력이 남아있다면 거절 (중복 방지)
        if (p.movePoints > 0) return; 

        Random rand = new Random();
        int dice = rand.nextInt(6) + 1; // 1~6
        
        p.movePoints = dice;
        p.hasRolled = true; // ⭐ 굴림 처리 완료 (passTurn에서 초기화됨)
        
        gameState.logMessage = String.format("🎲 %s 주사위 결과: %d", p.name, dice);
    }

    // 🏃 플레이어 이동
    public synchronized void movePlayer(int playerId, int dx, int dy) {
        if (gameState.isBattleMode) return; // 전투 중 이동 불가
        if (gameState.currentTurnPlayerId != playerId) return;
        
        Player p = gameState.players.get(playerId);

        // 이동력 체크
        if (p.movePoints <= 0) {
            gameState.logMessage = "🚫 이동력이 부족합니다!";
            return;
        }

        int newX = p.x + dx;
        int newY = p.y + dy;

        // 맵 범위 체크
        if (newX < 0 || newX >= 10 || newY < 0 || newY >= 10) return;
        
        // 물(1) 체크
        if (gameState.map[newY][newX] == 1) {
            gameState.logMessage = "🌊 물에는 들어갈 수 없습니다.";
            return; 
        }

        // 이동 수행
        p.x = newX;
        p.y = newY;
        p.movePoints--;

        // 🔴 몬스터 타일(2) 체크 -> 전투 시작!
        if (gameState.map[newY][newX] == 2) {
            initiateBattle(p, newX, newY); // 좌표 전달
        } else {
            // 일반 이동 로그 (너무 시끄러우면 주석 처리 가능)
            // gameState.logMessage = String.format("🏃 %s 이동함 (%d, %d)", p.name, newX, newY);
        }
    }

    // ⚔️ 전투 시작 로직
    private void initiateBattle(Player triggerPlayer, int x, int y) {
        // 전투가 일어난 좌표 저장 (승리 시 지우기 위해)
        this.battleTileX = x;
        this.battleTileY = y;
        
        gameState.isBattleMode = true;
        gameState.battleMemberIds.clear();
        gameState.monsters.clear();

        List<String> partyNames = new ArrayList<>();
        
        // 1. 전투 멤버 결성 (트리거한 사람 + 주변 2칸)
        gameState.battleMemberIds.add(triggerPlayer.id);
        partyNames.add(triggerPlayer.name);

        for (Player other : gameState.players) {
            if (other.id == triggerPlayer.id) continue;
            
            // 거리 계산 (대각선도 1칸으로 치는 체비쇼프 거리)
            int dist = Math.max(Math.abs(triggerPlayer.x - other.x), Math.abs(triggerPlayer.y - other.y));
            if (dist <= 2) {
                gameState.battleMemberIds.add(other.id);
                partyNames.add(other.name);
            }
        }

        // 2. 몬스터 생성 (고정 2마리)
        gameState.monsters.add(new Monster(0, "고블린", 50));
        gameState.monsters.add(new Monster(1, "오크", 80));

        gameState.logMessage = String.format("⚔️ 몬스터 발견! 파티: %s", String.join(", ", partyNames));
    }

    // 👊 전투 행동 처리 (공격, 스킬, 도망)
    public synchronized void processBattleAction(int playerId, BattleRequest req) {
        if (!gameState.isBattleMode) return;
        if (gameState.currentTurnPlayerId != playerId) return;

        // ⭐ [버그 수정] 전투 멤버가 아니면 행동 불가 (원격 개입 차단)
        if (!gameState.battleMemberIds.contains(playerId)) {
            return; 
        }

        Player p = gameState.players.get(playerId);
        
        // 1. 도망가기 (FLEE)
        if ("FLEE".equals(req.action)) {
            if (Math.random() < 0.5) { // 50% 확률
                endBattle(true); // 도망 성공 시 전투 종료 (맵으로 복귀)
                gameState.logMessage = "💨 " + p.name + " 파티가 도망에 성공했습니다!";
                // 도망 후 턴 넘기기
                passTurn(playerId); 
                return;
            } else {
                gameState.logMessage = "🚫 도망 실패! 몬스터에게 잡혔습니다.";
            }
        }
        // 2. 공격 및 스킬
        else {
            int damage = 0;
            boolean isAoE = false;
            String skillName = "공격";

            if ("ATTACK".equals(req.action)) damage = 15;
            else if ("SKILL1".equals(req.action)) { damage = 25; skillName = "강타"; }
            else if ("SKILL2".equals(req.action)) { damage = 10; isAoE = true; skillName = "광역기"; }

            // 데미지 적용
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

        // 몬스터 사망 처리
        checkMonsterDeath();
        
        // 승리 체크
        if (gameState.monsters.stream().allMatch(m -> m.isDead)) {
            endBattle(true);
            return;
        }

        // 몬스터 반격
        monsterCounterAttack();
        
        // 턴 넘기기
        passTurn(playerId);
    }

    private void monsterCounterAttack() {
        for (Monster m : gameState.monsters) {
            if (m.isDead) continue;
            if (!gameState.battleMemberIds.isEmpty()) {
                // 랜덤 타겟 공격
                int targetId = gameState.battleMemberIds.get(new Random().nextInt(gameState.battleMemberIds.size()));
                Player target = gameState.players.get(targetId);
                
                int dmg = 5 + new Random().nextInt(6); // 5~10
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

    // 전투 종료 처리
    private void endBattle(boolean win) {
        gameState.isBattleMode = false;
        
        // ⭐ [버그 수정] 승리 시 해당 타일을 평지(0)로 변경
        if (win && battleTileX != -1 && battleTileY != -1) {
            gameState.map[battleTileY][battleTileX] = 0;
            gameState.logMessage = "🎉 전투 승리! 몬스터가 사라졌습니다.";
        }
        
        // 좌표 초기화
        battleTileX = -1;
        battleTileY = -1;
        
        // 전투가 끝나면 현재 턴을 가진 사람이 맵에서 턴을 넘기도록 처리
        passTurn(gameState.currentTurnPlayerId);
    }

    // 🔄 턴 넘기기 (맵/전투 분리 로직)
    public synchronized void passTurn(int playerId) {
        if (gameState.currentTurnPlayerId != playerId) return;
        
        Player currentP = gameState.players.get(playerId);
        currentP.movePoints = 0; // 이동력 소멸
        currentP.hasRolled = false; // ⭐ [핵심] 다음 턴을 위해 주사위 상태 초기화

        // [CASE 1] 전투 중일 때
        if (gameState.isBattleMode) {
            // 전투 참가자 목록 안에서만 턴을 돌림
            int currentIndexInList = gameState.battleMemberIds.indexOf(playerId);
            
            // 예외 처리: 턴 주인이 전투 멤버가 아닌 경우
            if (currentIndexInList == -1) {
                gameState.currentTurnPlayerId = gameState.battleMemberIds.get(0);
            } else {
                int nextIndexInList = (currentIndexInList + 1) % gameState.battleMemberIds.size();
                gameState.currentTurnPlayerId = gameState.battleMemberIds.get(nextIndexInList);
            }
            
            // ⭐ 전투 중에는 라운드 숫자를 올리지 않음!
            Player nextP = gameState.players.get(gameState.currentTurnPlayerId);
            gameState.logMessage = String.format("⚔️ [전투] %s님의 차례입니다.", nextP.name);
        } 
        
        // [CASE 2] 맵 이동 중일 때
        else {
            // 전체 플레이어 목록에서 다음 사람 찾기
            int nextId = (gameState.currentTurnPlayerId + 1) % gameState.players.size();
            gameState.currentTurnPlayerId = nextId;
            
            // 한 바퀴 돌았으면 라운드 증가
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