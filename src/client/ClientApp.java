package client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClientApp extends JFrame {

    private NetworkManager network;
    private CardLayout cardLayout;
    private JPanel mainPanel;

    // 로비
    private JPanel lobbyPanel;
    private JTextField nameField, chatInput;
    private JTextArea chatArea;
    private JButton connectBtn, sendBtn;
    private JList<String> userList;

    // 게임
    private JPanel gamePanel;
    private JLabel lblOpponentInfo;
    private JPanel oppHandPanel; // 상대 패 그리는 공간
    private JPanel tablePanel;
    private JLabel lblOppCard, lblMyCard; 
    private JLabel lblGameStatus;         
    private JLabel lblScoreBoard;         
    private JPanel myHandPanel;
    private JButton[] myCardButtons = new JButton[9]; 

    public ClientApp() {
        setTitle("흑과 백 (Black & White) - Secure Client");
        setSize(1000, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        network = new NetworkManager();
        network.setOnMessageReceived(this::onMessage);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        initLobbyUI();
        initGameUI();

        mainPanel.add(lobbyPanel, "LOBBY");
        mainPanel.add(gamePanel, "GAME");

        add(mainPanel);
        setVisible(true);
    }

    private void initLobbyUI() {
        lobbyPanel = new JPanel(new BorderLayout());
        JPanel top = new JPanel();
        nameField = new JTextField("Player", 10);
        connectBtn = new JButton("게임 입장");
        connectBtn.addActionListener(e -> connectToServer());
        top.add(new JLabel("닉네임: "));
        top.add(nameField);
        top.add(connectBtn);
        lobbyPanel.add(top, BorderLayout.NORTH);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        userList = new JList<>();
        JSplitPane center = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, 
                new JScrollPane(chatArea), new JScrollPane(userList));
        center.setDividerLocation(700);
        lobbyPanel.add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        chatInput = new JTextField();
        sendBtn = new JButton("전송");
        sendBtn.addActionListener(e -> sendChat());
        chatInput.addActionListener(e -> sendChat());
        bottom.add(chatInput, BorderLayout.CENTER);
        bottom.add(sendBtn, BorderLayout.EAST);
        lobbyPanel.add(bottom, BorderLayout.SOUTH);
    }

    private void initGameUI() {
        gamePanel = new JPanel(new BorderLayout());
        gamePanel.setBackground(new Color(30, 30, 30)); 

        // [상단] 상대방 패 (서버가 준 개수대로 렌더링)
        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.setBackground(new Color(50, 50, 50));
        topContainer.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        lblOpponentInfo = new JLabel("상대방", SwingConstants.CENTER);
        lblOpponentInfo.setForeground(Color.WHITE);
        lblOpponentInfo.setFont(new Font("Malgun Gothic", Font.BOLD, 18));
        
        oppHandPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        oppHandPanel.setOpaque(false); 

        topContainer.add(lblOpponentInfo, BorderLayout.NORTH);
        topContainer.add(oppHandPanel, BorderLayout.CENTER);
        gamePanel.add(topContainer, BorderLayout.NORTH);

        // [중앙] 테이블 & 점수
        JPanel centerContainer = new JPanel(new BorderLayout());
        centerContainer.setBackground(new Color(0, 100, 0)); 

        JPanel statusPanel = new JPanel(new GridLayout(2, 1));
        statusPanel.setOpaque(false);
        lblGameStatus = new JLabel("대기 중...", SwingConstants.CENTER);
        lblGameStatus.setFont(new Font("Malgun Gothic", Font.BOLD, 24));
        lblGameStatus.setForeground(new Color(255, 215, 0)); 
        lblScoreBoard = new JLabel("나 0 : 0 상대", SwingConstants.CENTER);
        lblScoreBoard.setFont(new Font("Arial", Font.BOLD, 20));
        lblScoreBoard.setForeground(Color.WHITE);

        statusPanel.add(lblGameStatus);
        statusPanel.add(lblScoreBoard);
        centerContainer.add(statusPanel, BorderLayout.NORTH);

        tablePanel = new JPanel(new GridBagLayout());
        tablePanel.setOpaque(false);
        lblOppCard = createCardLabel("상대");
        lblMyCard = createCardLabel("나");
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 20, 0, 20);
        gbc.gridx = 0; tablePanel.add(lblOppCard, gbc);
        gbc.gridx = 1; tablePanel.add(lblMyCard, gbc);
        centerContainer.add(tablePanel, BorderLayout.CENTER);
        gamePanel.add(centerContainer, BorderLayout.CENTER);

        // [하단] 내 패 (랜덤 배치)
        JPanel bottomContainer = new JPanel(new BorderLayout());
        bottomContainer.setBackground(new Color(40, 40, 40));
        bottomContainer.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        JLabel lblMyName = new JLabel("나의 패", SwingConstants.CENTER);
        lblMyName.setForeground(Color.LIGHT_GRAY);
        lblMyName.setFont(new Font("Malgun Gothic", Font.BOLD, 14));
        bottomContainer.add(lblMyName, BorderLayout.NORTH);

        myHandPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        myHandPanel.setOpaque(false);

        List<Integer> randomOrder = new ArrayList<>();
        for(int i=0; i<=8; i++) randomOrder.add(i);
        Collections.shuffle(randomOrder);

        for (int num : randomOrder) {
            JButton btn = new JButton(String.valueOf(num));
            btn.setPreferredSize(new Dimension(60, 80));
            btn.setFont(new Font("Arial", Font.BOLD, 28));
            btn.setFocusPainted(false);
            if (num % 2 == 0) {
                btn.setBackground(Color.BLACK);
                btn.setForeground(Color.WHITE);
                btn.setBorder(new LineBorder(Color.GRAY, 2));
            } else {
                btn.setBackground(Color.WHITE);
                btn.setForeground(Color.BLACK);
                btn.setBorder(new LineBorder(Color.BLACK, 2));
            }
            int finalNum = num;
            btn.addActionListener(e -> submitCard(finalNum, btn));
            btn.setEnabled(false);
            myCardButtons[num] = btn;
            myHandPanel.add(btn);
        }
        bottomContainer.add(myHandPanel, BorderLayout.CENTER);
        gamePanel.add(bottomContainer, BorderLayout.SOUTH);
    }

    // --- 로직 ---
    private void connectToServer() {
        String name = nameField.getText().trim();
        if(name.isEmpty()) return;
        if(network.connect("127.0.0.1", 5000, name)) {
            chatArea.append("서버 접속 성공.\n");
            connectBtn.setEnabled(false);
            nameField.setEnabled(false);
        }
    }

    private void sendChat() {
        String msg = chatInput.getText();
        if(!msg.isEmpty()) {
            network.sendChat(msg);
            chatInput.setText("");
        }
    }

    private void submitCard(int num, JButton btn) {
        network.send(new Message(Message.Type.SUBMIT_TILE, num));
        updateTableCard(lblMyCard, num, false, null);
        btn.setVisible(false);
        setAllCardsEnabled(false);
        lblGameStatus.setText("상대방 대기 중...");
    }

    // [핵심 보안 로직] 서버가 준 개수만 가지고 화면을 그림 (데이터 은닉)
    private void refreshOpponentHand(int blackCount, int whiteCount) {
        oppHandPanel.removeAll();
        
        List<Color> handColors = new ArrayList<>();
        for(int i=0; i<blackCount; i++) handColors.add(Color.BLACK);
        for(int i=0; i<whiteCount; i++) handColors.add(Color.WHITE);
        
        // 위치로 추측 못하게 섞기
        Collections.shuffle(handColors);

        for (Color c : handColors) {
            JPanel card = new JPanel();
            card.setPreferredSize(new Dimension(40, 60));
            card.setBackground(c);
            card.setBorder(new LineBorder(Color.GRAY, 1));
            oppHandPanel.add(card);
        }
        oppHandPanel.revalidate();
        oppHandPanel.repaint();
    }

    private void updateTableCard(JLabel lbl, int number, boolean isHidden, String hiddenColor) {
        lbl.setOpaque(true);
        Color bg = Color.GRAY;
        Color fg = Color.WHITE;
        String text = "?";

        if (isHidden) {
            if ("BLACK".equals(hiddenColor)) { bg = Color.BLACK; fg = Color.WHITE; }
            else if ("WHITE".equals(hiddenColor)) { bg = Color.WHITE; fg = Color.BLACK; }
        } else {
            if (number % 2 == 0) { bg = Color.BLACK; fg = Color.WHITE; }
            else { bg = Color.WHITE; fg = Color.BLACK; }
            text = String.valueOf(number);
        }
        lbl.setBackground(bg);
        lbl.setForeground(fg);
        lbl.setText(text);
        lbl.setBorder(new LineBorder(Color.YELLOW, 3));
        lbl.setFont(new Font("Arial", Font.BOLD, 40));
    }

    private void clearTable() {
        resetCardLabel(lblOppCard, "상대");
        resetCardLabel(lblMyCard, "나");
    }

    private void resetCardLabel(JLabel lbl, String text) {
        lbl.setText(text);
        lbl.setBackground(new Color(0, 60, 0));
        lbl.setForeground(Color.WHITE);
        lbl.setBorder(new LineBorder(Color.WHITE, 2));
        lbl.setFont(new Font("Malgun Gothic", Font.BOLD, 16));
    }

    private JLabel createCardLabel(String text) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER);
        lbl.setPreferredSize(new Dimension(100, 140));
        resetCardLabel(lbl, text);
        return lbl;
    }

    private void setAllCardsEnabled(boolean enabled) {
        for(JButton btn : myCardButtons) if(btn.isVisible()) btn.setEnabled(enabled);
    }

    // --- 메시지 수신 ---
    private void onMessage(Message msg) {
        SwingUtilities.invokeLater(() -> {
            switch (msg.getType()) {
                case CHAT -> chatArea.append(msg.getData() + "\n");
                
                case GAME_START -> {
                    String[] info = ((String)msg.getData()).split(",");
                    String oppName = info[0];
                    boolean isFirst = Boolean.parseBoolean(info[1]);

                    cardLayout.show(mainPanel, "GAME");
                    lblOpponentInfo.setText("상대방: " + oppName);
                    lblGameStatus.setText(isFirst ? "당신의 선공!" : "상대방 선공!");
                    lblScoreBoard.setText("나 0 : 0 상대");
                }

                // [보안] 서버가 알려준 패 개수로 갱신
                case UPDATE_OPP_COUNTS -> {
                    String[] counts = ((String)msg.getData()).split(",");
                    int b = Integer.parseInt(counts[0]);
                    int w = Integer.parseInt(counts[1]);
                    refreshOpponentHand(b, w);
                }

                case YOUR_TURN -> {
                    lblGameStatus.setText("당신 차례! 카드 선택");
                    setAllCardsEnabled(true);
                }

                case OPPONENT_SUBMITTED -> {
                    lblGameStatus.setText("상대 제출 완료!");
                    String hint = (String) msg.getData();
                    updateTableCard(lblOppCard, -1, true, hint);
                }

                case ROUND_RESULT -> {
                    String[] parts = ((String)msg.getData()).split("/");
                    String res = parts[0];
                    int myC = Integer.parseInt(parts[1]);
                    int oppC = Integer.parseInt(parts[2]);
                    String myS = parts[3];
                    String oppS = parts[4];

                    updateTableCard(lblOppCard, oppC, false, null);
                    lblScoreBoard.setText("나 " + myS + " : " + oppS + " 상대");
                    
                    String txt = res.equals("WIN") ? "승리!" : (res.equals("LOSE") ? "패배" : "무승부");
                    lblGameStatus.setText("결과: " + txt);

                    Timer t = new Timer(3000, e -> {
                        clearTable();
                        lblGameStatus.setText("다음 라운드...");
                    });
                    t.setRepeats(false);
                    t.start();
                }

                case GAME_OVER -> {
                    JOptionPane.showMessageDialog(this, msg.getData());
                    cardLayout.show(mainPanel, "LOBBY");
                    for(JButton btn : myCardButtons) btn.setVisible(true);
                    clearTable();
                }
            }
        });
    }

    public static void main(String[] args) {
        new ClientApp();
    }
}