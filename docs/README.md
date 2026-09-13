# CookSync documentation

Everything written about CookSync, in one place. This folder doubles as the
source of the project's public page at
**<https://yaronserlin.github.io/cook-sync/>** — files here are both browsable in
the repository and served on the web.

## For users

| Document | What it covers |
|---|---|
| [User guide](user-guide.md) ([live](https://yaronserlin.github.io/cook-sync/user-guide.html)) | Using the app, screen by screen — accounts, search, Cooking Mode, publishing a recipe, settings, admin console |
| [Privacy policy](privacy.html) ([live](https://yaronserlin.github.io/cook-sync/privacy.html)) | What the app collects and the choices users have |
| [Terms of use](terms.html) ([live](https://yaronserlin.github.io/cook-sync/terms.html)) | Terms accepted by using the app |

## For readers of the project

| Document | What it covers |
|---|---|
| [Functional specification](functional-spec.md) ([live](https://yaronserlin.github.io/cook-sync/functional-spec.html)) | Actors, functional areas, domain model, business rules, external services, API surface |
| [Main README](../README.md) | What the project is, how the three modules fit together, how to build and run everything |
| [Client README](../cook-sync-client/README.md) | Android app — tech stack, package layout, configuration, release signing, tests |
| [Server README](../cook-sync-server/README.md) | Spring Boot API — package layout, per-endpoint API table, environment variables, profiles, Docker, health check |
| [Shared DTOs README](../cooksync-DTOs/README.md) | The request/response contract shared by client and server |

## Files in this folder

| Path | Purpose |
|---|---|
| `index.html` | The public project page (the GitHub Pages entry point) |
| `privacy.html`, `terms.html` | Legal documents, also opened from inside the app |
| `user-guide.md`, `functional-spec.md` | The two documents above |
| `media/` | Screenshots, demo recordings and the APK QR code — see [`media/README.md`](media/README.md) |
| `_config.yml`, `_layouts/`, `assets/` | The site itself — see [How the site is built](#how-the-site-is-built) |
| `logo.png` | App logo used by the project page |
| `google*.html` | Google Search Console site-verification file — do not remove |

## How the site is built

`_config.yml` and `_layouts/` turn this folder into the project site. Jekyll —
which GitHub Pages runs for us, with no build step to trigger by hand — renders
`user-guide.md` and `functional-spec.md` in place, so each document is **one
file** that reads well in the repository and is served as a real page with
navigation and a generated "on this page" index. There is no second
copy to keep in sync.

| Path | Purpose |
|---|---|
| `_config.yml` | Site metadata, the APK URL used by the layouts, and the excludes that keep this file out of the site |
| `_layouts/default.html` | Header, navigation and footer shared by every page |
| `_layouts/doc.html` | The document shell: title block, sidebar index, heading anchors, scrollable tables |
| `assets/site.css` | All styling, using the app's own colour tokens and typefaces |

To edit a document, edit its `.md` file — nothing else. To preview the site
locally:

```bash
gem install jekyll
cd docs && jekyll serve      # http://127.0.0.1:4000/cook-sync/
```

Two things to keep in mind when editing:

- **Keep the front matter block** at the top of each `.md` file. Jekyll only
  renders files that have one; without it the document would be served as a raw
  download instead of a page.
- **Links to files outside `docs/`** (the module READMEs, for example) must be
  absolute GitHub URLs. Relative `../` paths resolve in the repository but point
  off the end of the site.

## A note on formats

The two long-form documents are kept as **Markdown**: they render as real pages
on the site, they render in the repository too, they are searchable, and changes
to them show up as readable diffs in the history. If a PDF is needed for a formal
submission, generate it from the Markdown and place it in `pdf/` — keep the
Markdown as the source of truth rather than the other way around.

Editable originals (`.docx` and similar) do not belong in this folder: GitHub
cannot preview them, they produce no meaningful diffs, and they grow the
repository with every revision. If they must be kept, put them in `docs/source/`
and say plainly that they are archival copies.
