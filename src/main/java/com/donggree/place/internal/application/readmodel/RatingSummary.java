package com.donggree.place.internal.application.readmodel;

/**
 * 장소 별점 요약. 숨기지 않고 지우지 않은 리뷰만 센다.
 *
 * @param averageRating 소수 첫째 자리 평균. 리뷰가 없으면 null
 * @param reviewCount   리뷰 수
 */
public record RatingSummary(Double averageRating, long reviewCount) {

    public static final RatingSummary EMPTY = new RatingSummary(null, 0);

    public static RatingSummary of(Double rawAverage, long reviewCount) {
        if (reviewCount == 0 || rawAverage == null) {
            return EMPTY;
        }
        return new RatingSummary(Math.round(rawAverage * 10) / 10.0, reviewCount);
    }
}
