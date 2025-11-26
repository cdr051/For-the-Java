package client;

import shared.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class GamePanel extends JPanel {
    private ClientApp mainApp;
    private GameState gameState;
    
    // UI 컴포넌트
    private MapPanel mapPanel; // 맵 그리는 부분 분리
    private JPanel sidePanel;
    private JLabel lblTurnInfo;
    private JLabel lblMyStatus;
    private JButton btnRoll;
    private JButton btnEndTurn;

    public GamePanel(ClientApp app) {
        this.mainApp = app;
        setLayout(new BorderLayout());

        // 1. 맵 패널 (중앙)
        mapPanel = new MapPanel();
        add(mapPanel, BorderLayout.CENTER);

        // 2. 사이드 패널 (우측)
        sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setPreferredSize(new Dimension(200, 0));
        sidePanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        sidePanel.setBackground(new Color(230, 230, 230));

        lblTurnInfo = new JLabel("게임 대기 중...");
        lblTurnInfo.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblTurnInfo.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblMyStatus = new JLabel("-");
        lblMyStatus.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 버튼들
        btnRoll = new JButton("🎲 주사위 굴리기");
        btnRoll.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnRoll.addActionListener(e -> mainApp.send(new Message(Message.Type.ROLL_DICE, null)));

        btnEndTurn = new JButton("🛡️ 턴 종료");
        btnEndTurn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnEndTurn.addActionListener(e -> mainApp.send(new Message(Message.Type.TURN_PASS, null)));
        
        // 간격 띄우기 및 추가
        sidePanel.add(lblTurnInfo);
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
        mapPanel.repaint(); // 맵 다시 그리기 요청
        updateSidePanel();  // 사이드 패널 갱신
    }

    private void updateSidePanel() {
        if (gameState == null) return;

        Player currentP = gameState.players.get(gameState.currentTurnPlayerId);
        boolean isMyTurn = (gameState.currentTurnPlayerId == mainApp.getMyId());
        Player me = gameState.players.get(mainApp.getMyId());

        // ⭐ [수정] 라운드 정보와 턴 정보를 함께 표시
        lblTurnInfo.setText(
            "<html><center>" +
            "⏳ <b>ROUND " + gameState.roundNumber + "</b><br><br>" + // 라운드 표시
            "현재 턴:<br><font size='5'>" + currentP.name + "</font>" +
            "</center></html>"
        );
        
        // 내 턴이면 파란색, 아니면 검은색
        if (isMyTurn) lblTurnInfo.setForeground(Color.BLUE);
        else lblTurnInfo.setForeground(Color.BLACK);

        // ... (나머지 버튼 로직 유지)
        lblMyStatus.setText("<html>남은 이동력: <font color='red'>" + me.movePoints + "</font></html>");
        
        if (isMyTurn) {
            btnRoll.setEnabled(me.movePoints == 0); 
            btnEndTurn.setEnabled(true);
        } else {
            btnRoll.setEnabled(false);
            btnEndTurn.setEnabled(false);
        }
    }

    // 내부 클래스: 맵 그리기 전용
    class MapPanel extends JPanel {
        private final int TILE_SIZE = 50;
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (gameState == null) return;

            // 맵 그리기
            for (int y = 0; y < 10; y++) {
                for (int x = 0; x < 10; x++) {
                    int type = gameState.map[y][x];
                    if (type == 0) g.setColor(Color.LIGHT_GRAY);
                    else if (type == 1) g.setColor(Color.CYAN);
                    else g.setColor(Color.RED);
                    
                    g.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    g.setColor(Color.GRAY);
                    g.drawRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }
            }
            
            // 플레이어 그리기
            for (Player p : gameState.players) {
                g.setColor(p.color);
                g.fillOval(p.x * TILE_SIZE + 5, p.y * TILE_SIZE + 5, TILE_SIZE - 10, TILE_SIZE - 10);
                
                // 이름 (닉네임 수정 반영됨)
                g.setColor(Color.BLACK);
                g.setFont(new Font("SansSerif", Font.PLAIN, 10));
                g.drawString(p.name, p.x * TILE_SIZE, p.y * TILE_SIZE);
            }
        }
    }
}