package client;

import shared.BattleState;
import shared.GameState;
import javax.swing.*;
import java.awt.*; // BorderLayout, CardLayout, EventQueue 등이 포함됩니다.

public class ClientApp extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private NetworkManager networkManager;
    
    private LobbyPanel lobbyPanel;
    private GamePanel gamePanel;
    private HudPanel hudPanel;
    private BattlePanel battlePanel;

    public ClientApp() {
        setTitle("For The JAVA - Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        networkManager = new NetworkManager(this);
        
        // 1. 로비 패널 생성
        lobbyPanel = new LobbyPanel(networkManager);
        mainPanel.add(lobbyPanel, "LOBBY");

        // 2. 게임 맵 스크린 (GamePanel + HudPanel)
        // gameScreen 패널은 BorderLayout을 사용합니다.
        JPanel gameScreen = new JPanel(new BorderLayout()); 
        hudPanel = new HudPanel(networkManager);
        gamePanel = new GamePanel(networkManager);
        
        // --- 오류 발생 지점 (line 36-37) ---
        // 이 코드가 작동하려면 'import java.awt.BorderLayout;'이 필요합니다.
        gameScreen.add(gamePanel, BorderLayout.CENTER);
        gameScreen.add(hudPanel, BorderLayout.EAST);
        
        // 이 코드가 작동하려면 mainPanel이 CardLayout이어야 합니다.
        mainPanel.add(gameScreen, "GAME_MAP");
        // --- ----------------------- ---
        
        // 3. 전투 패널 생성
        battlePanel = new BattlePanel(networkManager);
        mainPanel.add(battlePanel, "BATTLE");
        
        add(mainPanel);
        pack();
        setLocationRelativeTo(null);
        setResizable(false);
        
        networkManager.connect("localhost", 9999);
    }

    // --- (이하 NetworkManager가 호출하는 메소드들) ---

    public void updateLog(String message) {
        if (lobbyPanel.isShowing()) {
            lobbyPanel.log(message);
        } else {
            hudPanel.log(message);
        }
    }

    public void showGameMap(GameState gameState) {
        gamePanel.updateState(gameState); 
        hudPanel.updateState(gameState);
        cardLayout.show(mainPanel, "GAME_MAP");
        setTitle("For The JAVA - Game Map");
    }
    
    public void updateGameState(GameState gameState) {
        gamePanel.updateState(gameState);
        hudPanel.updateState(gameState);
    }
    
    public void onDiceRolled(int result) {
        hudPanel.log("주사위 결과: " + result + "!");
        hudPanel.log(result + "칸 이하로 이동하세요.");
        hudPanel.disableDiceButton();
    }
    
    public void onTurnStarted(String message) {
        hudPanel.log(message);
        hudPanel.enableDiceButton();
    }
    
    public void onBattleStart(BattleState battleState) {
        EventQueue.invokeLater(() -> {
            battlePanel.startBattle(battleState);
            cardLayout.show(mainPanel, "BATTLE");
            setTitle("For The JAVA - Battle!");
        });
    }

    public void onBattleUpdate(BattleState battleState) {
        EventQueue.invokeLater(() -> {
            battlePanel.updateBattle(battleState);
        });
    }
    
    public void onBattleEnd(String message) {
        EventQueue.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, message);
            cardLayout.show(mainPanel, "GAME_MAP");
            setTitle("For The JAVA - Game Map");
        });
    }
    
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            new ClientApp().setVisible(true);
        });
    }
}