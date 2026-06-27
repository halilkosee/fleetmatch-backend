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
  echo "  ADMIN_EMAIL=admin@fleetmatch.com ADMIN_PASSWORD=123456 BASE_URL=http://localhost:8080 scripts/e2e/backend_negative_e2e.sh"
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

expect_fail() {
  local method="$1"
  local path="$2"
  local token="${3:-}"
  local data="${4:-}"
  local label="$5"
  shift 5
  local allowed=("$@")

  local status
  status="$(request "$method" "$path" "$token" "$data")"

  for code in "${allowed[@]}"; do
    if [[ "$status" == "$code" ]]; then
      echo "PASS expected failure: $label -> HTTP $status"
      return 0
    fi
  done

  fail "$label returned HTTP $status, expected one of: ${allowed[*]}"
}

json_get() {
  jq -r "$1" "$last_body"
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

create_load() {
  local broker_token="$1"
  local ref="$2"

  request POST "/api/loads" "$broker_token" "{
    \"pickupCity\":\"Chicago\",
    \"pickupState\":\"IL\",
    \"deliveryCity\":\"Dallas\",
    \"deliveryState\":\"TX\",
    \"equipmentType\":\"BOX_TRUCK_26FT\",
    \"weight\":12000,
    \"weightLbs\":12000,
    \"rate\":2400.00,
    \"notes\":\"Negative E2E test load $ref\",
    \"description\":\"Negative E2E beta load $ref\",
    \"pickupDate\":\"2026-07-01\",
    \"deliveryDate\":\"2026-07-03\",
    \"miles\":925,
    \"commodity\":\"General Freight\",
    \"referenceNumber\":\"NEG-$ref\"
  }" "200" >/dev/null
  json_get '.id'
}

submit_offer() {
  local fleet_token="$1"
  local load_id="$2"
  local amount="${3:-2300.00}"

  request POST "/api/loads/$load_id/offers" "$fleet_token" "{
    \"amount\":$amount,
    \"message\":\"Negative E2E offer $RUN_ID\"
  }" "200" >/dev/null
  json_get '.id'
}

setup_approved_company() {
  local type="$1"
  local prefix="$2"
  local company_name="$3"
  local plan="${4:-PRO}"

  local email company_id token pack phone
  pack="$(register_company_user "$type" "$prefix" "$company_name")"
  email="${pack%%|*}"
  phone="${pack##*|}"
  verify_email "$email"
  verify_phone "$phone"
  approve_pending_user_by_email "$admin_token" "$email"
  company_id="$(approve_pending_company_by_name "$admin_token" "$company_name")"
  if [[ "$plan" != "FREE" ]]; then
    assign_plan "$admin_token" "$company_id" "$plan"
  fi
  token="$(login "$email" "$PASSWORD")"
  echo "$email|$company_id|$token"
}

log "Health check"
request GET "/api/health" "" "" "200" >/dev/null

log "Admin login"
admin_token="$(login "$ADMIN_EMAIL" "$ADMIN_PASSWORD")"

log "Set up primary approved broker and fleet"
broker_pack="$(setup_approved_company BROKER "neg.broker" "NEG Broker Company" PRO)"
fleet_pack="$(setup_approved_company FLEET "neg.fleet" "NEG Fleet Company" PRO)"
broker_token="${broker_pack##*|}"
fleet_token="${fleet_pack##*|}"

log "Create primary load, offer, selected conversation"
load_id="$(create_load "$broker_token" "$RUN_ID-main")"
offer_id="$(submit_offer "$fleet_token" "$load_id")"
request PUT "/api/loads/$load_id/offers/$offer_id/select" "$broker_token" "" "200" >/dev/null
request GET "/api/loads/$load_id/conversation" "$broker_token" "" "200" >/dev/null
conversation_id="$(json_get '.id')"
request POST "/api/conversations/$conversation_id/messages" "$broker_token" "{\"body\":\"Message before negative tests $RUN_ID\"}" "200" >/dev/null
message_id="$(json_get '.id')"

log "NEG-01: unapproved broker company cannot create load"
unapproved_broker_pack="$(register_company_user BROKER "unapproved.broker" "NEG Unapproved Broker")"
unapproved_broker_email="${unapproved_broker_pack%%|*}"
unapproved_broker_phone="${unapproved_broker_pack##*|}"
verify_email "$unapproved_broker_email"
verify_phone "$unapproved_broker_phone"
approve_pending_user_by_email "$admin_token" "$unapproved_broker_email"
unapproved_broker_token="$(login "$unapproved_broker_email" "$PASSWORD")"
expect_fail POST "/api/loads" "$unapproved_broker_token" "{
  \"pickupCity\":\"Chicago\",
  \"pickupState\":\"IL\",
  \"deliveryCity\":\"Dallas\",
  \"deliveryState\":\"TX\",
  \"equipmentType\":\"BOX_TRUCK_26FT\",
  \"weight\":12000,
  \"weightLbs\":12000,
  \"rate\":2400.00,
  \"notes\":\"Should fail\",
  \"pickupDate\":\"2026-07-01\",
  \"deliveryDate\":\"2026-07-03\",
  \"miles\":925,
  \"commodity\":\"General Freight\",
  \"referenceNumber\":\"UNAPPROVED-$RUN_ID\"
}" "unapproved broker create load" 403

