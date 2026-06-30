---
name: happycamper-release
description: Use when cutting a HappyCamper release. Bumps the pom version, updates docs/whatsnew.json with the maintainer's release notes, runs the test suite, then — only after explicit approval — commits, tags, and pushes to trigger the CI installer build. Mirrors RELEASING.md.
---

# Cutting a HappyCamper Release

Execute `RELEASING.md` steps 1–8. Two moments require the human: choosing the
release notes, and approving the irreversible tag/push. Do **nothing**
irreversible (no commit, tag, or push) before the approval gate.

## 1. Preconditions
- Confirm the working repo is on `main` and up to date (`git status`, `git log --oneline -5`).
- Confirm the changes intended for this release are merged. Changes arrive via
  merged PRs — never cherry-pick.

## 2. Determine the version
- Read the current version from `pom.xml` (`<version>X.Y.Z-SNAPSHOT</version>`).
- Ask the maintainer for the new version, or propose one (patch vs minor) and
  confirm. Record `X.Y.Z` and whether the **minor** (`X.Y`) changed vs the last release.

## 3. Bump pom.xml
- Set `<version>` to `X.Y.Z-SNAPSHOT`.

## 4. Release-notes checkpoint (human input)
- Read `docs/whatsnew.json` and show its current `label` + `items`.
- Apply Model A:
  - **Same minor** as last release → keep `label`, ask the maintainer which
    notable user-facing items to **append** to `items`.
  - **New minor** → set `label` to `"vX.Y"` and ask for the new `items` list
    (replacing the old one).
  - A patch with nothing user-facing may leave the file unchanged.
- Write the agreed `whatsnew.json`. Keep it valid JSON.

## 5. Sanity test
- Run `mvn -B test`. If it fails, STOP and report — do not release.

## 6. Approval gate (irreversible — requires explicit "yes")
- Present, for review:
  - the exact new version and tag (`vX.Y.Z`),
  - `git diff` of all staged changes (pom + whatsnew),
  - the proposed commit message, and
  - the exact commands to be run:
    ```sh
    git add pom.xml docs/whatsnew.json
    git commit -m "Bump version to X.Y.Z"
    git tag vX.Y.Z
    git push origin main vX.Y.Z
    ```
- Wait for explicit approval. On anything other than a clear yes, STOP and
  leave all edits in the working tree.

## 7. Execute and hand off
- On approval, run the commands above in order.
- Then print the post-push checklist:
  - Wait ~10 minutes for the Actions build.
  - Spot-check the Releases page: four assets, correct installer version
    (https://github.com/jaymoran103/happycamper-heart/releases).
  - Verify the landing page shows the new version, today's date, and the
    updated what's-new list (https://jaymoran103.github.io/happycamper-heart/docs/).

## Boundaries
- No git or network action before the step 6 approval. Before that, only edit
  working-tree files and run tests.
- The commit message convention is `Bump version to X.Y.Z` (matches repo history).
