# ZeroOS website

The one-page site for GitHub Pages: a taste of the app, its features, the disclaimer, and the latest release of
each channel from GitHub Releases. It follows the launcher's look: black and white, Doto, dots instead of lines.

## Rules
- **No hardcoded text.** Every word people read is in [strings/en.json](strings/en.json). `index.html` only names
  keys (`data-t`, `data-t-attr`); `app.js` only uses `t("key")`. `./gradlew check` runs `checkSite`, which fails on
  text written into the HTML, JS or CSS, on missing strings and on unused ones.
- **The APKs are never hosted here.** Download buttons link to the files on GitHub Releases (LICENSE §2).
- **No trackers, no third-party requests** other than GitHub's releases API. The font is served from the site.
- **Screens come from the app's own screenshot tests**, listed in [config.json](config.json), so the site shows the
  app as it is. Pick screenshots with sample data only.
- **No personal data** (D-048): no phone model, real usage or the owner's apps.

## Build and preview
```
python3 scripts/build-site.py              # builds into build/site/
python3 scripts/build-site.py --check      # checks only
cd build/site && python3 -m http.server     # open http://localhost:8000
```
While the repo is private, GitHub's API doesn't list its releases, so the release cards say they couldn't be loaded
and link to the releases page. For a local preview, save a releases list as `build/site/<name>.json` and open
`index.html?releases=<name>.json`.

## Release channels
`config.json` → `channels`, checked in order; the newest published release of each is shown:

| Channel | GitHub release | Tag |
|---|---|---|
| Stable | not a pre-release | `v1.0.0` and later, no suffix |
| Beta | marked **pre-release** | `v1.1.0-beta.1`, `v1.1.0-rc.1` |
| Early builds | any | `v0.x.y`; hidden once a Stable release exists |

A channel with no release says "Not released yet", so Stable and Beta appear by themselves when the first one is
published. Drafts are never shown.

## Publishing
Live at https://iamabhinav-dev.github.io/ZeroOS/ (D-058). [.github/workflows/site.yml](../.github/workflows/site.yml)
checks for hardcoded text, builds the site and deploys `build/site/` whenever the site, or the screens and font it
shows, change on `main` (or when run by hand from the Actions tab). Settings → Pages → Source is "GitHub Actions";
the workflow fails at startup if Pages is switched off.
