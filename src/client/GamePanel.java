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
        setBackground(new Color(30, 30, 30)); // [원래 스타일] 다크 배경

        // 1. 맵 영역
        JPanel mapContainer = new JPanel(new GridBagLayout()); 
        mapContainer.setBackground(new Color(30, 30, 30)); 
        
        mapPanel = new MapPanel();
        // 크기는 paintComponent에서 결정되지만 기본값 설정
        mapPanel.setPreferredSize(new Dimension(800, 600)); 
        mapPanel.setBorder(new LineBorder(Color.DARK_GRAY, 2)); 
        
        mapContainer.add(mapPanel); 
        add(mapContainer, BorderLayout.CENTER);

        // 2. 사이드 패널
        sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setPreferredSize(new Dimension(250, 0)); 
        sidePanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        sidePanel.setBackground(new Color(45, 45, 45)); // [원래 스타일] 사이드바 배경

        lblTurnInfo = new JLabel("게임 대기 중...");
        lblTurnInfo.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblTurnInfo.setForeground(Color.WHITE); // [원래 스타일] 흰색 글씨
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
          
            g.setColor(new Color(20, 20, 20));
            g.fillRect(0, 0, getWidth(), getHeight());

            int mapCols = 12;
            int mapRows = 8;
            
            int panelW = getWidth();
            int panelH = getHeight();
            int tileW = panelW / mapCols;
            int tileH = panelH / mapRows;
            int TILE_SIZE = Math.min(tileW, tileH); 

            int startX = (panelW - (TILE_SIZE * mapCols)) / 2;
            int startY = (panelH - (TILE_SIZE * mapRows)) / 2;

            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 12; x++) {
                    int type = gameState.map[y][x];
                    
                    if (type == 0) g.setColor(Color.LIGHT_GRAY);       
                    else if (type == 1) g.setColor(new Color(0, 100, 200)); 
                    else if (type == 2) g.setColor(new Color(200, 50, 50)); 
                    else if (type == 3) g.setColor(Color.ORANGE);      
                    else if (type == 4) g.setColor(new Color(128, 0, 128)); // 보스
                    
                    int drawX = startX + (x * TILE_SIZE);
                    int drawY = startY + (y * TILE_SIZE);

                    g.fillRect(drawX, drawY, TILE_SIZE, TILE_SIZE);
                    g.setColor(Color.BLACK); 
                    g.drawRect(drawX, drawY, TILE_SIZE, TILE_SIZE);
                    
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
            
            for (Player p : gameState.players) {
                int drawX = startX + (p.x * TILE_SIZE);
                int drawY = startY + (p.y * TILE_SIZE);

                g.setColor(p.color);
                g.fillOval(drawX + TILE_SIZE/5, drawY + TILE_SIZE/5, TILE_SIZE * 3/5, TILE_SIZE * 3/5);
                g.setColor(Color.WHITE); 
                g.drawOval(drawX + TILE_SIZE/5, drawY + TILE_SIZE/5, TILE_SIZE * 3/5, TILE_SIZE * 3/5);
                
                int nameSize = Math.max(10, TILE_SIZE / 5);
                g.setFont(new Font("SansSerif", Font.PLAIN, nameSize));
                g.drawString(p.name, drawX, drawY);
            }
        }
    }
}