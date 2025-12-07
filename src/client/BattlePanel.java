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
    private JButton btnReturnLobby; 
    private int selectedMonsterIndex = 0;

    public BattlePanel(ClientApp app) {
        this.mainApp = app;
        setLayout(new BorderLayout());
        setBackground(new Color(40, 40, 40)); 

        monstersPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 50, 40));
        monstersPanel.setOpaque(false);
        monstersPanel.setPreferredSize(new Dimension(800, 250)); 
        add(monstersPanel, BorderLayout.NORTH);

        battleLogArea = new JTextArea() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        battleLogArea.setOpaque(false);
        battleLogArea.setEditable(false);
        battleLogArea.setBackground(new Color(0, 0, 0, 150)); 
        battleLogArea.setForeground(Color.WHITE);          
        battleLogArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        battleLogArea.setMargin(new Insets(10, 10, 10, 10)); 

        logScrollPane = new JScrollPane(battleLogArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY), "⚔️ 전투 기록", 
                TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, 
                new Font("SansSerif", Font.BOLD, 12), Color.WHITE));
        logScrollPane.setOpaque(false);
        logScrollPane.getViewport().setOpaque(false);
        logScrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        add(logScrollPane, BorderLayout.CENTER);

        JPanel bottomContainer = new JPanel(new BorderLayout());
        bottomContainer.setOpaque(false);

        playersPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        playersPanel.setOpaque(false);
        bottomContainer.add(playersPanel, BorderLayout.NORTH);

        actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 20));
        actionPanel.setBackground(new Color(0, 0, 0, 180)); 
        
        btnAttack = createActionButton("⚔️ 기본 공격", "ATTACK");
        btnSkill1 = createActionButton("스킬 1", "SKILL1"); 
        btnSkill2 = createActionButton("스킬 2", "SKILL2"); 
        btnFlee = createActionButton("🏃 도망가기", "FLEE");

        btnReturnLobby = new JButton("🏠 로비로 돌아가기");
        btnReturnLobby.setFont(new Font("SansSerif", Font.BOLD, 16));
        btnReturnLobby.setBackground(new Color(200, 50, 50));
        btnReturnLobby.setForeground(Color.WHITE);
        btnReturnLobby.setPreferredSize(new Dimension(300, 60));
        btnReturnLobby.addActionListener(e -> {
            mainApp.send(new Message(Message.Type.RESET_REQ, null));
            mainApp.switchToLobby(); 
        });
        
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
        Image bg = ResourceManager.getImage("battle", "bg_battle.png");
        if (bg != null) {
            g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
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

        if (state.monsters != null && !state.monsters.isEmpty()) {
            if (selectedMonsterIndex >= state.monsters.size()) selectedMonsterIndex = 0;
            Monster currentTarget = state.monsters.get(selectedMonsterIndex);
            if (currentTarget.isDead) {
                for (int i = 0; i < state.monsters.size(); i++) {
                    if (!state.monsters.get(i).isDead) {
                        selectedMonsterIndex = i; break;
                    }
                }
            }
        }
        
        actionPanel.removeAll();
        if (state.isGameOver) {
            actionPanel.add(btnReturnLobby);
        } else {
            actionPanel.add(btnAttack);
            actionPanel.add(btnSkill1);
            actionPanel.add(btnSkill2);
            actionPanel.add(btnFlee);
            updateSkillButtons(state);
        }

        logScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY), 
                "⚔️ 전투 기록 (남은 목숨: " + state.teamLives + ")", 
                TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, 
                new Font("SansSerif", Font.BOLD, 12), Color.WHITE));

        StringBuilder sb = new StringBuilder();
        if (state.battleLog != null) {
            for (String log : state.battleLog) sb.append(log).append("\n");
        }
        if (!battleLogArea.getText().equals(sb.toString())) {
            battleLogArea.setText(sb.toString());
            battleLogArea.setCaretPosition(battleLogArea.getDocument().getLength()); 
        }

        monstersPanel.removeAll();
        for (int i = 0; i < state.monsters.size(); i++) {
            Monster m = state.monsters.get(i);
            
            // ⭐ [핵심 수정] 몬스터 이름에 따른 이미지 매핑
            String imgName = "tile_monster.png"; 
            if (m.name.contains("오크")) imgName = "mob_orc.png";
            else if (m.name.contains("슬라임")) imgName = "mob_slime.png";
            else if (m.name.contains("늑대인간")) imgName = "mob_werewolf.png";
            else if (m.name.contains("스켈레톤")) imgName = "mob_skeleton.png";

            Image mobImg = ResourceManager.getImage("monster", imgName);
            
            JButton mBtn = new JButton();
            mBtn.setPreferredSize(new Dimension(160, 180));
            mBtn.setFocusable(false);
            
            if (mobImg != null) {
                mBtn.setIcon(new ImageIcon(mobImg.getScaledInstance(140, 140, Image.SCALE_SMOOTH)));
                mBtn.setText("<html><center>" + m.name + "<br>HP: " + m.hp + "</center></html>");
                mBtn.setHorizontalTextPosition(SwingConstants.CENTER);
                mBtn.setVerticalTextPosition(SwingConstants.BOTTOM);
                mBtn.setContentAreaFilled(false);
                mBtn.setForeground(Color.WHITE);
            } else {
                mBtn.setText("<html><center><b>" + m.name + "</b><br>HP: " + m.hp + "</center></html>");
                mBtn.setBackground(Color.WHITE);
            }

            if (m.isDead) {
                mBtn.setEnabled(false);
                mBtn.setText("<html><center>☠️ 처치됨</center></html>");
                mBtn.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            } else {
                if (i == selectedMonsterIndex) {
                    mBtn.setBorder(BorderFactory.createLineBorder(Color.YELLOW, 4));
                } else {
                    mBtn.setBorder(BorderFactory.createEmptyBorder()); 
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
            for(Player temp : state.players) { if(temp.id == id) { p = temp; break; } }
            if(p == null) continue;

            JPanel pPanel = new JPanel();
            pPanel.setLayout(new GridLayout(3, 1));
            pPanel.setPreferredSize(new Dimension(150, 80));
            pPanel.setBackground(p.color); 
            
            pPanel.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(Color.BLACK),
                    p.name + " (" + p.jobClass + ")",
                    TitledBorder.CENTER, TitledBorder.TOP,
                    new Font("SansSerif", Font.BOLD, 12), Color.BLACK));
            
            JLabel hpLbl = new JLabel("HP: " + p.hp + " / " + p.getTotalMaxHp(), SwingConstants.CENTER);
            hpLbl.setForeground(Color.BLACK);
            JLabel spdLbl = new JLabel("속도: " + p.getTotalSpeed(), SwingConstants.CENTER);
            spdLbl.setForeground(Color.DARK_GRAY);
            
            if (state.currentTurnPlayerId == id && !state.isGameOver) {
                hpLbl.setForeground(Color.RED);
                pPanel.setBorder(BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(Color.RED, 3), "▶ " + p.name, 
                        TitledBorder.CENTER, TitledBorder.TOP));
            }
            pPanel.add(hpLbl);
            pPanel.add(spdLbl);
            playersPanel.add(pPanel);
        }

        if (state.isGameOver) {
            btnReturnLobby.setEnabled(true);
        } else {
            boolean isMyTurn = (state.currentTurnPlayerId == mainApp.getMyId());
            setButtonsEnabled(isMyTurn);
        }
        
        revalidate();
        repaint();
    }
    
    private void updateSkillButtons(GameState state) {
        Player me = null;
        for(Player p : state.players) {
            if(p.id == mainApp.getMyId()) { me = p; break; }
        }
        if (me == null) return;

        switch (me.jobClass) {
            case "기사":
                btnSkill1.setText("🛡️ 방패들기");
                btnSkill2.setText("🔨 강타");
                break;
            case "마법사":
                btnSkill1.setText("⚡ 라이트닝");
                btnSkill2.setText("🔥 파이어볼");
                break;
            case "궁수":
                btnSkill1.setText("🏹 속사");
                btnSkill2.setText("👁️ 매의 눈");
                break;
            case "도적":
                btnSkill1.setText("🗡️ 찌르기");
                btnSkill2.setText("💨 연막탄");
                break;
        }
    }
    
    private void setButtonsEnabled(boolean enabled) {
        btnAttack.setEnabled(enabled);
        btnSkill1.setEnabled(enabled);
        btnSkill2.setEnabled(enabled);
        btnFlee.setEnabled(enabled);
    }
}