package client;

import shared.*;
import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;

public class ShopPanel extends JPanel {
    private ClientApp mainApp;
    
    // UI 컴포넌트
    private JLabel lblTitle;
    private JLabel lblShopperName; 
    private JLabel lblGold;
    private JLabel lblStats;
    private JLabel lblWarning;
    private JButton btnExit;

    public ShopPanel(ClientApp app) {
        this.mainApp = app;
        setLayout(new BorderLayout());
        // 배경색은 이미지가 없을 때를 대비한 예비용
        setBackground(new Color(40, 30, 20)); 

        // ------------------------------------------------
        // 1. 상단 (제목 + 구매자 + 골드 정보)
        // ------------------------------------------------
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);
        topPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));

        lblTitle = new JLabel("💰 여행자의 상점", SwingConstants.CENTER);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 36));
        lblTitle.setForeground(new Color(255, 200, 50)); 
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // ⭐ [수정] 방문객 표시 더 잘 보이게 강조
        lblShopperName = new JLabel("방문객: -");
        lblShopperName.setFont(new Font("SansSerif", Font.BOLD, 22)); // 폰트 키움
        lblShopperName.setForeground(new Color(100, 255, 100)); // 밝은 연두색
        lblShopperName.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 10));
        infoPanel.setOpaque(false);

        lblGold = new JLabel("팀 자금: - G");
        lblGold.setFont(new Font("SansSerif", Font.BOLD, 24));
        lblGold.setForeground(new Color(255, 215, 0)); 

        lblStats = new JLabel("내 정보 로딩...");
        lblStats.setFont(new Font("Monospaced", Font.PLAIN, 16));
        lblStats.setForeground(Color.LIGHT_GRAY);

        infoPanel.add(lblGold);
        infoPanel.add(lblStats);

        lblWarning = new JLabel(" ");
        lblWarning.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblWarning.setForeground(Color.RED);
        lblWarning.setAlignmentX(Component.CENTER_ALIGNMENT);

        topPanel.add(lblTitle);
        topPanel.add(Box.createVerticalStrut(10));
        topPanel.add(lblShopperName);
        topPanel.add(infoPanel);
        topPanel.add(lblWarning);

        add(topPanel, BorderLayout.NORTH);

        // ------------------------------------------------
        // 2. 중앙 (아이템 카드 목록)
        // ------------------------------------------------
        JPanel itemPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 20));
        itemPanel.setOpaque(false);

        // 이미지 버튼 생성 (파일명, 코드, 가격, 설명)
        itemPanel.add(createCardButton("card_atk.png", "ATK", 50, "공격력 +5"));
        itemPanel.add(createCardButton("card_hp.png", "MAXHP", 50, "최대체력 +20"));
        itemPanel.add(createCardButton("card_heal.png", "HEAL", 30, "체력 회복"));

        add(itemPanel, BorderLayout.CENTER);

        // ------------------------------------------------
        // 3. 하단 (나가기 버튼)
        // ------------------------------------------------
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 30, 0));

        btnExit = new JButton("나가기 (턴 종료)");
        btnExit.setPreferredSize(new Dimension(200, 50));
        btnExit.setFont(new Font("SansSerif", Font.BOLD, 16));
        btnExit.setBackground(new Color(150, 50, 50));
        btnExit.setForeground(Color.WHITE);
        btnExit.setFocusPainted(false);
        btnExit.addActionListener(e -> mainApp.send(new Message(Message.Type.SHOP_EXIT, null)));

        bottomPanel.add(btnExit);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    // 배경 이미지 그리기
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Image bg = ResourceManager.getImage("shop", "bg_shop.png");
        if (bg != null) {
            g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
        }
    }

    // ⭐ [핵심 수정] 이미지 카드 버튼 생성 (텍스트 잘림 해결)
    private JButton createCardButton(String imgName, String code, int price, String desc) {
        JButton btn = new JButton();
        // 버튼 전체 크기 (가로 220, 세로 320으로 조금 늘림)
        btn.setPreferredSize(new Dimension(220, 320));
        
        // 이미지 아이콘 로드 및 리사이징
        Image img = ResourceManager.getImage("shop", imgName);
        if (img != null) {
            // ⭐ 이미지를 버튼보다 작게 줄여서 텍스트 공간 확보 (세로 230px)
            btn.setIcon(new ImageIcon(img.getScaledInstance(200, 230, Image.SCALE_SMOOTH)));
        } else {
            // 이미지가 없으면 텍스트로 대체
            btn.setText("<html><center><h2>" + code + "</h2><br>" + desc + "</center></html>");
            btn.setBackground(new Color(60, 50, 40));
            btn.setForeground(Color.WHITE);
        }

        // 버튼 스타일
        btn.setBorder(BorderFactory.createLineBorder(new Color(200, 150, 50), 2));
        btn.setContentAreaFilled(false); // 배경 투명
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // 툴팁
        btn.setToolTipText(desc + " (비용: " + price + " G)");

        // 클릭 시 구매 요청
        btn.addActionListener(e -> mainApp.send(new Message(Message.Type.SHOP_BUY, code)));
        
        // ⭐ 정렬 설정: 이미지는 위로, 텍스트는 아래로
        btn.setVerticalAlignment(SwingConstants.TOP);        // 내용물을 위쪽 정렬
        btn.setHorizontalTextPosition(SwingConstants.CENTER); // 텍스트 가로 중앙
        btn.setVerticalTextPosition(SwingConstants.BOTTOM);   // 텍스트는 이미지 아래에 위치
        
        // 가격 텍스트 (잘림 방지를 위해 HTML로 여백 조정)
        btn.setText("<html><center>" +
                    "<font size='4' color='white'>" + desc + "</font><br>" +
                    "<font size='6' color='yellow'><b>" + price + " G</b></font>" +
                    "</center></html>");

        return btn;
    }

    public void updateState(GameState state) {
        if (state == null) return;
        try {
            int myId = mainApp.getMyId();
            Player shopper = null;
            if (state.currentTurnPlayerId >= 0 && state.currentTurnPlayerId < state.players.size()) {
                shopper = state.players.get(state.currentTurnPlayerId);
            }
            Player me = null;
            if (myId >= 0 && myId < state.players.size()) {
                me = state.players.get(myId);
            }

            if (shopper != null) {
                // 상단 라벨 업데이트
                lblShopperName.setText("🛒 현재 방문객: " + shopper.name + " (" + shopper.jobClass + ")");
                
                // 내 턴일 때만 버튼 활성화
                boolean isMyTurn = (state.currentTurnPlayerId == myId);
                setButtonsEnabled(isMyTurn);
                
                if(!isMyTurn) lblWarning.setText("다른 플레이어가 쇼핑 중입니다...");
            }

            if (me != null) {
                lblGold.setText(String.format("팀 자금: %d G", state.teamGold));
                lblStats.setText(String.format("   |   ⚔️ Atk: %d   ❤️ HP: %d / %d", 
                    me.getTotalAttack(), me.hp, me.getTotalMaxHp()));
            }
            
            if (state.shopWarning != null && !state.shopWarning.isEmpty()) {
                lblWarning.setText(state.shopWarning);
            } 
        } catch (Exception e) { e.printStackTrace(); }
    }
    
    private void setButtonsEnabled(boolean enabled) {
        JPanel centerPanel = (JPanel) ((BorderLayout)getLayout()).getLayoutComponent(BorderLayout.CENTER);
        for (Component c : centerPanel.getComponents()) {
            if (c instanceof JButton) c.setEnabled(enabled);
        }
        btnExit.setEnabled(enabled);
    }
}