package client;

import shared.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

public class GamePanel extends JPanel {
    private ClientApp mainApp;
    private GameState gameState;
    
    private MapPanel mapPanel;
    private JPanel sidePanel;
    private JLabel lblTurnInfo;
    private JLabel lblMyStatus;
    private JButton btnRoll;
    private JButton btnEndTurn;

    public GamePanel(ClientApp app) {
        this.mainApp = app;
        setLayout(new BorderLayout());
        setBackground(new Color(30, 30, 30));

        JPanel mapContainer = new JPanel(new BorderLayout()); 
        mapContainer.setBackground(new Color(30, 30, 30)); 
        
        mapPanel = new MapPanel();
        mapPanel.setBorder(new LineBorder(Color.DARK_GRAY, 2)); 
        
        mapContainer.add(mapPanel, BorderLayout.CENTER); 
        add(mapContainer, BorderLayout.CENTER);

        sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setPreferredSize(new Dimension(250, 0)); 
        sidePanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        sidePanel.setBackground(new Color(45, 45, 45)); 

        lblTurnInfo = new JLabel("게임 대기 중...");
        lblTurnInfo.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblTurnInfo.setForeground(Color.WHITE); 
        lblTurnInfo.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblMyStatus = new JLabel("-");
        lblMyStatus.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lblMyStatus.setForeground(Color.LIGHT_GRAY);
        lblMyStatus.setAlignmentX(Component.CENTER_ALIGNMENT);

        btnRoll = new JButton("🎲 주사위 굴리기");
        stylePlaceholderButton(btnRoll);
        btnRoll.addActionListener(e -> mainApp.send(new Message(Message.Type.ROLL_DICE, null)));

        btnEndTurn = new JButton("🛡️ 턴 종료");
        stylePlaceholderButton(btnEndTurn);
        btnEndTurn.addActionListener(e -> mainApp.send(new Message(Message.Type.TURN_PASS, null)));
        
        sidePanel.add(lblTurnInfo);
        sidePanel.add(Box.createVerticalStrut(30)); 
        sidePanel.add(lblMyStatus);
        sidePanel.add(Box.createVerticalGlue());    
        sidePanel.add(btnRoll);
        sidePanel.add(Box.createVerticalStrut(15)); 
        sidePanel.add(btnEndTurn);
        
        sidePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, Color.GRAY),
                new EmptyBorder(20, 20, 20, 20)
        ));
        add(sidePanel, BorderLayout.EAST);
    }

    private void stylePlaceholderButton(JButton btn) {
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setFont(new Font("SansSerif", Font.BOLD, 16));
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(70, 70, 70)); 
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(200, 50)); 
        btn.setMaximumSize(new Dimension(200, 50));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
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

        String turnColor = isMyTurn ? "#00BFFF" : "#FF6347"; 
        lblTurnInfo.setText(
            "<html><center>" +
            "⏳ ROUND " + gameState.roundNumber + "<br><br>" +
            "현재 턴<br><font size='6' color='" + turnColor + "'>" + 
            currentP.name + "</font>" +
            "</center></html>"
        );
        
        lblMyStatus.setText(
            "<html><div style='text-align: center; width: 180px;'>" +
            "<hr>" + 
            "<b>[ 내 정보 ]</b><br><br>" +
            "이동력: <font color='#00FF00'>" + me.movePoints + "</font><br>" +
            "팀 골드: <font color='#FFD700'>" + gameState.teamGold + " G</font><br>" +
            "HP: " + me.hp + " / " + me.getTotalMaxHp() +
            "</div></html>"
        );
        
        if (isMyTurn) {
            btnRoll.setEnabled(me.movePoints == 0 && !me.hasRolled); 
            btnEndTurn.setEnabled(true);
        } else {
            btnRoll.setEnabled(false);
            btnEndTurn.setEnabled(false);
        }
    }

    class MapPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (gameState == null) return;

            Image bgImg = ResourceManager.getImage("ui", "background.png");
            if (bgImg != null) {
                g.drawImage(bgImg, 0, 0, getWidth(), getHeight(), null);
            } else {
                g.setColor(new Color(20, 20, 20));
                g.fillRect(0, 0, getWidth(), getHeight());
            }

            int mapCols = 12;
            int mapRows = 8;
            

            int padding = 100; // 여백 크기

            int availableW = getWidth() - (padding * 2);
            int availableH = getHeight() - (padding * 2);
            
            int tileW = availableW / mapCols;
            int tileH = availableH / mapRows;
            int TILE_SIZE = Math.min(tileW, tileH); 
            TILE_SIZE = Math.max(TILE_SIZE, 40);

            // 중앙정렬
            int startX = (getWidth() - (TILE_SIZE * mapCols)) / 2;
            int startY = (getHeight() - (TILE_SIZE * mapRows)) / 2;

            // 맵 그리기
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 12; x++) {
                    int type = gameState.map[y][x];
                    
                    String fileName = null;
                    Color fallbackColor = Color.BLACK;

                    if (type == 0) { fileName = "tile_grass.png"; fallbackColor = Color.LIGHT_GRAY; }
                    else if (type == 1) { fileName = "tile_water.png"; fallbackColor = new Color(0, 100, 200); }
                    else if (type == 2) { fileName = "tile_mon.png"; fallbackColor = new Color(200, 50, 50); } 
                    else if (type == 3) { fileName = "tile_shop.png"; fallbackColor = Color.ORANGE; }
                    else if (type == 4) { fileName = "tile_boss.png"; fallbackColor = new Color(128, 0, 128); }
                    
                    Image tileImg = ResourceManager.getImage("map", fileName);
                    
                    int drawX = startX + (x * TILE_SIZE);
                    int drawY = startY + (y * TILE_SIZE);

                    if (tileImg != null) {
                        g.drawImage(tileImg, drawX, drawY, TILE_SIZE, TILE_SIZE, null);
                    } else { // 타일 이미지 없을시 기존 타일
                        g.setColor(fallbackColor);
                        g.fillRect(drawX, drawY, TILE_SIZE, TILE_SIZE);
                        g.setColor(Color.BLACK); 
                        g.drawRect(drawX, drawY, TILE_SIZE, TILE_SIZE);
                    }

                    int fontSize = Math.max(12, TILE_SIZE / 4);
                    g.setFont(new Font("SansSerif", Font.BOLD, fontSize));

                    if (type == 2) {
                        g.setColor(Color.WHITE);
                        g.drawString("M", drawX + TILE_SIZE/3, drawY + TILE_SIZE/2 + fontSize/2);
                    } else if (type == 3) {
                        g.setColor(Color.BLACK);
                        g.drawString("Shop", drawX + TILE_SIZE/5, drawY + TILE_SIZE/2 + fontSize/2);
                    } else if (type == 4) {
                        g.setColor(Color.WHITE);
                        g.drawString("BOSS", drawX + TILE_SIZE/5, drawY + TILE_SIZE/2 + fontSize/2);
                    }
                }
            }
            
            // 캐릭터 그리기
            for (Player p : gameState.players) {
                int drawX = startX + (p.x * TILE_SIZE);
                int drawY = startY + (p.y * TILE_SIZE);

                String charFile = "char_" + p.jobClass + ".png";
                Image charImg = ResourceManager.getImage("char", charFile);

                if (charImg != null) {
                    int pPadding = TILE_SIZE / 10;
                    g.drawImage(charImg, drawX + pPadding, drawY + pPadding, 
                              TILE_SIZE - 2*pPadding, TILE_SIZE - 2*pPadding, null);
                } else { // 캐릭터 이미지 없으면 원
                    g.setColor(p.color);
                    g.fillOval(drawX + TILE_SIZE/5, drawY + TILE_SIZE/5, TILE_SIZE * 3/5, TILE_SIZE * 3/5);
                    g.setColor(Color.WHITE); 
                    g.drawOval(drawX + TILE_SIZE/5, drawY + TILE_SIZE/5, TILE_SIZE * 3/5, TILE_SIZE * 3/5);
                }
                
                int nameSize = Math.max(10, TILE_SIZE / 5);
                g.setFont(new Font("SansSerif", Font.PLAIN, nameSize));
                g.setColor(Color.WHITE); 
                g.drawString(p.name, drawX, drawY);
            }
        }
    }
}