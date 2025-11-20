package client;

import shared.GameProtocol;
import shared.BattleState;
import shared.Monster;
import shared.AttackInfo;
import shared.AttackType;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.awt.event.ActionListener;

public class BattlePanel extends JPanel {
    private NetworkManager networkManager;
    
    private JButton singlePhysAtk, multiPhysAtk, singleMagAtk, multiMagAtk;
    private JRadioButton targetMonster1, targetMonster2;
    private ButtonGroup targetGroup;
    private JTextArea battleLog;
    private JLabel playerHealthLabel;
    private JLabel monster1HealthLabel, monster2HealthLabel;

    public BattlePanel(NetworkManager networkManager) {
        this.networkManager = networkManager;
        setLayout(new BorderLayout(10, 10));
        setPreferredSize(new Dimension(800, 600));

        battleLog = new JTextArea();
        battleLog.setEditable(false);
        add(new JScrollPane(battleLog), BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new GridLayout(3, 1));
        playerHealthLabel = new JLabel("Player HP: ");
        monster1HealthLabel = new JLabel("Monster 1 HP: ");
        monster2HealthLabel = new JLabel("Monster 2 HP: ");
        statusPanel.add(playerHealthLabel);
        statusPanel.add(monster1HealthLabel);
        statusPanel.add(monster2HealthLabel);
        add(statusPanel, BorderLayout.NORTH);

        JPanel controlPanel = new JPanel(new BorderLayout());
        
        JPanel targetPanel = new JPanel();
        targetGroup = new ButtonGroup();
        targetMonster1 = new JRadioButton("몬스터 1");
        targetMonster2 = new JRadioButton("몬스터 2");
        targetMonster1.setSelected(true);
        targetGroup.add(targetMonster1);
        targetGroup.add(targetMonster2);
        targetPanel.add(new JLabel("공격 대상:"));
        targetPanel.add(targetMonster1);
        targetPanel.add(targetMonster2);
        controlPanel.add(targetPanel, BorderLayout.NORTH);

        JPanel attackPanel = new JPanel(new GridLayout(2, 2));
        singlePhysAtk = new JButton("단일공격(물리)");
        multiPhysAtk = new JButton("다중공격(물리)");
        singleMagAtk = new JButton("단일공격(마법)");
        multiMagAtk = new JButton("다중공격(마법)");
        
        attackPanel.add(singlePhysAtk);
        attackPanel.add(multiPhysAtk);
        attackPanel.add(singleMagAtk);
        attackPanel.add(multiMagAtk);
        controlPanel.add(attackPanel, BorderLayout.CENTER);
        
        add(controlPanel, BorderLayout.SOUTH);

        singlePhysAtk.addActionListener(e -> sendAttack(AttackType.SINGLE_PHYSICAL));
        multiPhysAtk.addActionListener(e -> sendAttack(AttackType.MULTI_PHYSICAL));
        singleMagAtk.addActionListener(e -> sendAttack(AttackType.SINGLE_MAGICAL));
        multiMagAtk.addActionListener(e -> sendAttack(AttackType.MULTI_MAGICAL));
    }
    
    private void sendAttack(AttackType type) {
        int targetIndex = targetMonster1.isSelected() ? 0 : 1;
        AttackInfo info = new AttackInfo(type, targetIndex);
        networkManager.sendMessage(new GameProtocol(GameProtocol.C_MSG_ATTACK, info));
        setAttackButtonsEnabled(false);
    }
    
    // --- 👇 여기가 수정되었습니다 (Line 87) ---
    public void startBattle(BattleState battleState) {
        // GUI 업데이트를 EventQueue.invokeLater로 감쌉니다.
        EventQueue.invokeLater(() -> {
            battleLog.setText(""); // 87번 줄 오류 수정
            log("전투 시작!");
            updateBattle(battleState); // 이 메소드도 GUI를 수정하므로 invokeLater 내부에서 호출
            setAttackButtonsEnabled(true);
        });
    }
    public void updateBattle(BattleState battleState) {

        EventQueue.invokeLater(() -> {
            shared.Player player = battleState.getPlayerInTurn();
            List<Monster> monsters = battleState.getMonsters();
            
            playerHealthLabel.setText(player.getPlayerName() + " HP: " + player.getHealth());
            
            if (monsters.size() > 0) {
                monster1HealthLabel.setText(monsters.get(0).getName() + " HP: " + monsters.get(0).getHealth());
                targetMonster1.setEnabled(!monsters.get(0).isDead());
            }
            if (monsters.size() > 1) {
                monster2HealthLabel.setText(monsters.get(1).getName() + " HP: " + monsters.get(1).getHealth());
                targetMonster2.setEnabled(!monsters.get(1).isDead());
            }
            
            setAttackButtonsEnabled(true);
        });
    }
    // --- ----------------------- ---
    
    public void log(String message) {
        EventQueue.invokeLater(() -> {
            battleLog.append(message + "\n");
        });
    }
    
    private void setAttackButtonsEnabled(boolean enabled) {
        // 이 메소드는 invokeLater 내부(startBattle, updateBattle)에서만 
        // 호출되므로 스레드에 안전합니다.
        singlePhysAtk.setEnabled(enabled);
        multiPhysAtk.setEnabled(enabled);
        singleMagAtk.setEnabled(enabled);
        multiMagAtk.setEnabled(enabled);
    }
}