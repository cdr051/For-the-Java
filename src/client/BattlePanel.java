package client;

import shared.*;
import shared.Message.BattleRequest;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class BattlePanel extends JPanel {
    private ClientApp mainApp;
    private GameState gameState;

    // UI 영역 구분
    private JPanel monstersPanel; // 상단: 몬스터 목록
    private JPanel playersPanel;  // 하단 1: 플레이어 스탯
    private JPanel actionPanel;   // 하단 2: 버튼들

    // 전투 로그 관련
    private JTextArea battleLogArea;
    private JScrollPane logScrollPane;

    // 액션 버튼
    private JButton btnAttack, btnSkill1, btnSkill2, btnFlee;
    
    // 현재 타겟팅 중인 몬스터 인덱스
    private int selectedMonsterIndex = 0;

    public BattlePanel(ClientApp app) {
        this.mainApp = app;
        setLayout(new BorderLayout());
        setBackground(new Color(40, 40, 40)); // 배경: 어두운 회색 (전투 분위기)

        // ------------------------------------------------
        // 1. [상단] 몬스터 표시 영역
        // ------------------------------------------------
        monstersPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 20));
        monstersPanel.setOpaque(false);
        monstersPanel.setPreferredSize(new Dimension(800, 180));
        add(monstersPanel, BorderLayout.NORTH);

        // ------------------------------------------------
        // 2. [중앙] 전투 로그 영역
        // ------------------------------------------------
        battleLogArea = new JTextArea();
        battleLogArea.setEditable(false);
        battleLogArea.setBackground(new Color(20, 20, 20)); // 더 어두운 배경
        battleLogArea.setForeground(Color.WHITE);           // 흰색 글씨
        battleLogArea.setFont(new Font("Monospaced", Font.PLAIN, 14)); // 가독성 좋은 폰트
        battleLogArea.setMargin(new Insets(10, 10, 10, 10)); // 텍스트 여백

        logScrollPane = new JScrollPane(battleLogArea);
        // 테두리 제목 설정
        logScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY), 
                "⚔️ 전투 기록", 
                TitledBorder.DEFAULT_JUSTIFICATION, 
                TitledBorder.DEFAULT_POSITION, 
                new Font("SansSerif", Font.BOLD, 12), 
                Color.WHITE));
        logScrollPane.setOpaque(false);
        logScrollPane.getViewport().setOpaque(false);
        
        add(logScrollPane, BorderLayout.CENTER);

        // ------------------------------------------------
        // 3. [하단] 플레이어 상태 + 조작 버튼
        // ------------------------------------------------
        JPanel bottomContainer = new JPanel(new BorderLayout());
        bottomContainer.setOpaque(false);

        // 3-1. 플레이어 스탯 패널
        playersPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        playersPanel.setOpaque(false);
        bottomContainer.add(playersPanel, BorderLayout.NORTH);

        // 3-2. 액션 버튼 패널
        actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 20));
        actionPanel.setBackground(new Color(0, 0, 0, 150)); // 반투명 검정 배경
        
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

    // 버튼 생성 헬퍼 메서드
    private JButton createActionButton(String text, String actionCode) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setPreferredSize(new Dimension(140, 50));
        btn.setFocusable(false); // 버튼에 포커스가 가서 키보드 씹히는 것 방지 (보조)
        
        // 버튼 클릭 시 서버로 전투 행동 전송
        btn.addActionListener(e -> {
            mainApp.send(new Message(Message.Type.BATTLE_ACTION, 
                    new BattleRequest(actionCode, selectedMonsterIndex)));
        });
        return btn;
    }

    // ⭐ [핵심] 서버 상태를 받아 화면 갱신
    public void updateState(GameState state) {
        this.gameState = state;
        
        // 1. 로그 업데이트 (리스트 전체를 다시 그림)
        StringBuilder sb = new StringBuilder();
        if (state.battleLog != null) {
            for (String log : state.battleLog) {
                sb.append(log).append("\n");
            }
        }
        
        // 기존 텍스트와 다를 때만 갱신 (화면 깜빡임 방지)
        if (!battleLogArea.getText().equals(sb.toString())) {
            battleLogArea.setText(sb.toString());
            // 스크롤을 항상 맨 아래로
            battleLogArea.setCaretPosition(battleLogArea.getDocument().getLength()); 
        }

        // 2. 몬스터 그리기
        monstersPanel.removeAll();
        for (int i = 0; i < state.monsters.size(); i++) {
            Monster m = state.monsters.get(i);
            
            // 몬스터 정보: 이름, HP, 속도
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
                // 선택된 몬스터는 노란색 굵은 테두리
                if (i == selectedMonsterIndex) {
                    mBtn.setBorder(BorderFactory.createLineBorder(Color.YELLOW, 4));
                    mBtn.setBackground(new Color(255, 255, 220)); // 연한 노랑
                } else {
                    mBtn.setBorder(BorderFactory.createLineBorder(Color.RED, 1));
                    mBtn.setBackground(Color.WHITE);
                }
                
                // 클릭하면 타겟 변경 후 즉시 화면 갱신
                int finalI = i;
                mBtn.addActionListener(e -> {
                    this.selectedMonsterIndex = finalI;
                    updateState(this.gameState);
                });
            }
            monstersPanel.add(mBtn);
        }

        // 3. 플레이어 상태창 그리기 (전투 참가자만)
        playersPanel.removeAll();
        for (int id : state.battleMemberIds) {
            // ID로 플레이어 찾기 (전체 목록에서 검색)
            Player p = null;
            for(Player temp : state.players) {
                if(temp.id == id) { p = temp; break; }
            }
            if(p == null) continue;

            JPanel pPanel = new JPanel();
            pPanel.setLayout(new GridLayout(3, 1));
            pPanel.setPreferredSize(new Dimension(150, 80));
            pPanel.setBackground(p.color); // 플레이어 고유 색상 배경
            
            // 테두리: 이름 + 직업
            pPanel.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(Color.BLACK),
                    p.name + " (" + p.jobClass + ")",
                    TitledBorder.CENTER,
                    TitledBorder.TOP,
                    new Font("SansSerif", Font.BOLD, 12),
                    Color.BLACK
            ));
            
            // ⭐ [수정] 직업별 최대 체력 표시 (getTotalMaxHp)
            JLabel hpLbl = new JLabel("HP: " + p.hp + " / " + p.getTotalMaxHp(), SwingConstants.CENTER);
            hpLbl.setForeground(Color.BLACK);
            
            // 속도 표시
            JLabel spdLbl = new JLabel("속도: " + p.getTotalSpeed(), SwingConstants.CENTER);
            spdLbl.setForeground(Color.DARK_GRAY);
            
            // 턴 주인 강조 (빨간 글씨)
            if (state.currentTurnPlayerId == id) {
                hpLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
                hpLbl.setForeground(Color.RED);
                pPanel.setBorder(BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(Color.RED, 3), 
                        "▶ " + p.name, 
                        TitledBorder.CENTER, TitledBorder.TOP));
            }

            pPanel.add(hpLbl);
            pPanel.add(spdLbl);
            playersPanel.add(pPanel);
        }

        // 4. 버튼 활성화/비활성화 (내 턴일 때만 조작 가능)
        boolean isMyTurn = (state.currentTurnPlayerId == mainApp.getMyId());
        setButtonsEnabled(isMyTurn);
        
        // 화면 레이아웃 재배치 및 다시 그리기
        revalidate();
        repaint();
    }
    
    private void setButtonsEnabled(boolean enabled) {
        btnAttack.setEnabled(enabled);
        btnSkill1.setEnabled(enabled);
        btnSkill2.setEnabled(enabled);
        btnFlee.setEnabled(enabled);
    }
}