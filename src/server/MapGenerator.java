package server;

import shared.TileType;
import java.util.Random;

public class MapGenerator {
    private static final int MAP_HEIGHT = 6;
    private static final int MAP_WIDTH = 8;

    public static TileType[][] createMap() {
        TileType[][] map = new TileType[MAP_HEIGHT][MAP_WIDTH];
        Random rand = new Random();
        TileType[] events = {TileType.MONSTER, TileType.SHOP, TileType.TREASURE, TileType.EVENT};

        for (int y = 0; y < 6; y++) {
            for (int x = 0; x < 8; x++) {
                if (x == 0 && y == 5) map[y][x] = TileType.PLAYER;
                else if (x == 7 && y == 0) map[y][x] = TileType.BOSS;
                else map[y][x] = events[rand.nextInt(events.length)];
            }
        }
        return map;
    }
}