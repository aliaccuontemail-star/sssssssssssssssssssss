"""Rasterise the PinBom launcher icon (legacy PNGs, API 24–25) from the same
108-space geometry the vector drawables use."""
import numpy as np
from PIL import Image, ImageDraw

S = 4                    # supersample factor
V = 108.0                # brand viewport
W = int(V * S)
PX, PY = 54.0, 54.0      # glyph pivot
GLYPH_SCALE = 1.2        # same optical scale as ic_pinbom.xml

def sc(*vals):
    return [v * S for v in vals]

def glyph_mask(scale: float, ty: float) -> np.ndarray:
    """The 'B' (two counters) as a 0/255 mask, scaled about the pivot."""
    img = Image.new("L", (W, W), 0)
    d = ImageDraw.Draw(img)
    # upper bowl: rect + semicircle (r13 about (55,41))
    d.rectangle(sc(34, 28, 55, 54), fill=255)
    d.ellipse(sc(42, 28, 68, 54), fill=255)
    # lower bowl: rect + semicircle (r14 about (58,68))
    d.rectangle(sc(34, 54, 58, 82), fill=255)
    d.ellipse(sc(44, 54, 72, 82), fill=255)
    # rounded left corners (r4)
    d.rectangle(sc(34, 28, 38, 32), fill=0)
    d.pieslice(sc(34, 28, 42, 36), 180, 270, fill=255)
    d.rectangle(sc(34, 78, 38, 82), fill=0)
    d.pieslice(sc(34, 74, 42, 82), 90, 180, fill=255)
    # counters (holes)
    d.rectangle(sc(46, 36, 54, 46), fill=0)
    d.ellipse(sc(49, 36, 59, 46), fill=0)
    d.rectangle(sc(46, 59, 56, 71), fill=0)
    d.ellipse(sc(50, 59, 62, 71), fill=0)
    if scale != 1.0 or ty != 0.0:
        k = 1.0 / scale
        img = img.transform(
            (W, W), Image.AFFINE,
            (k, 0.0, PX * (1 - k), 0.0, k, PY * (1 - k) - ty * S),
            resample=Image.BILINEAR,
        )
    return np.asarray(img, dtype=np.float32) / 255.0

def container(round_icon: bool) -> np.ndarray:
    img = Image.new("L", (W, W), 0)
    d = ImageDraw.Draw(img)
    if round_icon:
        d.ellipse(sc(0, 0, V, V), fill=255)
    else:
        d.rounded_rectangle(sc(0, 0, V, V), radius=26 * S, fill=255)
    return np.asarray(img, dtype=np.float32) / 255.0

def render(round_icon: bool) -> Image.Image:
    yy, xx = np.mgrid[0:W, 0:W].astype(np.float32) / S        # 108-space coords
    # diagonal gradient #FF5A6A → #E60023 → #B3001B at 52%
    t = np.clip((xx + yy) / (2.0 * V), 0.0, 1.0)
    stops = np.array([0.0, 0.52, 1.0], dtype=np.float32)
    cols = np.array([[0xFF, 0x5A, 0x6A], [0xE6, 0x00, 0x23], [0xB3, 0x00, 0x1B]], dtype=np.float32)
    img = np.empty((W, W, 3), dtype=np.float32)
    for c in range(3):
        img[..., c] = np.interp(t, stops, cols[:, c])
    # top-start highlight: radial, centre (26,22), r 86, 12% white
    dist = np.sqrt((xx - 26.0) ** 2 + (yy - 22.0) ** 2)
    a = 0.12 * np.clip(1.0 - dist / 86.0, 0.0, 1.0)
    img = img * (1.0 - a[..., None]) + 255.0 * a[..., None]
    # bottom shade below y=46: up to 12% black
    a = 0.12 * np.clip((yy - 46.0) / (V - 46.0), 0.0, 1.0)
    img = img * (1.0 - a[..., None])
    # glyph shadow (#1A black, 2 units down) then the white glyph
    shadow = glyph_mask(GLYPH_SCALE / 1.2, 2.0)
    a = (0x1A / 255.0) * shadow
    img = img * (1.0 - a[..., None])
    g = glyph_mask(GLYPH_SCALE / 1.2, 0.0)
    img = img * (1.0 - g[..., None]) + 255.0 * g[..., None]
    alpha = container(round_icon)
    out = np.dstack([img, alpha * 255.0]).astype(np.uint8)
    return Image.fromarray(out, "RGBA")

DENSITIES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
BASE = "/home/user/persian-pintrest/app/src/main/res"
for name, px in DENSITIES.items():
    big = render(round_icon=False).resize((px, px), Image.LANCZOS)
    big.save(f"{BASE}/mipmap-{name}/ic_launcher.png")
    rnd = render(round_icon=True).resize((px, px), Image.LANCZOS)
    rnd.save(f"{BASE}/mipmap-{name}/ic_launcher_round.png")
    print(f"mipmap-{name}: {px}x{px} written")
