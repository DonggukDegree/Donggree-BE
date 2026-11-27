-- 동아리 요청 승인 적립 사유. 대상은 최초 등록이면 동아리 ID, 모집·기수 갱신이면 동아리 ID × 10000 + 기수다.
-- 셋 다 대상별 1건이라, 같은 동아리(·기수)는 회원과 관계없이 한 번만 적립한다.
ALTER TABLE public.point_history DROP CONSTRAINT point_history_reason_check;
ALTER TABLE public.point_history ADD CONSTRAINT point_history_reason_check CHECK (reason IN (
    'GRADUATION_REPORT_FIRST_VIEW', 'PLACE_REGISTER', 'PLACE_FIRST_REVIEW', 'PLACE_REVIEW',
    'PLACE_INFO_EDIT', 'MYSTERY_SOLVED', 'CLUB_FIRST_REGISTRATION', 'CLUB_RECRUITMENT',
    'CLUB_GENERATION_UPDATE'));

CREATE UNIQUE INDEX uk_point_history_club_first_registration ON public.point_history USING btree (target_id)
    WHERE reason = 'CLUB_FIRST_REGISTRATION';
CREATE UNIQUE INDEX uk_point_history_club_recruitment ON public.point_history USING btree (target_id)
    WHERE reason = 'CLUB_RECRUITMENT';
CREATE UNIQUE INDEX uk_point_history_club_generation_update ON public.point_history USING btree (target_id)
    WHERE reason = 'CLUB_GENERATION_UPDATE';
