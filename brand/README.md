# PinBom — brand assets

Source of truth for the PinBom mark. The same geometry is cut into three places;
if you change the glyph, change all three:

| Where | File |
|---|---|
| Design source | `pinbom-glyph.svg`, `pinbom-mark.svg`, `pinbom-background.svg` |
| Android vectors | `app/src/main/res/drawable/ic_pinbom_foreground.xml`, `…_background.xml`, `…_monochrome.xml`, `ic_pinbom.xml`, `ic_notification.xml` |
| Compose | `PinBomGlyphPath` / `PinBomGlyphVector` in `ui/components/Brand.kt` |

## Geometry

- Authored space: **108 × 108** (1:1 with the adaptive-icon canvas).
- Glyph: a bold geometric **B** — outer outline plus two counter subpaths, filled
  **`evenOdd`** so both counters are real holes. Glyph box: **x 34→72, y 28→82**
  (54 × 54, optically centred in the viewport).
- Rendered scale inside its container: **×1.2** → 64.8 units tall ≈ **60 %** of
  the container. That is the number to keep when regenerating anything: the two
  counters must still read at **24dp**.
- Container radius: **26 / 108 = 24 %** (squircle), or a full circle for
  `roundIcon`. Legacy PNGs bake the rounded corner; adaptive icons do not.
- Gradient: 135° **#FF5A6A → #E60023 (52 %) → #B3001B**, plus a 12 % white radial
  highlight at the top-start and a 12 % bottom shade.
- **No dots, no “spark” notch, no floating decorations** — the mark is the B and
  nothing else. The wordmark is plain text (Vazirmatn Black «پینبوم» /
  Inter Black “PinBom”), tight tracking, no dot flourish.

## Exporting

```bash
# Play Store 512×512 (no rounding — Play applies its own mask)
rsvg-convert -w 512 -h 512 pinbom-mark.svg -o play-store-512.png

# Legacy mipmaps (API 24–25), regenerated from the 108-space geometry
python3 /home/user/persian-pintrest/tools/make-launcher.py
```

`lockup-preview.png`, `sizes-strip.png` and `adaptive-preview.png` are rendered
sheets used for design review; both are produced by
`/home/user/persian-pintrest/tools/hotfix-brand.py`, which also rewrites every mark block in
`style-guide.html`. The wordmark previews are drawn with the bundled
Vazirmatn/Inter Black TTFs, matching what the app renders on device.
