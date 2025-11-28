package client;

import shared.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class GamePanel extends JPanel {
    private ClientApp mainApp;
    private GameState gameState;
    
    private MapPanel mapPanel;
    private JPanel sidePanel;
    private JLabel lblTurnInfo;
    private JLabel lblGold; 
    private JLabel lblStats;
    private JLabel lblMyStatus;
    private JButton btnRoll;
    private JButton btnEndTurn;

    public GamePanel(ClientApp app) {
        this.mainApp = app;
        setLayout(new BorderLayout());

        mapPanel = new MapPanel();
        add(mapPanel, BorderLayout.CENTER);

        sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setPreferredSize(new Dimension(220, 0)); // 너비 살짝 키움
        sidePanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        sidePanel.setBackground(new Color(230, 230, 230));

        lblTurnInfo = new JLabel("게임 대기 중...");
        lblTurnInfo.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblTurnInfo.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblGold = new JLabel("💰 0 G");
        lblGold.setFont(new Font("SansSerif", Font.BOLD, 20)); // 폰트 키움
        lblGold.setForeground(new Color(218, 165, 32)); 
        lblGold.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 스탯 라벨 설정
        lblStats = new JLabel("⚔️ - | ❤️ -/-");
        lblStats.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblStats.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblMyStatus = new JLabel("-");
        lblMyStatus.setAlignmentX(Component.CENTER_ALIGNMENT);

        btnRoll = new JButton("🎲 주사위 굴리기");
        btnRoll.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnRoll.addActionListener(e -> mainApp.send(new Message(Message.Type.ROLL_DICE, null)));

        btnEndTurn = new JButton("🛡️ 턴 종료");
        btnEndTurn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnEndTurn.addActionListener(e -> mainApp.send(new Message(Message.Type.TURN_PASS, null)));
        
        sidePanel.add(lblTurnInfo);
        sidePanel.add(Box.createVerticalStrut(20));
        sidePanel.add(lblGold); 
        sidePanel.add(Box.createVerticalStrut(10));
        sidePanel.add(lblStats); // ⭐ 패널 추가
        sidePanel.add(Box.createVerticalStrut(20));
        sidePanel.add(lblMyStatus);
        sidePanel.add(Box.createVerticalStrut(20));
        sidePanel.add(btnRoll);
        sidePanel.add(Box.createVerticalStrut(10));
        sidePanel.add(btnEndTurn);
        
        add(sidePanel, BorderLayout.EAST);
    }

    public void updateState(GameState state) {
        this.gameState = state;
        mapPanel.repaint(); 
        updateSidePanel();
    }

    private void updateSidePanel() {
        if (gameState == null) return;

        Player currentP = gameState.players.get(gameState.currentTurnPlayerId);
        boolean isMyTurn = (gameState.currentTurnPlayerId == mainApp.getMyId());
        Player me = gameState.players.get(mainApp.getMyId());

        lblTurnInfo.setText(
            "<html><center>" +
            "⏳ <b>ROUND " + gameState.roundNumber + "</b><br><br>" +
            "현재 턴:<br><font size='5'>" + currentP.name + "</font>" +
            "</center></html>"
        );
        
        if (isMyTurn) lblTurnInfo.setForeground(Color.BLUE);
        else lblTurnInfo.setForeground(Color.BLACK);

        lblGold.setText("💰 " + gameState.teamGold + " G");

        // 내 스탯 실시간 업데이트
        lblStats.setText(String.format("⚔️ %d  |  ❤️ %d/%d", me.attack, me.hp, me.maxHp));

        lblMyStatus.setText("<html>남은 이동력: <font color='red'>" + me.movePoints + "</font></html>");
        
        if (isMyTurn) {
            btnRoll.setEnabled(me.movePoints == 0); 
            btnEndTurn.setEnabled(true);
        } else {
            btnRoll.setEnabled(false);
            btnEndTurn.setEnabled(false);
        }
    }

    class MapPanel extends JPanel {
        private final int TILE_SIZE = 60; 
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (gameState == null) return;

            int mapPixelWidth = GameState.MAP_WIDTH * TILE_SIZE;
            int mapPixelHeight = GameState.MAP_HEIGHT * TILE_SIZE;
            int startX = (getWidth() - mapPixelWidth) / 2;
            int startY = (getHeight() - mapPixelHeight) / 2;

            g.translate(startX, startY);

            for (int y = 0; y < GameState.MAP_HEIGHT; y++) {
                for (int x = 0; x < GameState.MAP_WIDTH; x++) {
                    int type = gameState.map[y][x];
                    if (type == 0) g.setColor(Color.LIGHT_GRAY);
                    else if (type == 1) g.setColor(Color.CYAN);
                    else if (type == 2) g.setColor(Color.RED);
                    else if (type == 3) g.setColor(Color.ORANGE);
                    
                    g.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    g.setColor(Color.GRAY);
                    g.drawRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);

                    if (type == 3) {
                        g.setColor(Color.BLACK);
                        g.drawString("SHOP", x * TILE_SIZE + 10, y * TILE_SIZE + 35);
                    }
                }
            }
            
            for (Player p : gameState.players) {
                g.setColor(p.color);
                g.fillOval(p.x * TILE_SIZE + 5, p.y * TILE_SIZE + 5, TILE_SIZE - 10, TILE_SIZE - 10);
                g.setColor(Color.BLACK);
                g.setFont(new Font("SansSerif", Font.BOLD, 12));
                g.drawString(p.name, p.x * TILE_SIZE, p.y * TILE_SIZE);
            }
            g.translate(-startX, -startY);
        }
    }
}