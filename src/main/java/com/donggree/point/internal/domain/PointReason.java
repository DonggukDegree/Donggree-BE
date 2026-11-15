package com.donggree.point.internal.domain;

/**
 * 포인트 적립 사유와 지급액. 화면 문구는 FE가 코드로 매핑한다.
 *
 * <p>대상 ID(target_id)는 장소 활동이면 장소 ID, 졸업 리포트 최초 확인이면 null이다.
 * 같은 회원·사유·대상은 한 번만 적립하며, 리뷰 두 사유는 합쳐 회원당 장소별 1회다.
 */
public enum PointReason {
    GRADUATION_REPORT_FIRST_VIEW(500),
    PLACE_REGISTER(500),
    PLACE_FIRST_REVIEW(500),
    PLACE_REVIEW(100),
    PLACE_INFO_EDIT(100);

    private final int amount;

    PointReason(int amount) {
        this.amount = amount;
    }

    public int amount() {
        return amount;
    }
}
