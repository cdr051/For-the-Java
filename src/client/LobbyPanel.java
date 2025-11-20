package client;

import shared.GameProtocol;
import javax.swing.*;
import java.awt.*;

public class LobbyPanel extends JPanel {
    private NetworkManager networkManager;
    private JTextArea logArea;
    private JButton readyButton;
    private JComboBox<String> classSelector;

    public LobbyPanel(NetworkManager networkManager) {
        this.networkManager = networkManager;
        setLayout(new BorderLayout(5, 5));
        setPreferredSize(new Dimension(500, 400)); // 크기 조정

        logArea = new JTextArea("서버에 접속 중...");
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        
        String[] classes = {"Warrior", "Wizard"}; //
        classSelector = new JComboBox<>(classes);
        bottomPanel.add(new JLabel("클래스 선택:"));
        bottomPanel.add(classSelector);

        readyButton = new JButton("Ready");
        readyButton.addActionListener(e -> onReadyClick());
        bottomPanel.add(readyButton);
        
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void onReadyClick() {
        String selectedClass = (String) classSelector.getSelectedItem();
        log("클래스 [" + selectedClass + "] 선택. 준비 완료.");
        
        networkManager.sendMessage(new GameProtocol(GameProtocol.C_MSG_READY, selectedClass));
        
        readyButton.setEnabled(false);
        classSelector.setEnabled(false);
        readyButton.setText("Waiting...");
    }

    public void log(String message) {
        EventQueue.invokeLater(() -> {
            logArea.append("\n" + message);
        });
    }
}