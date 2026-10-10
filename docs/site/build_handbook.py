#!/usr/bin/env python3
"""Renders the DFramework Handbook: docs/USER_GUIDE.md and docs/TECHNICAL_MANUAL.md as two
linked HTML pages (index.html and technical-manual.html), ready to publish.

    python3 docs/site/build_handbook.py            # writes build/handbook/

The Markdown in docs/ is the source of truth; these pages are generated from it.

Needs the Python "Markdown" package (3.10.3). If it isn't installed, the script downloads
that exact release from PyPI, checks its SHA-256, and unpacks it under build/handbook/,
so no pip or virtual environment is required.
"""
import argparse
import hashlib
import html
import pathlib
import re
import subprocess
import sys
import urllib.request
import zipfile

REPO = pathlib.Path(__file__).resolve().parents[2]
MARKDOWN_VERSION = "3.10.3"
MARKDOWN_WHEEL = ("https://files.pythonhosted.org/packages/64/69/4a5af2bc115a9a33fefe51709749de8262be3f9ba063d1753a837cdbc49c/"
                  "markdown-3.10.3-py3-none-any.whl")
MARKDOWN_SHA256 = "fa6c92a00a4a3c98b22728c64a935ae1928250ae65058a6ded814d2cc29a4cea"


def load_markdown(cache):
    """Imports Markdown, fetching the pinned, checksum-verified wheel into `cache` if needed."""
    try:
        import markdown
        return markdown
    except ImportError:
        pass
    lib = cache / f"markdown-{MARKDOWN_VERSION}"
    if not (lib / "markdown").is_dir():
        cache.mkdir(parents=True, exist_ok=True)
        wheel = cache / f"markdown-{MARKDOWN_VERSION}.whl"
        print(f"Downloading Markdown {MARKDOWN_VERSION} from PyPI...")
        data = urllib.request.urlopen(MARKDOWN_WHEEL, timeout=60).read()
        digest = hashlib.sha256(data).hexdigest()
        if digest != MARKDOWN_SHA256:
            sys.exit(f"Checksum mismatch for the Markdown wheel: got {digest}, expected {MARKDOWN_SHA256}")
        wheel.write_bytes(data)
        with zipfile.ZipFile(wheel) as zf:
            zf.extractall(lib)
    sys.path.insert(0, str(lib))
    import markdown
    return markdown


def current_commit():
    try:
        return subprocess.run(["git", "rev-parse", "--short", "HEAD"], cwd=REPO, capture_output=True,
                              text=True, check=True).stdout.strip()
    except (OSError, subprocess.CalledProcessError):
        return "unknown"


parser = argparse.ArgumentParser(description="Render the DFramework Handbook pages from docs/*.md.")
parser.add_argument("--out", type=pathlib.Path, default=REPO / "build" / "handbook",
                    help="output directory (default: build/handbook)")
args = parser.parse_args()
out = args.out
out.mkdir(parents=True, exist_ok=True)
markdown = load_markdown(REPO / "build" / "handbook-tools")
repo = REPO
commit = current_commit()

DOCS = [  # (source, output, sidebar name, page title)
    ("docs/USER_GUIDE.md", "index.html", "User Guide", "DFramework User Guide"),
    ("docs/TECHNICAL_MANUAL.md", "technical-manual.html", "Technical Manual", "DFramework Technical Manual"),
]
LINKS = {"USER_GUIDE.md": "index.html", "TECHNICAL_MANUAL.md": "technical-manual.html"}

def github_slug(value, separator="-"):
    # Same rule GitHub uses, so the documents' own #anchor links keep working.
    value = re.sub(r"[^\w\- ]", "", value.strip().lower())
    return value.replace(" ", separator)

