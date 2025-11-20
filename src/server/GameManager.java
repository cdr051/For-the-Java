package server;

import shared.*;
import systems.*;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class GameManager {
    private List<ClientHandler> clients = new ArrayList<>();
    private GameState gameState;
    private int readyCount = 0;
    
    private Dice dice;
    private BattleSystem battleSystem;
    private ShopManager shopManager;
    private EventManager eventManager;

    public GameManager() {
        this.gameState = new GameState();
        this.dice = new Dice();
        this.battleSystem = new BattleSystem();
        this.shopManager = new ShopManager();
        this.eventManager = new EventManager();
        
        gameState.setMap(MapGenerator.createMap());
    }

    public synchronized void addClient(ClientHandler client) {
        clients.add(client);
    }

    private Consumer<String> logger = (log) -> {
        System.out.println(log);
        broadcast(new GameProtocol(GameProtocol.S_MSG_UPDATE_LOG, log), null);
    };

    public synchronized void handleMessage(ClientHandler from, GameProtocol msg) {
        String type = msg.getMessageType();

        if (gameState.isBattleActive() && !type.equals(GameProtocol.C_MSG_ATTACK)) {
            from.sendMessage(new GameProtocol(GameProtocol.S_MSG_UPDATE_LOG, "전투 중에는 다른 행동을 할 수 없습니다."));
            return;
        }

        switch(type) {
            case GameProtocol.C_MSG_READY:
                handleReady(from, (String) msg.getPayload());
                break;
            case GameProtocol.C_MSG_ROLL_DICE:
                handleRollDice(from);
                break;
            case GameProtocol.C_MSG_MOVE:
                handleMove(from, (Point) msg.getPayload());
                break;
            case GameProtocol.C_MSG_ATTACK:
                handleAttack(from, (AttackInfo) msg.getPayload());
                break;
        }
    }

    private void handleReady(ClientHandler from, String selectedClass) {
        if (from.isReady()) return;
        
        Player newPlayer;
        if (selectedClass.equals("Warrior")) {
            newPlayer = new Player(selectedClass, "Warrior", 10, 5, 2, 1, 30);
        } else {
            newPlayer = new Player(selectedClass, "Wizard", 5, 10, 1, 2, 30);
        }
        
        gameState.addPlayer(newPlayer);
        from.setPlayer(newPlayer);
        from.setReady(true);
        readyCount++;
        
        logger.accept(from.getPlayer().getPlayerName() + " is Ready! (Class: " + selectedClass + ")");

        if (readyCount == 2) {
            logger.accept("모두 준비 완료. 게임을 시작합니다.");
            broadcast(new GameProtocol(GameProtocol.S_MSG_GAME_START, gameState), null);
            broadcast(new GameProtocol(GameProtocol.S_MSG_YOUR_TURN, "Player 1 (Warrior)의 턴입니다."), null);
        }
    }

    private void handleRollDice(ClientHandler from) {
        if (from.getPlayerIndex() != gameState.getCurrentPlayerTurn()) {
            from.sendMessage(new GameProtocol(GameProtocol.S_MSG_UPDATE_LOG, "당신의 턴이 아닙니다."));
            return;
        }
        
        int roll = dice.rollD3();
        from.getPlayer().setLastDiceRoll(roll);
        from.sendMessage(new GameProtocol(GameProtocol.S_MSG_DICE_RESULT, roll));
    }

    // 이동 처리
    private void handleMove(ClientHandler from, Point target) {
        Player player = from.getPlayer();
        if (from.getPlayerIndex() != gameState.getCurrentPlayerTurn()) {
            from.sendMessage(new GameProtocol(GameProtocol.S_MSG_UPDATE_LOG, "당신의 턴이 아닙니다."));
            return;
        }
        if (player.getLastDiceRoll() == 0) {
            from.sendMessage(new GameProtocol(GameProtocol.S_MSG_UPDATE_LOG, "주사위를 먼저 굴리세요."));
            return;
        }
        
        if (isValidMove(player.getX(), player.getY(), target.x, target.y, player.getLastDiceRoll())) {
            player.setPosition(target.x, target.y);
            
            boolean battleStarted = processTileEvent(player, target.x, target.y);
            
            gameState.setTileAt(target.x, target.y, TileType.BLANK); 
            player.setLastDiceRoll(0); 
            
            if (!battleStarted) {
                // 전투가 시작되지 않았을 때만 턴 넘김
                gameState.nextTurn();
                broadcast(new GameProtocol(GameProtocol.S_MSG_UPDATE_STATE, gameState), null);
                logger.accept(gameState.getCurrentPlayer().getPlayerName() + "의 턴입니다.");
            }
        } else {
            from.sendMessage(new GameProtocol(GameProtocol.S_MSG_INVALID_MOVE, "이동할 수 없는 칸입니다."));
        }
    }
    
    // 공격 처리
    private void handleAttack(ClientHandler from, AttackInfo info) {
        if (from.getPlayerIndex() != gameState.getCurrentPlayerTurn()) {
            from.sendMessage(new GameProtocol(GameProtocol.S_MSG_UPDATE_LOG, "당신의 턴이 아닙니다."));
            return;
        }
        
        BattleState currentBattle = gameState.getCurrentBattle();
        Player player = currentBattle.getPlayerInTurn();
        List<Monster> monsters = currentBattle.getMonsters();

        battleSystem.handlePlayerAttack(player, monsters, info, logger);
        battleSystem.processMonsterTurn(player, monsters, logger);
        
        if (battleSystem.isBattleOver(monsters, player)) {
            logger.accept("전투 종료!");
            gameState.setBattle(null);
            gameState.nextTurn();
            
            broadcast(new GameProtocol(GameProtocol.S_MSG_BATTLE_END, "전투 승리! 맵으로 복귀합니다."), null);
            broadcast(new GameProtocol(GameProtocol.S_MSG_UPDATE_STATE, gameState), null);
            logger.accept(gameState.getCurrentPlayer().getPlayerName() + "의 턴입니다.");

        } else {
            broadcast(new GameProtocol(GameProtocol.S_MSG_BATTLE_UPDATE, gameState.getCurrentBattle()), null);
        }
    }

    private boolean processTileEvent(Player player, int x, int y) {
        TileType type = gameState.getTileAt(x, y);
        
        switch (type) {
            case MONSTER:
                Monster[] monsters = battleSystem.startMonsterBattle(logger);
                BattleState battle = new BattleState(player, Arrays.asList(monsters));
                gameState.setBattle(battle);
                broadcast(new GameProtocol(GameProtocol.S_MSG_BATTLE_START, battle), null);
                return true;
            case BOSS:
                return true;
            case SHOP:
                shopManager.openShop(logger);
                break;
            case TREASURE:
                eventManager.startTreasure(logger);
                break;
            case EVENT:
                eventManager.startEvent(logger);
                break;
            default:
                break;
        }
        return false;
    }
    
    private boolean isValidMove(int cX, int cY, int tX, int tY, int roll) {
        int dist = Math.abs(tX - cX) + Math.abs(tY - cY);
        return dist > 0 && dist <= roll;
    }

    public synchronized void broadcast(GameProtocol msg, ClientHandler exclude) {
        for (ClientHandler client : clients) {
            if (client != exclude) {
                client.sendMessage(msg);
            }
        }
    }
    
    public synchronized void removeClient(ClientHandler client) {
        clients.remove(client);
        if(client.isReady()) readyCount--;
    }
}