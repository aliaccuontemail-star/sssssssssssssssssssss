"""PinBom brand hotfix — regenerate every copy of the mark from the corrected
108-space geometry (no dots, no spark, two real counters), and rewrite the
style-guide SVGs in place.

Outputs:
  brand/pinbom-glyph.svg, brand/pinbom-mark.svg        (SVG deliverables)
  brand/style-guide.html                            (mark blocks rewritten)
  brand/sizes-strip.png, lockup-preview.png,
  brand/adaptive-preview.png, play-store-512.png    (preview rasters)
  .work/brand/*.png                                 (working copies)
"""
from pathlib import Path
import re
import numpy as np
from PIL import Image, ImageDraw, ImageFont

BRAND = Path("/home/user/persian-pintrest/brand")
WORK = Path("/home/user/persian-pintrest/brand/scratch")
FONT_DIR = Path("/home/user/persian-pintrest/app/src/main/res/font")

V = 108.0
S4 = 4                        # supersample
GLYPH_SCALE = 1.2
GX, GY = 53.0, 55.0           # glyph-box centre (x 34..72, y 28..82)
PATH = ("M38,28 H55 A13,13 0 0 1 55,54 H58 A14,14 0 0 1 58,82 H38 Q34,82 34,78 "
        "V32 Q34,28 38,28 Z M46,36 H54 A5,5 0 0 1 54,46 H46 Z "
        "M46,59 H56 A6,6 0 0 1 56,71 H46 Z")

DEFS108 = ('<defs>\n'
           ' <linearGradient id="bg" x1="0" y1="0" x2="108" y2="108" gradientUnits="userSpaceOnUse">\n'
           '   <stop offset="0" stop-color="#FF5A6A"/><stop offset="0.52" stop-color="#E60023"/>'
           '<stop offset="1" stop-color="#B3001B"/></linearGradient>\n'
           ' <radialGradient id="hi" cx="26" cy="22" r="86" gradientUnits="userSpaceOnUse">\n'
           '   <stop offset="0" stop-color="#FFFFFF" stop-opacity="0.12"/>'
           '<stop offset="1" stop-color="#FFFFFF" stop-opacity="0"/></radialGradient>\n'
           ' <linearGradient id="sh" x1="54" y1="46" x2="54" y2="108" gradientUnits="userSpaceOnUse">\n'
           '   <stop offset="0" stop-color="#000000" stop-opacity="0"/>'
           '<stop offset="1" stop-color="#000000" stop-opacity="0.12"/></linearGradient>\n'
           '</defs>')

GLYPH_SVG = ('<path d="%s" fill="#FFFFFF" fill-rule="evenodd" '
             'transform="translate(%.3f,%.3f) scale(%.4f) translate(%.0f,%.0f)"/>'
             % (PATH, 54.0 - GX * GLYPH_SCALE, 54.0 - GY * GLYPH_SCALE, GLYPH_SCALE, -GX, -GY))
SHADOW_SVG = ('<path d="%s" fill="#000000" fill-opacity="0.10" fill-rule="evenodd" '
              'transform="translate(%.3f,%.3f) scale(%.4f) translate(%.0f,%.0f)"/>'
              % (PATH, 54.0 - GX * GLYPH_SCALE, 54.0 + 2.0 - GY * GLYPH_SCALE, GLYPH_SCALE, -GX, -GY))

# ── SVG deliverables ────────────────────────────────────────────────────────
(BRAND / "pinbom-glyph.svg").write_text(
    f'<svg xmlns="http://www.w3.org/2000/svg" width="108" height="108" viewBox="0 0 108 108">'
    f'<g>{SHADOW_SVG}{GLYPH_SVG}</g></svg>', encoding="utf-8")

(BRAND / "pinbom-mark.svg").write_text(
    f'<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 108 108">{DEFS108}'
    f'<rect width="108" height="108" rx="26" fill="url(#bg)"/>'
    f'<rect width="108" height="108" rx="26" fill="url(#hi)"/>'
    f'<rect width="108" height="108" rx="26" fill="url(#sh)"/>'
    f'<g>{SHADOW_SVG}{GLYPH_SVG}</g></svg>', encoding="utf-8")
print("svg: pinbom-glyph.svg, pinbom-mark.svg written")