log "NEG-02: unapproved fleet company cannot submit offer"
unapproved_fleet_pack="$(register_company_user FLEET "unapproved.fleet" "NEG Unapproved Fleet")"
unapproved_fleet_email="${unapproved_fleet_pack%%|*}"
unapproved_fleet_phone="${unapproved_fleet_pack##*|}"
verify_email "$unapproved_fleet_email"
verify_phone "$unapproved_fleet_phone"
approve_pending_user_by_email "$admin_token" "$unapproved_fleet_email"
unapproved_fleet_token="$(login "$unapproved_fleet_email" "$PASSWORD")"
expect_fail POST "/api/loads/$load_id/offers" "$unapproved_fleet_token" "{\"amount\":2100.00,\"message\":\"Should fail\"}" "unapproved fleet submit offer" 403

log "NEG-03: FREE plan fleet cannot submit offer"
free_fleet_pack="$(setup_approved_company FLEET "free.fleet" "NEG Free Fleet" FREE)"
free_fleet_token="${free_fleet_pack##*|}"
expect_fail POST "/api/loads/$load_id/offers" "$free_fleet_token" "{\"amount\":2100.00,\"message\":\"Should fail due free plan\"}" "FREE fleet submit offer" 400

log "NEG-04: other broker cannot view offers for someone else's load"
other_broker_pack="$(setup_approved_company BROKER "other.broker" "NEG Other Broker" PRO)"
other_broker_token="${other_broker_pack##*|}"
expect_fail GET "/api/loads/$load_id/offers" "$other_broker_token" "" "other broker view offers" 403

log "NEG-05: other fleet cannot access conversation messages"
other_fleet_pack="$(setup_approved_company FLEET "other.fleet" "NEG Other Fleet" PRO)"
other_fleet_token="${other_fleet_pack##*|}"
expect_fail GET "/api/conversations/$conversation_id/messages?page=0&size=20" "$other_fleet_token" "" "other fleet read conversation" 403

log "NEG-06: driver cannot access messaging"
driver_email="driver.${RUN_ID}@easyfleetmatch.test"
request POST "/api/company/users" "$fleet_token" "{
  \"firstName\":\"E2E\",
  \"lastName\":\"Driver\",
  \"email\":\"$driver_email\",
  \"password\":\"$PASSWORD\",
  \"companyUserRole\":\"DRIVER\"
}" "201" >/dev/null
driver_token="$(login "$driver_email" "$PASSWORD")"
expect_fail GET "/api/conversations/$conversation_id/messages?page=0&size=20" "$driver_token" "" "driver read messages" 403
expect_fail POST "/api/conversations/$conversation_id/messages" "$driver_token" "{\"body\":\"Driver should fail\"}" "driver send message" 403

log "NEG-07: broker cannot start booked shipment"
request PUT "/api/loads/$load_id/offers/$offer_id/confirm" "$fleet_token" "" "200" >/dev/null
expect_fail PUT "/api/loads/$load_id/start" "$broker_token" "" "broker start booked load" 403

log "NEG-08: soft-deleted message disappears from message list"
request DELETE "/api/conversations/$conversation_id/messages/$message_id" "$broker_token" "" "200" >/dev/null
deleted_flag="$(json_get '.deleted')"
[[ "$deleted_flag" == "true" ]] || fail "Expected deleted message response deleted=true"
request GET "/api/conversations/$conversation_id/messages?page=0&size=20" "$fleet_token" "" "200" >/dev/null
visible_deleted_count="$(jq -r --arg id "$message_id" '[.content[] | select(.id == $id)] | length' "$last_body")"
[[ "$visible_deleted_count" == "0" ]] || fail "Deleted message is still visible in message list"
echo "PASS soft delete hides message from list"

log "NEG-09: archived conversation cannot receive messages after fleet decline"
decline_load_id="$(create_load "$broker_token" "$RUN_ID-decline")"
decline_offer_id="$(submit_offer "$fleet_token" "$decline_load_id" "2200.00")"
request PUT "/api/loads/$decline_load_id/offers/$decline_offer_id/select" "$broker_token" "" "200" >/dev/null
request GET "/api/loads/$decline_load_id/conversation" "$broker_token" "" "200" >/dev/null
decline_conversation_id="$(json_get '.id')"
request POST "/api/conversations/$decline_conversation_id/messages" "$broker_token" "{\"body\":\"Before decline $RUN_ID\"}" "200" >/dev/null
request PUT "/api/loads/$decline_load_id/offers/$decline_offer_id/decline" "$fleet_token" "" "200" >/dev/null
expect_fail POST "/api/conversations/$decline_conversation_id/messages" "$broker_token" "{\"body\":\"Should fail archived\"}" "archived conversation send message" 400
request GET "/api/loads/$decline_load_id" "$broker_token" "" "200" >/dev/null
decline_load_status="$(json_get '.status')"
[[ "$decline_load_status" == "POSTED" ]] || fail "Expected declined load to return POSTED, got $decline_load_status"
echo "PASS decline returns load to POSTED"

cat <<EOF

NEGATIVE E2E PASSED
Run ID: $RUN_ID
Primary Load ID: $load_id
Primary Offer ID: $offer_id
Primary Conversation ID: $conversation_id
Decline Load ID: $decline_load_id
Decline Conversation ID: $decline_conversation_id
EOF
