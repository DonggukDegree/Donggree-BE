package com.donggree;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

public class ModularityTests {
    @Test
    void verifyModularStructure() {
        // 모듈 간 의존성이 규칙에 맞는지 자동 검증
        // 예: transcript 모듈이 graduation 모듈의 internal 패키지에 접근하면 실패
        ApplicationModules.of(DonggreeApplication.class).verify();
    }
}