CSS = r"""
:root {
  /* Layout: a DOutlookBar-style sidebar (open document on top, the other at the bottom) beside a single reading column. */
  --paper: #f7f8fa; --ink: #1b1f27; --muted: #5a6371; --rule: #dde1e7;
  --accent: #2563c9; --accent-soft: #e3ecfb; --code-bg: #eef1f5; --code-ink: #24292f;
  --bar: #e4e1d6; --bar-header: #ece9d8; --bar-header-ink: #2b2a25; --bar-ink: #3b3a33; --bar-hover: #d6d2c3;
  --kw: #a3226d; --str: #1f7a45; --com: #6b7280; --num: #b4530a; --type: #1d5bbf; --ann: #7c3aed;
  --display: "Archivo", "Helvetica Neue", Arial, sans-serif;
  --body: "Atkinson Hyperlegible", "Segoe UI", Roboto, sans-serif;
  --mono: "JetBrains Mono", ui-monospace, "SFMono-Regular", Menlo, monospace;
}
@media (prefers-color-scheme: dark) {
  :root:not([data-theme="light"]) {
    color-scheme: dark;
    --paper: #15181e; --ink: #e5e8ee; --muted: #9aa3b1; --rule: #2a2f38;
    --accent: #7aa7ff; --accent-soft: #1e2a40; --code-bg: #1d2129; --code-ink: #dfe3ea;
    --bar: #1c1f26; --bar-header: #2b2c2a; --bar-header-ink: #e9e6da; --bar-ink: #c9ccd3; --bar-hover: #2a2e37;
    --kw: #f472b6; --str: #6ee7a0; --com: #8b93a1; --num: #fdba74; --type: #93c5fd; --ann: #c4b5fd;
  }
}
:root[data-theme="dark"] {
  color-scheme: dark;
  --paper: #15181e; --ink: #e5e8ee; --muted: #9aa3b1; --rule: #2a2f38;
  --accent: #7aa7ff; --accent-soft: #1e2a40; --code-bg: #1d2129; --code-ink: #dfe3ea;
  --bar: #1c1f26; --bar-header: #2b2c2a; --bar-header-ink: #e9e6da; --bar-ink: #c9ccd3; --bar-hover: #2a2e37;
  --kw: #f472b6; --str: #6ee7a0; --com: #8b93a1; --num: #fdba74; --type: #93c5fd; --ann: #c4b5fd;
}
* { box-sizing: border-box; }
html, body { background: var(--paper); }
body { color: var(--ink); font: 17px/1.65 var(--body); margin: 0; }
.shell { display: grid; grid-template-columns: 270px minmax(0, 1fr); min-height: 100vh; }

/* Sidebar, modelled on DOutlookBar */
.bar { background: var(--bar); border-right: 1px solid var(--rule); position: sticky; top: env(safe-area-inset-top, 0px);
  height: 100vh; display: flex; flex-direction: column; font-size: 14px; }
.bar-header { display: block; padding: 9px 16px; background: var(--bar-header); color: var(--bar-header-ink);
  border-bottom: 1px solid var(--rule); font: 600 13px/1.3 var(--display); letter-spacing: .04em; text-transform: uppercase;
  text-decoration: none; text-align: center; }
a.bar-header:hover { background: var(--bar-hover); }
.bar-header.open { border-top: 0; }
.bar-items { flex: 1; overflow-y: auto; padding-block: 10px; }
.bar-items a { display: block; padding: 5px 16px 5px 18px; color: var(--bar-ink); text-decoration: none; border-left: 3px solid transparent; line-height: 1.35; }
.bar-items a:hover { background: var(--bar-hover); border-left-color: var(--accent); }
.bar-foot { border-top: 1px solid var(--rule); }
.bar-meta { padding: 10px 16px; color: var(--muted); font: 12px/1.5 var(--mono); }

/* Reading column */
main { padding: 48px 56px 96px; max-width: 900px; }
.masthead { border-bottom: 1px solid var(--rule); padding-bottom: 20px; margin-bottom: 8px; }
.eyebrow { font: 600 12px/1 var(--display); letter-spacing: .12em; text-transform: uppercase; color: var(--accent); }
.provenance { color: var(--muted); font: 13px/1.5 var(--mono); margin-top: 10px; }
h1, h2, h3, h4 { font-family: var(--display); line-height: 1.2; text-wrap: balance; color: var(--ink); }
h1 { font-size: 2.6rem; font-weight: 800; letter-spacing: -.02em; margin: .3em 0 .2em; }
h2 { font-size: 1.65rem; font-weight: 700; margin: 2.4em 0 .6em; padding-top: .4em; border-top: 1px solid var(--rule); scroll-margin-top: 16px; }
h3 { font-size: 1.22rem; font-weight: 650; margin: 1.8em 0 .5em; scroll-margin-top: 16px; }
h4 { font-size: 1.02rem; margin: 1.4em 0 .4em; }
p, li { max-width: 68ch; }
a { color: var(--accent); text-underline-offset: 2px; }
a:focus-visible, .bar a:focus-visible { outline: 2px solid var(--accent); outline-offset: 2px; }
hr { border: 0; border-top: 1px solid var(--rule); margin: 2.5em 0; }
blockquote { margin: 1.4em 0; padding: 12px 18px; background: var(--accent-soft); border-radius: 6px; color: var(--ink); }
blockquote p { margin: .3em 0; }
code { font: .86em/1.5 var(--mono); background: var(--code-bg); color: var(--code-ink); padding: .12em .38em; border-radius: 4px; }
pre { background: var(--code-bg); border: 1px solid var(--rule); border-radius: 8px; padding: 16px 18px; overflow-x: auto; margin: 1.2em 0; }
pre code { background: none; padding: 0; font-size: 14px; line-height: 1.6; }
kbd { font: 600 .8em/1 var(--mono); padding: .25em .5em; border: 1px solid var(--rule); border-bottom-width: 2px; border-radius: 5px; background: var(--paper); }
.table-wrap { overflow-x: auto; margin: 1.2em 0; }
table { border-collapse: collapse; font-size: 15px; min-width: 60%; }
th, td { text-align: left; vertical-align: top; padding: 8px 14px 8px 0; border-bottom: 1px solid var(--rule); }
th { font: 600 13px/1.4 var(--display); letter-spacing: .03em; text-transform: uppercase; color: var(--muted); }
td code { white-space: nowrap; }
pre.mermaid { background: var(--paper); text-align: center; }
.page-foot { margin-top: 4em; padding-top: 16px; border-top: 1px solid var(--rule); color: var(--muted); font-size: 14px; }

/* highlight.js tokens, coloured from the theme */
.hljs-keyword, .hljs-literal, .hljs-built_in { color: var(--kw); }
.hljs-string { color: var(--str); }
.hljs-comment { color: var(--com); font-style: italic; }
.hljs-number { color: var(--num); }
.hljs-title, .hljs-type, .hljs-title.class_ { color: var(--type); }
.hljs-meta { color: var(--ann); }

@media (max-width: 860px) {
  .shell { display: block; }
  .bar { position: static; height: auto; border-right: 0; border-bottom: 1px solid var(--rule); }
  .bar-items { max-height: 40vh; }
  main { padding: 28px 16px 64px; }
  h1 { font-size: 2rem; }
}
@media (prefers-reduced-motion: reduce) { html { scroll-behavior: auto; } }
@media (prefers-reduced-motion: no-preference) { html { scroll-behavior: smooth; } }
"""

