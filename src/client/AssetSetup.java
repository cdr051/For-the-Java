package client;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

public class AssetSetup {
    private static final String ROOT_PATH = "images/";

    public static void main(String[] args) {
        System.out.println("🎨 에셋 재생성을 시작합니다...");
        
        createDirectory(ROOT_PATH);
        createDirectory(ROOT_PATH + "map/");
        createDirectory(ROOT_PATH + "character/");
        createDirectory(ROOT_PATH + "monster/");
        createDirectory(ROOT_PATH + "battle/");
        createDirectory(ROOT_PATH + "shop/");
        createDirectory(ROOT_PATH + "ui/");

        // --- 1. 전투 (배경) ---
        createImage("battle", "bg_battle.png", 800, 600, new Color(50, 20, 20), "전투 배경");

        // --- ⭐ [수정] 몬스터 4종 추가 ---
        // 오크 (초록)
        createImage("monster", "mob_orc.png", 150, 150, new Color(50, 100, 50), "오크");
        // 슬라임 (파랑/젤리)
        createImage("monster", "mob_slime.png", 150, 150, new Color(100, 200, 255), "슬라임");
        // 늑대인간 (갈색/회색)
        createImage("monster", "mob_werewolf.png", 150, 150, new Color(100, 80, 60), "늑대인간");
        // 스켈레톤 (하얀색/뼈)
        createImage("monster", "mob_skeleton.png", 150, 150, new Color(200, 200, 200), "스켈레톤");

        // --- 2. 캐릭터 ---
        createImage("character", "char_knight.png", 60, 60, Color.LIGHT_GRAY, "기사");
        createImage("character", "char_mage.png", 60, 60, new Color(100, 0, 200), "마법사");
        createImage("character", "char_archer.png", 60, 60, new Color(0, 150, 0), "궁수");
        createImage("character", "char_rogue.png", 60, 60, Color.DARK_GRAY, "도적");
        createImage("character", "char_default.png", 60, 60, Color.WHITE, "기본");

        // --- 3. 맵 ---
        createImage("map", "tile_grass.png", 60, 60, new Color(100, 200, 100), "땅");
        createImage("map", "tile_water.png", 60, 60, new Color(60, 100, 250), "물");
        createImage("map", "tile_monster.png", 60, 60, new Color(200, 60, 60), "몬스터");
        createImage("map", "tile_shop.png", 60, 60, new Color(255, 165, 0), "상점");
        
        // --- 4. 상점 ---
        createImage("shop", "bg_shop.png", 800, 600, new Color(60, 40, 20), "상점 배경");
        createImage("shop", "card_atk.png", 220, 300, new Color(150, 50, 50), "공격력");
        createImage("shop", "card_hp.png", 220, 300, new Color(50, 150, 50), "최대 체력");
        createImage("shop", "card_heal.png", 220, 300, new Color(50, 50, 150), "회복");

        // --- 5. UI (주사위) ---
        for(int i=1; i<=6; i++) createDiceImage("dice_" + i + ".png", i, Color.WHITE);
        for(int i=1; i<=4; i++) createDiceImage("dice_rolling_" + i + ".png", 0, new Color(240, 240, 240)); 
        createDiceImage("dice_question.png", -1, Color.WHITE);

        System.out.println("✅ 에셋 생성 완료! Refresh(F5) 해주세요.");
    }

    private static void createDirectory(String path) {
        File dir = new File(path);
        if (!dir.exists()) dir.mkdirs();
    }

    private static void createImage(String category, String filename, int w, int h, Color color, String text) {
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color); g.fillRect(0, 0, w, h);
        g.setColor(Color.BLACK); g.setStroke(new BasicStroke(2)); g.drawRect(0, 0, w, h);
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif", Font.BOLD, 14));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, (w - fm.stringWidth(text)) / 2, h / 2);
        g.dispose();
        try { ImageIO.write(image, "png", new File(ROOT_PATH + category + "/" + filename)); } catch (Exception e) {}
    }

    private static void createDiceImage(String filename, int number, Color bg) {
        int size = 100;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(bg); g.fillRoundRect(5, 5, size-10, size-10, 20, 20);
        g.setColor(Color.BLACK); g.setStroke(new BasicStroke(3)); g.drawRoundRect(5, 5, size-10, size-10, 20, 20);
        g.setColor(Color.BLACK);
        int dotSize = 14; int center = size / 2; int left = size / 4; int right = size * 3 / 4;

        if (number == 0) { 
            Random rand = new Random();
            for (int i = 0; i < 6; i++) {
                int rx = rand.nextInt(size - 30) + 15;
                int ry = rand.nextInt(size - 30) + 15;
                g.fillOval(rx, ry, dotSize, dotSize);
            }
        } else if (number == -1) {
            g.setFont(new Font("SansSerif", Font.BOLD, 50));
            FontMetrics fm = g.getFontMetrics();
            g.drawString("?", (size - fm.stringWidth("?")) / 2, (size + fm.getAscent()) / 2 - 5);
        } else {
            if (number % 2 != 0) g.fillOval(center - dotSize/2, center - dotSize/2, dotSize, dotSize);
            if (number >= 2) { g.fillOval(left - dotSize/2, left - dotSize/2, dotSize, dotSize); g.fillOval(right - dotSize/2, right - dotSize/2, dotSize, dotSize); }
            if (number >= 4) { g.fillOval(right - dotSize/2, left - dotSize/2, dotSize, dotSize); g.fillOval(left - dotSize/2, right - dotSize/2, dotSize, dotSize); }
            if (number == 6) { g.fillOval(left - dotSize/2, center - dotSize/2, dotSize, dotSize); g.fillOval(right - dotSize/2, center - dotSize/2, dotSize, dotSize); }
        }
        g.dispose();
        try { ImageIO.write(image, "png", new File(ROOT_PATH + "ui/" + filename)); } catch (IOException e) {}
    }
}