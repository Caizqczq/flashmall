#!/usr/bin/env bash
set -euo pipefail

NACOS_ENDPOINT="${NACOS_ENDPOINT:-http://127.0.0.1:8848}"
NACOS_GROUP="${NACOS_GROUP:-DEFAULT_GROUP}"
CONFIG_DIR="${CONFIG_DIR:-nacos/config}"

for file in "$CONFIG_DIR"/*.yaml; do
  data_id="$(basename "$file")"
  echo "Publishing $data_id to $NACOS_ENDPOINT, group=$NACOS_GROUP"
  curl -sS -X POST "$NACOS_ENDPOINT/nacos/v1/cs/configs" \
    --data-urlencode "dataId=$data_id" \
    --data-urlencode "group=$NACOS_GROUP" \
    --data-urlencode "type=yaml" \
    --data-urlencode "content@$file"
  echo
done
