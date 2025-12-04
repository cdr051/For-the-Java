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
        
        // 배경색 (이미지가 없을 때를 대비한 기본색)
        setBackground(new Color(40, 30, 20)); 

        // ----------------------------------------------------
        // 1. 상단 (제목 + 골드 정보)
        // ----------------------------------------------------
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false); // 배경이 보이게 투명 처리
        topPanel.setBorder(BorderFactory.createEmptyBorder(30, 0, 20, 0));

        // 제목 (배경 이미지에 제목이 포함되어 있다면 이 라벨은 지워도 됩니다)
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

        // ----------------------------------------------------
        // 2. 중앙 (상품 카드 3개)
        // ----------------------------------------------------
        JPanel centerWrapper = new JPanel(new GridBagLayout()); 
        centerWrapper.setOpaque(false);
        
        JPanel cardsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 0));
        cardsPanel.setOpaque(false);

        // 🔥 [수정] 아이템 버튼 생성 (이미지 우선 로직 적용)
        cardsPanel.add(createItemButton("ATK", "공격력 강화", "ATK +5", "50 G"));
        cardsPanel.add(createItemButton("MAXHP", "최대 체력", "MaxHP +20", "50 G"));
        cardsPanel.add(createItemButton("HEAL", "체력 회복", "HP +30", "30 G"));

        centerWrapper.add(cardsPanel);
        add(centerWrapper, BorderLayout.CENTER);

        // ----------------------------------------------------
        // 3. 하단 (경고 메시지 + 나가기 버튼)
        // ----------------------------------------------------
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 40, 0));
        bottomPanel.setPreferredSize(new Dimension(800, 150)); 

        lblWarning = new JLabel(" ", SwingConstants.CENTER); 
        lblWarning.setFont(new Font("SansSerif", Font.BOLD, 22));
        lblWarning.setForeground(Color.RED);
        
        JPanel btnPanel = new JPanel(); 
        btnPanel.setOpaque(false);
        
        // 🔥 [수정] 나가기 버튼도 이미지로 변경
        JButton btnExit = new JButton();
        btnExit.setPreferredSize(new Dimension(220, 60));
        btnExit.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnExit.setFocusPainted(false);
        btnExit.setContentAreaFilled(false);
        btnExit.setBorderPainted(false);

        // 나가기 버튼 이미지 로드 시도
        Image exitImg = ResourceManager.getImage("shop", "btn_exit.png");
        if (exitImg != null) {
            Image resized = exitImg.getScaledInstance(220, 60, Image.SCALE_SMOOTH);
            btnExit.setIcon(new ImageIcon(resized));
        } else {
            // 이미지가 없으면 기존 빨간 버튼 스타일 (Fallback)
            btnExit.setText("🚪 상점 나가기");
            btnExit.setFont(new Font("SansSerif", Font.BOLD, 18));
            btnExit.setBackground(new Color(100, 50, 50)); 
            btnExit.setForeground(Color.WHITE);
            btnExit.setContentAreaFilled(true);
            btnExit.setBorderPainted(true);
        }

        btnExit.addActionListener(e -> mainApp.send(new Message(Message.Type.SHOP_EXIT, null)));
        
        btnPanel.add(btnExit);

        bottomPanel.add(lblWarning, BorderLayout.NORTH);
        bottomPanel.add(btnPanel, BorderLayout.CENTER);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    // 🔥 [핵심] 상점 배경 이미지 그리기
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // 배경 이미지 로드 (images/shop/shop_bg.png)
        Image bgImg = ResourceManager.getImage("shop", "shop_bg.png");
        
        if (bgImg != null) {
            // 이미지가 있으면 화면 꽉 채우기
            g.drawImage(bgImg, 0, 0, getWidth(), getHeight(), null);
        } else {
            // 이미지가 없으면 기본 갈색 배경
            g.setColor(new Color(40, 30, 20));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    // 아이템 버튼 생성 메서드 (이미지/텍스트 자동 전환)
    private JButton createItemButton(String code, String fallbackTitle, String fallbackEffect, String fallbackPrice) {
        JButton btn = new JButton();
        
        int btnW = 240;
        int btnH = 350;
        btn.setPreferredSize(new Dimension(btnW, btnH));
        
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false); 
        btn.setBorderPainted(false);     
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 아이템 카드 이미지 로드 (예: card_ATK.png)
        String imgName = "card_" + code + ".png";
        Image img = ResourceManager.getImage("shop", imgName);

        if (img != null) {
            // 이미지 있으면 이미지 적용
            Image resized = img.getScaledInstance(btnW, btnH, Image.SCALE_SMOOTH);
            btn.setIcon(new ImageIcon(resized));
        } else {
            // 이미지 없으면 기존 텍스트 스타일 적용
            btn.setContentAreaFilled(true);
            btn.setBorderPainted(true);
            btn.setBackground(new Color(60, 50, 40));
            btn.setForeground(Color.WHITE);
            btn.setBorder(new LineBorder(new Color(120, 100, 50), 2));
            
            String html = "<html><center>" +
                          "<h2 style='margin:0;'>" + fallbackTitle + "</h2>" +
                          "<br><p style='color:#ccc;'>" + fallbackEffect + "</p>" +
                          "<br><h2 style='color: yellow;'>" + fallbackPrice + "</h2>" +
                          "</center></html>";
            btn.setText(html);
        }
        
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
            
            if (state.shopWarning != null && !state.shopWarning.isEmpty()) {
                lblWarning.setText("⚠️ " + state.shopWarning);
            } else {
                lblWarning.setText(" "); 
            }

            this.revalidate();
            this.repaint();
        } catch (Exception e) {
            System.out.println("상점 업데이트 오류: " + e.getMessage());
        }
    }
}