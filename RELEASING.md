# Releasing HappyCamper

Installers are built automatically by GitHub Actions (`.github/workflows/release.yml`) — never built or committed locally. The workflow runs `jpackage` on a macOS (ARM + Intel), Windows, and Linux runner, then attaches the four installers to a GitHub Release.

## Cutting a release

1. **Bring in changes** (if any): cherry-pick commits from the development repo onto `main`.

   ```sh
   git cherry-pick <sha>
   ```

2. **Bump the version** in `pom.xml` — the single source of truth. Change `<version>X.Y.Z-SNAPSHOT</version>` to the new version, e.g. `2.3.0-SNAPSHOT`. The `-SNAPSHOT` suffix is stripped automatically by CI; the rest becomes the installer version and the in-app title. Commit the bump.

   Also update the date in the `docs/index.html` fallback line (`<p class="version">...`) — the version text itself is overwritten live by the GitHub API, but the date is static.

3. **Tag and push.** The tag *push* is what triggers the build — `git tag` alone only creates the tag locally, so the two commands don't need to happen together, but nothing builds until the push.

   ```sh
   git tag v2.3.0
   git push origin main v2.3.0   # pushes the branch and the tag in one go
   ```

4. **Wait ~10 minutes.** The Actions run creates a GitHub Release for the tag with all four installers attached. Asset filenames are intentionally versionless (`HappyCamper-windows.msi`, etc.) so the `releases/latest/download/...` links in the README and landing page always point at the newest release.

5. **Spot-check** the [Releases page](https://github.com/jaymoran103/happycamper-heart/releases): four assets present, version number correct in the installer.

## Test builds (no release)

To verify the build without publishing anything, run the workflow manually: GitHub → **Actions → Build installers → Run workflow**. Installers appear as downloadable *artifacts* on the run page; no release or tag is created.

## Notes

- **Unsigned binaries**: macOS Gatekeeper and Windows SmartScreen show a warning on first launch; the README's install steps walk users through it. Removing the warnings would require an Apple Developer account ($99/yr, notarization) and a Windows code-signing certificate.
- **Re-doing a release**: delete the release and tag on GitHub, then re-tag and push again.
- **Intel Mac builds** use the `macos-15-intel` runner; if GitHub retires that label the job will fail — drop that matrix entry from the workflow when it does.
