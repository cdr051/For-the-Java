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
        
        updateMonsterSelection(state);
        updateActionButtons(state);
        updateBattleLog(state);
        updateMonstersPanel(state);
        updatePlayersPanel(state);
        
        if (state.isGameOver) {
            btnReturnLobby.setEnabled(true);
        } else {
            boolean isMyTurn = (state.currentTurnPlayerId == mainApp.getMyId());
            setButtonsEnabled(isMyTurn);
        }
        
        revalidate();
        repaint();
    }
    
    private void updateMonsterSelection(GameState state) {
        if (state.monsters == null || state.monsters.isEmpty()) return;
        
        if (selectedMonsterIndex >= state.monsters.size()) {
            selectedMonsterIndex = 0;
        }
        
        Monster currentTarget = state.monsters.get(selectedMonsterIndex);
        if (currentTarget.isDead) {
            // 살아있는 몬스터로 선택 변경
            for (int i = 0; i < state.monsters.size(); i++) {
                if (!state.monsters.get(i).isDead) {
                    selectedMonsterIndex = i;
                    break;
                }
            }
        }
    }
    
    private void updateActionButtons(GameState state) {
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
    }
    
    private void updateBattleLog(GameState state) {
        logScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY), 
                "⚔️ 전투 기록 (남은 목숨: " + state.teamLives + ")", 
                TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, 
                new Font("SansSerif", Font.BOLD, 12), Color.WHITE));

        StringBuilder sb = new StringBuilder();
        if (state.battleLog != null) {
            for (String log : state.battleLog) {
                sb.append(log).append("\n");
            }
        }
        
        String newText = sb.toString();
        if (!battleLogArea.getText().equals(newText)) {
            battleLogArea.setText(newText);
            battleLogArea.setCaretPosition(battleLogArea.getDocument().getLength());
        }
    }
    
    private void updateMonstersPanel(GameState state) {
        monstersPanel.removeAll();
        
        for (int i = 0; i < state.monsters.size(); i++) {
            Monster m = state.monsters.get(i);
            JButton mBtn = createMonsterButton(m, i, state);
            monstersPanel.add(mBtn);
        }
    }
    
    private JButton createMonsterButton(Monster m, int index, GameState state) {
        String imgName = getMonsterImageName(m);
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
            if (index == selectedMonsterIndex) {
                mBtn.setBorder(BorderFactory.createLineBorder(Color.YELLOW, 4));
            } else {
                mBtn.setBorder(BorderFactory.createEmptyBorder());
            }
            
            int finalIndex = index;
            mBtn.addActionListener(e -> {
                this.selectedMonsterIndex = finalIndex;
                updateState(this.gameState);
            });
        }
        
        return mBtn;
    }
    
    private String getMonsterImageName(Monster m) {
        if (m.name.contains("오크")) return "mob_orc.png";
        if (m.name.contains("슬라임")) return "mob_slime.png";
        if (m.name.contains("늑대인간")) return "mob_werewolf.png";
        if (m.name.contains("스켈레톤")) return "mob_skeleton.png";
        return "tile_monster.png";
    }
    
    private void updatePlayersPanel(GameState state) {
        playersPanel.removeAll();
        
        for (int id : state.battleMemberIds) {
            Player p = state.getPlayer(id);
            if (p == null) continue;
            
            JPanel pPanel = createPlayerPanel(p, id, state);
            playersPanel.add(pPanel);
        }
    }
    
    private JPanel createPlayerPanel(Player p, int id, GameState state) {
        JPanel pPanel = new JPanel();
        pPanel.setLayout(new GridLayout(3, 1));
        pPanel.setPreferredSize(new Dimension(150, 80));
        pPanel.setBackground(p.color);
        
        boolean isCurrentTurn = (state.currentTurnPlayerId == id && !state.isGameOver);
        
        pPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(isCurrentTurn ? Color.RED : Color.BLACK, isCurrentTurn ? 3 : 1),
                isCurrentTurn ? "▶ " + p.name : p.name + " (" + p.jobClass + ")",
                TitledBorder.CENTER, TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 12), Color.BLACK));
        
        JLabel hpLbl = new JLabel("HP: " + p.hp + " / " + p.getTotalMaxHp(), SwingConstants.CENTER);
        hpLbl.setForeground(isCurrentTurn ? Color.RED : Color.BLACK);
        
        JLabel spdLbl = new JLabel("속도: " + p.getTotalSpeed(), SwingConstants.CENTER);
        spdLbl.setForeground(Color.DARK_GRAY);
        
        pPanel.add(hpLbl);
        pPanel.add(spdLbl);
        
        return pPanel;
    }
    
    private void updateSkillButtons(GameState state) {
        Player me = state.getPlayer(mainApp.getMyId());
        if (me == null) return;

        switch (me.jobClass) {
            case GameConfig.JOB_KNIGHT:
                btnSkill1.setText("🛡️ 방패들기");
                btnSkill2.setText("🔨 강타");
                break;
            case GameConfig.JOB_MAGE:
                btnSkill1.setText("⚡ 라이트닝");
                btnSkill2.setText("🔥 파이어볼");
                break;
            case GameConfig.JOB_ARCHER:
                btnSkill1.setText("🏹 속사");
                btnSkill2.setText("👁️ 매의 눈");
                break;
            case GameConfig.JOB_ROGUE:
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