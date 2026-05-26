package com.f1.quiket.domain.user.entity.type;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * 명세 GAMIF-002 — 레벨별 다람쥐 명칭 매핑이 정책과 일치하는지 단언한다.
 */
class UserLevelTest {

    @Test
    void titleOf_matches_specification_for_each_level() {
        assertThat(UserLevel.titleOf(1)).isEqualTo("첫걸음 다람쥐");
        assertThat(UserLevel.titleOf(2)).isEqualTo("결심한 다람쥐");
        assertThat(UserLevel.titleOf(3)).isEqualTo("펜굴리는 다람쥐");
        assertThat(UserLevel.titleOf(4)).isEqualTo("노력형 다람쥐");
        assertThat(UserLevel.titleOf(5)).isEqualTo("열공 다람쥐");
        assertThat(UserLevel.titleOf(6)).isEqualTo("똑똑한 다람쥐");
        assertThat(UserLevel.titleOf(7)).isEqualTo("박식한 다람쥐");
        assertThat(UserLevel.titleOf(8)).isEqualTo("통달한 다람쥐");
        assertThat(UserLevel.titleOf(9)).isEqualTo("현자 다람쥐");
        assertThat(UserLevel.titleOf(10)).isEqualTo("레전드 다람쥐");
    }

    @Test
    void titleOf_clamps_out_of_range_levels_to_band_edges() {
        assertThat(UserLevel.titleOf(null)).isEqualTo("첫걸음 다람쥐");
        assertThat(UserLevel.titleOf(0)).isEqualTo("첫걸음 다람쥐");
        assertThat(UserLevel.titleOf(-3)).isEqualTo("첫걸음 다람쥐");
        assertThat(UserLevel.titleOf(11)).isEqualTo("레전드 다람쥐");
        assertThat(UserLevel.titleOf(999)).isEqualTo("레전드 다람쥐");
    }
}
