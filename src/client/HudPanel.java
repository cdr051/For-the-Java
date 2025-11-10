package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.function.Supplier; // 현재 턴을 가져오기 위해

/**
 * "주사위 굴리기" 버튼과 로그 창을 담당하는 HUD 패널입니다.
 */
public class HudPanel extends JPanel {
    private JButton rollDiceButton;
    private JTextArea logArea;
    private Supplier<Integer> getCurrentPlayerTurn; // 현재 턴 플레이어(1 or 2)

    // 'Runnable' (주사위 굴리기)과 'Supplier' (현재 턴 확인)를 받음
    public HudPanel(Runnable onDiceRollAction, Supplier<Integer> getCurrentPlayerTurn) {
        this.getCurrentPlayerTurn = getCurrentPlayerTurn;
        setLayout(new BorderLayout(5, 5));
        setPreferredSize(new Dimension(250, 600));

        // 1. 주사위 굴리기 버튼
        rollDiceButton = new JButton("Player 1 턴: 주사위 굴리기");
        rollDiceButton.addActionListener(e -> onDiceRollAction.run());
        add(rollDiceButton, BorderLayout.NORTH);

        // 2. 로그 영역
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(logArea);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void log(String message) {
        EventQueue.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    // 턴이 시작되면 버튼 활성화
    public void enableDiceButton() {
        int playerTurn = getCurrentPlayerTurn.get();
        rollDiceButton.setEnabled(true);
        rollDiceButton.setText("Player " + playerTurn + " 턴: 주사위 굴리기");
    }

    // 주사위를 굴리면 버튼 비활성화
    public void disableDiceButton() {
        int playerTurn = getCurrentPlayerTurn.get();
        rollDiceButton.setEnabled(false);
        rollDiceButton.setText("Player " + playerTurn + ", 이동할 칸을 선택...");
    }
}