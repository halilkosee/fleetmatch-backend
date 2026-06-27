#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
ADMIN_EMAIL="${ADMIN_EMAIL:-}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-}"
RUN_ID="${RUN_ID:-$(date +%s)}"
PASSWORD="${E2E_PASSWORD:-E2eTest!123}"

if [[ -z "$ADMIN_EMAIL" || -z "$ADMIN_PASSWORD" ]]; then
  echo "ADMIN_EMAIL and ADMIN_PASSWORD are required."
  echo "Example:"
  echo "  ADMIN_EMAIL=admin@fleetmatch.com ADMIN_PASSWORD=123456 BASE_URL=http://localhost:8080 scripts/e2e/backend_e2e.sh"
  exit 1
fi

tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT

last_body="$tmp_dir/body.json"
last_headers="$tmp_dir/headers.txt"

log() {
  printf '\n[%s] %s\n' "$(date '+%H:%M:%S')" "$*" >&2
}

fail() {
  echo "ERROR: $*" >&2
  if [[ -s "$last_body" ]]; then
    echo "Last response body:" >&2
    cat "$last_body" >&2
  fi
  exit 1
}

request() {
  local method="$1"
  local path="$2"
  local token="${3:-}"
  local data="${4:-}"
  local expected="${5:-}"

  local args=(-sS -X "$method" "$BASE_URL$path" -D "$last_headers" -o "$last_body" -w "%{http_code}")
  args+=(-H "Content-Type: application/json")
  if [[ -n "$token" ]]; then
    args+=(-H "Authorization: Bearer $token")
  fi
  if [[ -n "$data" ]]; then
    args+=(-d "$data")
  fi

  local status
  status="$(curl "${args[@]}")" || fail "Request failed: $method $path"

  if [[ -n "$expected" && "$status" != "$expected" ]]; then
    fail "$method $path returned HTTP $status, expected $expected"
  fi

  echo "$status"
}

json_get() {
  jq -r "$1" "$last_body"
}

health_check() {
  log "Health check"
  local status
  status="$(request GET "/api/health" "" "" "200")"
  echo "HTTP $status - $(cat "$last_body")"
}

login() {
  local email="$1"
  local password="$2"
  request POST "/api/auth/login" "" "{\"email\":\"$email\",\"password\":\"$password\"}" "200" >/dev/null
  json_get '.token'
}

register_company_user() {
  local type="$1"
  local prefix="$2"
  local company_name="$3"
  local email="${prefix}.${RUN_ID}@easyfleetmatch.test"
  local company_email="${prefix}.company.${RUN_ID}@easyfleetmatch.test"
  local phone_suffix
  phone_suffix="$(printf '%s' "$prefix" | cksum | awk '{print ($1 % 900) + 100}')"
  local phone="555-${RUN_ID: -3}-$phone_suffix"

  log "Register $type user: $email"
  request POST "/api/auth/register" "" "{
    \"companyLegalName\":\"$company_name $RUN_ID\",
    \"companyDbaName\":\"$company_name\",
    \"companyEmail\":\"$company_email\",
    \"companyPhone\":\"555-100-$RUN_ID\",
    \"companyType\":\"$type\",
    \"firstName\":\"E2E\",
    \"lastName\":\"$prefix\",
    \"email\":\"$email\",
    \"phone\":\"$phone\",
    \"password\":\"$PASSWORD\"
  }" "200" >/dev/null

  echo "$email|$phone"
}

verify_email() {
  local email="$1"

  request POST "/api/auth/resend-email-code" "" "{\"email\":\"$email\"}" "200" >/dev/null
  local code
  code="$(json_get '.debugCode')"
  [[ -n "$code" && "$code" != "null" ]] || fail "Email debug code not returned for $email"

  log "Verify email: $email"
  request POST "/api/auth/verify-email" "" "{\"email\":\"$email\",\"code\":\"$code\"}" "200" >/dev/null
}

verify_phone() {
  local phone="$1"

  request POST "/api/auth/resend-phone-code" "" "{\"phone\":\"$phone\"}" "200" >/dev/null
  local code
  code="$(json_get '.debugCode')"
  [[ -n "$code" && "$code" != "null" ]] || fail "Phone debug code not returned for $phone"

  log "Verify phone: $phone"
  request POST "/api/auth/verify-phone" "" "{\"phone\":\"$phone\",\"code\":\"$code\"}" "200" >/dev/null
}

