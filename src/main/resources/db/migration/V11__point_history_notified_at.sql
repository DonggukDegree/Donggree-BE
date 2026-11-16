-- 포인트 적립 안내 시각. 화면이 "적립됐어요" 안내를 띄운 시각으로, 같은 적립을 두 번 안내하지 않게 한다.
-- V10을 이미 적용한 DB가 있어 V10을 고치지 않고 칼럼을 따로 추가한다. 기존 행은 비어 있어 아직 안내하지 않은 상태다.
ALTER TABLE public.point_history ADD COLUMN notified_at timestamp(6) without time zone;