# ── style-guide.html: rebuild every mark svg block, drop the wordmark dot ───
html_path = BRAND / "style-guide.html"
html = html_path.read_text(encoding="utf-8")

block_re = re.compile(r'<svg ([^>]*?)viewBox="0 0 (\d+) \2"[^>]*>(.*?)</svg>', re.S)
rebuilt = 0


def rebuild(m):
    global rebuilt
    head, S, body = m.group(1), int(m.group(2)), m.group(3)
    if PATH.split()[0] not in body and "M30,31" not in body:
        return m.group(0)
    rebuilt += 1
    k = GLYPH_SCALE * S / V
    glyph = (f'<path d="{PATH}" fill="#FFFFFF" fill-rule="evenodd" '
             f'transform="translate({S/2 - GX*k:.3f},{S/2 - GY*k:.3f}) scale({k:.5f}) '
             f'translate({-GX:.0f},{-GY:.0f})"/>')
    shadow = (f'<path d="{PATH}" fill="#000000" fill-opacity="0.10" fill-rule="evenodd" '
              f'transform="translate({S/2 - GX*k:.3f},{S/2 - GY*k + 2*S/V:.3f}) scale({k:.5f}) '
              f'translate({-GX:.0f},{-GY:.0f})"/>')
    rx = f"{S*26/V:.2f}"
    defs = (f'<linearGradient id="bg{S}" x1="0" y1="0" x2="{S}" y2="{S}" gradientUnits="userSpaceOnUse">'
            f'<stop offset="0" stop-color="#FF5A6A"/><stop offset="0.52" stop-color="#E60023"/>'
            f'<stop offset="1" stop-color="#B3001B"/></linearGradient>'
            f'<radialGradient id="hi{S}" cx="{S*0.26:.2f}" cy="{S*0.22:.2f}" r="{S*0.86:.2f}" '
            f'gradientUnits="userSpaceOnUse"><stop offset="0" stop-color="#FFFFFF" stop-opacity="0.12"/>'
            f'<stop offset="1" stop-color="#FFFFFF" stop-opacity="0"/></radialGradient>')
    rounded = "circle" not in body or "rect" in body
    if rounded:
        cover = (f'<rect width="{S}" height="{S}" rx="{rx}" fill="url(#bg{S})"/>'
                 f'<rect width="{S}" height="{S}" rx="{rx}" fill="url(#hi{S})"/>')
    else:
        cover = (f'<circle cx="{S/2}" cy="{S/2}" r="{S/2}" fill="url(#bg{S})"/>'
                 f'<circle cx="{S/2}" cy="{S/2}" r="{S/2}" fill="url(#hi{S})"/>')
    return f'<svg {head}viewBox="0 0 {S} {S}">{defs}{cover}{shadow}{glyph}</svg>'


html = block_re.sub(rebuild, html)
html = re.sub(r'[ \t]*\.wordmark \.dot \{[^}]*\}\n', '', html)
html = re.sub(r'<span class="dot"></span>\s*', '', html)
html_path.write_text(html, encoding="utf-8")
print(f"style-guide.html: {rebuilt} mark blocks rebuilt; wordmark dot removed")

# ── rasters ────────────────────────────────────────────────────────────────
W = int(V * S4)


def sc(*vals):
    return [v * S4 for v in vals]


def glyph_mask(scale: float = 1.0, ty: float = 0.0) -> np.ndarray:
    img = Image.new("L", (W, W), 0)
    d = ImageDraw.Draw(img)
    d.rectangle(sc(34, 28, 55, 54), fill=255)
    d.ellipse(sc(42, 28, 68, 54), fill=255)
    d.rectangle(sc(34, 54, 58, 82), fill=255)
    d.ellipse(sc(44, 54, 72, 82), fill=255)
    d.rectangle(sc(34, 28, 38, 32), fill=0)
    d.pieslice(sc(34, 28, 42, 36), 180, 270, fill=255)
    d.rectangle(sc(34, 78, 38, 82), fill=0)
    d.pieslice(sc(34, 74, 42, 82), 90, 180, fill=255)
    d.rectangle(sc(46, 36, 54, 46), fill=0)
    d.ellipse(sc(49, 36, 59, 46), fill=0)
    d.rectangle(sc(46, 59, 56, 71), fill=0)
    d.ellipse(sc(50, 59, 62, 71), fill=0)
    if scale != 1.0 or ty != 0.0:
        k = 1.0 / scale
        img = img.transform((W, W), Image.AFFINE,
                            (k, 0.0, (54.0 + (GX - 53.0)) * (1 - k),
                             0.0, k, (54.0 + (GY - 54.0)) * (1 - k) - ty * S4),
                            resample=Image.BILINEAR)
    return np.asarray(img, dtype=np.float32) / 255.0


