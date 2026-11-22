#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成 B站封面 1280x720。"""
import os
from PIL import Image, ImageDraw, ImageFont

W, H = 1280, 720
TEXT = (230, 237, 243)
MUTED = (139, 148, 158)
ACCENT = (77, 163, 255)
GOOD = (63, 185, 80)
CARD = (22, 27, 34)

FP = ["/system/fonts/NotoSansCJK-Regular.ttc",
      "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc"]


def f(sz):
    for p in FP:
        if os.path.exists(p):
            return ImageFont.truetype(p, sz)
    return ImageFont.load_default()


img = Image.new("RGB", (W, H), (10, 13, 18))
d = ImageDraw.Draw(img)
# 背景渐变 + 柔光
for y in range(H):
    k = y / (H - 1)
    d.line([(0, y), (W, y)], fill=(int(10 + 8 * k), int(13 + 9 * k), int(18 + 12 * k)))
glow = Image.new("L", (W, H), 0)
ImageDraw.Draw(glow).ellipse([-200, -420, 900, 320], fill=70)
from PIL import ImageFilter
glow = glow.filter(ImageFilter.GaussianBlur(150))
img = Image.composite(Image.new("RGB", (W, H), (24, 74, 150)), img, glow)
d = ImageDraw.Draw(img)

d.text((80, 130), "国产 ROM 老把无障碍悄悄关掉", font=f(38), fill=MUTED)
d.text((74, 200), "3 秒", font=f(150), fill=ACCENT, stroke_width=2, stroke_fill=ACCENT)
d.text((420, 250), "拨回来", font=f(110), fill=TEXT, stroke_width=2, stroke_fill=TEXT)
d.text((80, 430), "免 root  ·  41 KB  ·  零联网", font=f(44), fill=GOOD)
d.text((80, 540), "github.com/Abc123796/accessibility-lock", font=f(32), fill=MUTED)

# 右侧示意卡：开关
cx, cy = 1010, 330
d.rounded_rectangle([cx - 210, cy - 120, cx + 210, cy + 120], radius=30, fill=CARD, outline=(42, 49, 60), width=2)
d.rounded_rectangle([cx - 170, cy - 80, cx - 90, cy], radius=20, fill=ACCENT)
d.text((cx - 130, cy - 50), "◉", font=f(40), fill=(11, 14, 19), anchor="mm")
d.text((cx + 10, cy - 60), "自动点击器", font=f(34), fill=TEXT, anchor="lm")
# 开关本体
d.rounded_rectangle([cx - 10, cy + 10, cx + 130, cy + 74], radius=32, fill=ACCENT)
d.ellipse([cx + 66, cy + 16, cx + 124, cy + 68], fill=(245, 248, 252))
d.text((cx + 60, cy + 92), "已恢复 ✓", font=f(28), fill=GOOD, anchor="lm")

out = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "cover.png")
img.save(os.path.normpath(out), quality=95)
print("cover ->", os.path.normpath(out))
