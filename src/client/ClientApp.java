package client;

import shared.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.List;

public class ClientApp extends JFrame {
    private static final String SERVER_IP = "127.0.0.1"; 
    private static final int PORT = 9999;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    
    private int myId = -1;
    private String myName;

    private CardLayout cardLayout;
    private JPanel mainContainer;
    
    // 패널들
    private LobbyPanel lobbyPanel;
    private GamePanel gamePanel;
    private BattlePanel battlePanel;
    private ShopPanel shopPanel;

    public ClientApp() {
        setTitle("For The King");
        setSize(1000, 800); 
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);

        // 패널 생성
        lobbyPanel = new LobbyPanel(this);
        gamePanel = new GamePanel(this);
        battlePanel = new BattlePanel(this);
        shopPanel = new ShopPanel(this); 

        // 패널 등록
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
        if (!gamePanel.isVisible()) return;
        send(new Message(Message.Type.MOVE_REQ, new int[]{dx, dy}));
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
                                setExtendedState(JFrame.MAXIMIZED_BOTH);
                                
                                gamePanel.updateState(initialState);
                                cardLayout.show(mainContainer, "GAME");
                                mainContainer.requestFocusInWindow();
                            });
                            break;
                            
                        case STATE_UPDATE:
                            GameState state = (GameState) msg.payload;
                            SwingUtilities.invokeLater(() -> {
                                if (state.isBattleMode) {
                                    battlePanel.updateState(state);
                                    cardLayout.show(mainContainer, "BATTLE");
                                } else if (state.isShopMode) {
                                    shopPanel.updateState(state);
                                    cardLayout.show(mainContainer, "SHOP");
                                } else {
                                    gamePanel.updateState(state);
                                    cardLayout.show(mainContainer, "GAME");
                                    mainContainer.requestFocusInWindow(); 
                                }
                            });
                            break;
                    }
                }
            } catch (Exception e) {
                System.out.println("연결 끊김: " + e.getMessage());
            }
        }).start();
    }

    public void send(Message msg) {
        try {
            if (out != null) {
                out.reset(); 
                out.writeObject(msg);
                out.flush();
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    public int getMyId() { return myId; }
    public String getMyName() { return myName; }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientApp::new);
    }
}