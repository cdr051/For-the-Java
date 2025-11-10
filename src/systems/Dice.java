package systems;
import java.util.Random;

public class Dice {
    private Random rand;

    public Dice() {
        this.rand = new Random();
    }

    public int rollD3() {
        return rand.nextInt(3) + 1; // 0~2 + 1
    }
}