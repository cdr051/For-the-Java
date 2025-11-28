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
    
    private JButton btnAttack, btnSkill1, btnSkill2, btnFlee;
    private JLabel lblStatus;

    // 선택된 몬스터 (0 or 1)
    private int selectedMonsterIndex = 0;

    public BattlePanel(ClientApp app) {
        this.mainApp = app;
        setLayout(new BorderLayout());
        setBackground(new Color(50, 20, 20)); // 어두운 붉은색 배경

        // 1. 상단: 몬스터 영역
        monstersPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 20));
        monstersPanel.setOpaque(false);
        monstersPanel.setPreferredSize(new Dimension(800, 200));
        add(monstersPanel, BorderLayout.NORTH);

        // 2. 중앙: 전투 로그/상태
        lblStatus = new JLabel("전투 개시!", SwingConstants.CENTER);
        lblStatus.setForeground(Color.WHITE);
        lblStatus.setFont(new Font("SansSerif", Font.BOLD, 20));
        add(lblStatus, BorderLayout.CENTER);

        // 3. 하단: 플레이어 및 액션바
        JPanel bottomContainer = new JPanel(new BorderLayout());
        bottomContainer.setOpaque(false);

        // 플레이어 상태창
        playersPanel = new JPanel(new FlowLayout());
        playersPanel.setOpaque(false);
        bottomContainer.add(playersPanel, BorderLayout.NORTH);

        // 액션 버튼들
        actionPanel = new JPanel(new FlowLayout());
        actionPanel.setBackground(new Color(0, 0, 0, 150));
        
        btnAttack = createActionButton("⚔️ 기본 공격", "ATTACK");
        btnSkill1 = createActionButton("⚡ 단일 스킬", "SKILL1");
        btnSkill2 = createActionButton("🔥 광역 스킬", "SKILL2");
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
        btn.addActionListener(e -> {
            mainApp.send(new Message(Message.Type.BATTLE_ACTION, new BattleRequest(actionCode, selectedMonsterIndex)));
        });
        return btn;
    }

    public void updateState(GameState state) {
        this.gameState = state;
        
        // 1. 몬스터 그리기
        monstersPanel.removeAll();
        for (int i = 0; i < state.monsters.size(); i++) {
            Monster m = state.monsters.get(i);
            JButton mBtn = new JButton("<html><center>" + m.name + "<br>HP: " + m.hp + "/" + m.maxHp + "</center></html>");
            mBtn.setPreferredSize(new Dimension(150, 150));
            
            if (m.isDead) {
                mBtn.setEnabled(false);
                mBtn.setBackground(Color.GRAY);
                mBtn.setText("<html><center>☠️ 처치됨</center></html>");
            } else {
                // 선택된 몬스터 강조
                if (i == selectedMonsterIndex) mBtn.setBorder(BorderFactory.createLineBorder(Color.YELLOW, 3));
                else mBtn.setBorder(BorderFactory.createLineBorder(Color.RED, 1));
                
                mBtn.setBackground(Color.WHITE);
                int finalI = i;
                mBtn.addActionListener(e -> {
                    this.selectedMonsterIndex = finalI;
                    updateState(this.gameState); // 화면 갱신
                });
            }
            monstersPanel.add(mBtn);
        }

        // 2. 플레이어 그리기 (전투 참가자만)
        playersPanel.removeAll();
        for (int id : state.battleMemberIds) {
            Player p = state.players.get(id);
            JPanel pPanel = new JPanel();
            pPanel.setPreferredSize(new Dimension(120, 80));
            pPanel.setBackground(p.color);
            pPanel.setBorder(BorderFactory.createTitledBorder(p.name));
            pPanel.add(new JLabel("HP: " + p.hp + "/100"));
            playersPanel.add(pPanel);
        }

        // 3. 버튼 활성화 (내 턴일 때만)
        boolean isMyTurn = (state.currentTurnPlayerId == mainApp.getMyId());
        btnAttack.setEnabled(isMyTurn);
        btnSkill1.setEnabled(isMyTurn);
        btnSkill2.setEnabled(isMyTurn);
        btnFlee.setEnabled(isMyTurn);
        
        if (isMyTurn) lblStatus.setText("당신의 턴입니다! 행동을 선택하세요.");
        else lblStatus.setText("다른 플레이어의 행동을 기다리는 중...");

        revalidate();
        repaint();
    }
}