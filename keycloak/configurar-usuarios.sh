#!/usr/bin/env bash
# Define as senhas dos usuarios do realm "seguranca" (admin e manutencao) no Keycloak em execucao.
# As senhas vem do ambiente ou do arquivo .env (nunca de arquivo versionado).
# Uso (na raiz do repositorio, com a extensao OAuth2 no ar):
#   ./keycloak/configurar-usuarios.sh
set -euo pipefail

cd "$(dirname "$0")/.."

if [ -f .env ]; then
    set -a
    # shellcheck disable=SC1091
    . ./.env
    set +a
fi

KC_BOOTSTRAP_ADMIN_USERNAME="${KC_BOOTSTRAP_ADMIN_USERNAME:-admin}"
KC_BOOTSTRAP_ADMIN_PASSWORD="${KC_BOOTSTRAP_ADMIN_PASSWORD:-keycloak-demo}"
OAUTH2_ADMIN_PASSWORD="${OAUTH2_ADMIN_PASSWORD:-admin-oauth2-demo}"
OAUTH2_MANUTENCAO_PASSWORD="${OAUTH2_MANUTENCAO_PASSWORD:-manutencao-oauth2-demo}"

COMPOSE="docker compose -f docker-compose.yml -f docker-compose.oauth2.yml"

# as senhas sao passadas por variavel de ambiente (nao aparecem na linha de comando do kcadm)
$COMPOSE exec -T \
    -e KC_USER="$KC_BOOTSTRAP_ADMIN_USERNAME" -e KC_PASS="$KC_BOOTSTRAP_ADMIN_PASSWORD" \
    -e SENHA_ADMIN="$OAUTH2_ADMIN_PASSWORD" -e SENHA_MANUTENCAO="$OAUTH2_MANUTENCAO_PASSWORD" \
    keycloak bash -c '
        set -e
        KCADM=/opt/keycloak/bin/kcadm.sh
        $KCADM config credentials --server http://localhost:8080 --realm master --user "$KC_USER" --password "$KC_PASS" >/dev/null
        $KCADM set-password -r seguranca --username admin --new-password "$SENHA_ADMIN"
        $KCADM set-password -r seguranca --username manutencao --new-password "$SENHA_MANUTENCAO"
    '

echo "Senhas definidas para os usuarios admin e manutencao do realm seguranca"
