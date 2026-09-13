# CookSync documentation

Everything written about CookSync, in one place. This folder doubles as the
source of the project's public page at
**<https://yaronserlin.github.io/cook-sync/>** — files here are both browsable in
the repository and served on the web.

## For users

| Document | What it covers |
|---|---|
| [User guide](user-guide.md) | Using the app, screen by screen — accounts, search, Cooking Mode, publishing a recipe, settings, admin console |
| [Privacy policy](privacy.html) ([live](https://yaronserlin.github.io/cook-sync/privacy.html)) | What the app collects and the choices users have |
| [Terms of use](terms.html) ([live](https://yaronserlin.github.io/cook-sync/terms.html)) | Terms accepted by using the app |

## For readers of the project

| Document | What it covers |
|---|---|
| [Functional specification](functional-spec.md) | Actors, functional areas, domain model, business rules, external services, API surface |
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
| `logo.png` | App logo used by the project page |
| `google*.html` | Google Search Console site-verification file — do not remove |

## A note on formats

The two long-form documents are kept as **Markdown**: they render directly in the
browser on GitHub with no download, they are searchable, and changes to them show
up as readable diffs in the history. If a PDF is needed for a formal submission,
generate it from the Markdown and place it in `pdf/` — keep the Markdown as the
source of truth rather than the other way around.

Editable originals (`.docx` and similar) do not belong in this folder: GitHub
cannot preview them, they produce no meaningful diffs, and they grow the
repository with every revision. If they must be kept, put them in `docs/source/`
and say plainly that they are archival copies.

GitHub Pages runs Jekyll over this folder, so the `.md` files are also converted
to plain HTML pages at their public URLs. That conversion is incidental — these
documents are written to be read in the repository.
