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
- **Build locally before pushing app code changes.** The Claude Code cloud
  sandbox has no Android SDK preinstalled, but you can set one up in about
  a minute (network access to `dl.google.com` works fine) and it will
  catch real compile errors — including ones a plain Kotlin-only check
  misses, like ViewBinding fields going nullable because a layout ID only
  exists in `res/layout/` and not in `res/layout-land/` (this bit us once,
  see commit `aad024c`). Setup:

  ```bash
  mkdir -p /home/user/android-sdk/cmdline-tools
  cd /home/user/android-sdk/cmdline-tools
  curl -sS -o /tmp/cmdline-tools.zip \
    https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
  unzip -q /tmp/cmdline-tools.zip && mv cmdline-tools latest && rm /tmp/cmdline-tools.zip

  export ANDROID_HOME=/home/user/android-sdk
  yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$ANDROID_HOME" \
    "platform-tools" "platforms;android-37.0" "build-tools;36.0.0"
  yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$ANDROID_HOME" --licenses

  echo "sdk.dir=$ANDROID_HOME" >> /home/user/AmritaCafe/local.properties
  ```

  Match `platforms;android-<N>.0` and `build-tools;<N>` to whatever
  `compileSdk` currently is in `app/build.gradle` — check with
  `grep compileSdk app/build.gradle` first, since it changes over time and
  `sdkmanager`'s package name isn't always the plain number (e.g.
  `compileSdk 37` needed `platforms;android-37.0`, not `;android-37`; run
  `sdkmanager --list | grep platforms` if the exact name isn't obvious).

  Then before pushing: `cd /home/user/AmritaCafe && export ANDROID_HOME=/home/user/android-sdk
  && ./gradlew :app:assembleDebug --stacktrace`. This container is
  ephemeral — a new session starts with no SDK, so this setup doesn't
  carry forward automatically and needs redoing each session (fast after
  the first `sdkmanager` download, since Gradle/dependency caches under
  `~/.gradle` may or may not persist depending on the session).
- Even with a local build passing, still check the GitHub Actions run for
  `.github/workflows/release.yml` on `master` after pushing — CI is the
  source of truth for what actually shipped, and it also runs the release
  step (tagging, APK upload) that a local build doesn't.