def render_mark(shape: str = "squircle") -> Image.Image:
    """shape: squircle | circle | square | glyph"""
    yy, xx = np.mgrid[0:W, 0:W].astype(np.float32) / S4
    t = np.clip((xx + yy) / (2.0 * V), 0.0, 1.0)
    stops = np.array([0.0, 0.52, 1.0], dtype=np.float32)
    cols = np.array([[0xFF, 0x5A, 0x6A], [0xE6, 0x00, 0x23], [0xB3, 0x00, 0x1B]], dtype=np.float32)
    img = np.empty((W, W, 3), dtype=np.float32)
    for c in range(3):
        img[..., c] = np.interp(t, stops, cols[:, c])
    dist = np.sqrt((xx - 26.0) ** 2 + (yy - 22.0) ** 2)
    a = 0.12 * np.clip(1.0 - dist / 86.0, 0.0, 1.0)
    img = img * (1.0 - a[..., None]) + 255.0 * a[..., None]
    a = 0.12 * np.clip((yy - 46.0) / (V - 46.0), 0.0, 1.0)
    img = img * (1.0 - a[..., None])
    a = (0x1A / 255.0) * glyph_mask(ty=2.0 / GLYPH_SCALE)
    img = img * (1.0 - a[..., None])
    g = glyph_mask()
    img = img * (1.0 - g[..., None]) + 255.0 * g[..., None]

    m = Image.new("L", (W, W), 0)
    d = ImageDraw.Draw(m)
    if shape == "squircle":
        d.rounded_rectangle(sc(0, 0, V, V), radius=26 * S4, fill=255)
    elif shape == "circle":
        d.ellipse(sc(0, 0, V, V), fill=255)
    elif shape == "square":
        d.rectangle(sc(0, 0, V, V), fill=255)
    else:                                   # glyph on transparency
        m = Image.fromarray((g * 255).astype(np.uint8), "L")
        img = np.dstack([img, np.zeros_like(g)])[..., :3] * 0 + 255.0
    alpha = np.asarray(m, dtype=np.float32)
    return Image.fromarray(np.dstack([img, alpha]).astype(np.uint8), "RGBA")


def paste(canvas: Image.Image, mark: Image.Image, cx: int, cy: int, px: int) -> None:
    small = mark.resize((px, px), Image.LANCZOS)
    canvas.alpha_composite(small, (int(cx - px / 2), int(cy - px / 2)))


def font(name: str, size: int) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(str(FONT_DIR / name), size)


def text(canvas, xy, s, f, fill, anchor="la"):
    ImageDraw.Draw(canvas).text(xy, s, font=f, fill=fill, anchor=anchor, direction="ltr")


MARKS = {s: render_mark() for s in (24, 28, 48, 96, 192)}
GLYPH_RGBA = render_mark("glyph")

# sizes-strip.png — 24/28/48/96 on light and dark
card = Image.new("RGBA", (760, 460), (255, 255, 255, 255))
d = ImageDraw.Draw(card)
d.rounded_rectangle((0, 0, 759, 219), 18, fill=(255, 255, 255, 255), outline=(230, 230, 230, 255))
d.rounded_rectangle((0, 240, 759, 459), 18, fill=(18, 18, 18, 255))
label, small = font("inter_medium.ttf", 20), font("inter_regular.ttf", 15)
text(card, (24, 20), "Mark · 24 / 28 / 48 / 96 · light", label, (17, 17, 17, 255))
text(card, (24, 260), "Mark · 24 / 28 / 48 / 96 · dark", label, (245, 245, 245, 255))
x = 90
for s in (24, 28, 48, 96):
    paste(card, MARKS[s], x, 132, s)
    text(card, (x, 186), f"{s}dp", small, (140, 140, 140, 255), anchor="ma")
    x += 170
