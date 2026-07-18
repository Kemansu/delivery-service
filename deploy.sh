#!/usr/bin/env bash
# Деплой бэкенда don-polesie.ru из git-исходников.
# Использование: /root/delivery-service/deploy.sh
# Шаги: git pull → сборка jar в контейнере Maven (JDK 21) → бэкап старого jar →
#       копирование нового в docker-starter → пересборка образа → рестарт.
set -euo pipefail

REPO=/root/delivery-service
STARTER=/var/www/don_polesie/docker-starter
JAR=back-end-0.0.1-SNAPSHOT.jar
# origin — наш форк с прод-правками; upstream — репозиторий разработчика бэка.
# Обновления разработчика подтягиваются вручную:
#   git fetch upstream && git merge upstream/main   (потом ./deploy.sh)
FORK=https://github.com/Kemansu/delivery-service.git
UPSTREAM=https://github.com/vnikolaenko-dev/delivery-service.git

cd "$REPO"
git remote set-url origin "$FORK" 2>/dev/null || git remote add origin "$FORK"
git remote get-url upstream >/dev/null 2>&1 || git remote add upstream "$UPSTREAM"

echo "== git pull (наш форк Kemansu) =="
git pull --ff-only origin main

echo "== сборка (maven в docker, JDK 21) =="
docker run --rm -v "$REPO":/build -v /root/.m2:/root/.m2 -w /build \
  maven:3.9-eclipse-temurin-21 mvn -q -DskipTests package

test -f "$REPO/target/$JAR"

echo "== бэкап и замена jar =="
cp -a "$STARTER/$JAR" "$STARTER/$JAR.bak.$(date +%Y%m%d%H%M%S)"
cp "$REPO/target/$JAR" "$STARTER/$JAR"

echo "== пересборка образа и рестарт =="
cd "$STARTER"
docker compose build app
docker compose up -d app

echo "== ожидание старта Spring =="
ok=""
for i in $(seq 1 36); do
  sleep 5
  if curl -sf 'http://127.0.0.1:8080/api/find/product/?pageNumber=0' >/dev/null; then
    ok=1; break
  fi
  echo "  ждём... ($((i*5))с)"
done
[ -n "$ok" ] && echo "OK: каталог отвечает" || { echo "FAIL: каталог не отвечает за 3 мин"; docker logs --tail 50 delivery-app; exit 1; }

# держим не больше трёх последних бэкапов jar
ls -t "$STARTER/$JAR".bak.* 2>/dev/null | tail -n +4 | xargs -r rm -f
echo "Готово: $(cd "$REPO" && git log --oneline -1)"
