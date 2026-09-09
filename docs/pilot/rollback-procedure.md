# OpenAPI Pilot Recovery

Published Maven coordinates are immutable. Recovery never deletes or overwrites
a coordinate automatically because a three-artifact upload is not atomic and
consumers may already have cached any successful artifact.

## Before Publication

`checkPilotPublicationState` examines every file declared by the validated
manifest:

- `absent` permits publication;
- `identical` makes a retry idempotent and skips upload;
- `partial` blocks publication because only part of the release exists;
- `conflict` blocks publication because remote content differs.

Authentication failures and unexpected repository responses also stop the job.

## After A Failed Publication

1. Preserve the workflow manifest, remote-state report, and recovery artifact.
2. Inspect the registry for each model, API, and client coordinate in the
   manifest.
3. If all files exist and match, rerun the pilot; it classifies them as
   `identical` and performs verification without another upload.
4. If the state is partial or conflicting, quarantine that contract version.
   Do not remove or replace its files.
5. Correct the source or operational fault, increment `info.version` with an
   appropriate patch or higher SemVer change, add the new baseline, and run the
   full pilot again.

The last known good coordinates remain available throughout recovery. Service
applications continue using their pinned previous version until a new pilot
receipt is verified. Registry-side retention, environment approvals, and a real
failure/retry exercise require external validation in the protected `release`
environment.
