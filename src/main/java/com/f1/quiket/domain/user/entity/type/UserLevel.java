package com.f1.quiket.domain.user.entity.type;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사용자 레벨 명칭
 */
@Getter
@RequiredArgsConstructor
public enum UserLevel {

    LEVEL_1(1, "첫걸음 다람쥐"),
    LEVEL_2(2, "결심한 다람쥐"),
    LEVEL_3(3, "펜굴리는 다람쥐"),
    LEVEL_4(4, "노력형 다람쥐"),
    LEVEL_5(5, "열공 다람쥐"),
    LEVEL_6(6, "똑똑한 다람쥐"),
    LEVEL_7(7, "박식한 다람쥐"),
    LEVEL_8(8, "통달한 다람쥐"),
    LEVEL_9(9, "현자 다람쥐"),
    LEVEL_10(10, "레전드 다람쥐");

    private final int level;
    private final String title;

    /**
     * 레벨 기준 명칭 조회
     */
    public static String titleOf(Integer level) {
        return Arrays.stream(values())
                .filter(userLevel -> userLevel.level == normalize(level))
                .findFirst()
                .orElse(LEVEL_1)
                .getTitle();
    }

    /**
     * 레벨 범위 보정
     */
    private static int normalize(Integer level) {
        if (level == null || level < LEVEL_1.level) {
            return LEVEL_1.level;
        }
        if (level > LEVEL_10.level) {
            return LEVEL_10.level;
        }
        return level;
    }
}
