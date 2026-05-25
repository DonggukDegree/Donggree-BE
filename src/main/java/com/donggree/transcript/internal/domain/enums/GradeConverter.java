package com.donggree.transcript.internal.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Grade enum과 DB 문자열 값 간의 변환을 담당하는 JPA 컨버터.
 * Grade enum 이름(A_PLUS)이 아닌 실제 성적 표기(A+)로 DB에 저장한다.
 */
@Converter(autoApply = false)
public class GradeConverter implements AttributeConverter<Grade, String> {

    @Override
    public String convertToDatabaseColumn(Grade grade) {
        if (grade == null) {
            return null;
        }
        return grade.getValue();
    }

    @Override
    public Grade convertToEntityAttribute(String dbValue) {
        if (dbValue == null) {
            return null;
        }
        return Grade.fromValue(dbValue);
    }
}