approve_pending_user_by_email() {
  local admin_token="$1"
  local email="$2"

  request GET "/api/admin/users/pending" "$admin_token" "" "200" >/dev/null
  local user_id
  user_id="$(jq -r --arg email "$email" '.[] | select(.email == $email) | .id' "$last_body" | head -n 1)"
  [[ -n "$user_id" && "$user_id" != "null" ]] || fail "Pending user not found for $email"

  log "Approve user: $email"
  request PUT "/api/admin/users/$user_id/approve" "$admin_token" "" "200" >/dev/null
}

approve_pending_company_by_name() {
  local admin_token="$1"
  local company_name="$2"

  request GET "/api/admin/companies/pending" "$admin_token" "" "200" >/dev/null
  local company_id
  company_id="$(jq -r --arg name "$company_name $RUN_ID" '.[] | select(.legalName == $name) | .id' "$last_body" | head -n 1)"
  [[ -n "$company_id" && "$company_id" != "null" ]] || fail "Pending company not found for $company_name $RUN_ID"

  log "Approve company: $company_name $RUN_ID"
  request PATCH "/api/admin/companies/$company_id/approve" "$admin_token" "" "200" >/dev/null

  echo "$company_id"
}

assign_plan() {
  local admin_token="$1"
  local company_id="$2"
  local plan_name="$3"

  request GET "/api/v1/admin/subscriptions/plans" "$admin_token" "" "200" >/dev/null
  local plan_id
  plan_id="$(jq -r --arg name "$plan_name" '.[] | select(.name == $name) | .id' "$last_body" | head -n 1)"
  [[ -n "$plan_id" && "$plan_id" != "null" ]] || fail "Subscription plan not found: $plan_name"

  log "Assign $plan_name plan to company $company_id"
  request POST "/api/v1/admin/subscriptions/assign" "$admin_token" "{
    \"companyId\":\"$company_id\",
    \"subscriptionPlanId\":\"$plan_id\",
    \"startDate\":\"2026-06-27\",
    \"endDate\":null,
    \"autoRenew\":true,
    \"customPrice\":null,
    \"vehicleLimitOverride\":null,
    \"userLimitOverride\":null,
    \"monthlyLoadLimitOverride\":null,
    \"loadLimitOverride\":null,
    \"canSubmitOffersOverride\":null,
    \"canViewContactInfoOverride\":null
  }" "200" >/dev/null
}

health_check

log "Admin login"
admin_token="$(login "$ADMIN_EMAIL" "$ADMIN_PASSWORD")"
[[ -n "$admin_token" && "$admin_token" != "null" ]] || fail "Admin token not returned"

broker_pack="$(register_company_user BROKER "broker" "E2E Broker Company")"
fleet_pack="$(register_company_user FLEET "fleet" "E2E Fleet Company")"
broker_email="${broker_pack%%|*}"
broker_phone="${broker_pack##*|}"
fleet_email="${fleet_pack%%|*}"
fleet_phone="${fleet_pack##*|}"

verify_email "$broker_email"
verify_phone "$broker_phone"
verify_email "$fleet_email"
verify_phone "$fleet_phone"

approve_pending_user_by_email "$admin_token" "$broker_email"
approve_pending_user_by_email "$admin_token" "$fleet_email"

broker_company_id="$(approve_pending_company_by_name "$admin_token" "E2E Broker Company")"
fleet_company_id="$(approve_pending_company_by_name "$admin_token" "E2E Fleet Company")"

assign_plan "$admin_token" "$broker_company_id" "PRO"
assign_plan "$admin_token" "$fleet_company_id" "PRO"

log "Broker login"
broker_token="$(login "$broker_email" "$PASSWORD")"

log "Fleet login"
fleet_token="$(login "$fleet_email" "$PASSWORD")"

log "Broker updates company settings"
request PUT "/api/companies/me/settings" "$broker_token" "{
  \"dbaName\":\"E2E Broker DBA $RUN_ID\",
  \"website\":\"https://broker-$RUN_ID.easyfleetmatch.test\",
  \"companyEmail\":\"broker.company.updated.$RUN_ID@easyfleetmatch.test\",
  \"companyPhone\":\"555-300-$RUN_ID\",
  \"fleetSize\":null,
  \"description\":\"Broker settings updated by E2E\"
}" "200" >/dev/null

log "Locked company fields remain unchanged"
request GET "/api/companies/me" "$broker_token" "" "200" >/dev/null
company_legal_name="$(json_get '.legalName')"
company_type="$(json_get '.type')"
[[ "$company_legal_name" == "E2E Broker Company $RUN_ID" ]] || fail "Expected company legalName to stay locked"
[[ "$company_type" == "BROKER" ]] || fail "Expected company type to stay locked"

