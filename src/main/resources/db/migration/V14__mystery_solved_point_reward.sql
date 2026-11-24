-- 기존 회원·사유·대상 유일 인덱스를 재사용한다. MYSTERY_SOLVED의 대상은 서버 문제 날짜(yyyyMMdd)다.
ALTER TABLE public.point_history DROP CONSTRAINT point_history_reason_check;
ALTER TABLE public.point_history ADD CONSTRAINT point_history_reason_check CHECK (reason IN (
    'GRADUATION_REPORT_FIRST_VIEW', 'PLACE_REGISTER', 'PLACE_FIRST_REVIEW', 'PLACE_REVIEW',
    'PLACE_INFO_EDIT', 'MYSTERY_SOLVED'));
