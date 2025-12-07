package client;

import shared.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
        sidePanel.setPreferredSize(new Dimension(240, 0)); // 패널 넓이
        sidePanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        sidePanel.setBackground(new Color(240, 235, 220)); // 종이 질감 배경

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
            // 1. 애니메이션 먼저 시작 (시각적 피드백)
            diceDisplay.startRolling(); 
            // 2. 서버에 굴리기 요청 전송
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
        sidePanel.add(diceDisplay); // 주사위 애니메이션 위치
        sidePanel.add(Box.createVerticalStrut(10));
        sidePanel.add(btnRoll);
        sidePanel.add(Box.createVerticalStrut(20));
        sidePanel.add(lblMyStatus);
        sidePanel.add(Box.createVerticalStrut(20));
        sidePanel.add(btnEndTurn);
        
        add(sidePanel, BorderLayout.EAST);
    }

    // 서버 상태 업데이트 반영
    public void updateState(GameState state) {
        this.gameState = state;
        mapPanel.repaint(); // 맵 다시 그리기
        
        // 내 턴이 아니거나 이동력이 초기화되면 주사위 리셋 (선택 사항)
        Player me = null;
        for(Player p : state.players) { if(p.id == mainApp.getMyId()) { me = p; break; } }
        
        if (me != null && me.movePoints == 0 && !diceDisplay.isAnimating) {
             // 턴 시작 전 물음표 상태 유지
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

        // 턴 정보 및 목숨 표시
        lblTurnInfo.setText("<html><center>⏳ <b>ROUND " + gameState.roundNumber + "</b><br>" +
            "<font size='5' color='red'>❤️ x " + gameState.teamLives + "</font><br><br>" + // ⭐ 목숨 표시
            "현재 턴:<br><font size='5' color='" + (isMyTurn ? "blue" : "black") + "'>" + 
            currentP.name + "</font></center></html>");

        // 내 상태
        lblMyStatus.setText("<html><center>이동력: <font color='red'><b>" + me.movePoints + "</b></font><br>" +
            "HP: " + me.hp + " / " + me.getTotalMaxHp() + "<br>Gold: " + me.gold + " G</center></html>");

        // 버튼 활성화 로직
        if (isMyTurn) {
            // 아직 안 굴렸고, 이동력도 없다면 굴리기 가능
            boolean canRoll = !me.hasRolled && me.movePoints == 0;
            btnRoll.setEnabled(canRoll);
            
            // 굴리기 가능 상태면 주사위를 '?'로 초기화 (애니메이션 중이 아닐 때만)
            if (canRoll && !diceDisplay.isAnimating) diceDisplay.reset();
            
            btnEndTurn.setEnabled(true);
        } else {
            btnRoll.setEnabled(false);
            btnEndTurn.setEnabled(false);
        }
    }
    
    // 외부(ClientApp)에서 호출하여 애니메이션 멈추고 결과 표시
    public void stopDiceAnimation(int result) {
        diceDisplay.stopRolling(result);
    }

    // =================================================================
    // 🎲 내부 클래스: 주사위 애니메이션 패널
    // =================================================================
    class DiceDisplay extends JPanel {
        private Image currentImage;
        private Timer rollTimer;
        private int rollIndex = 1;
        private boolean isAnimating = false;
        
        // 최소 애니메이션 시간 보장을 위한 변수들
        private long startTime;
        private int pendingResult = -1; // 서버에서 받은 결과 임시 저장
        private static final int MIN_DURATION = 800; // 최소 0.8초는 굴러감

        public DiceDisplay() {
            setPreferredSize(new Dimension(120, 120));
            setOpaque(false); // 배경 투명
            // 초기 이미지는 물음표
            currentImage = ResourceManager.getImage("ui", "dice_question.png"); 

            // 0.05초(50ms)마다 이미지 변경하는 타이머 (빠르게 구름)
            rollTimer = new Timer(50, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // 1. 구르는 이미지 계속 변경
                    rollIndex = (rollIndex % 4) + 1; // 1~4 반복
                    currentImage = ResourceManager.getImage("ui", "dice_rolling_" + rollIndex + ".png");
                    repaint();
                    
                    // 2. 최소 시간이 지났고, 결과값이 도착했다면 멈춤
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
            if (isAnimating) return; // 이미 돌고 있으면 무시
            isAnimating = true;
            pendingResult = -1; // 결과 초기화
            startTime = System.currentTimeMillis(); // 시작 시간 기록
            rollTimer.start();
        }

        public void stopRolling(int resultNumber) {
            // 애니메이션 중일 때만 결과를 받음 (이동 중 숫자 변경 방지)
            if (isAnimating) {
                pendingResult = resultNumber; // 결과 예약
                
                // 만약 이미 최소 시간을 넘겼다면 즉시 종료
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed >= MIN_DURATION) {
                    finalizeRoll();
                }
            }
        }
        
        // 실제 애니메이션 종료 및 결과 표시
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
                currentImage = ResourceManager.getImage("ui", "dice_question.png"); // 대기 상태 (?)
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
                // 중앙 정렬
                int x = (getWidth() - 100) / 2;
                int y = (getHeight() - 100) / 2;
                g.drawImage(currentImage, x, y, 100, 100, this);
            }
        }
    }

    // =================================================================
    // 🗺️ 내부 클래스: 맵 렌더링 패널 (이미지 적용)
    // =================================================================
    class MapPanel extends JPanel {
        private final int TILE_SIZE = 60; 
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (gameState == null) return;

            // 배경
            g.setColor(new Color(30, 30, 30));
            g.fillRect(0, 0, getWidth(), getHeight());

            // 중앙 정렬 오프셋
            int mapW = 10 * TILE_SIZE;
            int mapH = 10 * TILE_SIZE;
            int offsetX = (getWidth() - mapW) / 2;
            int offsetY = (getHeight() - mapH) / 2;

            // 1. 타일 그리기
            for (int y = 0; y < 10; y++) {
                for (int x = 0; x < 10; x++) {
                    int drawX = offsetX + x * TILE_SIZE;
                    int drawY = offsetY + y * TILE_SIZE;
                    int type = gameState.map[y][x];
                    
                    Image tileImg = null;
                    if (type == 0) tileImg = ResourceManager.getImage("map", "tile_grass.png");
                    else if (type == 1) tileImg = ResourceManager.getImage("map", "tile_water.png");
                    else if (type == 2) tileImg = ResourceManager.getImage("map", "tile_monster.png");
                    else if (type == 3) tileImg = ResourceManager.getImage("map", "tile_shop.png");

                    if (tileImg != null) {
                        g.drawImage(tileImg, drawX, drawY, TILE_SIZE, TILE_SIZE, this);
                    } else {
                        // 이미지 없을 때 색깔 대체
                        if (type == 0) g.setColor(new Color(100, 150, 50)); 
                        else if (type == 1) g.setColor(new Color(50, 100, 200)); 
                        else if (type == 2) g.setColor(new Color(180, 50, 50)); 
                        else if (type == 3) g.setColor(new Color(200, 150, 50)); 
                        g.fillRect(drawX, drawY, TILE_SIZE, TILE_SIZE);
                        g.setColor(new Color(0,0,0, 50)); 
                        g.drawRect(drawX, drawY, TILE_SIZE, TILE_SIZE);
                    }
                }
            }
            
            // 2. 플레이어 그리기
            for (Player p : gameState.players) {
                int drawX = offsetX + p.x * TILE_SIZE;
                int drawY = offsetY + p.y * TILE_SIZE;
                
                String imgName = "char_default.png";
                if(p.jobClass.equals("기사")) imgName = "char_knight.png";
                else if(p.jobClass.equals("마법사")) imgName = "char_mage.png";
                else if(p.jobClass.equals("궁수")) imgName = "char_archer.png";
                else if(p.jobClass.equals("도적")) imgName = "char_rogue.png";
                
                Image charImg = ResourceManager.getImage("character", imgName);
                
                if (charImg != null) {
                    g.drawImage(charImg, drawX + 5, drawY + 5, TILE_SIZE - 10, TILE_SIZE - 10, this);
                } else {
                    g.setColor(p.color);
                    g.fillOval(drawX + 10, drawY + 10, TILE_SIZE - 20, TILE_SIZE - 20);
                    g.setColor(Color.BLACK); 
                    g.drawOval(drawX + 10, drawY + 10, TILE_SIZE - 20, TILE_SIZE - 20);
                }

                // 닉네임
                g.setColor(Color.WHITE); 
                g.setFont(new Font("SansSerif", Font.BOLD, 11));
                FontMetrics fm = g.getFontMetrics();
                int textW = fm.stringWidth(p.name);
                
                g.setColor(new Color(0,0,0,150)); 
                g.fillRect(drawX + (TILE_SIZE-textW)/2 - 2, drawY - 12, textW + 4, 14);
                
                g.setColor(Color.WHITE); 
                g.drawString(p.name, drawX + (TILE_SIZE - textW) / 2, drawY);
            }
        }
    }
}