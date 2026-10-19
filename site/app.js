// ZeroOS site: theme switch, the download gate and the latest release per channel. All text comes from
// strings/en.json through t(); this file has no user-facing text of its own.
(function () {
  "use strict";

  const data = JSON.parse(document.getElementById("site-data").textContent);
  const strings = data.strings;
  const config = data.config;
  const THEME_KEY = "zeroos-theme";

  /** The string for [key], with {name} placeholders filled from [values]. */
  function t(key, values) {
    let text = strings[key];
    if (text === undefined) throw new Error("missing string: " + key);
    Object.keys(values || {}).forEach(function (name) {
      text = text.split("{" + name + "}").join(String(values[name]));
    });
    return text;
  }

  function element(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined) node.textContent = text;
    return node;
  }

  /** Only links to GitHub are followed, whatever the API returns. */
  function githubUrl(url) {
    return typeof url === "string" && url.indexOf("https://github.com/") === 0 ? url : null;
  }

  // Theme: follows the phone or computer until switched; the choice is remembered on this browser only.
  const root = document.documentElement;
  const themeButton = document.getElementById("theme");
  const themeLabel = document.getElementById("theme-label");
  const prefersLight = window.matchMedia("(prefers-color-scheme: light)");

  function currentTheme() {
    return root.dataset.theme || (prefersLight.matches ? "light" : "dark");
  }

  function showTheme() {
    themeLabel.textContent = t(currentTheme() === "light" ? "theme_light" : "theme_dark");
  }

  try {
    const saved = localStorage.getItem(THEME_KEY);
    if (saved === "light" || saved === "dark") root.dataset.theme = saved;
  } catch (e) {
    // Storage blocked: follow the system theme.
  }
  showTheme();
  prefersLight.addEventListener("change", showTheme);
  themeButton.addEventListener("click", function () {
    root.dataset.theme = currentTheme() === "light" ? "dark" : "light";
    try {
      localStorage.setItem(THEME_KEY, root.dataset.theme);
    } catch (e) {
      // Not remembered; still switched for this visit.
    }
    showTheme();
  });

  // Download gate: the download buttons appear only after the disclaimer is accepted, on every visit.
  const accept = document.getElementById("accept");
  const channels = document.getElementById("channels");
  const lockedNote = document.getElementById("locked-note");

  function syncGate() {
    channels.dataset.locked = String(!accept.checked);
    lockedNote.hidden = accept.checked;
  }

  accept.addEventListener("change", syncGate);
  syncGate();

  // Releases: the newest published release of each channel, from GitHub's API (no key, no cookies).
  function channelOf(release) {
    return config.channels.find(function (channel) {
      const typeMatches = channel.prerelease === undefined || channel.prerelease === release.prerelease;
      return typeMatches && new RegExp(channel.tagPattern).test(release.tag_name);
    });
  }

  function latestPerChannel(releases) {
    const latest = {};
    releases
      .filter(function (release) { return !release.draft; })
      .forEach(function (release) {
        const channel = channelOf(release);
        if (!channel) return;
        const known = latest[channel.id];
        if (!known || Date.parse(release.published_at) > Date.parse(known.published_at)) latest[channel.id] = release;
      });
    return latest;
  }

  function card(id) {
    const node = channels.querySelector('[data-channel="' + id + '"]');
    return {
      node: node,
      version: node.querySelector('[data-role="version"]'),
      meta: node.querySelector('[data-role="meta"]'),
      actions: node.querySelector('[data-role="actions"]')
    };
  }

  function showRelease(id, release) {
    const parts = card(id);
    parts.node.dataset.state = "released";
    parts.version.textContent = t("release_version", { version: release.tag_name.replace(/^v/, "") });
    const date = new Date(release.published_at).toLocaleDateString(root.lang, { year: "numeric", month: "long", day: "numeric" });
    parts.meta.textContent = t("release_date", { date: date });
    parts.actions.replaceChildren();

    const apk = (release.assets || []).find(function (asset) { return asset.name.endsWith(config.apkSuffix); });
    const apkUrl = apk && githubUrl(apk.browser_download_url);
    if (apkUrl) {
      const download = element("a", "button", t("release_download"));
      download.href = apkUrl;
      parts.actions.append(download, element("span", "channel-meta", t("release_size", { size: (apk.size / 1e6).toFixed(1) })));
    } else {
      parts.actions.append(element("p", "channel-meta", t("release_no_apk")));
    }
    const pageUrl = githubUrl(release.html_url);
    if (pageUrl) {
      const notes = element("a", "link", t("release_notes"));
      notes.href = pageUrl;
      parts.actions.append(notes);
    }
  }

  function showNone(id) {
    const parts = card(id);
    parts.node.dataset.state = "none";
    parts.version.textContent = "";
    parts.meta.textContent = t("channel_none");
  }

  function render(releases) {
    const latest = latestPerChannel(releases);
    config.channels.forEach(function (channel) {
      const retired = channel.retireWhen && latest[channel.retireWhen];
      card(channel.id).node.hidden = Boolean(retired);
      if (latest[channel.id]) showRelease(channel.id, latest[channel.id]);
      else showNone(channel.id);
    });
  }

  function showError() {
    config.channels.forEach(function (channel) {
      const parts = card(channel.id);
      parts.node.dataset.state = "error";
      parts.meta.textContent = t("releases_error");
    });
  }

  /** Local previews may pass ?releases=<file>.json from the same folder instead of calling GitHub. */
  function releasesUrl() {
    const preview = new URLSearchParams(location.search).get("releases");
    return preview && /^[\w.-]+\.json$/.test(preview) ? preview : config.releasesApi;
  }

  fetch(releasesUrl(), { headers: { Accept: "application/vnd.github+json" } })
    .then(function (response) {
      if (!response.ok) throw new Error("HTTP " + response.status);
      return response.json();
    })
    .then(function (releases) {
      if (!Array.isArray(releases)) throw new Error("unexpected response");
      render(releases);
    })
    .catch(showError);
})();
