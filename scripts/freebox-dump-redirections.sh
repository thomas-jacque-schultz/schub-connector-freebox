#!/usr/bin/env bash
#
# Instantané des redirections de ports de la Freebox.
#
# Sert de filet : si une redirection disparaît, c'est ce fichier qui dit ce qu'elle était.
# Écrit deux formats côte à côte — le JSON brut de la box (complet, avec les identifiants,
# rejouable) et une table lisible pour comparer d'un coup d'œil.
#
#   FREEBOX_APP_TOKEN=... ./freebox-dump-redirections.sh [dossier]
#
# Le dossier par défaut est le répertoire courant. Ne jamais committer le résultat dans un
# repo public : il expose la liste des ports ouverts et les IP internes.
set -euo pipefail

BASE_URL="${FREEBOX_BASE_URL:-http://mafreebox.freebox.fr}"
APP_ID="${FREEBOX_APP_ID:-fr.schultz.schub.portmanager}"
OUT_DIR="${1:-.}"
STAMP="$(date +%Y-%m-%dT%H-%M-%S)"

for tool in curl jq openssl; do
    command -v "$tool" >/dev/null || { echo "Outil manquant : $tool" >&2; exit 1; }
done

[ -n "${FREEBOX_APP_TOKEN:-}" ] || {
    echo "FREEBOX_APP_TOKEN non défini." >&2
    echo "Le jeton vit dans le secret Docker FREEBOX_APP_TOKEN du Swarm." >&2
    exit 1
}

api="${BASE_URL}/api/v$(curl -fsS "${BASE_URL}/api_version" | jq -r '.api_version | split(".")[0]')"

challenge="$(curl -fsS "${api}/login/" | jq -r '.result.challenge')"
password="$(printf '%s' "$challenge" | openssl dgst -sha1 -hmac "$FREEBOX_APP_TOKEN" -r | cut -d' ' -f1)"
session="$(curl -fsS -X POST "${api}/login/session/" \
    -H 'Content-Type: application/json' \
    -d "$(jq -nc --arg id "$APP_ID" --arg pw "$password" '{app_id:$id, password:$pw}')" \
    | jq -r '.result.session_token')"

[ "$session" != "null" ] && [ -n "$session" ] || { echo "Ouverture de session refusée." >&2; exit 1; }

raw="$(curl -fsS "${api}/fw/redir/" -H "X-Fbx-App-Auth: ${session}")"

json_file="${OUT_DIR}/freebox-redirections-${STAMP}.json"
text_file="${OUT_DIR}/freebox-redirections-${STAMP}.txt"

jq '.result | sort_by(.lan_ip, .wan_port_start, .ip_proto)' <<<"$raw" > "$json_file"

{
    echo "# Redirections Freebox — instantané du $(date '+%Y-%m-%d %H:%M:%S %Z')"
    echo "# $(jq '.result | length' <<<"$raw") règles. Un libellé commençant par [schub] désigne une règle posée par l'app."
    echo
    printf '%-4s %-6s %-12s %-22s %s\n' "ID" "ÉTAT" "WAN" "→ LAN" "LIBELLÉ"
    jq -r '.result | sort_by(.lan_ip, .wan_port_start, .ip_proto)[] |
        [ (.id|tostring),
          (if .enabled then "ouvert" else "fermé" end),
          (.ip_proto + "/" + (.wan_port_start|tostring) + (if .wan_port_end != .wan_port_start then "-" + (.wan_port_end|tostring) else "" end)),
          (.lan_ip + ":" + (.lan_port|tostring)),
          (.comment // "")
        ] | @tsv' <<<"$raw" \
    | while IFS=$'\t' read -r id state wan lan comment; do
        printf '%-4s %-6s %-12s %-22s %s\n' "$id" "$state" "$wan" "$lan" "$comment"
      done
} > "$text_file"

echo "Instantané écrit :"
echo "  $json_file   (brut, rejouable)"
echo "  $text_file   (lisible)"