FONTS = ('<link rel="preconnect" href="https://fonts.googleapis.com">'
         '<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>'
         '<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Archivo:wght@500;600;700;800'
         '&family=Atkinson+Hyperlegible:ital,wght@0,400;0,700;1,400&family=JetBrains+Mono:wght@400;600&display=swap">')
HLJS = ('<script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/highlight.min.js"></script>'
        '<script>document.querySelectorAll("pre code[class*=language-]").forEach(function (el) {'
        ' if (!el.className.includes("mermaid")) { try { hljs.highlightElement(el); } catch (e) {} } });</script>')

def render(src):
    md = markdown.Markdown(extensions=["tables", "fenced_code", "toc"],
                           extension_configs={"toc": {"slugify": github_slug}})
    body = md.convert(src)
    # Mermaid blocks render natively in the viewer from <pre class="mermaid">.
    body = re.sub(r'<pre><code class="language-mermaid">(.*?)</code></pre>',
                  lambda m: '<pre class="mermaid">' + m.group(1) + '</pre>', body, flags=re.S)
    body = body.replace("<table>", '<div class="table-wrap"><table>').replace("</table>", "</table></div>")
    for md_name, page in LINKS.items():
        body = body.replace(f'href="{md_name}', f'href="{page}')
    body = re.sub(r'<a href="(https?://[^"]+)"', r'<a href="\1" target="_blank" rel="noopener"', body)
    h2s = [(t["id"], t["name"]) for t in md.toc_tokens[0]["children"]] if md.toc_tokens else []
    return body, h2s

