package client;

import javax.swing.*;
import java.awt.*;

public class ClientApp extends JFrame {

    private JTextField nameField;
    private JTextArea chatArea;
    private JTextField inputField;
    private JList<String> playerListUI;

    private JButton connectButton, sendButton;

    private NetworkManager network;

    public ClientApp() {
        setTitle("For The Java - Lobby");
        setSize(650, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        network = new NetworkManager();

        // 서버 메시지 처리
        network.setOnMessageReceived(msg -> {
            switch (msg.getType()) {

                case CHAT -> SwingUtilities.invokeLater(() ->
                        chatArea.append(msg.getData().toString() + "\n"));

                case PLAYER_JOIN -> SwingUtilities.invokeLater(() ->
                        chatArea.append("[JOIN] " + msg.getData() + "\n"));

                case PLAYER_LIST -> SwingUtilities.invokeLater(() -> {
                    var arr = ((java.util.List<String>) msg.getData())
                            .toArray(new String[0]);
                    playerListUI.setListData(arr);
                });
            }
        });

        // 상단 (닉네임 + 접속)
        JPanel top = new JPanel(new FlowLayout());
        nameField = new JTextField(15);
        connectButton = new JButton("접속하기");
        top.add(new JLabel("닉네임:"));
        top.add(nameField);
        top.add(connectButton);
        add(top, BorderLayout.NORTH);

        // ------- 왼쪽: 플레이어 리스트 -------
        playerListUI = new JList<>();
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(new JLabel("Players"), BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(playerListUI), BorderLayout.CENTER);

        add(leftPanel, BorderLayout.WEST);

        // ------- 중앙: 채팅창 -------
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        // ------- 하단: 채팅 입력 -------
        JPanel bottomPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        sendButton = new JButton("전송");
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

        // 이벤트 연결
        connectButton.addActionListener(e -> connect());
        sendButton.addActionListener(e -> sendChat());
        inputField.addActionListener(e -> sendChat());

        setVisible(true);
    }

    private void connect() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "닉네임을 입력하세요.");
            return;
        }

        boolean ok = network.connect("127.0.0.1", 5000, name);
        if (!ok) {
            chatArea.append("[SYSTEM] 서버 접속 실패.\n");
            return;
        }

        chatArea.append("[SYSTEM] 서버에 연결됨.\n");
        connectButton.setEnabled(false);
        nameField.setEnabled(false);

        // 대기방 플레이어 리스트 요청
        network.send(new Message(Message.Type.REQUEST_PLAYER_LIST, null));
    }

    private void sendChat() {
        if (inputField.getText().isEmpty()) return;
        network.sendChat(inputField.getText());
        inputField.setText("");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientApp::new);
    }
}
