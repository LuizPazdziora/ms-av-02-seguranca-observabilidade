#!/usr/bin/env bash
# Teste de aceitacao do MS-AV-02 em Docker.
# Usa somente docker compose e curl contra as portas publicadas em 127.0.0.1
# (todos os servicos testados rodam em conteineres).
#
# Uso (na raiz do repositorio):
#   ./scripts/aceitacao.sh            # passos obrigatorios (1 a 16)
#   ./scripts/aceitacao.sh --oauth2   # passos obrigatorios + bloco da extensao OAuth2 (opcional)
#
# As senhas vem do .env ou dos padroes de demonstracao do docker-compose.yml.
set -euo pipefail

cd "$(dirname "$0")/.."

OAUTH2=false
[ "${1:-}" = "--oauth2" ] && OAUTH2=true

if [ -f .env ]; then
    set -a
    # shellcheck disable=SC1091
    . ./.env
    set +a
fi

SENHA_ADMIN="${DEMO_ADMIN_PASSWORD:-admin-demo}"
SENHA_MANUTENCAO="${DEMO_MANUTENCAO_PASSWORD:-manutencao-demo}"
GRAFANA_USER="${GRAFANA_ADMIN_USER:-admin}"
GRAFANA_PASS="${GRAFANA_ADMIN_PASSWORD:-grafana-demo}"

API=http://127.0.0.1:8080
ACT=http://127.0.0.1:8081
PROM=http://127.0.0.1:9095
GRAF=http://127.0.0.1:3000
JSON="Content-Type: application/json"

# ---------------------------------------------------------------- utilitarios
passo() { printf '\n==> Passo %s - %s\n' "$1" "$2"; }
ok() { printf '    [OK] %s\n' "$1"; }
falha() { printf '    [FALHA] %s\n' "$1" >&2; exit 1; }

# status HTTP de uma chamada: status <curl args...>
status() { curl -s -o /dev/null -w '%{http_code}' "$@"; }

# confere o status HTTP: espera <codigo> <descricao> <curl args...>
espera() {
    local esperado="$1" descricao="$2"; shift 2
    local obtido
    obtido=$(status "$@")
    [ "$obtido" = "$esperado" ] || falha "$descricao: esperado $esperado, obtido $obtido"
    ok "$descricao -> $obtido"
}

login() { status -X POST -H "$JSON" -d "{\"username\":\"$1\",\"senha\":\"$2\"}" "$API/login"; }

# valor de uma metrica no /actuator/prometheus (inteiro)
metrica_actuator() {
    curl -s "$ACT/actuator/prometheus" | grep "^$1{" | grep 'application="login"' | awk '{printf "%d", $2}'
}

# valor de uma consulta instantanea no Prometheus (inteiro)
consulta_prometheus() {
    curl -s -G "$PROM/api/v1/query" --data-urlencode "query=$1" \
        | sed -nE 's/.*"value":\[[0-9.e+]+,"([0-9.e+-]+)"\].*/\1/p' | awk '{printf "%d", $1}'
}

# valor de uma consulta pelo datasource provisionado do Grafana (inteiro)
consulta_grafana() {
    local corpo
    corpo="{\"from\":\"now-5m\",\"to\":\"now\",\"queries\":[{\"refId\":\"A\",\"datasource\":{\"type\":\"prometheus\",\"uid\":\"prometheus\"},\"expr\":\"$1\",\"instant\":true,\"range\":false}]}"
    curl -s -u "$GRAFANA_USER:$GRAFANA_PASS" -H "$JSON" -X POST -d "$corpo" "$GRAF/api/ds/query" \
        | sed -nE 's/.*"values":\[\[[^]]*\],\[([0-9.e+-]+)\]\].*/\1/p' | awk '{printf "%d", $1}'
}

confere() {
    local descricao="$1" esperado="$2" obtido="$3"
    [ "$obtido" = "$esperado" ] || falha "$descricao: esperado $esperado, obtido '$obtido'"
    ok "$descricao = $obtido"
}

