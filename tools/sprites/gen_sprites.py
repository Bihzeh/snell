#!/usr/bin/env python3
"""Generate the Snell GUI sprite set (white masters, tinted at draw time in-game).

One-time author tool (the resulting PNGs + .mcmeta are committed so the build never depends on this).

Outputs into mod/src/main/resources/assets/snell/textures/gui/sprites/:
  - icon/<name>.png   : 30 Tabler icons rasterized from the bundled tabler-snell.ttf, white ink centred
                        in a 64x64 transparent square (no .mcmeta -> default Stretch scaling).
  - shape/rrect.png(+sm) + *_outline + pill.png : white rounded-rect / capsule 9-slice masters with a
                        .png.mcmeta declaring gui.scaling=nine_slice. Tinted per use in-game.

Run: tools/sprites/venv/bin/python tools/sprites/gen_sprites.py   (or any python with Pillow+freetype)
"""
import json
import re
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "mod/src/main/resources/assets/snell"
TTF = RES / "font/tabler-snell.ttf"
SNELLUI = ROOT / "mod/src/main/kotlin/gg/snell/mod/ui/SnellUi.kt"
OUT = RES / "textures/gui/sprites"
WHITE = (255, 255, 255, 255)


def icon_map() -> dict[str, str]:
    """Parse the `"name" to 'char'` pairs from the ICONS block in SnellUi.kt."""
    text = SNELLUI.read_text(encoding="utf-8")
    block = re.search(r"ICONS[^=]*=\s*mapOf\((.*?)\)", text, re.S).group(1)
    return {m.group(1): m.group(2) for m in re.finditer(r'"(\w+)"\s+to\s+\'(.)\'', block)}


def gen_icons(size: int = 64) -> int:
    (OUT / "icon").mkdir(parents=True, exist_ok=True)
    icons = icon_map()
    # Pick a glyph pixel size that leaves a little padding inside the square.
    font = ImageFont.truetype(str(TTF), int(size * 0.86))
    for name, ch in icons.items():
        img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        l, t, r, b = d.textbbox((0, 0), ch, font=font)
        gx = (size - (r - l)) / 2 - l
        gy = (size - (b - t)) / 2 - t
        d.text((gx, gy), ch, font=font, fill=WHITE)
        img.save(OUT / "icon" / f"{name}.png")
    return len(icons)


def _mcmeta(path: Path, w: int, h: int, border) -> None:
    path.with_suffix(".png.mcmeta").write_text(
        json.dumps({"gui": {"scaling": {"type": "nine_slice", "width": w, "height": h, "border": border}}}, indent=2)
        + "\n"
    )


def rrect(name: str, side: int, radius: int, outline: bool) -> None:
    (OUT / "shape").mkdir(parents=True, exist_ok=True)
    # 4x supersample for a clean antialiased corner, then downscale.
    s = 4
    big = Image.new("RGBA", (side * s, side * s), (0, 0, 0, 0))
    d = ImageDraw.Draw(big)
    box = [0, 0, side * s - 1, side * s - 1]
    if outline:
        d.rounded_rectangle(box, radius=radius * s, outline=WHITE, width=s)
    else:
        d.rounded_rectangle(box, radius=radius * s, fill=WHITE)
    img = big.resize((side, side), Image.LANCZOS)
    img.save(OUT / "shape" / f"{name}.png")
    _mcmeta(OUT / "shape" / f"{name}.png", side, side, radius)


def pill(name: str, w: int, h: int) -> None:
    (OUT / "shape").mkdir(parents=True, exist_ok=True)
    s = 4
    big = Image.new("RGBA", (w * s, h * s), (0, 0, 0, 0))
    ImageDraw.Draw(big).rounded_rectangle([0, 0, w * s - 1, h * s - 1], radius=(h // 2) * s, fill=WHITE)
    big.resize((w, h), Image.LANCZOS).save(OUT / "shape" / f"{name}.png")
    # Horizontal capsule: caps = h/2 on left/right, full height on top/bottom (fixed-height pills).
    _mcmeta(OUT / "shape" / f"{name}.png", w, h, {"left": h // 2, "right": h // 2, "top": h // 2, "bottom": h // 2})


def main() -> None:
    n = gen_icons()
    rrect("rrect", 24, 6, outline=False)
    rrect("rrect_outline", 24, 6, outline=True)
    rrect("rrect_sm", 12, 3, outline=False)
    rrect("rrect_sm_outline", 12, 3, outline=True)
    pill("pill", 40, 20)
    print(f"generated {n} icons + 5 shape masters into {OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
