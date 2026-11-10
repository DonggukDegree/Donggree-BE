package com.donggree.graduation.internal.application.evaluation;

import com.donggree.graduation.internal.domain.rule.CreditAdjustment;
import com.donggree.graduation.internal.domain.rule.RuleConfiguration;
import java.util.List;

/** 저장된 규칙 설정을 판정 입력으로 읽는 출력 포트. */
public interface RuleConfigurationReader {
    RuleConfiguration read(String typeName, String settings);

    List<String> readMajorRoles(String settings);

    List<CreditAdjustment> readCreditAdjustments(String settings);
}
