# Releasing HappyCamper

Installers are built automatically by GitHub Actions (`.github/workflows/release.yml`) — never built or committed locally. The workflow runs `jpackage` on a macOS (ARM + Intel), Windows, and Linux runner, then attaches the four installers to a GitHub Release.

## Cutting a release

1. **Confirm changes are on `main`.** Releasable work lands via merged PRs — there is no cherry-picking step. Make sure `main` is up to date and the changes you intend to ship are merged.

2. **Pick the version.** Decide patch (`2.3.x`, bug fixes / small tweaks) vs minor (`2.4.0`, notable new features). This choice drives the what's-new update in step 4.

3. **Bump the version** in `pom.xml` — the single source of truth. Change `<version>X.Y.Z-SNAPSHOT</version>` to the new version, e.g. `2.3.3-SNAPSHOT`. The `-SNAPSHOT` suffix is stripped automatically by CI; the rest becomes the installer version and the in-app title.

4. **Update `docs/whatsnew.json`** — the "what's new" panel on the landing page. It tracks the current minor version:
   - **Same minor** as the last release → keep `label` (e.g. `"v2.3"`) and **append** this release's notable, user-facing items to `items`.
   - **New minor** → set `label` to `"vX.Y"` and **replace** `items` with the new minor's notes.
   - A patch with nothing user-facing can leave it unchanged.

   The landing-page version *and date* update themselves live from the GitHub Release API — there is nothing to hand-edit for either.

5. **Run the tests.** `mvn -B test` as a sanity gate before publishing.

6. **Commit** the version bump and any `whatsnew.json` change together.

7. **Tag and push.** The tag *push* is what triggers the build — `git tag` alone only creates the tag locally, so the two commands don't need to happen together, but nothing builds until the push.

   ```sh
   git tag v2.3.3
   git push origin main v2.3.3   # pushes the branch and the tag in one go
   ```

8. **Wait ~10 minutes, then verify.** The Actions run creates a GitHub Release for the tag with all four installers attached. Confirm:
   - the [Releases page](https://github.com/jaymoran103/happycamper-heart/releases) shows four assets and the correct installer version, and
   - the [landing page](https://jaymoran103.github.io/happycamper-heart/docs/) shows the new version, today's date, and the updated what's-new list.

   Asset filenames are intentionally versionless (`HappyCamper-windows.msi`, etc.) so the `releases/latest/download/...` links in the README and landing page always point at the newest release.

## Test builds (no release)

To verify the build without publishing anything, run the workflow manually: GitHub → **Actions → Build installers → Run workflow**. Installers appear as downloadable *artifacts* on the run page; no release or tag is created.

## Notes

- **Unsigned binaries**: macOS Gatekeeper and Windows SmartScreen show a warning on first launch; the README's install steps walk users through it. Removing the warnings would require an Apple Developer account ($99/yr, notarization) and a Windows code-signing certificate.
- **Re-doing a release**: delete the release and tag on GitHub, then re-tag and push again.
- **Intel Mac builds** use the `macos-15-intel` runner; if GitHub retires that label the job will fail — drop that matrix entry from the workflow when it does.
