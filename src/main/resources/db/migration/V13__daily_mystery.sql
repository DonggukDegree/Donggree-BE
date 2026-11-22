-- 일일 출제는 날짜 유일, 플레이어 행은 회원별 잠금으로 중복/동시 제출을 직렬화한다.
CREATE TABLE daily_mystery_case (
    game_date date PRIMARY KEY,
    case_data jsonb NOT NULL
);
CREATE TABLE mystery_player (
    member_id bigint PRIMARY KEY,
    game_date date,
    status varchar(20) NOT NULL DEFAULT 'NOT_STARTED',
    session jsonb NOT NULL DEFAULT '{"attempts":[],"departments":[],"buildings":[],"methods":[]}'::jsonb,
    last_played_date date,
    current_streak integer NOT NULL DEFAULT 0,
    completed_games integer NOT NULL DEFAULT 0,
    won_games integer NOT NULL DEFAULT 0,
    total_attempts integer NOT NULL DEFAULT 0,
    CONSTRAINT mystery_player_status_check CHECK (status IN ('NOT_STARTED','IN_PROGRESS','WON','LOST')),
    CONSTRAINT mystery_player_statistics_check CHECK (current_streak >= 0 AND completed_games >= 0 AND won_games BETWEEN 0 AND completed_games AND total_attempts >= 0),
    CONSTRAINT mystery_player_attempts_check CHECK (jsonb_typeof(session->'attempts') = 'array' AND jsonb_array_length(session->'attempts') <= 10)
);
