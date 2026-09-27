# Working on AmritaCafe — read this first

## Always bump the version before pushing to master

`app/build.gradle` has:

```
versionCode 80110
versionName "8.0"
```

**Every push to `master` that changes app code (anything under `app/src/`) must
bump `versionCode` by at least 1 first.** Bump `versionName` too when it's a
meaningful release, not just a hotfix.

### Why this matters

`.github/workflows/release.yml` triggers on every push to `master`. It reads
`versionCode` from `app/build.gradle` and uses it as the **release tag**:

```yaml
tag_name: ${{ steps.version_info.outputs.VERSION_CODE }}
```

If `versionCode` is unchanged from the last push, the workflow creates a
release for a tag that **already exists**. `softprops/action-gh-release`
doesn't fail on that — it silently **overwrites the existing release's APK
asset in place**. The result:

- The old beta build's download link now serves different code than what
  people think they downloaded.
- The in-app update checker (`UpdateChecker.kt`) compares `versionCode` to
  decide if a build is "newer." If it hasn't changed, tablets/phones running
  the app will **never detect this as an update** — "Force Check for
  Updates" will report "You're up to date!" even though the code changed.
- There's no new tagged release to point people at, and no historical record
  of what shipped when — the previous tag's release notes/APK are just gone.

This has already happened once (commits `c926c9b`/`8db4849`/`aad024c` all
shipped under the same `80110` tag on 2026-09-27) — don't repeat it.

### The rule

Before committing any change under `app/src/`:

1. Open `app/build.gradle`.
2. Increment `versionCode` by 1 (e.g. `80110` → `80111`).
3. Update `versionName` if appropriate.
4. Commit the version bump together with (or immediately before) the code
   change, then push to `master`.

Changes that don't touch app code (docs, this file, `.gs` Google Apps
Script files, workflow config) don't need a version bump.

## Other standing instructions for this repo

- Commit and push directly to `master`. Don't create feature branches or
  PRs unless explicitly asked — this repo's owner has asked for direct
  commits to `master` going forward.
- There's no Android SDK in the Claude Code cloud sandbox, so a local
  Gradle build isn't possible here. After pushing, check the GitHub
  Actions run for `.github/workflows/release.yml` on `master` to confirm
  it actually compiled — don't assume success just because the push
  succeeded.
