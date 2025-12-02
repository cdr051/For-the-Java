package client;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

public class ResourceManager {
    // 메모리 캐시
    private static final Map<String, Image> imageCache = new HashMap<>();
    
    // 프로젝트 폴더 바로 아래 images 폴더
    private static final String ROOT_PATH = "images/";

    public static Image getImage(String category, String filename) {
        // 추후 "map/tile_grass.png" 형태로 저장
        String fullPath = ROOT_PATH + category + "/" + filename;
        
        if (imageCache.containsKey(fullPath)) {
            return imageCache.get(fullPath);
        }

        try {
            File f = new File(fullPath);
            if (f.exists()) {
                Image img = ImageIO.read(f);
                imageCache.put(fullPath, img); // 메모리에 저장
                // 디버깅용
                return img;
            }
        } catch (Exception e) {
            System.out.println("이미지 로드 실패: " + fullPath);
        }
        
        // 파일이 없으면 null 반환
        return null;
    }
}