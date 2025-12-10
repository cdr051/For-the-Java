package client;

import shared.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.List;

public class ClientApp extends JFrame {
    private static final String SERVER_IP = GameConfig.DEFAULT_SERVER_IP;
    private static final int PORT = GameConfig.SERVER_PORT;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private int myId = -1;
    private String myName;

    private CardLayout cardLayout;
    private JPanel mainContainer;

    private LobbyPanel lobbyPanel;
    private GamePanel gamePanel;
    private BattlePanel battlePanel;
    private ShopPanel shopPanel;

    public ClientApp() {
        setTitle("For The King - 접속 중...");

        // ⭐ [수정] 초기 실행 시(로비)에는 기본 크기로 설정
        setSize(850, 650);
        setLocationRelativeTo(null); // 화면 중앙 배치
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);

        lobbyPanel = new LobbyPanel(this);
        gamePanel = new GamePanel(this);
        battlePanel = new BattlePanel(this);
        shopPanel = new ShopPanel(this);

        mainContainer.add(lobbyPanel, "LOBBY");
        mainContainer.add(gamePanel, "GAME");
        mainContainer.add(battlePanel, "BATTLE");
        mainContainer.add(shopPanel, "SHOP");

        add(mainContainer);

        setupKeyBindings();

        setVisible(true);
        myName = JOptionPane.showInputDialog(this, "닉네임을 입력하세요:", "모험가 입장", JOptionPane.QUESTION_MESSAGE);
        if (myName == null || myName.trim().isEmpty()) myName = "Unknown";

        connect();
    }

    private void setupKeyBindings() {
        InputMap inputMap = mainContainer.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = mainContainer.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "MOVE_UP");
        actionMap.put("MOVE_UP", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                sendMove(0, -1);
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "MOVE_DOWN");
        actionMap.put("MOVE_DOWN", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                sendMove(0, 1);
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "MOVE_LEFT");
        actionMap.put("MOVE_LEFT", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                sendMove(-1, 0);
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "MOVE_RIGHT");
        actionMap.put("MOVE_RIGHT", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                sendMove(1, 0);
            }
        });
    }

    private void sendMove(int dx, int dy) {
        if (!gamePanel.isVisible()) return;
        send(new Message(Message.Type.MOVE_REQ, new int[] { dx, dy }));
    }

    public void switchToLobby() {
        SwingUtilities.invokeLater(() -> cardLayout.show(mainContainer, "LOBBY"));
    }

    // ⭐ [추가] 창 모드 전환 헬퍼 메서드
    private void setWindowMode(boolean isGameMode) {
        if (isGameMode) {
            // 게임 중: 최대화
            if (getExtendedState() != JFrame.MAXIMIZED_BOTH) {
                setExtendedState(JFrame.MAXIMIZED_BOTH);
            }
        } else {
            // 로비: 일반 크기 복구
            if (getExtendedState() != JFrame.NORMAL) {
                setExtendedState(JFrame.NORMAL);
                setSize(850, 650);
                setLocationRelativeTo(null); // 다시 중앙으로
            }
        }
    }

    private void connect() {
        new Thread(() -> {
            try {
                socket = new Socket(SERVER_IP, PORT);
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(socket.getInputStream());
                System.out.println("✅ 서버 연결 성공");

                while (true) {
                    Message msg = null;
                    try {
                        msg = (Message) in.readObject();
                    } catch (ClassNotFoundException e) {
                        System.err.println("❌ 잘못된 메시지 형식 수신: " + e.getMessage());
                        continue;
                    } catch (ClassCastException e) {
                        System.err.println("❌ 메시지 타입 변환 실패: " + e.getMessage());
                        continue;
                    }
                    
                    if (msg == null || msg.type == null) continue;

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
                                // ⭐ 게임 시작 시 창 최대화
                                setWindowMode(true);
                                
                                gamePanel.updateState(initialState);
                                cardLayout.show(mainContainer, "GAME");
                                mainContainer.requestFocusInWindow();
                            });
                            break;

                        case STATE_UPDATE:
                            GameState state = (GameState) msg.payload;
                            SwingUtilities.invokeLater(() -> {
                                boolean isLobbyState = (state.roundNumber == 0);
                                setWindowMode(!isLobbyState);

                                if (!state.isBattleMode && !state.isShopMode && state.currentTurnPlayerId == myId) {
                                    Player me = state.players.get(myId);
                                    if (me.hasRolled && me.movePoints > 0) {
                                        gamePanel.stopDiceAnimation(me.movePoints);
                                    }
                                }

                                if (state.isBattleMode) {
                                    battlePanel.updateState(state);
                                    cardLayout.show(mainContainer, "BATTLE");
                                } else if (state.isShopMode) {
                                    shopPanel.updateState(state);
                                    cardLayout.show(mainContainer, "SHOP");
                                } else {
                                    if (!isLobbyState) {
                                        gamePanel.updateState(state);
                                        if (!lobbyPanel.isVisible()) {
                                            cardLayout.show(mainContainer, "GAME");
                                            mainContainer.requestFocusInWindow();
                                        }
                                    } else {
                                        cardLayout.show(mainContainer, "LOBBY");
                                    }
                                }
                            });
                            break;
                    }
                }
            } catch (java.net.ConnectException e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this,
                        "서버에 연결할 수 없습니다.\n서버가 실행 중인지 확인해주세요.",
                        "연결 실패",
                        JOptionPane.ERROR_MESSAGE);
                    System.exit(0);
                });
            } catch (java.io.EOFException e) {
                System.out.println("🔌 서버 연결이 종료되었습니다.");
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this,
                        "서버와의 연결이 끊어졌습니다.",
                        "연결 종료",
                        JOptionPane.WARNING_MESSAGE);
                });
            } catch (IOException e) {
                System.err.println("❌ 네트워크 오류: " + e.getMessage());
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this,
                        "네트워크 오류가 발생했습니다: " + e.getMessage(),
                        "오류",
                        JOptionPane.ERROR_MESSAGE);
                });
            } catch (Exception e) {
                System.err.println("❌ 예상치 못한 오류: " + e.getMessage());
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this,
                        "예상치 못한 오류가 발생했습니다.",
                        "오류",
                        JOptionPane.ERROR_MESSAGE);
                });
            } finally {
                cleanupConnection();
            }
        }).start();
    }
    
    private void cleanupConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("⚠️ 리소스 정리 중 오류: " + e.getMessage());
        }
    }

    public void send(Message msg) {
        try {
            if (out != null) {
                out.reset();
                out.writeObject(msg);
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getMyId() {
        return myId;
    }

    public String getMyName() {
        return myName;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientApp::new);
    }
}