-- 장소 설명 최대 길이를 300자에서 500자로 늘린다. 수정 이력의 전후 설명도 같은 길이를 담는다.
-- varchar 길이를 늘리는 변경이라 기존 데이터는 그대로 남는다.
ALTER TABLE public.place ALTER COLUMN description TYPE character varying(500);
ALTER TABLE public.place_edit_history
    ALTER COLUMN before_description TYPE character varying(500),
    ALTER COLUMN after_description TYPE character varying(500);
