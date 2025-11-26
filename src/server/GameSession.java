package server;

import client.Message;
import java.util.*;

public class GameSession extends Thread {
    private ClientHandler player1;
    private ClientHandler player2;

    private List<Integer> p1Deck;
    private List<Integer> p2Deck;

    private int p1Score = 0;
    private int p2Score = 0;
    private int currentRound = 1;

    private int p1Card = -1;
    private int p2Card = -1;
    private boolean p1IsFirst;

    public GameSession(ClientHandler p1, ClientHandler p2) {
        this.player1 = p1;
        this.player2 = p2;
        // 초기화
        this.p1Deck = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8));
        this.p2Deck = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8));
    }

    @Override
    public void run() {
        p1IsFirst = Math.random() < 0.5;
        System.out.println("[SESSION] 시작: " + player1.getPlayerName() + " vs " + player2.getPlayerName());

        player1.send(new Message(Message.Type.GAME_START, player2.getPlayerName() + "," + p1IsFirst));
        player2.send(new Message(Message.Type.GAME_START, player1.getPlayerName() + "," + !p1IsFirst));

        sendOpponentHandInfo();

        try { Thread.sleep(1000); } catch (Exception e) {}
        startRound();
    }

    private void sendOpponentHandInfo() {
        String p2Counts = countBlackWhite(p2Deck);
        player1.send(new Message(Message.Type.UPDATE_OPP_COUNTS, p2Counts));

        String p1Counts = countBlackWhite(p1Deck);
        player2.send(new Message(Message.Type.UPDATE_OPP_COUNTS, p1Counts));
    }

    private String countBlackWhite(List<Integer> deck) {
        int black = 0;
        int white = 0;
        for (int num : deck) {
            if (num % 2 == 0) black++;
            else white++;
        }
        return black + "," + white;
    }

    private void startRound() {
        if (currentRound > 9) {
            finishGame();
            return;
        }
        p1Card = -1;
        p2Card = -1;
        requestMove(p1IsFirst ? player1 : player2);
    }

    private void requestMove(ClientHandler player) {
        player.send(new Message(Message.Type.YOUR_TURN, null));
    }

    public synchronized void handleSubmission(ClientHandler sender, int cardValue) {
        boolean isP1 = (sender == player1);

        if (isP1) {
            if(!p1Deck.contains(cardValue)) return; 
            p1Card = cardValue;
            p1Deck.remove(Integer.valueOf(cardValue));
        } else {
            if(!p2Deck.contains(cardValue)) return;
            p2Card = cardValue;
            p2Deck.remove(Integer.valueOf(cardValue));
        }

        String colorHint = (cardValue % 2 == 0) ? "BLACK" : "WHITE";

        if ((isP1 == p1IsFirst) && (isP1 ? p2Card : p1Card) == -1) {
            ClientHandler next = isP1 ? player2 : player1;
            next.send(new Message(Message.Type.OPPONENT_SUBMITTED, colorHint));
            requestMove(next);
        } 
        else if (p1Card != -1 && p2Card != -1) {
            resolveRound();
        }
    }

    private void resolveRound() {
        String winner = "DRAW";
        if (p1Card > p2Card) {
            p1Score++;
            p1IsFirst = true;
            winner = "P1";
        } else if (p2Card > p1Card) {
            p2Score++;
            p1IsFirst = false;
            winner = "P2";
        }

        // 결과 전송
        String p1Res = String.format("%s/%d/%d/%d/%d", 
            (winner.equals("P1") ? "WIN" : (winner.equals("DRAW") ? "DRAW" : "LOSE")), p1Card, p2Card, p1Score, p2Score);
        
        String p2Res = String.format("%s/%d/%d/%d/%d", 
            (winner.equals("P2") ? "WIN" : (winner.equals("DRAW") ? "DRAW" : "LOSE")), p2Card, p1Card, p2Score, p1Score);

        player1.send(new Message(Message.Type.ROUND_RESULT, p1Res));
        player2.send(new Message(Message.Type.ROUND_RESULT, p2Res));

        // 감소된 카드 다시 동기화
        sendOpponentHandInfo();

        currentRound++;

        new Thread(() -> {
            try { Thread.sleep(3000); } catch(Exception e){}
            startRound();
        }).start();
    }

    private void finishGame() {
        String res;
        if (p1Score > p2Score) res = "승리! (" + player1.getPlayerName() + ")";
        else if (p2Score > p1Score) res = "승리! (" + player2.getPlayerName() + ")";
        else res = "무승부!";
        
        Message msg = new Message(Message.Type.GAME_OVER, res);
        player1.send(msg);
        player2.send(msg);
    }
}