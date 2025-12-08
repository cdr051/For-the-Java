package client;

import shared.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

public class GamePanel extends JPanel {
    private ClientApp mainApp;
    private GameState gameState;
    
    // UI 컴포넌트
    private MapPanel mapPanel; 
    private JPanel sidePanel;
    private JLabel lblTurnInfo;
    private JLabel lblMyStatus;
    private JButton btnRoll;
    private JButton btnEndTurn;
    
    // 주사위 디스플레이 패널
    private DiceDisplay diceDisplay;

    public GamePanel(ClientApp app) {
        this.mainApp = app;
        setLayout(new BorderLayout());

        // 1. 맵 패널 (중앙)
        mapPanel = new MapPanel();
        add(mapPanel, BorderLayout.CENTER);

        // 2. 사이드 패널 (우측)
        sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setPreferredSize(new Dimension(240, 0)); 
        sidePanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        sidePanel.setBackground(new Color(240, 235, 220)); 

        lblTurnInfo = new JLabel("게임 대기 중...");
        lblTurnInfo.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblTurnInfo.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 주사위 패널 생성
        diceDisplay = new DiceDisplay();
        diceDisplay.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblMyStatus = new JLabel("-");
        lblMyStatus.setFont(new Font("Monospaced", Font.PLAIN, 14));
        lblMyStatus.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 주사위 굴리기 버튼
        btnRoll = new JButton("🎲 주사위 굴리기");
        btnRoll.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnRoll.setFocusable(false);
        btnRoll.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnRoll.setPreferredSize(new Dimension(180, 40));
        btnRoll.addActionListener(e -> {
            diceDisplay.startRolling(); 
            mainApp.send(new Message(Message.Type.ROLL_DICE, null));
        });

        // 턴 종료 버튼
        btnEndTurn = new JButton("🛡️ 턴 종료");
        btnEndTurn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnEndTurn.setFocusable(false);
        btnEndTurn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnEndTurn.setPreferredSize(new Dimension(180, 40));
        btnEndTurn.addActionListener(e -> mainApp.send(new Message(Message.Type.TURN_PASS, null)));
        
        // 패널에 요소 추가
        sidePanel.add(lblTurnInfo);
        sidePanel.add(Box.createVerticalStrut(20));
        sidePanel.add(diceDisplay); 
        sidePanel.add(Box.createVerticalStrut(10));
        sidePanel.add(btnRoll);
        sidePanel.add(Box.createVerticalStrut(20));
        sidePanel.add(lblMyStatus);
        sidePanel.add(Box.createVerticalStrut(20));
        sidePanel.add(btnEndTurn);
        
        add(sidePanel, BorderLayout.EAST);
    }

    public void updateState(GameState state) {
        this.gameState = state;
        mapPanel.repaint(); // 맵 다시 그리기
        
        Player me = null;
        for(Player p : state.players) { if(p.id == mainApp.getMyId()) { me = p; break; } }
        
        if (me != null && me.movePoints == 0 && !diceDisplay.isAnimating) {
            // 주사위 애니메이션 중이 아닐 때 상태 업데이트
        }

        updateSidePanel();  
    }

    private void updateSidePanel() {
        if (gameState == null || gameState.players.isEmpty()) return;

        Player currentP = gameState.players.get(gameState.currentTurnPlayerId);
        boolean isMyTurn = (gameState.currentTurnPlayerId == mainApp.getMyId());
        Player me = null;
        for(Player p : gameState.players) { if(p.id == mainApp.getMyId()) { me = p; break; } }
        if(me == null) return;

        lblTurnInfo.setText("<html><center>⏳ <b>ROUND " + gameState.roundNumber + "</b><br>" +
            "<font size='5' color='red'>❤️ x " + gameState.teamLives + "</font><br><br>" + 
            "현재 턴:<br><font size='5' color='" + (isMyTurn ? "blue" : "black") + "'>" + 
            currentP.name + "</font></center></html>");

        lblMyStatus.setText("<html><center>이동력: <font color='red'><b>" + me.movePoints + "</b></font><br>" +
            "HP: " + me.hp + " / " + me.getTotalMaxHp() + "<br>Gold: " + me.gold + " G</center></html>");

        if (isMyTurn) {
            boolean canRoll = !me.hasRolled && me.movePoints == 0;
            btnRoll.setEnabled(canRoll);
            if (canRoll && !diceDisplay.isAnimating) diceDisplay.reset();
            btnEndTurn.setEnabled(true);
        } else {
            btnRoll.setEnabled(false);
            btnEndTurn.setEnabled(false);
        }
    }
    
    public void stopDiceAnimation(int result) {
        diceDisplay.stopRolling(result);
    }

    class DiceDisplay extends JPanel {
        private Image currentImage;
        private javax.swing.Timer rollTimer; 
        private int rollIndex = 1;
        private boolean isAnimating = false;
        
        private long startTime;
        private int pendingResult = -1; 
        private static final int MIN_DURATION = 800; 

