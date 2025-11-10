package client;

import javax.swing.JFrame;
import java.awt.BorderLayout;
import java.awt.EventQueue;

/**
 * 클라이언트 GUI의 메인 프레임(JFrame)입니다.
 * GamePanel(맵)과 HudPanel(컨트롤)을 조립합니다.
 */
public class ClientApp extends JFrame {

    private GamePanel gamePanel;
    private HudPanel hudPanel;

    public ClientApp() {
        setTitle("For The JAVA - 턴제 맵 테스트");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 1. HudPanel 생성. '주사위 굴리기' 버튼(onDiceRoll)과
        //    '현재 턴'을 물어보는 람다식(gamePanel::getCurrentPlayer)을 전달합니다.
        hudPanel = new HudPanel(
            this::onDiceRoll,       // 주사위 굴리기 액션
            () -> gamePanel.getCurrentPlayer() // 현재 턴 확인
        );

        // 2. GamePanel 생성 (HudPanel을 넘겨서 로그를 찍을 수 있게 함)
        gamePanel = new GamePanel(hudPanel);

        // 3. JFrame에 두 패널을 배치
        add(gamePanel, BorderLayout.CENTER);
        add(hudPanel, BorderLayout.EAST);

        pack();
        setLocationRelativeTo(null);
        setResizable(false);
    }

    /**
     * HudPanel의 '주사위 굴리기' 버튼이 눌리면 호출되는 메소드
     */
    private void onDiceRoll() {
        gamePanel.rollDice();
    }

    /**
     * 프로그램 시작점 (이 파일을 실행하세요)
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                ClientApp frame = new ClientApp();
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}