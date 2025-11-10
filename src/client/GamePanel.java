package client;

import shared.TileType;
import systems.BattleSystem;
import systems.Dice;
import systems.EventManager;
import systems.ShopManager;
import javax.swing.*;
import java.awt.*;

public class GamePanel extends JPanel {
    private static final int MAP_HEIGHT = 6;
    private static final int MAP_WIDTH = 8;
    
    private TileType[][] mapData;
    private JButton[][] mapButtons = new JButton[MAP_HEIGHT][MAP_WIDTH];
    
    //턴제
    private int currentPlayer = 1; // 1=P1, 2=P2
    private int player1X = 0, player1Y = 5;
    private int player2X = 0, player2Y = 5;

    // 기능 클래스
    private Dice dice;
    private BattleSystem battleSystem;
    private ShopManager shopManager;
    private EventManager eventManager;
    
    private HudPanel hud; // 로그 출력
    private int currentDiceRoll = 0; // 주사위 결과

    public GamePanel(HudPanel hud) {
        this.hud = hud; 
        this.dice = new Dice();
        this.battleSystem = new BattleSystem();
        this.shopManager = new ShopManager();
        this.eventManager = new EventManager();
        this.mapData = server.MapGenerator.createMap();
        this.mapData[5][0] = TileType.BLANK; 

        setLayout(new GridLayout(MAP_HEIGHT, MAP_WIDTH, 2, 2));
        for (int y = 0; y < MAP_HEIGHT; y++) {
            for (int x = 0; x < MAP_WIDTH; x++) {
                JButton tileButton = new JButton();
                tileButton.setPreferredSize(new Dimension(100, 100));
                
                final int currentY = y;
                final int currentX = x;
                
                tileButton.addActionListener(e -> handleTileClick(currentY, currentX));
                
                add(tileButton);
                mapButtons[y][x] = tileButton;
                updateButtonVisuals(y, x);
            }
        }
        drawPlayerPositions();
    }
    
    public int getCurrentPlayer() {
        return this.currentPlayer;
    }

    public void rollDice() {
        if (currentDiceRoll != 0) return;

        this.currentDiceRoll = dice.rollD3();
        hud.log("--- Player " + currentPlayer + " 턴 ---");
        hud.log("주사위 결과: " + currentDiceRoll);
        
        int currentX = (currentPlayer == 1) ? player1X : player2X;
        int currentY = (currentPlayer == 1) ? player1Y : player2Y;

        hud.log(currentDiceRoll + "칸 이하로 이동하세요.");
        hud.disableDiceButton(); // 주사위 버튼 비활성화
    }

    /**
     * (GamePanel의 타일 클릭 시) 타일 클릭 처리
     */
    private void handleTileClick(int y, int x) {
        //주사위 굴린지 확인
        if (currentDiceRoll == 0) {
            hud.log("먼저 [주사위 굴리기] 버튼을 눌러주세요.");
            return;
        }

        //플레이어의 현재 좌표 가져오기
        int oldX = (currentPlayer == 1) ? player1X : player2X;
        int oldY = (currentPlayer == 1) ? player1Y : player2Y;

        //이동거리 확인
        if (!isValidMove(oldX, oldY, x, y, currentDiceRoll)) {
            hud.log("이동 불가: (" + x + "," + y + ")는 " + currentDiceRoll + "칸 이내(상하좌우)가 아닙니다.");
            return;
        }
        
        //이동처리
        if (currentPlayer == 1) {
            player1X = x; player1Y = y;
        } else {
            player2X = x; player2Y = y;
        }
        hud.log("Player " + currentPlayer + " 이동: (" + oldX + "," + oldY + ") -> (" + x + "," + y + ")");

        processTileEvent(y, x);
        drawPlayerPositions();
        
        //턴 끝
        currentDiceRoll = 0;
        currentPlayer = (currentPlayer == 1) ? 2 : 1; // 턴 전환
        hud.log("--- Player " + currentPlayer + "의 턴입니다. ---");
        hud.enableDiceButton(); // 주사위 버튼 다시 활성화
    }
    
    private void drawPlayerPositions() {
        for (int y = 0; y < MAP_HEIGHT; y++) {
            for (int x = 0; x < MAP_WIDTH; x++) {
            	
                updateButtonVisuals(y, x);
                
                boolean p1Here = (player1X == x && player1Y == y);
                boolean p2Here = (player2X == x && player2Y == y);

                if (p1Here && p2Here) {
                    mapButtons[y][x].setText("P1 / P2");
                } else if (p1Here) {
                    mapButtons[y][x].setText("Player 1");
                } else if (p2Here) {
                    mapButtons[y][x].setText("Player 2");
                }
            }
        }
    }

    private void processTileEvent(int y, int x) {
        TileType type = mapData[y][x];
        hud.log("도착한 타일: " + type);

        switch (type) {
            case MONSTER:
                battleSystem.startMonsterBattle(hud::log);
                mapData[y][x] = TileType.BLANK;
                break;
            case BOSS:
                battleSystem.startBossBattle(hud::log);
                mapData[y][x] = TileType.BLANK;
                break;
            case SHOP:
                shopManager.openShop(hud::log);
                mapData[y][x] = TileType.BLANK;
                break;
            case TREASURE:
                eventManager.startTreasure(hud::log);
                mapData[y][x] = TileType.BLANK;
                break;
            case EVENT:
                eventManager.startEvent(hud::log);
                mapData[y][x] = TileType.BLANK;
                break;
            case BLANK:
                hud.log("이 칸은 이미 탐험이 끝난 빈 칸입니다.");
                break;
            case PLAYER:
                hud.log("시작 지점입니다.");
                break;
        }
    }
    
    private boolean isValidMove(int currentX, int currentY, int targetX, int targetY, int diceRoll) {
        int distance = Math.abs(targetX - currentX) + Math.abs(targetY - currentY);
        return distance > 0 && distance <= diceRoll;
    }

    private void updateButtonVisuals(int y, int x) {
        JButton button = mapButtons[y][x];
        TileType type = mapData[y][x];
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
        button.setEnabled(true); 
    }
}