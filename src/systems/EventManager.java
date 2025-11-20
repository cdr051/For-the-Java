package systems;
import java.util.function.Consumer;

public class EventManager {
    
    public void startEvent(Consumer<String> logger) {
        logger.accept("이벤트");
        // 이벤트 로직
    }
    
    public void startTreasure(Consumer<String> logger) {
        logger.accept("보물");
        // 보물 로직
    }
}