x = 90
for s in (24, 28, 48, 96):
    paste(card, MARKS[s], x, 372, s)
    text(card, (x, 424), f"{s}dp", small, (150, 150, 150, 255), anchor="ma")
    x += 170
card.save(BRAND / "sizes-strip.png")

# lockup-preview.png — LTR / RTL, light / dark, 28dp mark + 22sp wordmark
lk = Image.new("RGBA", (760, 420), (255, 255, 255, 255))
d = ImageDraw.Draw(lk)
d.rounded_rectangle((0, 0, 759, 199), 18, fill=(255, 255, 255, 255), outline=(230, 230, 230, 255))
d.rounded_rectangle((0, 220, 759, 419), 18, fill=(18, 18, 18, 255))
en_black, fa_black = font("inter_black.ttf", 46), font("vazirmatn_black.ttf", 46)
text(lk, (24, 16), "Lockup · LTR (en) · mark START", label, (17, 17, 17, 255))
text(lk, (24, 236), "Lockup · RTL (fa) · mark START (right)", label, (245, 245, 245, 255))
paste(lk, MARKS[48], 60, 118, 48)
text(lk, (104, 118), "PinBom", en_black, (17, 17, 17, 255), anchor="lm")
text(lk, (700, 118), "پینبوم", fa_black, (17, 17, 17, 255), anchor="rm")
paste(lk, MARKS[48], 700, 338, 48)
text(lk, (656, 338), "پینبوم", fa_black, (245, 245, 245, 255), anchor="rm")
lk.save(BRAND / "lockup-preview.png")

# adaptive-preview.png — safe zone + the three system masks + round
ad = Image.new("RGBA", (900, 420), (255, 255, 255, 255))
d = ImageDraw.Draw(ad)
text(ad, (24, 18), "Adaptive icon · 108dp canvas · 66dp safe circle", label, (17, 17, 17, 255))
big = render_mark().resize((300, 300), Image.LANCZOS)
d = ImageDraw.Draw(big)
d.ellipse((300 * 21 / 108, 300 * 21 / 108, 300 * 87 / 108, 300 * 87 / 108),
          outline=(255, 255, 255, 160), width=2)
d.rounded_rectangle((0, 0, 299, 299), radius=300 * 26 / 108, outline=(255, 255, 255, 90), width=2)
ad.alpha_composite(big, (40, 70))
for i, shape in enumerate(("squircle", "circle", "square")):
    paste(ad, render_mark(shape), 470 + i * 140, 220, 108)
paste(ad, GLYPH_RGBA, 470 + 3 * 140, 220, 108)
for i, name in enumerate(("squircle", "circle", "square", "glyph")):
    text(ad, (470 + i * 140, 296), name, small, (140, 140, 140, 255), anchor="ma")
ad.save(BRAND / "adaptive-preview.png")

# play-store-512.png — 512×512 rounded-square icon on white
store = Image.new("RGBA", (512, 512), (255, 255, 255, 0))
store.alpha_composite(render_mark("squircle").resize((512, 512), Image.LANCZOS))
store.save(BRAND / "play-store-512.png")

# working copies
WORK.mkdir(parents=True, exist_ok=True)
card.crop((0, 0, 760, 219)).save(WORK / "strip_sizes.png")
card.crop((0, 240, 760, 460)).save(WORK / "strip_dark.png")
variants = Image.new("RGBA", (600, 180), (255, 255, 255, 255))
for i, shape in enumerate(("squircle", "circle", "square")):
    paste(variants, render_mark(shape), 100 + i * 150, 90, 108)
variants.save(WORK / "variants.png")
render_mark("squircle").resize((512, 512), Image.LANCZOS).save(WORK / "icon512.png")
render_mark("circle").resize((512, 512), Image.LANCZOS).save(WORK / "round512.png")
render_mark("circle").resize((512, 512), Image.LANCZOS).save(WORK / "icon_round512.png")
ad.save(WORK / "adaptive-check.png")
GLYPH_RGBA.resize((216, 216), Image.LANCZOS).save(WORK / "glyph_only.png")
print("rasters: sizes-strip, lockup-preview, adaptive-preview, play-store-512 + working copies")
