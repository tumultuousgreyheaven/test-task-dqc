#!/usr/bin/env bash
# Проверка условия приёмки: накат → откат → повторный накат без ошибок.
#
# Требуется Docker с compose v2. Скрипт сам поднимает только PostgreSQL;
# приложение на время проверки останавливается, потому что откат удаляет таблицы.
# После скрипта БД остаётся накатанной (приложение: docker compose up -d app).
set -euo pipefail

cd "$(dirname "$0")/.."

# значения из .env, если он есть (иначе действуют умолчания из docker-compose.yml)
if [ -f .env ]; then
  set -a
  # shellcheck disable=SC1091
  . ./.env
  set +a
fi
PG_USER="${POSTGRES_USER:-dqc}"
PG_DB="${POSTGRES_DB:-dqc}"

# число changeset'ов во всех файлах миграций = на сколько шагов откатываемся
CHANGESETS=$(cat db/changelog/changes/*.sql | grep -c '^--changeset')

step() { printf '\n=== %s ===\n' "$*"; }

# число таблиц и последовательностей схемы public без служебных таблиц Liquibase
count_objects() {
  docker compose exec -T db psql -U "$PG_USER" -d "$PG_DB" -tA -c \
    "SELECT count(*) FROM pg_class
      WHERE relnamespace = 'public'::regnamespace
        AND relkind IN ('r', 'S')
        AND relname NOT LIKE 'databasechangelog%'"
}

migrate() { docker compose run --rm -T migrate "$@"; }

docker compose stop app >/dev/null 2>&1 || true
docker compose up -d --wait db

step "1/3 накат (update)"
migrate update
AFTER_FIRST=$(count_objects)
echo "объектов в схеме: $AFTER_FIRST"
[ "$AFTER_FIRST" -gt 0 ] || { echo "ОШИБКА: после наката схема пуста" >&2; exit 1; }

step "2/3 откат ($CHANGESETS changeset'ов)"
migrate rollback-count --count="$CHANGESETS"
AFTER_ROLLBACK=$(count_objects)
echo "объектов в схеме: $AFTER_ROLLBACK"
[ "$AFTER_ROLLBACK" -eq 0 ] || { echo "ОШИБКА: после отката остались объекты" >&2; exit 1; }

step "3/3 повторный накат (update)"
migrate update
AFTER_SECOND=$(count_objects)
echo "объектов в схеме: $AFTER_SECOND"
[ "$AFTER_SECOND" -eq "$AFTER_FIRST" ] || { echo "ОШИБКА: схема после повторного наката отличается от первой" >&2; exit 1; }

printf '\nOK: накат → откат → повторный накат прошли без ошибок.\n'
