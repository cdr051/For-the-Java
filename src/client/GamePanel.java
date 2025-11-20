package client;

import shared.GameProtocol;
import shared.GameState;
import shared.Player;
import shared.TileType;
import javax.swing.*;
import java.awt.*;

public class GamePanel extends JPanel {
    private static final int MAP_HEIGHT = 6;
    private static final int MAP_WIDTH = 8;
    private JButton[][] mapButtons = new JButton[MAP_HEIGHT][MAP_WIDTH];
    private NetworkManager networkManager;

    public GamePanel(NetworkManager networkManager) {
        this.networkManager = networkManager;
        setLayout(new GridLayout(MAP_HEIGHT, MAP_WIDTH, 2, 2));
        setPreferredSize(new Dimension(800, 600));

        for (int y = 0; y < MAP_HEIGHT; y++) {
            for (int x = 0; x < MAP_WIDTH; x++) {
                JButton tileButton = new JButton();
                tileButton.setPreferredSize(new Dimension(100, 100));
                
                final int currentY = y;
                final int currentX = x;
                tileButton.addActionListener(e -> {
                   networkManager.sendMessage(new GameProtocol(GameProtocol.C_MSG_MOVE, new Point(currentX, currentY)));
                });
                
                add(tileButton);
                mapButtons[y][x] = tileButton;
            }
        }
    }
    
    public void updateState(GameState gameState) {
        TileType[][] mapData = gameState.getMap();
        for (int y = 0; y < MAP_HEIGHT; y++) {
            for (int x = 0; x < MAP_WIDTH; x++) {
                updateButtonVisuals(y, x, mapData[y][x]);
            }
        }
        drawPlayerPositions(gameState.getPlayers());
    }

    private void drawPlayerPositions(java.util.List<Player> players) {
        if (players.isEmpty()) return;
        
        // P1 위치 표시
        Player p1 = players.get(0);
        mapButtons[p1.getY()][p1.getX()].setText(p1.getPlayerName());
        
        // P2 위치 표시
        if (players.size() > 1) {
            Player p2 = players.get(1);
            if (p1.getX() == p2.getX() && p1.getY() == p2.getY()) {
                mapButtons[p1.getY()][p1.getX()].setText("P1 / P2");
            } else {
                mapButtons[p2.getY()][p2.getX()].setText(p2.getPlayerName());
            }
        }
    }

    private void updateButtonVisuals(int y, int x, TileType type) {
        JButton button = mapButtons[y][x];
        button.setText(type.name());
        
        switch (type) {
            case PLAYER:   button.setBackground(Color.GREEN);   break;
            case BOSS:     button.setBackground(Color.RED);     break;
            case MONSTER:  button.setBackground(Color.ORANGE);  break;
            case SHOP:     button.setBackground(Color.CYAN);    break;
            case TREASURE: button.setBackground(Color.YELLOW);  break;
            case EVENT:    button.setBackground(Color.MAGENTA); break;
            case BLANK:
                button.setText("Blank");
                button.setBackground(Color.LIGHT_GRAY);
                break;
        }
    }
}