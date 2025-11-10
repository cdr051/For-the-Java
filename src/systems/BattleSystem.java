package systems;
import java.util.function.Consumer;

public class BattleSystem {
    
    // 테스트 배틀 시스템
    public boolean startMonsterBattle(Consumer<String> logger) {
        logger.accept("!!! 몬스터와 전투를 시작합니다 !!!");
        // 전투 로직 구현
        logger.accept("...전투 승리! 보상을 획득합니다.");
        return true; // 전투 승리 여부 반환
    }

    // 보스 전투 테스트
    public void startBossBattle(Consumer<String> logger) {
        logger.accept("!!! 최종 보스와의 전투를 시작합니다 !!!");
        // 보스전 로직 구현
    }
}