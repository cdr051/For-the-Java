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
        setBackground(new Color(40, 30, 20)); 

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);
        topPanel.setBorder(BorderFactory.createEmptyBorder(30, 0, 0, 0));

        JLabel lblTitle = new JLabel("상점", SwingConstants.CENTER);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 36));
        lblTitle.setForeground(Color.ORANGE);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 10));
        infoPanel.setOpaque(false);

        lblGold = new JLabel("💰 골드: 로딩중...");
        lblGold.setFont(new Font("SansSerif", Font.BOLD, 24));
        lblGold.setForeground(new Color(255, 215, 0)); 

        lblStats = new JLabel("⚔️ 공격력: -  |  ❤️ 체력: -/-");
        lblStats.setFont(new Font("SansSerif", Font.BOLD, 24));
        lblStats.setForeground(new Color(135, 206, 235)); 

        infoPanel.add(lblGold);
        infoPanel.add(lblStats);
        infoPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        topPanel.add(lblTitle);
        topPanel.add(Box.createVerticalStrut(10)); 
        topPanel.add(infoPanel);

        add(topPanel, BorderLayout.NORTH);

        JPanel centerWrapper = new JPanel(new GridBagLayout()); 
        centerWrapper.setOpaque(false);
        
        JPanel cardsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 0));
        cardsPanel.setOpaque(false);

        cardsPanel.add(createCardButton("공격력 강화", "공격력 +5", "50 G", "ATK"));
        cardsPanel.add(createCardButton("최대 체력", "MaxHP +20", "50 G", "MAXHP"));
        cardsPanel.add(createCardButton("체력 회복", "HP +30", "30 G", "HEAL"));

        centerWrapper.add(cardsPanel);
        add(centerWrapper, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS)); 
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 40, 0)); 
        
        // 경고 메시지 라벨 생성
        lblWarning = new JLabel(" "); // 초기값은 공백 (안 보이게)
        lblWarning.setFont(new Font("SansSerif", Font.BOLD, 20));
        lblWarning.setForeground(Color.RED); // 빨간색
        lblWarning.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton btnExit = new JButton("🚪 상점 나가기");
        btnExit.setFont(new Font("SansSerif", Font.BOLD, 18));
        btnExit.setPreferredSize(new Dimension(200, 60));
        btnExit.setMaximumSize(new Dimension(200, 60)); // BoxLayout 크기 고정용
        btnExit.setBackground(new Color(100, 50, 50)); 
        btnExit.setForeground(Color.WHITE);
        btnExit.setFocusPainted(false);
        btnExit.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnExit.addActionListener(e -> mainApp.send(new Message(Message.Type.SHOP_EXIT, null)));
        
        // 라벨 -> 간격 -> 버튼 순서로 추가
        bottomPanel.add(lblWarning);
        bottomPanel.add(Box.createVerticalStrut(15)); // 간격
        bottomPanel.add(btnExit);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JButton createCardButton(String title, String desc, String price, String code) {
        String html = "<html><center>" +
                      "<div style='width: 150px; height: 100px; background-color: #555; color: #aaa;'>" + 
                      "<br><br>[ 이미지 ]" + 
                      "</div><br>" +
                      "<h2>" + title + "</h2>" +
                      "<p>" + desc + "</p><br>" +
                      "<h2 style='color: yellow;'>" + price + "</h2>" +
                      "</center></html>";

        JButton btn = new JButton(html);
        btn.setPreferredSize(new Dimension(220, 350)); 
        btn.setBackground(new Color(80, 50, 30));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(new LineBorder(new Color(150, 100, 50), 3)); 
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
            lblStats.setText(String.format("⚔️ 공격력: %d  |  ❤️ 체력: %d / %d", me.attack, me.hp, me.maxHp));
            
            // 경고 메시지 업데이트
            if (state.shopWarning != null && !state.shopWarning.isEmpty()) {
                lblWarning.setText(state.shopWarning);
            } else {
                lblWarning.setText(" "); // 경고 없으면 공백 처리
            }

            this.revalidate();
            this.repaint();
        } catch (Exception e) {
            System.out.println("상점 업데이트 오류: " + e.getMessage());
        }
    }
}