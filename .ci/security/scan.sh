#!/usr/bin/env sh

set -u

MODE="${1:-all}"
WORKSPACE="${SECURITY_WORKSPACE:-$(pwd)}"
REPORT_DIR="${SECURITY_REPORT_DIR:-${WORKSPACE}/build/reports/security}"
SBOM_PATH="${SECURITY_SBOM_PATH:-${WORKSPACE}/build/reports/bom.json}"
TRIVY_CACHE_DIR="${TRIVY_CACHE_DIR:-${HOME}/.cache/trivy}"
SCAN_TIMEOUT="${SECURITY_SCAN_TIMEOUT:-10m}"
EXECUTION_PROFILE="${SECURITY_EXECUTION_PROFILE:-manual}"

scan_failed=0
temporary_directory=""
started_at="$(date -u '+%Y-%m-%dT%H:%M:%SZ')"

log() {
  printf '%s\n' "$1"
}

fail() {
  log "ERROR: $1" >&2
  scan_failed=1
}

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    fail "Required command is not available: $1"
    return 1
  fi
}

run_trivy() {
  require_command trivy || return

  if [ ! -f "${SBOM_PATH}" ]; then
    fail "CycloneDX SBOM not found at ${SBOM_PATH}. Run ./gradlew cyclonedxBom first."
    return
  fi

  log "Generating the Trivy vulnerability report..."
  if ! trivy sbom \
    --quiet \
    --skip-version-check \
    --cache-dir "${TRIVY_CACHE_DIR}" \
    --timeout "${SCAN_TIMEOUT}" \
    --severity UNKNOWN,LOW,MEDIUM,HIGH,CRITICAL \
    --format sarif \
    --output "${REPORT_DIR}/trivy-vulnerabilities.sarif" \
    "${SBOM_PATH}"; then
    fail "Trivy could not generate the vulnerability report."
  fi

  log "Generating the Trivy misconfiguration report..."
  if ! trivy fs \
    --quiet \
    --skip-version-check \
    --cache-dir "${TRIVY_CACHE_DIR}" \
    --timeout "${SCAN_TIMEOUT}" \
    --offline-scan \
    --scanners misconfig \
    --skip-dirs '**/.git' \
    --skip-dirs '**/.gradle' \
    --skip-dirs '**/build' \
    --severity UNKNOWN,LOW,MEDIUM,HIGH,CRITICAL \
    --format sarif \
    --output "${REPORT_DIR}/trivy-misconfigurations.sarif" \
    "${WORKSPACE}"; then
    fail "Trivy could not generate the misconfiguration report."
  fi

  log "Applying the critical vulnerability gate..."
  if ! trivy sbom \
    --quiet \
    --skip-version-check \
    --cache-dir "${TRIVY_CACHE_DIR}" \
    --timeout "${SCAN_TIMEOUT}" \
    --severity CRITICAL \
    --exit-code 1 \
    --format json \
    --output "${temporary_directory}/critical-vulnerabilities.json" \
    "${SBOM_PATH}"; then
    fail "Critical vulnerabilities were detected or the critical vulnerability scan failed."
  fi

  log "Applying the fixable high vulnerability gate..."
  if ! trivy sbom \
    --quiet \
    --skip-version-check \
    --cache-dir "${TRIVY_CACHE_DIR}" \
    --timeout "${SCAN_TIMEOUT}" \
    --severity HIGH \
    --ignore-unfixed \
    --exit-code 1 \
    --format json \
    --output "${temporary_directory}/fixable-high-vulnerabilities.json" \
    "${SBOM_PATH}"; then
    fail "Fixable high vulnerabilities were detected or the high vulnerability scan failed."
  fi

  log "Applying the high and critical misconfiguration gate..."
  if ! trivy fs \
    --quiet \
    --skip-version-check \
    --cache-dir "${TRIVY_CACHE_DIR}" \
    --timeout "${SCAN_TIMEOUT}" \
    --offline-scan \
    --scanners misconfig \
    --skip-dirs '**/.git' \
    --skip-dirs '**/.gradle' \
    --skip-dirs '**/build' \
    --severity HIGH,CRITICAL \
    --exit-code 1 \
    --format json \
    --output "${temporary_directory}/high-critical-misconfigurations.json" \
    "${WORKSPACE}"; then
    fail "High or critical misconfigurations were detected, or the misconfiguration scan failed."
  fi
}

run_gitleaks() {
  require_command gitleaks || return

  if [ ! -d "${WORKSPACE}/.git" ]; then
    fail "Git repository not found at ${WORKSPACE}."
    return
  fi

  log "Scanning the complete Git history for secrets..."
  if ! gitleaks git "${WORKSPACE}" \
    --redact=100 \
    --report-format sarif \
    --report-path "${REPORT_DIR}/gitleaks.sarif" \
    --exit-code 1 \
    --no-banner; then
    fail "Confirmed or potential secrets were detected, or the secret scan failed. Review the redacted SARIF report."
  fi
}

finalize_reports() {
  find "${REPORT_DIR}" -type f -exec chmod 600 {} \; 2>/dev/null || true

  if [ "$(id -u)" = "0" ] &&
     [ "${REPORT_UID:-}" != "" ] &&
     [ "${REPORT_GID:-}" != "" ]; then
    chown -R "${REPORT_UID}:${REPORT_GID}" "${REPORT_DIR}" 2>/dev/null || true
  fi
}

write_execution_summary() {
  status="$1"
  completed_at="$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
  commit="$(git -C "${WORKSPACE}" rev-parse HEAD 2>/dev/null || printf 'unknown')"

  printf '%s\n' \
    '{' \
    '  "schemaVersion": 1,' \
    "  \"profile\": \"${EXECUTION_PROFILE}\"," \
    "  \"mode\": \"${MODE}\"," \
    "  \"commit\": \"${commit}\"," \
    "  \"startedAt\": \"${started_at}\"," \
    "  \"completedAt\": \"${completed_at}\"," \
    "  \"status\": \"${status}\"," \
    '  "reportsRedacted": true' \
    '}' >"${REPORT_DIR}/security-scan-${MODE}-summary.json"
}

case "${MODE}" in
  all | trivy | secrets)
    ;;
  *)
    log "Usage: $0 [all|trivy|secrets]" >&2
    exit 2
    ;;
esac

case "${EXECUTION_PROFILE}" in
  '' | *[!a-z0-9-]*)
    log "SECURITY_EXECUTION_PROFILE must contain only lowercase letters, digits, and hyphens." >&2
    exit 2
    ;;
esac

mkdir -p "${REPORT_DIR}" "${TRIVY_CACHE_DIR}"
temporary_directory="$(mktemp -d)"
trap 'rm -rf "${temporary_directory}"' EXIT HUP INT TERM

if [ "${MODE}" = "all" ] || [ "${MODE}" = "trivy" ]; then
  run_trivy
fi

if [ "${MODE}" = "all" ] || [ "${MODE}" = "secrets" ]; then
  run_gitleaks
fi

finalize_reports

if [ "${scan_failed}" -ne 0 ]; then
  write_execution_summary failed
  finalize_reports
  log "Security scan failed. Findings remain redacted in ${REPORT_DIR}." >&2
  exit 1
fi

write_execution_summary passed
finalize_reports
log "Security scan completed successfully. Redacted reports are available in ${REPORT_DIR}."
