package client;

import shared.GameProtocol;
import shared.GameState; // GameState 임포트
import javax.swing.*;
import java.awt.*;

public class HudPanel extends JPanel {
    private JButton rollDiceButton;
    private JTextArea logArea;
    private NetworkManager networkManager;
    private JLabel turnLabel;

    public HudPanel(NetworkManager networkManager) {
        this.networkManager = networkManager;
        setLayout(new BorderLayout(5, 5));
        setPreferredSize(new Dimension(250, 600));

        turnLabel = new JLabel(" ", SwingConstants.CENTER);
        turnLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        add(turnLabel, BorderLayout.NORTH);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(logArea);
        add(scrollPane, BorderLayout.CENTER);
        
        rollDiceButton = new JButton("주사위 굴리기");
        rollDiceButton.addActionListener(e -> {
            networkManager.sendMessage(new GameProtocol(GameProtocol.C_MSG_ROLL_DICE, null));
        });
        add(rollDiceButton, BorderLayout.SOUTH);
        
        enableDiceButton(); 
    }

    public void log(String message) {
        EventQueue.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public void updateState(GameState gameState) {
        String turnText = gameState.getCurrentPlayer().getPlayerName() + "의 턴";
        turnLabel.setText(turnText);
    }

    public void enableDiceButton() {
        rollDiceButton.setEnabled(true);
        rollDiceButton.setText(turnLabel.getText() + " : 주사위 굴리기");
    }

    public void disableDiceButton() {
        rollDiceButton.setEnabled(false);
        rollDiceButton.setText(turnLabel.getText() + " : 이동할 칸을 선택...");
    }
}