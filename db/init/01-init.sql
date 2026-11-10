-- 최초 DB 초기화 시 1회 실행 (fresh 볼륨에서만).
-- 쿼리 실행시간 측정용 확장. shared_preload_libraries=pg_stat_statements 와 함께 동작한다.
-- 이미 데이터가 있는 DB 라면 수동으로: docker exec -it donggree-db psql -U root -d donggree -c "CREATE EXTENSION IF NOT EXISTS pg_stat_statements;"
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