json_escape() { printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g'; }

# ---------------------------------------------------------------- passos obrigatorios
passo 1 "docker compose build (inclui os testes automatizados)"
docker compose build
ok "imagem construida e testes automatizados aprovados no build"

passo 2 "docker compose up -d e espera todos os servicos ficarem healthy"
docker compose up -d --wait --wait-timeout 300
docker compose ps
ok "servicos healthy"

passo 3 "API disponivel"
espera 401 "POST /login com corpo vazio" -X POST -H "$JSON" -d '{}' "$API/login"

passo 4 "Swagger"
espera 200 "GET /v3/api-docs" "$API/v3/api-docs"
DOCS=$(curl -s "$API/v3/api-docs")
for caminho in '"/login"' '"/trocarsenha/{userid}"' '"/unlock/{userid}"' '"/":'; do
    printf '%s' "$DOCS" | grep -q "$caminho" || falha "caminho $caminho ausente no OpenAPI"
done
ok "os cinco endpoints oficiais estao documentados (POST /login, PUT /trocarsenha, POST /, PUT /unlock, GET /)"
espera 200 "GET /swagger-ui/index.html" "$API/swagger-ui/index.html"

# contadores e gauge antes das operacoes do teste
VALIDOS=$(metrica_actuator login_validos_total)
INVALIDOS=$(metrica_actuator login_invalidos_total)
BLOQUEADOS_INICIAL=$(metrica_actuator usuarios_bloqueados)
echo "    valores iniciais: validos=$VALIDOS invalidos=$INVALIDOS bloqueados=$BLOQUEADOS_INICIAL"

SUFIXO="$(date +%s)$RANDOM"
USUARIO="aceite$SUFIXO"
BLOQUEAR="bloqueio$SUFIXO"
SENHA1="senha1-$SUFIXO"
SENHA2="senha2-$SUFIXO"

passo 5 "Cinco endpoints oficiais com os codigos esperados"
espera 201 "POST / (manutencao) cadastra $USUARIO" -X POST -u "manutencao:$SENHA_MANUTENCAO" -H "$JSON" \
    -d "{\"username\":\"$USUARIO\",\"senha\":\"$SENHA1\"}" "$API/"
espera 409 "POST / (manutencao) repetido" -X POST -u "manutencao:$SENHA_MANUTENCAO" -H "$JSON" \
    -d "{\"username\":\"$USUARIO\",\"senha\":\"$SENHA1\"}" "$API/"
espera 400 "POST / (manutencao) com username vazio" -X POST -u "manutencao:$SENHA_MANUTENCAO" -H "$JSON" \
    -d '{"username":"","senha":"x"}' "$API/"
espera 200 "GET / (manutencao)" -u "manutencao:$SENHA_MANUTENCAO" "$API/"
LISTA=$(curl -s -u "manutencao:$SENHA_MANUTENCAO" "$API/")
printf '%s' "$LISTA" | grep -q "\"$USUARIO\"" || falha "usuario cadastrado ausente no GET /"
if printf '%s' "$LISTA" | grep -qE '"senha"|"password"|\$2a\$'; then falha "GET / expoe senha ou hash"; fi
ok "GET / lista o usuario sem senha nem hash"
[ "$(login "$USUARIO" "$SENHA1")" = 200 ] || falha "login valido de $USUARIO"
VALIDOS=$((VALIDOS + 1)); ok "POST /login valido -> 200"
espera 401 "PUT /trocarsenha com senha atual errada" -X PUT -H "$JSON" -d "{\"atual\":\"errada\",\"nova\":\"$SENHA2\"}" "$API/trocarsenha/$USUARIO"
espera 400 "PUT /trocarsenha com nova senha vazia" -X PUT -H "$JSON" -d "{\"atual\":\"$SENHA1\",\"nova\":\"\"}" "$API/trocarsenha/$USUARIO"
espera 404 "PUT /trocarsenha de usuario inexistente" -X PUT -H "$JSON" -d '{"atual":"a","nova":"b"}' "$API/trocarsenha/inexistente$SUFIXO"
espera 200 "PUT /trocarsenha" -X PUT -H "$JSON" -d "{\"atual\":\"$SENHA1\",\"nova\":\"$SENHA2\"}" "$API/trocarsenha/$USUARIO"
[ "$(login "$USUARIO" "$SENHA2")" = 200 ] || falha "login com a senha nova"
VALIDOS=$((VALIDOS + 1)); ok "POST /login com a senha nova -> 200"
[ "$(login "inexistente$SUFIXO" "x")" = 401 ] || falha "login de usuario inexistente"
INVALIDOS=$((INVALIDOS + 1)); ok "POST /login invalido -> 401"
espera 200 "PUT /unlock (admin)" -X PUT -u "admin:$SENHA_ADMIN" "$API/unlock/$USUARIO"
espera 404 "PUT /unlock (admin) de usuario inexistente" -X PUT -u "admin:$SENHA_ADMIN" "$API/unlock/inexistente$SUFIXO"

passo 6 "Permissoes: 401 sem credenciais e 403 com a role errada"
espera 401 "POST / sem credenciais" -X POST -H "$JSON" -d '{"username":"x","senha":"y"}' "$API/"
espera 403 "POST / com admin (sem ROLE_MANUTENCAO)" -X POST -u "admin:$SENHA_ADMIN" -H "$JSON" -d '{"username":"x","senha":"y"}' "$API/"
espera 401 "GET / sem credenciais" "$API/"
espera 403 "GET / com admin (sem ROLE_MANUTENCAO)" -u "admin:$SENHA_ADMIN" "$API/"
espera 401 "PUT /unlock sem credenciais" -X PUT "$API/unlock/$USUARIO"
espera 403 "PUT /unlock com manutencao (sem ROLE_ADMIN)" -X PUT -u "manutencao:$SENHA_MANUTENCAO" "$API/unlock/$USUARIO"
espera 401 "GET / com senha errada" -u "manutencao:errada" "$API/"
espera 403 "GET / com usuario do sistema (ROLE_USER)" -u "$USUARIO:$SENHA2" "$API/"

passo 7 "Gera logins validos e invalidos em quantidades conhecidas"
for _ in 1 2 3; do [ "$(login "$USUARIO" "$SENHA2")" = 200 ] || falha "login valido"; done
VALIDOS=$((VALIDOS + 3))
for _ in 1 2; do [ "$(login "inexistente$SUFIXO" "x")" = 401 ] || falha "login invalido"; done
INVALIDOS=$((INVALIDOS + 2))
ok "3 logins validos e 2 invalidos gerados"

passo 8 "Bloqueia um usuario com duas falhas seguidas"
espera 201 "POST / (manutencao) cadastra $BLOQUEAR" -X POST -u "manutencao:$SENHA_MANUTENCAO" -H "$JSON" \
    -d "{\"username\":\"$BLOQUEAR\",\"senha\":\"$SENHA1\"}" "$API/"
[ "$(login "$BLOQUEAR" "errada-1")" = 401 ] || falha "1a falha"
[ "$(login "$BLOQUEAR" "errada-2")" = 401 ] || falha "2a falha"
[ "$(login "$BLOQUEAR" "$SENHA1")" = 401 ] || falha "login com a senha correta deveria ser negado (bloqueado)"
INVALIDOS=$((INVALIDOS + 3))
BLOQUEADOS=$((BLOQUEADOS_INICIAL + 1))
ok "usuario bloqueado na 2a falha; senha correta negada (401)"

passo 9 "Valores no /actuator/prometheus (porta 8081)"
confere "login_validos_total" "$VALIDOS" "$(metrica_actuator login_validos_total)"
confere "login_invalidos_total" "$INVALIDOS" "$(metrica_actuator login_invalidos_total)"
confere "usuarios_bloqueados" "$BLOQUEADOS" "$(metrica_actuator usuarios_bloqueados)"

passo 10 "Prometheus coleta as metricas (espera dois ciclos de coleta)"
sleep 12
curl -s "$PROM/api/v1/targets" | grep -q '"job":"login"' || falha "alvo login ausente"
curl -s "$PROM/api/v1/targets" | grep -q '"health":"up"' || falha "alvo login nao esta up"
ok "alvo login -> up"
confere "Prometheus login_validos_total" "$VALIDOS" "$(consulta_prometheus 'sum(login_validos_total{application="login"})')"
confere "Prometheus login_invalidos_total" "$INVALIDOS" "$(consulta_prometheus 'sum(login_invalidos_total{application="login"})')"
confere "Prometheus usuarios_bloqueados" "$BLOQUEADOS" "$(consulta_prometheus 'max(usuarios_bloqueados{application="login"})')"

passo 11 "Grafana: saude, dashboard provisionado e consulta pelo datasource"
espera 200 "GET /api/health" "$GRAF/api/health"
espera 200 "GET /api/dashboards/uid/ms-av-02-login" -u "$GRAFANA_USER:$GRAFANA_PASS" "$GRAF/api/dashboards/uid/ms-av-02-login"
confere "Grafana login_validos_total" "$VALIDOS" "$(consulta_grafana 'sum(login_validos_total{application=\"login\"})')"
confere "Grafana login_invalidos_total" "$INVALIDOS" "$(consulta_grafana 'sum(login_invalidos_total{application=\"login\"})')"
confere "Grafana usuarios_bloqueados" "$BLOQUEADOS" "$(consulta_grafana 'max(usuarios_bloqueados{application=\"login\"})')"

passo 12 "Desbloqueia o usuario com o admin"
espera 200 "PUT /unlock/$BLOQUEAR (admin)" -X PUT -u "admin:$SENHA_ADMIN" "$API/unlock/$BLOQUEAR"
[ "$(login "$BLOQUEAR" "$SENHA1")" = 200 ] || falha "login apos desbloqueio"
VALIDOS=$((VALIDOS + 1)); ok "login apos desbloqueio -> 200"

passo 13 "Gauge volta ao valor anterior ao bloqueio"
confere "Actuator usuarios_bloqueados" "$BLOQUEADOS_INICIAL" "$(metrica_actuator usuarios_bloqueados)"
sleep 12
confere "Prometheus usuarios_bloqueados" "$BLOQUEADOS_INICIAL" "$(consulta_prometheus 'max(usuarios_bloqueados{application="login"})')"

passo 14 "Persistencia apos restart e apos down/up (sem -v)"
# deixa um usuario bloqueado para conferir que o bloqueio persiste
[ "$(login "$BLOQUEAR" "errada-1")" = 401 ] && [ "$(login "$BLOQUEAR" "errada-2")" = 401 ] || falha "novo bloqueio"
confere "usuarios_bloqueados antes do restart" "$BLOQUEADOS" "$(metrica_actuator usuarios_bloqueados)"

verifica_persistencia() {
    confere "login_validos_total zerado (Counter)" "0" "$(metrica_actuator login_validos_total)"
    confere "login_invalidos_total zerado (Counter)" "0" "$(metrica_actuator login_invalidos_total)"
    confere "usuarios_bloqueados reflete o banco (Gauge)" "$BLOQUEADOS" "$(metrica_actuator usuarios_bloqueados)"
    [ "$(login "$USUARIO" "$SENHA2")" = 200 ] || falha "usuario e senha trocada deveriam continuar validos"
    ok "usuario cadastrado e senha trocada continuam validos"
    [ "$(login "$BLOQUEAR" "$SENHA1")" = 401 ] || falha "usuario deveria continuar bloqueado"
    ok "estado de bloqueio persistiu"
    espera 200 "dashboard continua provisionado" -u "$GRAFANA_USER:$GRAFANA_PASS" "$GRAF/api/dashboards/uid/ms-av-02-login"
}

docker compose restart login-service
docker compose up -d --wait --wait-timeout 300
ok "login-service reiniciado"
verifica_persistencia

docker compose down
docker compose up -d --wait --wait-timeout 300
ok "ambiente recriado com down + up (volumes preservados)"
verifica_persistencia

espera 200 "PUT /unlock/$BLOQUEAR (admin) - limpeza" -X PUT -u "admin:$SENHA_ADMIN" "$API/unlock/$BLOQUEAR"

passo 15 "Importacao do JSON do dashboard"
UID_TESTE="ms-av-02-login-import-teste"
# troca somente o uid (o "id" do topo do arquivo exportado ja e null)
DASHBOARD=$(sed -E "s/\"uid\": *\"ms-av-02-login\"/\"uid\": \"$UID_TESTE\"/" \
    observabilidade/grafana/dashboards/dashboard-login.json)
RESPOSTA=$(curl -s -u "$GRAFANA_USER:$GRAFANA_PASS" -H "$JSON" -X POST \
    -d "{\"dashboard\": $DASHBOARD, \"overwrite\": true}" "$GRAF/api/dashboards/db")
printf '%s' "$RESPOSTA" | grep -q '"status":"success"' || falha "importacao do dashboard: $RESPOSTA"
ok "dashboard importado: $(printf '%s' "$RESPOSTA" | sed -nE 's/.*"url":"([^"]*)".*/\1/p')"
espera 200 "DELETE do dashboard de teste" -u "$GRAFANA_USER:$GRAFANA_PASS" -X DELETE "$GRAF/api/dashboards/uid/$UID_TESTE"

passo 16 "Nenhum segredo publicado"
if git ls-files | grep -qxE '\.env'; then falha ".env esta versionado"; fi
ok "git ls-files nao lista .env"
# "access_toke[n]" evita que o proprio script seja encontrado pela busca
if git grep -nE "\{noop\}|BEGIN (RSA |EC )?PRIVATE KEY|access_toke[n]"; then
    falha "padrao sensivel encontrado no repositorio"
fi
ok "git grep sem senhas noop, chaves privadas ou tokens de acesso"
docker run --rm -v "$PWD":/repo zricethezav/gitleaks:v8.30.0 detect --source /repo -v --redact \
    && ok "gitleaks sem vazamentos em todo o historico" \
    || falha "gitleaks apontou vazamentos (veja acima)"

printf '\n==> ACEITACAO OBRIGATORIA: todos os passos aprovados\n'

# ---------------------------------------------------------------- extensao OAuth2 (opcional)
if [ "$OAUTH2" = true ]; then
(
    set -euo pipefail
    passo "OAuth2" "Extensao opcional: Keycloak + Authorization Code com PKCE"
    COMPOSE_OAUTH2="docker compose -f docker-compose.yml -f docker-compose.oauth2.yml"
    $COMPOSE_OAUTH2 up -d --wait --wait-timeout 400
    ./keycloak/configurar-usuarios.sh
    SENHA_KC_ADMIN="${OAUTH2_ADMIN_PASSWORD:-admin-oauth2-demo}"
    SENHA_KC_MANUTENCAO="${OAUTH2_MANUTENCAO_PASSWORD:-manutencao-oauth2-demo}"
    KC=http://localhost:9090/realms/seguranca/protocol/openid-connect
    REDIRECT=http://localhost:8080/swagger-ui/oauth2-redirect.html

    # simula o navegador: Authorization Code + PKCE com o client publico do Swagger UI
    token_pkce() {
        local usuario="$1" senha="$2" jar verifier challenge pagina acao destino code
        jar=$(mktemp)
        verifier=$(openssl rand -base64 48 | tr -d '=+/\n' | cut -c1-64)
        challenge=$(printf '%s' "$verifier" | openssl dgst -sha256 -binary | openssl base64 -A | tr '+/' '-_' | tr -d '=')
        pagina=$(curl -s -c "$jar" -b "$jar" -G "$KC/auth" --data-urlencode client_id=seguranca-swagger \
            --data-urlencode response_type=code --data-urlencode scope=openid --data-urlencode redirect_uri="$REDIRECT" \
            --data-urlencode code_challenge="$challenge" --data-urlencode code_challenge_method=S256 --data-urlencode state=aceite)
        acao=$(printf '%s' "$pagina" | grep -o 'action="[^"]*"' | head -1 | sed 's/action="//; s/"$//; s/&amp;/\&/g')
        destino=$(curl -s -o /dev/null -w '%{redirect_url}' -c "$jar" -b "$jar" \
            --data-urlencode username="$usuario" --data-urlencode password="$senha" "$acao")
        rm -f "$jar"
        code=$(printf '%s' "$destino" | sed -n 's/.*[?&]code=\([^&]*\).*/\1/p')
        [ -n "$code" ] || return 1
        curl -s "$KC/token" --data-urlencode grant_type=authorization_code --data-urlencode client_id=seguranca-swagger \
            --data-urlencode code="$code" --data-urlencode redirect_uri="$REDIRECT" --data-urlencode code_verifier="$verifier" \
            | sed -nE 's/.*"access_toke[n]":"([^"]*)".*/\1/p'
    }

    TOKEN_MANUTENCAO=$(token_pkce manutencao "$SENHA_KC_MANUTENCAO") || falha "token PKCE de manutencao"
    TOKEN_ADMIN=$(token_pkce admin "$SENHA_KC_ADMIN") || falha "token PKCE de admin"
    ok "tokens obtidos via Authorization Code + PKCE"
    espera 401 "GET / sem token" "$API/"
    espera 401 "GET / com Basic (nao aceito no profile oauth2)" -u "manutencao:$SENHA_MANUTENCAO" "$API/"
    espera 200 "GET / com token de manutencao" -H "Authorization: Bearer $TOKEN_MANUTENCAO" "$API/"
    espera 403 "PUT /unlock/$USUARIO com token de manutencao" -X PUT -H "Authorization: Bearer $TOKEN_MANUTENCAO" "$API/unlock/$USUARIO"
    espera 200 "PUT /unlock/$USUARIO com token de admin" -X PUT -H "Authorization: Bearer $TOKEN_ADMIN" "$API/unlock/$USUARIO"
    espera 403 "GET / com token de admin" -H "Authorization: Bearer $TOKEN_ADMIN" "$API/"
    espera 200 "POST /login continua publico" -X POST -H "$JSON" -d "{\"username\":\"$USUARIO\",\"senha\":\"$SENHA2\"}" "$API/login"

    # volta ao profile padrao (Basic) sem o Keycloak
    $COMPOSE_OAUTH2 stop keycloak
    $COMPOSE_OAUTH2 rm -f keycloak
    docker compose up -d --wait --wait-timeout 300
    printf '\n==> EXTENSAO OAUTH2: aprovada\n'
) || { printf '\n==> EXTENSAO OAUTH2: falhou (nao invalida os passos obrigatorios)\n' >&2; exit 2; }
fi
