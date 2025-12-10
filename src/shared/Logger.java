package shared;

/**
 * 간단한 로깅 유틸리티 클래스
 * System.out.println을 대체하여 로그 레벨 관리
 */
public class Logger {
    public enum Level {
        DEBUG, INFO, WARN, ERROR
    }
    
    private static Level currentLevel = Level.INFO;
    private static final boolean ENABLE_EMOJI = true;
    
    /**
     * 로그 레벨 설정
     */
    public static void setLevel(Level level) {
        currentLevel = level;
    }
    
    /**
     * DEBUG 레벨 로그
     */
    public static void debug(String message) {
        if (shouldLog(Level.DEBUG)) {
            System.out.println(formatMessage("🔍", "DEBUG", message));
        }
    }
    
    /**
     * INFO 레벨 로그
     */
    public static void info(String message) {
        if (shouldLog(Level.INFO)) {
            System.out.println(formatMessage("ℹ️", "INFO", message));
        }
    }
    
    /**
     * WARN 레벨 로그
     */
    public static void warn(String message) {
        if (shouldLog(Level.WARN)) {
            System.err.println(formatMessage("⚠️", "WARN", message));
        }
    }
    
    /**
     * ERROR 레벨 로그
     */
    public static void error(String message) {
        if (shouldLog(Level.ERROR)) {
            System.err.println(formatMessage("❌", "ERROR", message));
        }
    }
    
    /**
     * ERROR 레벨 로그 (예외 포함)
     */
    public static void error(String message, Throwable throwable) {
        if (shouldLog(Level.ERROR)) {
            System.err.println(formatMessage("❌", "ERROR", message));
            throwable.printStackTrace();
        }
    }
    
    /**
     * 성공 메시지
     */
    public static void success(String message) {
        if (shouldLog(Level.INFO)) {
            System.out.println(formatMessage("✅", "SUCCESS", message));
        }
    }
    
    private static boolean shouldLog(Level level) {
        return level.ordinal() >= currentLevel.ordinal();
    }
    
    private static String formatMessage(String emoji, String level, String message) {
        if (ENABLE_EMOJI) {
            return emoji + " [" + level + "] " + message;
        } else {
            return "[" + level + "] " + message;
        }
    }
}

