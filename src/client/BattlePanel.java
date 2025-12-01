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

        monstersPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 20));
        monstersPanel.setOpaque(false);
        monstersPanel.setPreferredSize(new Dimension(800, 180));
        add(monstersPanel, BorderLayout.NORTH);

        battleLogArea = new JTextArea();
        battleLogArea.setEditable(false);
        battleLogArea.setBackground(new Color(20, 20, 20)); 
        battleLogArea.setForeground(Color.WHITE);           
        battleLogArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        battleLogArea.setMargin(new Insets(10, 10, 10, 10));

        logScrollPane = new JScrollPane(battleLogArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY), 
                "⚔️ 전투 기록", 
                TitledBorder.DEFAULT_JUSTIFICATION, 
                TitledBorder.DEFAULT_POSITION, 
                new Font("SansSerif", Font.BOLD, 12), 
                Color.WHITE));
        logScrollPane.setOpaque(false);
        add(logScrollPane, BorderLayout.CENTER);

        JPanel bottomContainer = new JPanel(new BorderLayout());
        bottomContainer.setOpaque(false);

        playersPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        playersPanel.setOpaque(false);
        bottomContainer.add(playersPanel, BorderLayout.NORTH);

        actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 20));
        actionPanel.setBackground(new Color(0, 0, 0, 150)); 
        
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
            String txt = String.format("<html><center><b>%s</b><br>HP: %d/%d<br>SPD: %d</center></html>", 
                                       m.name, m.hp, m.maxHp, m.speed);
            JButton mBtn = new JButton(txt);
            mBtn.setPreferredSize(new Dimension(140, 120));
            mBtn.setFocusable(false);
            
            if (m.isDead) {
                mBtn.setEnabled(false);
                mBtn.setBackground(Color.DARK_GRAY);
                mBtn.setForeground(Color.LIGHT_GRAY);
                mBtn.setText("<html><center>☠️<br>처치됨</center></html>");
                mBtn.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            } else {
                if (i == selectedMonsterIndex) {
                    mBtn.setBorder(BorderFactory.createLineBorder(Color.YELLOW, 4));
                    mBtn.setBackground(new Color(255, 255, 220)); 
                } else {
                    mBtn.setBorder(BorderFactory.createLineBorder(Color.RED, 1));
                    mBtn.setBackground(Color.WHITE);
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
            pPanel.setLayout(new GridLayout(3, 1));
            pPanel.setPreferredSize(new Dimension(150, 80));
            pPanel.setBackground(p.color); 
            
            pPanel.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(Color.BLACK),
                    p.name + " (" + p.jobClass + ")",
                    TitledBorder.CENTER,
                    TitledBorder.TOP,
                    new Font("SansSerif", Font.BOLD, 12),
                    Color.BLACK));
            
            JLabel hpLbl = new JLabel("HP: " + p.hp + " / " + p.getTotalMaxHp(), SwingConstants.CENTER);
            JLabel spdLbl = new JLabel("속도: " + p.getTotalSpeed(), SwingConstants.CENTER);
            
            if (state.currentTurnPlayerId == id) {
                hpLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
                hpLbl.setForeground(Color.RED);
                pPanel.setBorder(BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(Color.RED, 3), "▶ " + p.name, 
                        TitledBorder.CENTER, TitledBorder.TOP));
            }

            pPanel.add(hpLbl);
            pPanel.add(spdLbl);
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