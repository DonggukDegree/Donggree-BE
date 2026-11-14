-- 동글맵 카테고리 개편(2026-11-12)과 숨은 맛집 태그.
-- 없앤 상세카테고리(포차·카공 카페)는 같은 카테고리의 기타로 옮긴다. 운영은 동글맵 배포 전이라 대상이 없다.
UPDATE public.place SET sub_category = 'BAR_ETC' WHERE sub_category = 'POCHA';
UPDATE public.place SET sub_category = 'STUDY_ETC' WHERE sub_category = 'STUDY_FRIENDLY_CAFE';

-- 숨은 맛집 태그. 메인 카테고리와 별개인 추가 범주로 장소마다 0개 이상.
CREATE TABLE public.place_tag (
    place_id bigint NOT NULL,
    tag character varying(20) NOT NULL,
    CONSTRAINT place_tag_pkey PRIMARY KEY (place_id, tag),
    CONSTRAINT place_tag_place_id_fkey FOREIGN KEY (place_id) REFERENCES public.place (id) ON DELETE CASCADE,
    CONSTRAINT place_tag_tag_check CHECK (
        tag IN ('SHORTCUT', 'STUDY', 'GATHERING', 'MEAL_DATE', 'MOOD', 'SOLO_DINING', 'COFFEE', 'GENUINE'))
);

-- 수정 이력에 태그 전후 값. 기존 이력은 태그가 없던 시절이라 빈 배열.
ALTER TABLE public.place_edit_history
    ADD COLUMN before_tags jsonb NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN after_tags jsonb NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE public.place_edit_history
    ADD CONSTRAINT place_edit_history_tags_check CHECK (
        jsonb_typeof(before_tags) = 'array' AND jsonb_typeof(after_tags) = 'array');
