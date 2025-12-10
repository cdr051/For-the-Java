package client;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

public class ResourceManager {
    // 메모리 캐시 (한번 로드한 이미지는 재사용)
    private static final Map<String, Image> imageCache = new HashMap<>();
    
    // 프로젝트 루트 기준 images 폴더 경로 (실행 환경에 따라 조정 필요할 수 있음)
    // 이클립스/인텔리제이 프로젝트 최상단에 "images" 폴더를 만들어야 합니다.
    private static final String ROOT_PATH = "images/";

    // 이미지 불러오기 (없으면 null 반환)
    public static Image getImage(String category, String filename) {
        String key = category + "/" + filename;
        
        if (imageCache.containsKey(key)) {
            return imageCache.get(key);
        }

        try {
            // 경로: images/map/grass.png, images/ui/panel_bg.png 등
            String fullPath = ROOT_PATH + key;
            File f = new File(fullPath);
            
            if (f.exists()) {
                Image img = ImageIO.read(f);
                imageCache.put(key, img);
                return img;
            } else {
                // System.out.println("이미지 없음: " + fullPath); // 디버깅용
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null; // 이미지가 없으면 null -> 그릴 때 색깔로 대체하면 됨
    }
}