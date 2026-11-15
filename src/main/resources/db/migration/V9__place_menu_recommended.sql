-- 동글맵 메뉴 추천 표시. 기존 메뉴는 모두 추천 아님으로 둔다.
-- 수정 이력의 before_menus·after_menus JSON은 이전 행에 recommended 키가 없어도 추천 아님으로 읽는다.
ALTER TABLE public.place_menu
    ADD COLUMN recommended boolean NOT NULL DEFAULT false;
