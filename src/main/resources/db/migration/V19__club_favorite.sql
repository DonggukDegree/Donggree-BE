-- 동아리 찜. 회원은 member FK 없이 ID로만 참조하며 탈퇴 시 그 회원의 행을 지운다.
CREATE TABLE public.club_favorite (
    member_id bigint NOT NULL,
    club_id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    CONSTRAINT club_favorite_pkey PRIMARY KEY (member_id, club_id),
    CONSTRAINT club_favorite_club_id_fkey FOREIGN KEY (club_id) REFERENCES public.club (id) ON DELETE CASCADE
);

CREATE INDEX idx_club_favorite_club_id ON public.club_favorite USING btree (club_id);
