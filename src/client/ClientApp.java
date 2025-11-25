package client;

import shared.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.List;

public class ClientApp extends JFrame {
    private static final String SERVER_IP = "127.0.0.1"; // 로컬 IP
    private static final int PORT = 9999;

    // 네트워크
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    
    // 플레이어 정보
    private int myId = -1;
    private String myName;

    // UI 레이아웃
    private CardLayout cardLayout;
    private JPanel mainContainer;
    
    // 하위 패널들
    private LobbyPanel lobbyPanel;
    private GamePanel gamePanel;
    private BattlePanel battlePanel; // ⭐ [추가] 전투 패널

    public ClientApp() {
        setTitle("For The King - 접속 중...");
        setSize(850, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 1. 레이아웃 설정
        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);

        // 2. 패널 생성 (this 전달)
        lobbyPanel = new LobbyPanel(this);
        gamePanel = new GamePanel(this);
        battlePanel = new BattlePanel(this); // ⭐ 생성

        // 3. 패널 등록
        mainContainer.add(lobbyPanel, "LOBBY");
        mainContainer.add(gamePanel, "GAME");
        mainContainer.add(battlePanel, "BATTLE"); // ⭐ 등록

        add(mainContainer);

        // 4. 키보드 입력 설정 (KeyBindings)
        setupKeyBindings();

        // 5. 닉네임 입력 및 접속
        setVisible(true);
        myName = JOptionPane.showInputDialog(this, "닉네임을 입력하세요:", "모험가 입장", JOptionPane.QUESTION_MESSAGE);
        if (myName == null || myName.trim().isEmpty()) myName = "Unknown";

        connect();
    }

    // ⭐ 키 바인딩: 포커스 문제 없이 이동 처리
    private void setupKeyBindings() {
        InputMap inputMap = mainContainer.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = mainContainer.getActionMap();

        // 상하좌우 매핑
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "MOVE_UP");
        actionMap.put("MOVE_UP", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { sendMove(0, -1); }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "MOVE_DOWN");
        actionMap.put("MOVE_DOWN", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { sendMove(0, 1); }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "MOVE_LEFT");
        actionMap.put("MOVE_LEFT", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { sendMove(-1, 0); }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "MOVE_RIGHT");
        actionMap.put("MOVE_RIGHT", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { sendMove(1, 0); }
        });
    }

    private void sendMove(int dx, int dy) {
        // 게임 맵 화면이 아닐 때(로비, 전투 중)는 키보드 이동 무시
        if (!gamePanel.isVisible()) return;
        
        send(new Message(Message.Type.MOVE_REQ, new int[]{dx, dy}));
    }

    private void connect() {
        new Thread(() -> {
            try {
                socket = new Socket(SERVER_IP, PORT);
                
                // 스트림 생성 순서 중요 (Deadlock 방지)
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(socket.getInputStream());

                System.out.println("✅ 서버 연결 성공");

                while (true) {
                    Message msg = (Message) in.readObject();

                    switch (msg.type) {
                        case LOGIN:
                            this.myId = (int) msg.payload;
                            setTitle("For The King - " + myName + " (P" + (myId + 1) + ")");
                            send(new Message(Message.Type.SET_NAME, myName));
                            break;

                        case LOBBY_UPDATE:
                            List<Player> players = (List<Player>) msg.payload;
                            SwingUtilities.invokeLater(() -> lobbyPanel.updatePlayerList(players, myId));
                            break;

                        case CHAT:
                            String chatMsg = (String) msg.payload;
                            SwingUtilities.invokeLater(() -> lobbyPanel.appendChat(chatMsg));
                            break;

                        case START_GAME:
                            GameState initialState = (GameState) msg.payload;
                            SwingUtilities.invokeLater(() -> {
                                gamePanel.updateState(initialState);
                                cardLayout.show(mainContainer, "GAME");
                                mainContainer.requestFocusInWindow();
                            });
                            break;
                            
                        case STATE_UPDATE:
                            GameState state = (GameState) msg.payload;
                            SwingUtilities.invokeLater(() -> {
                                // ⭐ [핵심] 전투 모드 여부에 따라 화면 전환
                                if (state.isBattleMode) {
                                    // 전투 중이면 BattlePanel 표시
                                    battlePanel.updateState(state);
                                    cardLayout.show(mainContainer, "BATTLE");
                                } else {
                                    // 전투 아니면 GamePanel (맵) 표시
                                    gamePanel.updateState(state);
                                    cardLayout.show(mainContainer, "GAME");
                                    // 맵으로 돌아왔을 때 키보드 포커스 확보
                                    mainContainer.requestFocusInWindow(); 
                                }
                            });
                            break;
                    }
                }
            } catch (Exception e) {
                System.out.println("연결 끊김: " + e.getMessage());
                SwingUtilities.invokeLater(() -> 
                    JOptionPane.showMessageDialog(this, "서버 연결이 종료되었습니다.", "알림", JOptionPane.ERROR_MESSAGE)
                );
            }
        }).start();
    }

    public void send(Message msg) {
        try {
            if (out != null) {
                out.reset(); // 객체 상태 갱신 필수
                out.writeObject(msg);
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getMyId() { return myId; }
    public String getMyName() { return myName; }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientApp::new);
    }
}