log "Broker creates load"
request POST "/api/loads" "$broker_token" "{
  \"pickupCity\":\"Chicago\",
  \"pickupState\":\"IL\",
  \"deliveryCity\":\"Dallas\",
  \"deliveryState\":\"TX\",
  \"equipmentType\":\"BOX_TRUCK_26FT\",
  \"weight\":12000,
  \"weightLbs\":12000,
  \"rate\":2400.00,
  \"notes\":\"E2E test load $RUN_ID\",
  \"description\":\"E2E beta-ready load $RUN_ID\",
  \"pickupDate\":\"2026-07-01\",
  \"deliveryDate\":\"2026-07-03\",
  \"miles\":925,
  \"commodity\":\"General Freight\",
  \"referenceNumber\":\"E2E-$RUN_ID\"
}" "200" >/dev/null
load_id="$(json_get '.id')"
load_status="$(json_get '.status')"
[[ "$load_status" == "POSTED" ]] || fail "Expected load POSTED, got $load_status"
echo "Load: $load_id -> $load_status"

log "Fleet submits offer"
request POST "/api/loads/$load_id/offers" "$fleet_token" "{
  \"amount\":2300.00,
  \"message\":\"Fleet E2E offer $RUN_ID\"
}" "200" >/dev/null
offer_id="$(json_get '.id')"
offer_status="$(json_get '.status')"
[[ "$offer_status" == "PENDING" ]] || fail "Expected offer PENDING, got $offer_status"
echo "Offer: $offer_id -> $offer_status"

log "Broker selects offer"
request PUT "/api/loads/$load_id/offers/$offer_id/select" "$broker_token" "" "200" >/dev/null
offer_status="$(json_get '.status')"
[[ "$offer_status" == "SELECTED" ]] || fail "Expected offer SELECTED, got $offer_status"

request GET "/api/loads/$load_id" "$broker_token" "" "200" >/dev/null
load_status="$(json_get '.status')"
[[ "$load_status" == "AWAITING_FLEET_CONFIRMATION" ]] || fail "Expected load AWAITING_FLEET_CONFIRMATION, got $load_status"
echo "After select: load -> $load_status, offer -> $offer_status"

log "Conversation exists after selection"
request GET "/api/loads/$load_id/conversation" "$broker_token" "" "200" >/dev/null
conversation_id="$(json_get '.id')"
[[ -n "$conversation_id" && "$conversation_id" != "null" ]] || fail "Conversation not returned"
echo "Conversation: $conversation_id"

log "Notifications exist after offer selection"
request GET "/api/notifications/unread-count" "$fleet_token" "" "200" >/dev/null
fleet_unread="$(json_get '.unreadCount')"
[[ "$fleet_unread" -ge 1 ]] || fail "Expected fleet unread notifications, got $fleet_unread"

log "Fleet marks notifications as read"
request POST "/api/notifications/read-all" "$fleet_token" "" "200" >/dev/null
request GET "/api/notifications/unread-count" "$fleet_token" "" "200" >/dev/null
fleet_unread_after_read_all="$(json_get '.unreadCount')"
[[ "$fleet_unread_after_read_all" == "0" ]] || fail "Expected fleet unread notifications to be 0 after read-all"

log "Broker sends message"
request POST "/api/conversations/$conversation_id/messages" "$broker_token" "{\"body\":\"Broker E2E dispatch message $RUN_ID\"}" "200" >/dev/null
broker_message_id="$(json_get '.id')"
echo "Broker message: $broker_message_id"

log "Fleet reads messages"
request GET "/api/conversations/$conversation_id/messages?page=0&size=20" "$fleet_token" "" "200" >/dev/null
message_count="$(json_get '.content | length')"
[[ "$message_count" -ge 1 ]] || fail "Expected at least one message, got $message_count"

log "Fleet replies"
request POST "/api/conversations/$conversation_id/messages" "$fleet_token" "{\"body\":\"Fleet E2E confirmation message $RUN_ID\"}" "200" >/dev/null
fleet_message_id="$(json_get '.id')"
echo "Fleet message: $fleet_message_id"

log "Fleet confirms assignment"
request PUT "/api/loads/$load_id/offers/$offer_id/confirm" "$fleet_token" "" "200" >/dev/null
offer_status="$(json_get '.status')"
[[ "$offer_status" == "CONFIRMED" ]] || fail "Expected offer CONFIRMED, got $offer_status"

