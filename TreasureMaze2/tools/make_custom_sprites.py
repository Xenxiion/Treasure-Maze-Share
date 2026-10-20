# Draws the three 16x16 sprites that the Kenney Tiny Dungeon pack does not contain
# (bomb, coin, heart) using the pack's own outline color, so they blend in.
# Original work for this project; no third-party license applies to these three files.
from PIL import Image
import os

here = os.path.dirname(os.path.abspath(__file__))
out = os.path.join(here, "..", "assets", "custom")
os.makedirs(out, exist_ok=True)

# Outline color sampled from the Tiny Dungeon chest tile (dark red-brown).
chest = Image.open(os.path.join(here, "..", "assets", "kenney-tiny-dungeon", "tile_0090.png")).convert("RGBA")
OUT = chest.getpixel((0, 0))
for y in range(16):
    for x in range(16):
        p = chest.getpixel((x, y))
        if p[3] > 0:
            OUT = p
            break
    else:
        continue
    break

def make(rows, palette, name):
    """rows: 16 strings of 16 chars; palette maps a char to an RGBA tuple; '.' is transparent."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), palette[ch])
    img.save(os.path.join(out, name))

# o outline, k body dark, g body mid, w highlight, y gold, d gold shadow, f fuse, s spark, r red, p pink
PAL = {
    "o": OUT, "k": (38, 40, 56, 255), "g": (70, 74, 96, 255), "w": (150, 160, 190, 255),
    "y": (240, 190, 60, 255), "d": (190, 120, 40, 255), "l": (255, 235, 150, 255),
    "f": (170, 120, 70, 255), "s": (255, 120, 40, 255), "t": (255, 225, 90, 255),
    "r": (210, 50, 60, 255), "p": (255, 130, 140, 255),
}

BOMB = [
    "................",
    "..........t.....",
    ".........tst....",
    "..........s.....",
    "..........f.....",
    "........ooof....",
    ".....ooookkoo...",
    "....okkkkkkkko..",
    "...okkwwkkkkkko.",
    "...okwwkkkkkkko.",
    "...okkkkkkkkkko.",
    "...okkkkkkkkkko.",
    "...okgkkkkkkgko.",
    "....okggkkggko..",
    ".....ooooooooo..",
    "................",
]
COIN = [
    "................",
    "................",
    ".....oooooo.....",
    "....oyyyyyyo....",
    "...oyllyyyydo...",
    "..oyly..yyyydo..",
    "..oyy.yyyy.ydo..",
    "..oyyyyyyyyydo..",
    "..oyyyyyyyyydo..",
    "..oyy.yyyy.ydo..",
    "..oydy..yyddo...",
    "...oddyyyddo....",
    "....oddddddo....",
    ".....oooooo.....",
    "................",
    "................",
]
HEART = [
    "................",
    "................",
    "...oooo..oooo...",
    "..orpprooprrro..",
    ".orppprrrrrrrro.",
    ".orpprrrrrrrrro.",
    ".orrrrrrrrrrrro.",
    ".orrrrrrrrrrrro.",
    "..orrrrrrrrrro..",
    "...orrrrrrrro...",
    "....orrrrrro....",
    ".....orrrro.....",
    "......orro......",
    ".......oo.......",
    "................",
    "................",
]
make(BOMB, PAL, "bomb.png")
make(COIN, PAL, "coin.png")
make(HEART, PAL, "heart.png")
print("wrote bomb.png, coin.png, heart.png")
