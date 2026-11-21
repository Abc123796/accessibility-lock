#!/usr/bin/env python3
"""生成 192x192 的应用图标（纯 Python，无外部素材）。"""
import os
from PIL import Image, ImageDraw

S = 192
img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
d = ImageDraw.Draw(img)

# 圆角底板
d.rounded_rectangle([0, 0, S - 1, S - 1], radius=42, fill=(30, 36, 48, 255))
# 内层高光
d.rounded_rectangle([6, 6, S - 7, S - 7], radius=38, outline=(108, 195, 255, 70), width=2)

blue = (111, 195, 255, 255)
# 锁梁（圆弧）
d.arc([62, 40, 130, 108], start=180, end=360, fill=blue, width=13)
d.line([68, 74, 68, 92], fill=blue, width=13)
d.line([124, 74, 124, 92], fill=blue, width=13)

# 锁体
d.rounded_rectangle([46, 88, 146, 154], radius=16, fill=blue)
# 钥匙孔
d.ellipse([86, 106, 106, 126], fill=(30, 36, 48, 255))
d.rounded_rectangle([91, 118, 101, 140], radius=4, fill=(30, 36, 48, 255))

out = os.path.join(os.path.dirname(os.path.abspath(__file__)), "res", "drawable", "ic_launcher.png")
os.makedirs(os.path.dirname(out), exist_ok=True)
img.save(out)
print("icon ->", out)