request GET "/api/loads/$load_id" "$broker_token" "" "200" >/dev/null
load_status="$(json_get '.status')"
[[ "$load_status" == "BOOKED" ]] || fail "Expected load BOOKED, got $load_status"
echo "After confirm: load -> $load_status, offer -> $offer_status"

log "Fleet starts shipment"
request PUT "/api/loads/$load_id/start" "$fleet_token" "" "200" >/dev/null
load_status="$(json_get '.status')"
[[ "$load_status" == "IN_TRANSIT" ]] || fail "Expected load IN_TRANSIT, got $load_status"
echo "After start: load -> $load_status"

log "Fleet marks delivered"
request PUT "/api/loads/$load_id/deliver" "$fleet_token" "" "200" >/dev/null
load_status="$(json_get '.status')"
[[ "$load_status" == "DELIVERED" ]] || fail "Expected load DELIVERED, got $load_status"
echo "After deliver: load -> $load_status"

log "Final broker load check"
request GET "/api/loads/$load_id" "$broker_token" "" "200" >/dev/null
final_status="$(json_get '.status')"
[[ "$final_status" == "DELIVERED" ]] || fail "Expected final load DELIVERED, got $final_status"

log "Dashboard KPI checks"
request GET "/api/dashboard/broker" "$broker_token" "" "200" >/dev/null
delivered_loads="$(json_get '.deliveredLoads')"
[[ "$delivered_loads" -ge 1 ]] || fail "Expected broker deliveredLoads >= 1"
request GET "/api/dashboard/fleet" "$fleet_token" "" "200" >/dev/null
fleet_delivered_loads="$(json_get '.deliveredLoads')"
[[ "$fleet_delivered_loads" -ge 1 ]] || fail "Expected fleet deliveredLoads >= 1"

log "Duplicate load"
request POST "/api/loads/$load_id/duplicate" "$broker_token" "" "200" >/dev/null
duplicate_load_id="$(json_get '.id')"
duplicate_status="$(json_get '.status')"
[[ "$duplicate_status" == "POSTED" ]] || fail "Expected duplicate load POSTED, got $duplicate_status"

log "Search filters"
request GET "/api/loads/search/paged?pickupState=IL&deliveryState=TX&equipmentType=BOX_TRUCK_26FT&page=0&size=10" "$fleet_token" "" "200" >/dev/null
search_count="$(json_get '.content | length')"
[[ "$search_count" -ge 1 ]] || fail "Expected search to return at least one load"

log "Audit logs created"
request GET "/api/admin/audit-logs?page=0&size=20" "$admin_token" "" "200" >/dev/null
audit_count="$(json_get '.content | length')"
[[ "$audit_count" -ge 1 ]] || fail "Expected audit logs"

log "Change broker email"
new_broker_email="broker.changed.${RUN_ID}@easyfleetmatch.test"
request POST "/api/users/me/change-email/request" "$broker_token" "{\"newEmail\":\"$new_broker_email\"}" "200" >/dev/null
email_change_code="$(json_get '.debugCode')"
request POST "/api/users/me/change-email/verify" "$broker_token" "{\"newEmail\":\"$new_broker_email\",\"code\":\"$email_change_code\"}" "200" >/dev/null
broker_email="$new_broker_email"
broker_token="$(login "$broker_email" "$PASSWORD")"

log "Change broker phone"
new_broker_phone="555-400-${RUN_ID: -4}"
request POST "/api/users/me/change-phone/request" "$broker_token" "{\"newPhone\":\"$new_broker_phone\"}" "200" >/dev/null
phone_change_code="$(json_get '.debugCode')"
request POST "/api/users/me/change-phone/verify" "$broker_token" "{\"newPhone\":\"$new_broker_phone\",\"code\":\"$phone_change_code\"}" "200" >/dev/null

log "Forgot/reset password"
new_password="E2eReset!123"
request POST "/api/auth/forgot-password" "" "{\"email\":\"$broker_email\"}" "200" >/dev/null
reset_code="$(json_get '.debugCode')"
request POST "/api/auth/reset-password" "" "{\"email\":\"$broker_email\",\"code\":\"$reset_code\",\"newPassword\":\"$new_password\"}" "200" >/dev/null
broker_token="$(login "$broker_email" "$new_password")"

cat <<EOF

E2E PASSED
Run ID: $RUN_ID
Broker email: $broker_email
Fleet email: $fleet_email
Load ID: $load_id
Offer ID: $offer_id
Conversation ID: $conversation_id
Duplicate Load ID: $duplicate_load_id
Final load status: $final_status
EOF
