# Makes assets/custom/bomb_blue.png (the blue Mimic-trap bomb) by recoloring assets/custom/bomb.png.
# Original work for this project; no third-party license applies.
from PIL import Image
import os
here = os.path.dirname(os.path.abspath(__file__))
src = os.path.join(here, "..", "assets", "custom", "bomb.png")
dst = os.path.join(here, "..", "assets", "custom", "bomb_blue.png")
swap = {(38, 40, 56): (16, 36, 110), (70, 74, 96): (40, 100, 215), (150, 160, 190): (170, 215, 255)}  # dark, body, shine
img = Image.open(src).convert("RGBA")
for y in range(img.height):
    for x in range(img.width):
        p = img.getpixel((x, y))
        if p[:3] in swap:
            img.putpixel((x, y), swap[p[:3]] + (p[3],))
img.save(dst)
