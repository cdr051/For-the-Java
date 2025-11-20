package systems;

import shared.Monster;
import shared.Player;
import shared.AttackInfo;
import shared.AttackType; // [수정] shared 패키지의 Enum 사용
import java.util.List;
import java.util.function.Consumer;

public class BattleSystem {

    private int battleTurn = 0;

    public Monster[] startMonsterBattle(Consumer<String> logger) {
        logger.accept("!!! 몬스터와 전투를 시작합니다 !!!");
        this.battleTurn = 0;
        Monster monster1 = new Monster("슬라임A", 5, 0, 1, 1, 15);
        Monster monster2 = new Monster("슬라임B", 5, 0, 1, 1, 15);
        return new Monster[] { monster1, monster2 };
    }

    public void handlePlayerAttack(Player player, List<Monster> monsters, AttackInfo info, Consumer<String> logger) {
        
        if (monsters == null || monsters.isEmpty()) return;

        switch (info.getAttackType()) {
            case SINGLE_PHYSICAL:
                Monster target = monsters.get(info.getTargetIndex());
                int damage = player.getPhysicalAttack() - target.getPhysicalDefense();
                damage = Math.max(0, damage);
                logger.accept(player.getPlayerName() + "이(가) " + target.getName() + "에게 " + damage + " 물리 데미지!");
                if (target.takeDamage(damage)) logger.accept(target.getName() + " 처치!");
                break;
                
            case MULTI_PHYSICAL:
                int multiPhysDamage = player.getPhysicalAttack() / 2;
                logger.accept(player.getPlayerName() + "이(가) 광역 물리 공격!");
                for (Monster m : monsters) {
                    if (m.isDead()) continue;
                    int dmg = multiPhysDamage - m.getPhysicalDefense();
                    dmg = Math.max(0, dmg);
                    logger.accept(m.getName() + "에게 " + dmg + " 데미지!");
                    if (m.takeDamage(dmg)) logger.accept(m.getName() + " 처치!");
                }
                break;
                
            case SINGLE_MAGICAL:
                Monster magicTarget = monsters.get(info.getTargetIndex());
                int magicDamage = player.getMagicalAttack() - magicTarget.getMagicalDefense();
                magicDamage = Math.max(0, magicDamage);
                logger.accept(player.getPlayerName() + "이(가) " + magicTarget.getName() + "에게 " + magicDamage + " 마법 데미지!");
                if (magicTarget.takeDamage(magicDamage)) logger.accept(magicTarget.getName() + " 처치!");
                break;
                
            case MULTI_MAGICAL:
                int multiMagDamage = player.getMagicalAttack() / 2;
                logger.accept(player.getPlayerName() + "이(가) 광역 마법 공격!");
                for (Monster m : monsters) {
                    if (m.isDead()) continue;
                    int dmg = multiMagDamage - m.getMagicalDefense();
                    dmg = Math.max(0, dmg);
                    logger.accept(m.getName() + "에게 " + dmg + " 데미지!");
                    if (m.takeDamage(dmg)) logger.accept(m.getName() + " 처치!");
                }
                break;
        }
    }
    
    public void processMonsterTurn(Player player, List<Monster> monsters, Consumer<String> logger) {
        battleTurn++; 
        logger.accept("--- 몬스터 턴 (턴: " + battleTurn + ") ---");
        
        for (Monster m : monsters) {
            if (!m.isDead()) { 
                m.scaleStats(battleTurn);
                int damage = m.getPhysicalAttack() - player.getPhysicalDefense();
                damage = Math.max(0, damage);
                logger.accept(m.getName() + "이(가) " + player.getPlayerName() + "에게 " + damage + " 데미지!");
                if(player.takeDamage(damage)) {
                    logger.accept(player.getPlayerName() + "이(가) 쓰러졌습니다...");
                }
            }
        }
    }

    public boolean isBattleOver(List<Monster> monsters, Player player) {
        if (player.isDead()) return true;
        boolean allMonstersDead = true;
        for (Monster m : monsters) {
            if (!m.isDead()) {
                allMonstersDead = false;
                break;
            }
        }
        return allMonstersDead;
    }
}