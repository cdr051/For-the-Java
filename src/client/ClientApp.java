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
        
        lobbyPanel = new LobbyPanel(networkManager);
        mainPanel.add(lobbyPanel, "LOBBY");

        JPanel gameScreen = new JPanel(new BorderLayout()); 
        hudPanel = new HudPanel(networkManager);
        gamePanel = new GamePanel(networkManager);

        gameScreen.add(gamePanel, BorderLayout.CENTER);
        gameScreen.add(hudPanel, BorderLayout.EAST);

        mainPanel.add(gameScreen, "GAME_MAP");

        battlePanel = new BattlePanel(networkManager);
        mainPanel.add(battlePanel, "BATTLE");
        
        add(mainPanel);
        pack();
        setLocationRelativeTo(null);
        setResizable(false);
        
        networkManager.connect("localhost", 9999);
    }

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