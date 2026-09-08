#!/usr/bin/env bash

set -euo pipefail

repository_root="${1:?repository root is required}"
manifest="${2:?pilot manifest is required}"
staged_repository="${3:?staged repository is required}"
fixture_root="$(mktemp -d "${TMPDIR:-/tmp}/openapi-pilot-fixtures.XXXXXX")"
trap 'rm -rf "${fixture_root}"' EXIT

consumer=("${repository_root}/gradlew" -p "${repository_root}/.ci/openapi-pilot-consumer")

expect_success() {
    local name="$1"
    shift
    if ! "$@" >"${fixture_root}/${name}.log" 2>&1; then
        cat "${fixture_root}/${name}.log" >&2
        echo "Expected ${name} to succeed." >&2
        exit 1
    fi
}

expect_failure() {
    local name="$1"
    shift
    if "$@" >"${fixture_root}/${name}.log" 2>&1; then
        cat "${fixture_root}/${name}.log" >&2
        echo "Expected ${name} to fail." >&2
        exit 1
    fi
}

read_state() {
    python3 -c 'import json,sys; print(json.load(open(sys.argv[1], encoding="utf-8"))["state"])' "$1"
}

identical_report="${fixture_root}/identical.json"
expect_success identical "${consumer[@]}" checkPilotPublicationState \
    "-PpilotManifest=${manifest}" \
    "-PpilotRepositoryUrl=file://${staged_repository}" \
    "-PpilotStateReport=${identical_report}" \
    --no-daemon --console=plain
test "$(read_state "${identical_report}")" = "identical"

absent_repository="${fixture_root}/absent"
mkdir -p "${absent_repository}"
absent_report="${fixture_root}/absent.json"
expect_success absent "${consumer[@]}" checkPilotPublicationState \
    "-PpilotManifest=${manifest}" \
    "-PpilotRepositoryUrl=file://${absent_repository}" \
    "-PpilotStateReport=${absent_report}" \
    --no-daemon --console=plain
test "$(read_state "${absent_report}")" = "absent"

first_payload="$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1], encoding="utf-8"))["artifacts"][0]["files"][0]["path"])' "${manifest}")"
partial_repository="${fixture_root}/partial"
mkdir -p "${partial_repository}/$(dirname "${first_payload}")"
cp "${staged_repository}/${first_payload}" "${partial_repository}/${first_payload}"
partial_report="${fixture_root}/partial.json"
expect_failure partial "${consumer[@]}" checkPilotPublicationState \
    "-PpilotManifest=${manifest}" \
    "-PpilotRepositoryUrl=file://${partial_repository}" \
    "-PpilotStateReport=${partial_report}" \
    --no-daemon --console=plain
test "$(read_state "${partial_report}")" = "partial"

conflict_repository="${fixture_root}/conflict"
mkdir -p "${conflict_repository}"
cp -R "${staged_repository}/." "${conflict_repository}/"
printf 'conflict' >>"${conflict_repository}/${first_payload}"
conflict_report="${fixture_root}/conflict.json"
expect_failure conflict "${consumer[@]}" checkPilotPublicationState \
    "-PpilotManifest=${manifest}" \
    "-PpilotRepositoryUrl=file://${conflict_repository}" \
    "-PpilotStateReport=${conflict_report}" \
    --no-daemon --console=plain
test "$(read_state "${conflict_report}")" = "conflict"

expect_failure missing-payload "${consumer[@]}" verifyPilotPublication \
    "-PpilotManifest=${manifest}" \
    "-PpilotRepositoryUrl=file://${absent_repository}" \
    --no-daemon --console=plain

mismatch_repository="${fixture_root}/mismatch"
expect_failure mismatched-version "${repository_root}/gradlew" \
    -p "${repository_root}" generateOpenApiPilotManifest \
    -PopenApiContract=docs/openapi/warehouse-inventory-catalog.yaml \
    -PopenApiPilotVersion=9.9.9 \
    "-PopenApiRepositoryDirectory=${mismatch_repository}" \
    "-PopenApiPilotManifest=${fixture_root}/mismatch-manifest.json" \
    -PopenApiPilotCommit=0000000000000000000000000000000000000000 \
    --no-daemon --console=plain

contaminated_repository="${fixture_root}/contaminated"
mkdir -p "${contaminated_repository}/invalid/unrelated/1.0.0"
printf '<project/>' >"${contaminated_repository}/invalid/unrelated/1.0.0/unrelated-1.0.0.pom"
expect_failure contaminated-staging "${repository_root}/gradlew" \
    -p "${repository_root}" generateOpenApiPilotManifest \
    -PopenApiContract=docs/openapi/warehouse-inventory-catalog.yaml \
    -PopenApiPilotVersion=1.0.0 \
    "-PopenApiRepositoryDirectory=${contaminated_repository}" \
    "-PopenApiPilotManifest=${fixture_root}/contaminated-manifest.json" \
    -PopenApiPilotCommit=0000000000000000000000000000000000000000 \
    --no-daemon --console=plain

expect_failure missing-selector "${repository_root}/gradlew" \
    -p "${repository_root}" generateOpenApiPilotManifest \
    "-PopenApiRepositoryDirectory=${fixture_root}/missing-selector" \
    "-PopenApiPilotManifest=${fixture_root}/missing-selector-manifest.json" \
    --no-daemon --console=plain

echo "OpenAPI pilot failure fixtures passed."
