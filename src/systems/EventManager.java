package systems;
import java.util.function.Consumer;

public class EventManager {
    
    public void startEvent(Consumer<String> logger) {
        logger.accept("특별 이벤트를 발견했습니다! (내용: ...)");
        // 이벤트 로직
    }
    
    public void startTreasure(Consumer<String> logger) {
        logger.accept("보물 상자를 발견했습니다! (보상: ...)");
        // 보물 로직
    }
}