#!/usr/bin/env bash
set -euo pipefail

version="8.30.1"
checksum="551f6fc83ea457d62a0d98237cbad105af8d557003051f41f3e7ca7b3f2470eb"
install_dir="${RUNNER_TEMP:-/tmp}/gitleaks-${version}"
archive="${install_dir}/gitleaks.tar.gz"

mkdir -p "${install_dir}"
curl --fail --location --silent --show-error \
  --output "${archive}" \
  "https://github.com/gitleaks/gitleaks/releases/download/v${version}/gitleaks_${version}_linux_x64.tar.gz"
printf '%s  %s\n' "${checksum}" "${archive}" | sha256sum --check --status
tar --extract --gzip --file "${archive}" --directory "${install_dir}" gitleaks

if [[ -n "${GITHUB_PATH:-}" ]]; then
  printf '%s\n' "${install_dir}" >>"${GITHUB_PATH}"
else
  printf '%s\n' "${install_dir}"
fi
