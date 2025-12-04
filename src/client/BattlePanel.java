package client;

import shared.*;
import shared.Message.BattleRequest;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class BattlePanel extends JPanel {
    private ClientApp mainApp;
    private GameState gameState;

    private JPanel monstersPanel; 
    private JPanel playersPanel;  
    private JPanel actionPanel;   

    private JTextArea battleLogArea;
    private JScrollPane logScrollPane;

    private JButton btnAttack, btnSkill1, btnSkill2, btnFlee;
    private int selectedMonsterIndex = 0;

    public BattlePanel(ClientApp app) {
        this.mainApp = app;
        setLayout(new BorderLayout());
        setBackground(new Color(40, 40, 40)); 

        monstersPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 40));
        monstersPanel.setOpaque(false);
        monstersPanel.setPreferredSize(new Dimension(800, 250));
        add(monstersPanel, BorderLayout.NORTH);

        battleLogArea = new JTextArea();
        battleLogArea.setEditable(false);
        battleLogArea.setBackground(new Color(0, 0, 0, 100));
        battleLogArea.setForeground(Color.WHITE);           
        battleLogArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        battleLogArea.setMargin(new Insets(10, 10, 10, 10));

        logScrollPane = new JScrollPane(battleLogArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY), 
                "⚔️ 전투 기록", 
                TitledBorder.DEFAULT_JUSTIFICATION, 
                TitledBorder.DEFAULT_POSITION, 
                new Font("SansSerif", Font.BOLD, 12), 
                Color.WHITE));
        logScrollPane.setOpaque(false);
        logScrollPane.getViewport().setOpaque(false);
        
        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.setBorder(BorderFactory.createEmptyBorder(10, 50, 10, 50));
        centerWrapper.add(logScrollPane, BorderLayout.CENTER);
        
        add(centerWrapper, BorderLayout.CENTER);

        JPanel bottomContainer = new JPanel(new BorderLayout());
        bottomContainer.setOpaque(false);

        playersPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        playersPanel.setOpaque(false);
        bottomContainer.add(playersPanel, BorderLayout.NORTH);

        actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 20));
        actionPanel.setBackground(new Color(0, 0, 0, 180));
        
        // 액션 버튼들 (나중에 이것들도 이미지 버튼으로 바꿀 수 있음)
        btnAttack = createActionButton("⚔️ 기본 공격", "ATTACK");
        btnSkill1 = createActionButton("⚡ 강타", "SKILL1");
        btnSkill2 = createActionButton("🔥 광역기", "SKILL2");
        btnFlee = createActionButton("🏃 도망가기", "FLEE");
        
        actionPanel.add(btnAttack);
        actionPanel.add(btnSkill1);
        actionPanel.add(btnSkill2);
        actionPanel.add(btnFlee);
        
        bottomContainer.add(actionPanel, BorderLayout.SOUTH);
        add(bottomContainer, BorderLayout.SOUTH);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // images/battle/battle_bg.png 로드
        Image bgImg = ResourceManager.getImage("battle", "battle_bg.png");
        if (bgImg != null) {
            g.drawImage(bgImg, 0, 0, getWidth(), getHeight(), null);
        } else {
            // 이미지 없으면 어두운 회색
            g.setColor(new Color(30, 30, 30));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    private JButton createActionButton(String text, String actionCode) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setPreferredSize(new Dimension(140, 50));
        btn.setFocusable(false);
        btn.addActionListener(e -> {
            mainApp.send(new Message(Message.Type.BATTLE_ACTION, 
                    new BattleRequest(actionCode, selectedMonsterIndex)));
        });
        return btn;
    }

    public void updateState(GameState state) {
        this.gameState = state;
        
        // 로그 업데이트
        StringBuilder sb = new StringBuilder();
        if (state.battleLog != null) {
            for (String log : state.battleLog) {
                sb.append(log).append("\n");
            }
        }
        if (!battleLogArea.getText().equals(sb.toString())) {
            battleLogArea.setText(sb.toString());
            battleLogArea.setCaretPosition(battleLogArea.getDocument().getLength()); 
        }

        monstersPanel.removeAll();
        for (int i = 0; i < state.monsters.size(); i++) {
            Monster m = state.monsters.get(i);
            
            JButton mBtn = new JButton();
            mBtn.setPreferredSize(new Dimension(150, 180));
            mBtn.setFocusable(false);
            mBtn.setContentAreaFilled(false);
            mBtn.setBorderPainted(false);

            String imgName = "mon_goblin.png";
            if (m.id == 99) imgName = "mon_dragon.png";
            else if (m.name.contains("오크")) imgName = "mon_orc.png";
            
            Image monImg = ResourceManager.getImage("battle", imgName);

            if (monImg != null) {
                monImg = monImg.getScaledInstance(120, 120, Image.SCALE_SMOOTH);
                mBtn.setIcon(new ImageIcon(monImg));
                
                mBtn.setText(String.format("<html><center><font color='white'><b>%s</b></font><br><font color='#ff6666'>HP: %d/%d</font></center></html>", 
                        m.name, m.hp, m.maxHp));
                mBtn.setVerticalTextPosition(SwingConstants.BOTTOM);
                mBtn.setHorizontalTextPosition(SwingConstants.CENTER);
            } else {
                mBtn.setContentAreaFilled(true);
                mBtn.setBorderPainted(true);
                mBtn.setText(String.format("<html><center><b>%s</b><br>HP: %d/%d</center></html>", 
                                       m.name, m.hp, m.maxHp));
                mBtn.setBackground(Color.WHITE);
            }
            
            // 상태 처리 (사망, 선택 등)
            if (m.isDead) {
                mBtn.setEnabled(false);
                mBtn.setDisabledIcon(UIManager.getIcon("OptionPane.errorIcon")); // 임시 아이콘 또는 흑백처리
                mBtn.setText("<html><center>☠️<br>처치됨</center></html>");
                mBtn.setBackground(Color.DARK_GRAY);
            } else {
                if (i == selectedMonsterIndex) {
                    // 선택된 몬스터는 테두리 강조
                    mBtn.setBorderPainted(true);
                    mBtn.setBorder(BorderFactory.createLineBorder(Color.YELLOW, 3));
                } else {
                    mBtn.setBorderPainted(false);
                }
                
                int finalI = i;
                mBtn.addActionListener(e -> {
                    this.selectedMonsterIndex = finalI;
                    updateState(this.gameState);
                });
            }
            monstersPanel.add(mBtn);
        }

        playersPanel.removeAll();
        for (int id : state.battleMemberIds) {
            Player p = null;
            for(Player temp : state.players) {
                if(temp.id == id) { p = temp; break; }
            }
            if(p == null) continue;

            JPanel pPanel = new JPanel();
            pPanel.setLayout(new BorderLayout());
            pPanel.setPreferredSize(new Dimension(120, 100));

            pPanel.setOpaque(false);

            String charFile = "char_" + p.jobClass + ".png";
            Image charImg = ResourceManager.getImage("char", charFile);
            
            JLabel faceLbl = new JLabel();
            faceLbl.setHorizontalAlignment(SwingConstants.CENTER);
            if (charImg != null) {
                charImg = charImg.getScaledInstance(50, 50, Image.SCALE_SMOOTH);
                faceLbl.setIcon(new ImageIcon(charImg));
            } else {
                faceLbl.setText(p.jobClass.substring(0, 1));
                faceLbl.setOpaque(true);
                faceLbl.setBackground(p.color);
                faceLbl.setForeground(Color.WHITE);
                faceLbl.setFont(new Font("SansSerif", Font.BOLD, 20));
                faceLbl.setPreferredSize(new Dimension(50, 50));
            }

            JLabel infoLbl = new JLabel(
                String.format("<html><center><font color='white'>%s</font><br><font color='#00ff00'>HP: %d</font></center></html>", 
                p.name, p.hp), SwingConstants.CENTER);
            
            pPanel.add(faceLbl, BorderLayout.CENTER);
            pPanel.add(infoLbl, BorderLayout.SOUTH);

            if (state.currentTurnPlayerId == id) {
                pPanel.setBorder(BorderFactory.createLineBorder(Color.CYAN, 2));
            } else {
                pPanel.setBorder(BorderFactory.createEmptyBorder());
            }

            playersPanel.add(pPanel);
        }

        boolean isMyTurn = (state.currentTurnPlayerId == mainApp.getMyId());
        btnAttack.setEnabled(isMyTurn);
        btnSkill1.setEnabled(isMyTurn);
        btnSkill2.setEnabled(isMyTurn);
        btnFlee.setEnabled(isMyTurn);
        
        revalidate();
        repaint();
    }
}