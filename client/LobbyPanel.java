package client;

import shared.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;

public class LobbyPanel extends JPanel {
    private ClientApp mainApp;
    private DefaultListModel<String> playerListModel;
    private JTextArea chatArea;
    private JTextField chatInput;
    private JComboBox<String> jobCombo;
    private JButton btnAction; // 준비 or 시작 버튼

    private boolean isMyReady = false;
    private boolean amIHost = false;

    public LobbyPanel(ClientApp app) {
        this.mainApp = app;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 1. 플레이어 목록 (좌측)
        playerListModel = new DefaultListModel<>();
        JList<String> playerList = new JList<>(playerListModel);
        playerList.setBorder(new TitledBorder("접속자 목록"));
        add(new JScrollPane(playerList), BorderLayout.WEST);
        playerList.setPreferredSize(new Dimension(200, 0));

        // 2. 채팅 영역 (중앙)
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        // 3. 하단 컨트롤 패널 (직업선택, 채팅입력, 버튼)
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        
        // 직업 선택
        JPanel jobPanel = new JPanel();
        jobPanel.add(new JLabel("직업: "));
        jobCombo = new JComboBox<>(new String[]{"기사", "마법사", "궁수", "도적"});
        jobPanel.add(jobCombo);
        
        // 직업 변경 시 서버로 전송
        jobCombo.addActionListener(e -> {
            String selected = (String) jobCombo.getSelectedItem();
            mainApp.send(new Message(Message.Type.CHANGE_JOB, selected));
        });

        // 채팅 입력
        chatInput = new JTextField();
        chatInput.addActionListener(e -> {
            String text = chatInput.getText();
            if(!text.trim().isEmpty()) {
                mainApp.send(new Message(Message.Type.CHAT, mainApp.getMyName() + ": " + text));
                chatInput.setText("");
            }
        });

        // 준비/시작 버튼
        btnAction = new JButton("준비 완료");
        btnAction.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnAction.addActionListener(e -> handleActionButton());

        bottomPanel.add(jobPanel, BorderLayout.WEST);
        bottomPanel.add(chatInput, BorderLayout.CENTER);
        bottomPanel.add(btnAction, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    // 서버로부터 플레이어 목록 갱신 수신
    public void updatePlayerList(List<Player> players, int myId) {
        playerListModel.clear();
        boolean allReady = true;

        for (Player p : players) {
            String status = p.isHost ? "[방장]" : (p.isReady ? "[준비됨]" : "[대기]");
            String meMark = (p.id == myId) ? " (나)" : "";
            playerListModel.addElement(String.format("%s %s (%s)%s", status, p.name, p.jobClass, meMark));

            if (p.id == myId) {
                this.amIHost = p.isHost;
            }
            if (!p.isReady) allReady = false;
        }

        // 버튼 상태 업데이트 logic
        if (amIHost) {
            btnAction.setText("게임 시작");
            btnAction.setEnabled(allReady); // 모두 준비되면 활성화
        } else {
            btnAction.setText(isMyReady ? "준비 취소" : "준비 완료");
        }
    }

    public void appendChat(String msg) {
        chatArea.append(msg + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    private void handleActionButton() {
        if (amIHost) {
            // 방장: 게임 시작 요청
            mainApp.send(new Message(Message.Type.START_GAME, null));
        } else {
            // 일반유저: 준비 토글
            isMyReady = !isMyReady;
            mainApp.send(new Message(Message.Type.READY, isMyReady));
        }
    }
}