        public DiceDisplay() {
            setPreferredSize(new Dimension(120, 120));
            setOpaque(false); 
            currentImage = ResourceManager.getImage("ui", "dice_question.png"); 
            rollTimer = new javax.swing.Timer(50, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    rollIndex = (rollIndex % 4) + 1; 
                    currentImage = ResourceManager.getImage("ui", "dice_rolling_" + rollIndex + ".png");
                    repaint();
                    
                    if (pendingResult != -1) {
                        long elapsed = System.currentTimeMillis() - startTime;
                        if (elapsed >= MIN_DURATION) {
                            finalizeRoll();
                        }
                    }
                }
            });
        }

        public void startRolling() {
            if (isAnimating) return; 
            isAnimating = true;
            pendingResult = -1; 
            startTime = System.currentTimeMillis(); 
            rollTimer.start();
        }

        public void stopRolling(int resultNumber) {
            if (isAnimating) {
                pendingResult = resultNumber; 
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed >= MIN_DURATION) {
                    finalizeRoll();
                }
            }
        }
        
        private void finalizeRoll() {
            rollTimer.stop();
            isAnimating = false;
            setNumber(pendingResult);
            pendingResult = -1;
        }
        
        public void setNumber(int num) {
            if (num > 0 && num <= 6) {
                currentImage = ResourceManager.getImage("ui", "dice_" + num + ".png");
            } else {
                currentImage = ResourceManager.getImage("ui", "dice_question.png"); 
            }
            repaint();
        }
        
        public void reset() {
            if(!isAnimating) setNumber(0);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (currentImage != null) {
                int x = (getWidth() - 100) / 2;
                int y = (getHeight() - 100) / 2;
                g.drawImage(currentImage, x, y, 100, 100, this);
            }
        }
    }

    class MapPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (gameState == null) return;

            g.setColor(new Color(30, 30, 30));
            g.fillRect(0, 0, getWidth(), getHeight());

            int cols = 12;
            int rows = 8;
            
            // 여백 확보 (85%)
            int availableW = (int)(getWidth() * 0.85);
            int availableH = (int)(getHeight() * 0.85);
            
            int tileSize = Math.min(availableW / cols, availableH / rows);
            
            int totalMapW = tileSize * cols;
            int totalMapH = tileSize * rows;
            int offsetX = (getWidth() - totalMapW) / 2;
            int offsetY = (getHeight() - totalMapH) / 2;

            // 테두리
            g.setColor(new Color(60, 60, 60));
            g.drawRect(offsetX - 2, offsetY - 2, totalMapW + 4, totalMapH + 4);

            // 1. 타일 그리기
            for (int y = 0; y < rows; y++) {
                for (int x = 0; x < cols; x++) {
                    int drawX = offsetX + x * tileSize;
                    int drawY = offsetY + y * tileSize;
                    int type = gameState.map[y][x];
                    
                    Image tileImg = null;
                    if (type == 0) tileImg = ResourceManager.getImage("map", "tile_grass.png");
                    else if (type == 1) tileImg = ResourceManager.getImage("map", "tile_water.png");
                    else if (type == 2) tileImg = ResourceManager.getImage("map", "tile_monster.png");
                    else if (type == 3) tileImg = ResourceManager.getImage("map", "tile_shop.png");

                    if (tileImg != null) {
                        g.drawImage(tileImg, drawX, drawY, tileSize, tileSize, this);
                    } else {
                        if (type == 0) g.setColor(new Color(100, 150, 50)); 
                        else if (type == 1) g.setColor(new Color(50, 100, 200)); 
                        else if (type == 2) g.setColor(new Color(180, 50, 50)); 
                        else if (type == 3) g.setColor(new Color(200, 150, 50)); 
                        g.fillRect(drawX, drawY, tileSize, tileSize);
                    }
                    g.setColor(new Color(0,0,0, 50)); 
                    g.drawRect(drawX, drawY, tileSize, tileSize);
                }
            }
            
            // 플레이어 겹쳤을때 좌표별로 묶ㅇ므
            Map<String, java.util.List<Player>> tileMap = new HashMap<>();
            for (Player p : gameState.players) {
                String key = p.x + "," + p.y;
                if (!tileMap.containsKey(key)) tileMap.put(key, new ArrayList<>());
                tileMap.get(key).add(p);
            }

            for (java.util.List<Player> group : tileMap.values()) {
                group.sort((p1, p2) -> Integer.compare(p1.id, p2.id));
                
                int count = group.size();
                int offsetStep = tileSize / 5;
                
                for (int i = 0; i < count; i++) {
                    Player p = group.get(i);
                    
                    int baseX = offsetX + p.x * tileSize;
                    int baseY = offsetY + p.y * tileSize;
                    int shift = (int)((i - (count - 1) / 2.0) * offsetStep);
                    int drawX = baseX + shift;
                    int drawY = baseY + shift;
                    
                    String imgName = "char_default.png";
                    if(p.jobClass.equals("기사")) imgName = "char_knight.png";
                    else if(p.jobClass.equals("마법사")) imgName = "char_mage.png";
                    else if(p.jobClass.equals("궁수")) imgName = "char_archer.png";
                    else if(p.jobClass.equals("도적")) imgName = "char_rogue.png";
                    
                    Image charImg = ResourceManager.getImage("character", imgName);
                    double scale = (count > 1) ? 0.6 : 0.85;
                    int charSize = (int)(tileSize * scale);
                    int charOffset = (tileSize - charSize) / 2;

                    if (charImg != null) {
                        g.drawImage(charImg, drawX + charOffset, drawY + charOffset, charSize, charSize, this);
                    } else {
                        g.setColor(p.color);
                        g.fillOval(drawX + charOffset, drawY + charOffset, charSize, charSize);
                    }

                    g.setColor(Color.WHITE); 
                    int fontSize = Math.max(10, tileSize / 5);
                    g.setFont(new Font("SansSerif", Font.BOLD, fontSize));
                    FontMetrics fm = g.getFontMetrics();
                    int textW = fm.stringWidth(p.name);

                    g.setColor(new Color(0,0,0,150)); 
                    int nameY = drawY + charOffset - 5; // 머리 위
                    g.fillRect(drawX + charOffset + (charSize - textW)/2 - 2, nameY - fontSize, textW + 4, fontSize + 4);

                    g.setColor(Color.WHITE); 
                    g.drawString(p.name, drawX + charOffset + (charSize - textW)/2, nameY);
                }
            }
        }
    }
}