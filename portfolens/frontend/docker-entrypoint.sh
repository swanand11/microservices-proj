#!/bin/sh
set -eu
api_base_url="${API_BASE_URL:-}"
case "$api_base_url" in
  *"</"*|*"<"*|*">"*|*"'"*|*'"'*) echo "API_BASE_URL contains unsafe characters" >&2; exit 1 ;;
esac
runtime_config=/usr/share/nginx/html/runtime-config.js
if [ -w "$runtime_config" ]; then
  printf 'window.PORTFOLENS_CONFIG = { apiBaseUrl: "%s" };\n' "$api_base_url" > "$runtime_config"
elif [ ! -f "$runtime_config" ]; then
  echo "runtime config is missing and the frontend filesystem is read-only" >&2
  exit 1
fi
