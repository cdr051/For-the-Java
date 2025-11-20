package systems;
import java.util.function.Consumer;

public class ShopManager {
    
    public void openShop(Consumer<String> logger) {
        logger.accept("상점");
        // 상점 로직 shop GUI와 연동필요
    }
}