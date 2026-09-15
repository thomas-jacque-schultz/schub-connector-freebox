#!/usr/bin/env bash
#
# Appairage de l'application "Schub Port Manager" auprès de la Freebox.
#
# À lancer une seule fois, depuis une machine du LAN, avec un accès physique à la box :
# la validation se fait sur l'écran de la Freebox Server. Le jeton obtenu ne périme pas
# et doit être stocké comme secret Docker (voir la fin du script).
#
#   ./freebox-pair.sh          appaire et affiche le jeton
#   ./freebox-pair.sh --check  vérifie un jeton existant (FREEBOX_APP_TOKEN) et liste les redirections
#
set -euo pipefail

BASE_URL="${FREEBOX_BASE_URL:-http://mafreebox.freebox.fr}"
APP_ID="${FREEBOX_APP_ID:-fr.schultz.schub.portmanager}"
APP_NAME="${FREEBOX_APP_NAME:-Schub Port Manager}"
APP_VERSION="${FREEBOX_APP_VERSION:-1.0.0}"
DEVICE_NAME="${FREEBOX_DEVICE_NAME:-dynamis}"

for tool in curl jq openssl; do
    command -v "$tool" >/dev/null || { echo "Outil manquant : $tool" >&2; exit 1; }
done

# La Freebox annonce sa version d'API ; on en dérive le préfixe /api/vXX utilisé partout.
api_prefix() {
    local major
    major="$(curl -fsS "${BASE_URL}/api_version" | jq -r '.api_version | split(".")[0]')"
    echo "${BASE_URL}/api/v${major}"
}

# Ouvre une session à partir d'un jeton d'appairage et renvoie le session_token.
open_session() {
    local api="$1" token="$2" challenge password response
    challenge="$(curl -fsS "${api}/login/" | jq -r '.result.challenge')"
    password="$(printf '%s' "$challenge" | openssl dgst -sha1 -hmac "$token" -r | cut -d' ' -f1)"

    response="$(curl -fsS -X POST "${api}/login/session/" \
        -H 'Content-Type: application/json' \
        -d "$(jq -nc --arg id "$APP_ID" --arg pw "$password" '{app_id:$id, password:$pw}')" || true)"

    if [ "$(jq -r '.success' <<<"$response")" != "true" ]; then
        echo "Ouverture de session refusée : $(jq -r '.error_code // "?"' <<<"$response") / $(jq -r '.msg // "?"' <<<"$response")" >&2
        exit 1
    fi

    if [ "$(jq -r '.result.permissions.settings // false' <<<"$response")" != "true" ]; then
        echo >&2
        echo "L'application est appairée mais n'a PAS la permission « Modification des réglages de la Freebox »." >&2
        echo "Accordez-la dans Freebox OS > Paramètres > Gestion des accès > Applications, puis relancez --check." >&2
        exit 1
    fi

    jq -r '.result.session_token' <<<"$response"
}

check_token() {
    local api token session
    api="$(api_prefix)"
    token="${FREEBOX_APP_TOKEN:-}"
    [ -n "$token" ] || { echo "FREEBOX_APP_TOKEN non défini." >&2; exit 1; }

    session="$(open_session "$api" "$token")"
    echo "Session ouverte, permission 'settings' accordée."
    echo
    echo "Redirections actuellement déclarées sur la box :"
    curl -fsS "${api}/fw/redir/" -H "X-Fbx-App-Auth: ${session}" \
        | jq -r '.result[] | "  \(if .enabled then "ouvert" else "fermé " end)  \(.ip_proto)/\(.wan_port_start)\(if .wan_port_end != .wan_port_start then "-\(.wan_port_end)" else "" end) -> \(.lan_ip):\(.lan_port)   \(.comment)"'
}

pair() {
    local api response app_token track_id status
    api="$(api_prefix)"
    echo "Freebox détectée sur ${api}"

    response="$(curl -fsS -X POST "${api}/login/authorize/" \
        -H 'Content-Type: application/json' \
        -d "$(jq -nc --arg id "$APP_ID" --arg name "$APP_NAME" --arg version "$APP_VERSION" --arg device "$DEVICE_NAME" \
              '{app_id:$id, app_name:$name, app_version:$version, device_name:$device}')")"

    if [ "$(jq -r '.success' <<<"$response")" != "true" ]; then
        echo "Demande d'appairage refusée : $(jq -r '.msg // "?"' <<<"$response")" >&2
        exit 1
    fi

    app_token="$(jq -r '.result.app_token' <<<"$response")"
    track_id="$(jq -r '.result.track_id' <<<"$response")"

    echo
    echo ">>> Allez appuyer sur la flèche DROITE de l'écran de la Freebox Server pour autoriser \"${APP_NAME}\"."
    echo "    (l'invite reste affichée quelques dizaines de secondes)"
    echo

    while true; do
        status="$(curl -fsS "${api}/login/authorize/${track_id}" | jq -r '.result.status')"
        case "$status" in
            granted) echo "Appairage accepté."; break ;;
            pending) printf '.' ; sleep 2 ;;
            timeout) echo; echo "Délai dépassé sans validation. Relancez le script." >&2; exit 1 ;;
            denied)  echo; echo "Appairage refusé sur la box." >&2; exit 1 ;;
            *)       echo; echo "Statut inattendu : $status" >&2; exit 1 ;;
        esac
    done

    echo
    echo "Jeton d'appairage :"
    echo "  ${app_token}"
    echo
    echo "Étapes suivantes :"
    echo "  1. Dans Freebox OS > Paramètres > Gestion des accès > Applications, accordez à"
    echo "     \"${APP_NAME}\" la permission « Modification des réglages de la Freebox »."
    echo "  2. Créez le secret Docker sur le manager Swarm :"
    echo "       printf '%s' '${app_token}' | docker secret create FREEBOX_APP_TOKEN -"
    echo "  3. Vérifiez :"
    echo "       FREEBOX_APP_TOKEN='${app_token}' $0 --check"
}

case "${1:-}" in
    --check) check_token ;;
    ""|--pair) pair ;;
    *) echo "Usage: $0 [--pair|--check]" >&2; exit 1 ;;
esac
