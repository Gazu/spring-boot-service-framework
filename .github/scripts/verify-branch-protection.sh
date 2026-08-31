#!/usr/bin/env bash

set -euo pipefail

root_directory="$(git rev-parse --show-toplevel)"
contract_path="${root_directory}/.ci/pull-request-contract.json"

for command in curl git jq; do
  if ! command -v "${command}" >/dev/null 2>&1; then
    printf 'Required command is not available: %s\n' "${command}" >&2
    exit 1
  fi
done

repository="$(jq --raw-output '.repository' "${contract_path}")"
branch="$(jq --raw-output '.branchProtection.branch' "${contract_path}")"
api_url="${GITHUB_API_URL:-https://api.github.com}"
token="${GITHUB_TOKEN:-}"
username=""

if [[ -z "${token}" ]]; then
  credential="$(printf 'protocol=https\nhost=github.com\n\n' | git credential fill)"
  username="$(printf '%s\n' "${credential}" | sed -n 's/^username=//p')"
  token="$(printf '%s\n' "${credential}" | sed -n 's/^password=//p')"
  unset credential
fi

if [[ -z "${token}" ]]; then
  printf 'GitHub authentication is required through GITHUB_TOKEN or the Git credential helper.\n' >&2
  exit 1
fi

curl_arguments=(
  --silent
  --show-error
  --fail
  -H 'Accept: application/vnd.github+json'
  -H 'X-GitHub-Api-Version: 2022-11-28'
)
if [[ -n "${username}" ]]; then
  curl_arguments+=(--user "${username}:${token}")
else
  curl_arguments+=(-H "Authorization: Bearer ${token}")
fi

protection="$(curl "${curl_arguments[@]}" \
  "${api_url}/repos/${repository}/branches/${branch}/protection")"
repository_settings="$(curl "${curl_arguments[@]}" \
  "${api_url}/repos/${repository}")"
unset token username curl_arguments

expected_protection="$(jq --compact-output '
  .branchProtection + {
    checks: [.requiredChecks[] | {
      context: .name,
      appId: $appId
    }]
  }
' --argjson appId "$(jq '.branchProtection.statusCheckAppId' "${contract_path}")" \
  "${contract_path}")"

actual_protection="$(printf '%s' "${protection}" | jq --compact-output \
  --arg branch "${branch}" '
  {
    branch: $branch,
    strictStatusChecks: .required_status_checks.strict,
    statusCheckApp: "github-actions",
    statusCheckAppId: (.required_status_checks.checks[0].app_id // null),
    requiredApprovingReviewCount:
      .required_pull_request_reviews.required_approving_review_count,
    dismissStaleReviews:
      .required_pull_request_reviews.dismiss_stale_reviews,
    requireLastPushApproval:
      .required_pull_request_reviews.require_last_push_approval,
    requireCodeOwnerReviews:
      .required_pull_request_reviews.require_code_owner_reviews,
    enforceAdmins: .enforce_admins.enabled,
    requireLinearHistory: .required_linear_history.enabled,
    requireConversationResolution:
      .required_conversation_resolution.enabled,
    allowBypass: false,
    allowForcePushes: .allow_force_pushes.enabled,
    allowDeletions: .allow_deletions.enabled,
    checks: [.required_status_checks.checks[] | {
      context: .context,
      appId: .app_id
    }]
  }
')"

expected_merge_policy="$(jq --compact-output '.mergePolicy' "${contract_path}")"
actual_merge_policy="$(printf '%s' "${repository_settings}" | jq --compact-output '
  {
    allowMergeCommits: .allow_merge_commit,
    allowSquashMerge: .allow_squash_merge,
    allowRebaseMerge: .allow_rebase_merge,
    allowAutoMerge: .allow_auto_merge,
    deleteHeadBranches: .delete_branch_on_merge
  }
')"

if [[ "${actual_protection}" != "${expected_protection}" ]]; then
  printf 'Branch protection does not match the versioned contract.\n' >&2
  printf 'Expected: %s\nActual:   %s\n' \
    "${expected_protection}" "${actual_protection}" >&2
  exit 1
fi

if [[ "${actual_merge_policy}" != "${expected_merge_policy}" ]]; then
  printf 'Repository merge policy does not match the versioned contract.\n' >&2
  printf 'Expected: %s\nActual:   %s\n' \
    "${expected_merge_policy}" "${actual_merge_policy}" >&2
  exit 1
fi

printf 'Pull request branch protection and merge policy match the versioned contract.\n'
