#!/usr/bin/env bash

set -euo pipefail

version="0.72.0"
expected_sha256="bbb64b9695866ce4a7a8f5c9592002c5961cab378577fa3f8a040df362b9b2ea"
install_dir="${RUNNER_TEMP:-/tmp}/trivy-${version}"
archive="${install_dir}/trivy.tar.gz"

if [[ "$(uname -s)-$(uname -m)" != "Linux-x86_64" ]]; then
  printf 'Unsupported Trivy installer platform: %s-%s\n' "$(uname -s)" "$(uname -m)" >&2
  exit 1
fi

mkdir -p "${install_dir}"
curl --fail --location --silent --show-error \
  "https://github.com/aquasecurity/trivy/releases/download/v${version}/trivy_${version}_Linux-64bit.tar.gz" \
  --output "${archive}"

printf '%s  %s\n' "${expected_sha256}" "${archive}" | sha256sum --check -
tar --extract --gzip --file "${archive}" --directory "${install_dir}" trivy
chmod 0755 "${install_dir}/trivy"

if [[ -n "${GITHUB_PATH:-}" ]]; then
  printf '%s\n' "${install_dir}" >> "${GITHUB_PATH}"
else
  printf 'Trivy installed at %s\n' "${install_dir}/trivy"
fi