rendered = {}
for source, page, name, title in DOCS:
    rendered[page] = render(pathlib.Path(repo, source).read_text())

for source, page, name, title in DOCS:
    body, h2s = rendered[page]
    # Drop the document's own H1 and contents list: the masthead and sidebar replace them.
    body = re.sub(r"^<h1[^>]*>.*?</h1>", "", body, count=1, flags=re.S)
    body = re.sub(r'<h2 id="contents">Contents</h2>\s*<ol>.*?</ol>\s*<hr />', "", body, count=1, flags=re.S)
    headers_above = [d for d in DOCS[: DOCS.index((source, page, name, title)) + 1]]
    headers_below = [d for d in DOCS[DOCS.index((source, page, name, title)) + 1:]]
    def header(d, is_open):
        cls = "bar-header open" if is_open else "bar-header"
        if is_open:
            return f'<div class="{cls}" aria-current="page">{html.escape(d[2])}</div>'
        return f'<a class="{cls}" href="{d[1]}">{html.escape(d[2])}</a>'
    items = "".join(f'<a href="#{i}">{html.escape(n)}</a>' for i, n in h2s if i != "contents")
    bar = ('<nav class="bar" aria-label="Handbook">'
           + "".join(header(d, d[1] == page) for d in headers_above)
           + f'<div class="bar-items">{items}</div>'
           + '<div class="bar-foot">' + "".join(header(d, False) for d in headers_below)
           + f'<div class="bar-meta">dframework-swing 4.0.0-SNAPSHOT<br>Java 21 · FlatLaf 3.7.2<br>commit {html.escape(commit)}</div></div>'
           + "</nav>")
    subtitle = ("How to use every component, with examples that compile." if page == "index.html"
                else "How DFramework is built, and why.")
    masthead = (f'<header class="masthead"><div class="eyebrow">DFramework · since 2001</div>'
                f'<h1>{html.escape(name)}</h1><p>{subtitle}</p>'
                f'<div class="provenance">Rendered from {html.escape(source)} at commit {html.escape(commit)}</div></header>')
    foot = ('<footer class="page-foot">Source: <a href="https://github.com/dalixinc/dframework" target="_blank" '
            f'rel="noopener">github.com/dalixinc/dframework</a> · {html.escape(source)}</footer>')
    content = (f"<title>{'DFramework Handbook' if page == 'index.html' else html.escape(title)}</title>"
               f"<style>{CSS}</style>{FONTS}"
               f'<div class="shell">{bar}<main id="top">{masthead}{body}{foot}</main></div>{HLJS}')
    if page != "index.html":
        # Supporting pages are served without the publish skeleton, so they carry their own.
        content = ('<!doctype html><html lang="en"><head><meta charset="utf-8">'
                   '<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">'
                   '<style>:root{color-scheme:light;padding-top:env(safe-area-inset-top,0px);'
                   'padding-bottom:env(safe-area-inset-bottom,0px)}body{margin:0}img{max-width:100%}'
                   '[hidden]{display:none!important}</style></head><body>' + content + '</body></html>')
    pathlib.Path(out, page).write_text(content)
    print(f"{out / page}: {len(content):,} bytes, {len(h2s)} chapters")
