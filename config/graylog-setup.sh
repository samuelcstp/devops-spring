#!/bin/sh
set -eu

GRAYLOG_URL="http://graylog:9000"

echo "Aguardando o Graylog ficar pronto..."
i=0
while [ "$i" -lt 60 ]; do
  if curl -sf "${GRAYLOG_URL}/api/system/lbstatus" | grep -q ALIVE; then
    break
  fi
  i=$((i + 1))
  sleep 5
done

if ! curl -sf "${GRAYLOG_URL}/api/system/lbstatus" | grep -q ALIVE; then
  echo "Graylog não respondeu ALIVE a tempo."
  exit 1
fi

echo "Abrindo sessão na API..."
body=$(curl -sf -H 'Content-Type: application/json' -H 'X-Requested-By: setup' \
  -X POST "${GRAYLOG_URL}/api/system/sessions" \
  -d '{"username":"admin","password":"admin","host":"graylog-setup"}')

token=$(printf '%s' "$body" | sed -n 's/.*"session_id"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
if [ -z "$token" ]; then
  echo "Não foi possível obter a sessão do Graylog. Resposta: $body"
  exit 1
fi

inputs=$(curl -sf -u "${token}:session" -H 'X-Requested-By: setup' \
  "${GRAYLOG_URL}/api/system/inputs")

if printf '%s' "$inputs" | grep -q 'org.graylog2.inputs.gelf.udp.GELFUDPInput'; then
  echo "Input GELF UDP já existe."
  exit 0
fi

echo "Criando input GELF UDP na porta 12201..."
code=$(curl -s -o /tmp/graylog-input-response.txt -w '%{http_code}' \
  -u "${token}:session" \
  -H 'Content-Type: application/json' \
  -H 'X-Requested-By: setup' \
  -X POST "${GRAYLOG_URL}/api/system/inputs" \
  --data-binary @/graylog-gelf-input.json)

if [ "$code" -lt 200 ] || [ "$code" -ge 300 ]; then
  echo "Falha ao criar o input GELF UDP (HTTP ${code})."
  cat /tmp/graylog-input-response.txt
  exit 1
fi

echo "Input GELF UDP criado."
