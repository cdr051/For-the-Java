package client;

import shared.*;
import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;

public class ShopPanel extends JPanel {
    private ClientApp mainApp;
    
    private JLabel lblGold;
    private JLabel lblStats;
    private JLabel lblWarning;

    public ShopPanel(ClientApp app) {
        this.mainApp = app;
        setLayout(new BorderLayout());
        setBackground(new Color(40, 30, 20)); // 상점 배경 (갈색톤)

        // 1. 상단 (제목 + 골드 정보)
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);
        topPanel.setBorder(BorderFactory.createEmptyBorder(30, 0, 20, 0));

        JLabel lblTitle = new JLabel("상점", SwingConstants.CENTER);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 40));
        lblTitle.setForeground(Color.ORANGE);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 10));
        infoPanel.setOpaque(false);

        lblGold = new JLabel("💰 골드: - G");
        lblGold.setFont(new Font("SansSerif", Font.BOLD, 24));
        lblGold.setForeground(new Color(255, 215, 0)); 

        lblStats = new JLabel("내 정보 로딩중...");
        lblStats.setFont(new Font("SansSerif", Font.BOLD, 20));
        lblStats.setForeground(Color.LIGHT_GRAY); 

        infoPanel.add(lblGold);
        infoPanel.add(lblStats);
        infoPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        topPanel.add(lblTitle);
        topPanel.add(Box.createVerticalStrut(10)); 
        topPanel.add(infoPanel);

        add(topPanel, BorderLayout.NORTH);

        // 2. 중앙 (상품 카드 목록) - 화면 정중앙 배치
        JPanel centerWrapper = new JPanel(new GridBagLayout()); 
        centerWrapper.setOpaque(false);
        
        JPanel cardsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 0));
        cardsPanel.setOpaque(false);

        // 나중에 이미지 넣을 카드 버튼들
        cardsPanel.add(createPlaceholderCard("공격력 강화", "ATK +5", "50 G", "ATK"));
        cardsPanel.add(createPlaceholderCard("최대 체력", "MaxHP +20", "50 G", "MAXHP"));
        cardsPanel.add(createPlaceholderCard("체력 회복", "HP +30", "30 G", "HEAL"));

        centerWrapper.add(cardsPanel);
        add(centerWrapper, BorderLayout.CENTER);

        // 3. 하단 (경고 메시지 + 나가기 버튼) - 구조 수정
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 40, 0)); // 하단 여백 넉넉히
        bottomPanel.setPreferredSize(new Dimension(800, 150)); // 높이 확보

        // 경고 메시지 (중앙)
        lblWarning = new JLabel(" ", SwingConstants.CENTER); // 초기값 공백
        lblWarning.setFont(new Font("SansSerif", Font.BOLD, 22));
        lblWarning.setForeground(Color.RED);
        
        // 나가기 버튼 (하단)
        JPanel btnPanel = new JPanel(); // 버튼 감싸는 패널
        btnPanel.setOpaque(false);
        
        JButton btnExit = new JButton("🚪 상점 나가기");
        btnExit.setFont(new Font("SansSerif", Font.BOLD, 18));
        btnExit.setPreferredSize(new Dimension(220, 60));
        btnExit.setBackground(new Color(100, 50, 50)); 
        btnExit.setForeground(Color.WHITE);
        btnExit.setFocusPainted(false);
        btnExit.addActionListener(e -> mainApp.send(new Message(Message.Type.SHOP_EXIT, null)));
        
        btnPanel.add(btnExit);

        // 배치: 위쪽엔 경고 메시지, 아래쪽엔 버튼
        bottomPanel.add(lblWarning, BorderLayout.NORTH);
        bottomPanel.add(btnPanel, BorderLayout.CENTER);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JButton createPlaceholderCard(String title, String effect, String price, String code) {
        // 나중에 이미지를 넣을 수 있게 HTML로 레이아웃 잡기
        String html = "<html><center>" +
                      "<div style='width: 180px; height: 120px; border:1px solid gray; background-color: #333; color: #aaa;'>" + 
                      "<br><br>[ 이미지 공간 ]" + 
                      "</div><br>" +
                      "<h2 style='margin:0;'>" + title + "</h2>" +
                      "<p style='margin:5px; color:#ccc;'>" + effect + "</p>" +
                      "<h2 style='color: yellow; margin:5px;'>" + price + "</h2>" +
                      "</center></html>";

        JButton btn = new JButton(html);
        btn.setPreferredSize(new Dimension(240, 350)); 
        btn.setBackground(new Color(60, 50, 40));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(new LineBorder(new Color(120, 100, 50), 2)); 
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btn.addActionListener(e -> mainApp.send(new Message(Message.Type.SHOP_BUY, code)));
        return btn;
    }

    public void updateState(GameState state) {
        if (state == null) return;
        
        try {
            int myId = mainApp.getMyId();
            if (myId < 0 || myId >= state.players.size()) return;

            Player me = state.players.get(myId);
            
            lblGold.setText(String.format("💰 골드: %d G", state.teamGold));
            lblStats.setText(String.format("   |   ⚔️ Atk: %d   ❤️ HP: %d / %d", 
                me.getTotalAttack(), me.hp, me.getTotalMaxHp()));
            
            // 경고 메시지 업데이트
            if (state.shopWarning != null && !state.shopWarning.isEmpty()) {
                lblWarning.setText("⚠️ " + state.shopWarning);
            } else {
                lblWarning.setText(" "); // 경고 없으면 공백 유지 (레이아웃 틀어짐 방지)
            }

            this.revalidate();
            this.repaint();
        } catch (Exception e) {
            System.out.println("상점 업데이트 오류: " + e.getMessage());
        }
    }
}