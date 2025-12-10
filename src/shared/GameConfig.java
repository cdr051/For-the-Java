package shared;

/**
 * 게임 설정 상수 클래스
 * 모든 하드코딩된 값들을 중앙에서 관리
 */
public class GameConfig {
    // 서버 설정
    public static final int SERVER_PORT = 9999;
    public static final String DEFAULT_SERVER_IP = "127.0.0.1";
    
    // 맵 설정
    public static final int MAP_COLS = 12;
    public static final int MAP_ROWS = 8;
    
    // 맵 타일 타입
    public static final int TILE_GRASS = 0;
    public static final int TILE_WATER = 1;
    public static final int TILE_MONSTER = 2;
    public static final int TILE_SHOP = 3;
    
    // 게임 초기값
    public static final int INITIAL_TEAM_GOLD = 100;
    public static final int INITIAL_TEAM_LIVES = 3;
    public static final int INITIAL_PLAYER_GOLD = 100;
    
    // 상점 아이템 가격
    public static final int SHOP_ATK_COST = 50;
    public static final int SHOP_HP_COST = 50;
    public static final int SHOP_HEAL_COST = 30;
    
    // 상점 아이템 효과
    public static final int SHOP_ATK_BONUS = 5;
    public static final int SHOP_HP_BONUS = 20;
    public static final int SHOP_HEAL_AMOUNT = 30;
    
    // 전투 설정
    public static final int BATTLE_PARTICIPATION_RANGE = 2; // 거리 2 이내 플레이어 참전
    public static final int BATTLE_MONSTER_COUNT = 2; // 전투당 몬스터 수
    public static final int BATTLE_REWARD_PER_PLAYER = 30; // 전투 승리 시 플레이어당 골드
    public static final double FLEE_SUCCESS_RATE = 0.5; // 도망 성공 확률
    
    // 주사위 설정
    public static final int DICE_MIN = 1;
    public static final int DICE_MAX = 6;
    
    // 이동 검증
    public static final int MAX_MOVE_DELTA = 1; // 한 번에 이동할 수 있는 최대 거리
    
    // 몬스터 타입
    public static final int MONSTER_TYPE_SLIME = 0;
    public static final int MONSTER_TYPE_SKELETON = 1;
    public static final int MONSTER_TYPE_ORC = 2;
    public static final int MONSTER_TYPE_WEREWOLF = 3;
    
    // 몬스터 스탯 (기본값 + 라운드당 증가)
    public static final int SLIME_BASE_HP = 20;
    public static final int SLIME_HP_PER_ROUND = 8;
    public static final int SLIME_BASE_ATK = 5;
    public static final int SLIME_ATK_PER_ROUND = 2;
    public static final int SLIME_SPEED = 8;
    
    public static final int SKELETON_BASE_HP = 35;
    public static final int SKELETON_HP_PER_ROUND = 10;
    public static final int SKELETON_BASE_ATK = 12;
    public static final int SKELETON_ATK_PER_ROUND = 3;
    public static final int SKELETON_SPEED = 10;
    
    public static final int ORC_BASE_HP = 60;
    public static final int ORC_HP_PER_ROUND = 15;
    public static final int ORC_BASE_ATK = 10;
    public static final int ORC_ATK_PER_ROUND = 3;
    public static final int ORC_SPEED = 4;
    
    public static final int WEREWOLF_BASE_HP = 50;
    public static final int WEREWOLF_HP_PER_ROUND = 12;
    public static final int WEREWOLF_BASE_ATK = 15;
    public static final int WEREWOLF_ATK_PER_ROUND = 4;
    public static final int WEREWOLF_SPEED = 16;
    
    // 스킬 배율
    public static final double KNIGHT_SKILL2_MULTIPLIER = 1.5; // 강타
    public static final double MAGE_SKILL1_MULTIPLIER = 0.8; // 라이트닝
    public static final double MAGE_SKILL2_MULTIPLIER = 2.0; // 파이어볼
    public static final double ARCHER_SKILL1_MULTIPLIER = 0.5; // 속사
    public static final int ARCHER_SKILL1_HITS = 3; // 속사 발동 횟수
    public static final int ARCHER_SKILL2_BUFF_TURNS = 2; // 매의 눈 지속 턴
    public static final double ROGUE_SKILL1_MIN_MULTIPLIER = 1.0; // 급소 찌르기 최소
    public static final double ROGUE_SKILL1_MAX_MULTIPLIER = 3.0; // 급소 찌르기 최대
    public static final int ROGUE_SKILL2_DEBUFF_TURNS = 2; // 연막탄 디버프 지속 턴
    
    // 버프/디버프
    public static final int MONSTER_ATK_DEBUFF_DIVISOR = 2; // 공격력 감소 배율
    public static final int PLAYER_ATK_BUFF_MULTIPLIER = 2; // 공격력 증가 배율
    
    // 직업 이름 상수
    public static final String JOB_KNIGHT = "기사";
    public static final String JOB_MAGE = "마법사";
    public static final String JOB_ARCHER = "궁수";
    public static final String JOB_ROGUE = "도적";
    
    // 직업별 기본 스탯
    public static final int KNIGHT_BASE_HP = 100;
    public static final int KNIGHT_BASE_ATK = 10;
    public static final int KNIGHT_BASE_SPD = 5;
    
    public static final int MAGE_BASE_HP = 60;
    public static final int MAGE_BASE_ATK = 40;
    public static final int MAGE_BASE_SPD = 10;
    
    public static final int ARCHER_BASE_HP = 80;
    public static final int ARCHER_BASE_ATK = 20;
    public static final int ARCHER_BASE_SPD = 15;
    
    public static final int ROGUE_BASE_HP = 70;
    public static final int ROGUE_BASE_ATK = 30;
    public static final int ROGUE_BASE_SPD = 20;
    
    // 맵 생성 확률
    public static final double MAP_WATER_PROBABILITY = 0.25;
    public static final double MAP_MONSTER_PROBABILITY = 0.35;
    public static final double MAP_SHOP_PROBABILITY = 0.40;
    public static final int MAP_MIN_ACCESSIBLE_TILES = 30;
    
    private GameConfig() {
        // 인스턴스화 방지
    }
}

