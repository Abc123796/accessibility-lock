#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""渲染「无障碍锁」宣传短片：1080x1920 · 20fps · 24s。
每帧用 PIL 画，ffmpeg 编码。无外部素材、无字体依赖（用系统 Noto/文泉驿）。
"""
import os
import subprocess
import sys

from PIL import Image, ImageDraw, ImageFont

W, H = 1080, 1920
FPS = 20
DUR = 24.0

BG_TOP = (10, 13, 18)
BG_BOT = (15, 20, 28)
CARD = (22, 27, 34, 255)
STROKE = (42, 49, 60, 255)
TEXT = (230, 237, 243, 255)
MUTED = (139, 148, 158, 255)
ACCENT = (77, 163, 255, 255)
GOOD = (63, 185, 80, 255)
BAD = (248, 81, 73, 255)

FONT_PATHS = [
    "/system/fonts/NotoSansCJK-Regular.ttc",
    "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
    "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc",
]

_cache = {}


def font(size, bold=False):
    key = (size, bold)
    if key in _cache:
        return _cache[key]
    f = None
    for p in FONT_PATHS:
        if os.path.exists(p):
            try:
                f = ImageFont.truetype(p, size)
                break
            except Exception:
                continue
    if f is None:
        f = ImageFont.load_default()
    _cache[key] = f
    return f


def clamp(x, lo=0.0, hi=1.0):
    return max(lo, min(hi, x))


def ease_out(x):
    x = clamp(x)
    return 1 - (1 - x) ** 3


def ease_io(x):
    x = clamp(x)
    return x * x * (3 - 2 * x)


def seg(t, a, b):
    """把 t 映射到 [a,b] 区间内的 0..1"""
    if b <= a:
        return 1.0 if t >= b else 0.0
    return clamp((t - a) / (b - a))


def bg_image():
    img = Image.new("RGB", (W, H), BG_TOP)
    px = img.load()
    for y in range(H):
        k = y / (H - 1)
        r = int(BG_TOP[0] + (BG_BOT[0] - BG_TOP[0]) * k)
        g = int(BG_TOP[1] + (BG_BOT[1] - BG_TOP[1]) * k)
        b = int(BG_TOP[2] + (BG_BOT[2] - BG_TOP[2]) * k)
        for x in range(W):
            px[x, y] = (r, g, b)
    # 顶部柔光
    glow = Image.new("L", (W, H), 0)
    gd = ImageDraw.Draw(glow)
    gd.ellipse([W * 0.1, -H * 0.28, W * 0.9, H * 0.28], fill=26)
    glow = glow.filter(__import__("PIL.ImageFilter", fromlist=["ImageFilter"]).GaussianBlur(160))
    tint = Image.new("RGB", (W, H), (30, 80, 160))
    img = Image.composite(tint, img, glow.point(lambda v: int(v * 6)))
    return img.convert("RGBA")


def scene1(d, t):
    """0-3.4s 痛点"""
    a = seg(t, 0.1, 0.6)
    dy = int((1 - ease_out(a)) * 40)
    d.text((W / 2, 700 + dy), "我的无障碍", font=font(72), fill=TEXT, anchor="mm", stroke_width=0)
    b = seg(t, 0.5, 1.1)
    d.text((W / 2, 830 + dy), "又自己关了", font=font(96, True), fill=BAD, anchor="mm", stroke_width=2,
           stroke_fill=BAD)
    c = seg(t, 1.5, 2.1)
    if c > 0:
        d.text((W / 2, 980), "每次都得手动去设置里再打开一次", font=font(38), fill=MUTED, anchor="mm")
    # 闪烁告警点
    if int(t * 4) % 2 == 0 and a > 0.5:
        d.ellipse([W / 2 - 330, 682, W / 2 - 300, 712], fill=BAD)


def toggle(d, cx, cy, on, scale=1.0):
    w, h = int(150 * scale), int(84 * scale)
    x0, y0 = cx - w / 2, cy - h / 2
    track = ACCENT if on else (58, 66, 78, 255)
    d.rounded_rectangle([x0, y0, x0 + w, y0 + h], radius=h / 2, fill=track)
    r = h / 2 - 6 * scale
    kx = x0 + w - r - 6 * scale if on else x0 + r + 6 * scale
    d.ellipse([kx - r, cy - r, kx + r, cy + r], fill=(245, 248, 252, 255))


def scene2(d, t):
    """3.4-8.2s 自动恢复演示"""
    card_a = seg(t, 0.0, 0.4)
    if card_a <= 0:
        return
    x0, y0, x1, y1 = 90, 620, W - 90, 900
    d.rounded_rectangle([x0, y0, x1, y1], radius=34, fill=CARD, outline=STROKE, width=2)
    # 图标
    d.rounded_rectangle([x0 + 40, y0 + 46, x0 + 40 + 108, y0 + 46 + 108], radius=26, fill=ACCENT)
    d.text((x0 + 94, y0 + 100), "◉", font=font(56), fill=(11, 14, 19, 255), anchor="mm")
    d.text((x0 + 176, y0 + 82), "自动点击器", font=font(50, True), fill=TEXT, anchor="ls")
    d.text((x0 + 176, y0 + 138), "无障碍服务", font=font(32), fill=MUTED, anchor="ls")

    # 开关状态：1s 关 → 3s 开
    if t < 1.0:
        on = True
    elif t < 2.6:
        on = False
        k = seg(t, 1.0, 1.5)
        if k < 1:
            d.ellipse([x1 - 230, y0 + 30, x1 - 40, y0 + 220], outline=BAD, width=3)
        d.text((x1 - 130, y0 + 210), "已关闭", font=font(30), fill=BAD, anchor="mm")
    else:
        on = True
        p = seg(t, 2.6, 3.0)
        if p >= 1:
            d.text((x1 - 130, y0 + 210), "已恢复 ✓", font=font(30), fill=GOOD, anchor="mm")
    toggle(d, x1 - 130, y0 + 100, on)

    # 底部说明
    if t > 2.8:
        d.text((W / 2, 1010), "3 秒内，自己拨回来", font=font(52, True), fill=TEXT, anchor="mm")
    else:
        d.text((W / 2, 1010), "它在后台每 3 秒看一次", font=font(44), fill=MUTED, anchor="mm")


def chip(d, cx, cy, text, on):
    w = font(36).getbbox(text)[2] + 70
    h = 78
    fill = (28, 40, 56, 255) if on else (24, 28, 35, 255)
    line = ACCENT if on else STROKE
    d.rounded_rectangle([cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2], radius=20, fill=fill, outline=line,
                        width=2)
    d.text((cx, cy), text, font=font(36), fill=TEXT if on else MUTED, anchor="mm")


def scene3(d, t):
    """8.2-12.6s 3 秒"""
    a = seg(t, 0.0, 0.35)
    if a <= 0:
        return
    scale = 0.9 + 0.1 * ease_out(a)
    size = int(300 * scale)
    d.text((W / 2 - 30, 700), "3", font=font(size, True), fill=ACCENT, anchor="mm", stroke_width=2,
           stroke_fill=ACCENT)
    d.text((W / 2 + 120, 760), "秒内", font=font(64), fill=TEXT, anchor="lm")
    d.text((W / 2 + 120, 640), "检测到就", font=font(44), fill=MUTED, anchor="lm")
    d.text((W / 2 + 120, 880), "拨回来", font=font(64), fill=TEXT, anchor="lm")

    ch = [("① 读系统开关", 1.0), ("② 写回设置", 1.9), ("③ 复核是否真绑上", 2.8)]
    for i, (label, at) in enumerate(ch):
        p = seg(t, at, at + 0.35)
        if p > 0:
            cy = 1150 + i * 120
            chip(d, W / 2, cy, label, p > 0.5)


def scene4(d, t):
    """12.6-17.6s 卖点"""
    rows = [
        ("免 root", "只需一次 adb 授权", ACCENT),
        ("41 KB", "无第三方依赖，零网络权限", GOOD),
        ("自愈", "开机自启，被杀 15 分钟爬起来", (210, 153, 34, 255)),
    ]
    for i, (big, small, col) in enumerate(rows):
        at = i * 0.75
        p = seg(t, at, at + 0.45)
        if p <= 0:
            continue
        dx = int((1 - ease_out(p)) * -120)
        cy = 700 + i * 240
        d.rounded_rectangle([90 + dx, cy - 90, W - 90 + dx, cy + 90], radius=28, fill=CARD, outline=STROKE,
                            width=2)
        d.text((140 + dx, cy), big, font=font(64, True), fill=col, anchor="lm")
        d.text((420 + dx, cy), small, font=font(34), fill=MUTED, anchor="lm")


def scene5(d, t):
    """17.6-21.6s 安装"""
    d.text((W / 2, 620), "三步跑起来", font=font(56, True), fill=TEXT, anchor="mm")
    steps = [("1", "安装 APK"), ("2", "执行一次授权"), ("3", "选服务 · 启动守护")]
    for i, (n, label) in enumerate(steps):
        p = seg(t, i * 0.6, i * 0.6 + 0.4)
        if p <= 0:
            continue
        cy = 760 + i * 180
        d.ellipse([120, cy - 42, 204, cy + 42], fill=(28, 40, 56, 255), outline=ACCENT, width=2)
        d.text((162, cy), n, font=font(40, True), fill=ACCENT, anchor="mm")
        d.text((240, cy), label, font=font(44), fill=TEXT, anchor="lm")
    if t > 1.9:
        d.rounded_rectangle([100, 1300, W - 100, 1480], radius=22, fill=(16, 20, 26, 255), outline=STROKE,
                            width=2)
        d.text((130, 1345), "adb shell pm grant", font=font(30), fill=GOOD, anchor="lm")
        d.text((130, 1400), "cn.dsh.alaslock android.permission", font=font(26), fill=MUTED, anchor="lm")
        d.text((130, 1445), "  .WRITE_SECURE_SETTINGS", font=font(26), fill=MUTED, anchor="lm")


def scene6(d, t):
    """21.6-24s 结尾"""
    p = seg(t, 0.0, 0.5)
    if p <= 0:
        return
    d.text((W / 2, 760), "无障碍锁", font=font(96, True), fill=TEXT, anchor="mm", stroke_width=2,
           stroke_fill=TEXT)
    d.text((W / 2, 900), "github.com/Abc123796", font=font(40), fill=ACCENT, anchor="mm")
    d.text((W / 2, 960), "accessibility-lock", font=font(40), fill=ACCENT, anchor="mm")
    d.text((W / 2, 1080), "MIT 开源 · 零网络权限 · 41 KB", font=font(34), fill=MUTED, anchor="mm")


SCENES = [
    (0.0, 3.4, scene1),
    (3.4, 8.2, scene2),
    (8.2, 12.6, scene3),
    (12.6, 17.6, scene4),
    (17.6, 21.6, scene5),
    (21.6, DUR, scene6),
]


def render(outdir):
    os.makedirs(outdir, exist_ok=True)
    base = bg_image()
    total = int(DUR * FPS)
    for f in range(total):
        t = f / FPS
        frame = base.copy()
        for (s0, s1, fn) in SCENES:
            if s0 <= t < s1:
                lt = t - s0
                layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
                d = ImageDraw.Draw(layer)
                fn(d, lt)
                # 场景进出淡入淡出
                fade_in = clamp(lt / 0.35)
                fade_out = clamp((s1 - t) / 0.35)
                k = min(fade_in, fade_out)
                if k < 0.999:
                    a = layer.split()[3].point(lambda v, k=k: int(v * k))
                    layer.putalpha(a)
                frame = Image.alpha_composite(frame, layer)
                break

        # 底部进度条
        d2 = ImageDraw.Draw(frame)
        pw = int((W - 160) * clamp(t / DUR))
        d2.rounded_rectangle([80, H - 90, W - 80, H - 84], radius=3, fill=(38, 46, 58, 255))
        if pw > 0:
            d2.rounded_rectangle([80, H - 90, 80 + pw, H - 84], radius=3, fill=ACCENT)

        # 全片淡入淡出
        g = 1.0
        if t < 0.4:
            g = t / 0.4
        if t > DUR - 0.6:
            g = min(g, (DUR - t) / 0.6)
        if g < 1.0:
            frame = Image.blend(Image.new("RGBA", (W, H), (0, 0, 0, 255)), frame, max(0.0, g))

        frame.convert("RGB").save(os.path.join(outdir, "%05d.png" % f), quality=92)
        if f % 40 == 0:
            print("frame", f, "/", total, flush=True)


def main():
    outdir = "/tmp/promo_frames"
    render(outdir)
    mp4 = sys.argv[1] if len(sys.argv) > 1 else "/root/workspace/alaslock/docs/promo.mp4"
    os.makedirs(os.path.dirname(mp4), exist_ok=True)
    subprocess.run([
        "ffmpeg", "-y", "-framerate", str(FPS), "-i", os.path.join(outdir, "%05d.png"),
        "-c:v", "libx264", "-pix_fmt", "yuv420p", "-crf", "20", "-preset", "medium",
        "-movflags", "+faststart", mp4,
    ], check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    print("mp4 ->", mp4, os.path.getsize(mp4) // 1024, "KB")

    gif = os.path.join(os.path.dirname(mp4), "promo.gif")
    subprocess.run([
        "ffmpeg", "-y", "-i", mp4, "-vf", "fps=12,scale=432:-1:flags=lanczos,split[a][b];[a]palettegen=max_colors=128[p];[b][p]paletteuse=dither=bayer",
        gif,
    ], check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    print("gif ->", gif, os.path.getsize(gif) // 1024, "KB")


if __name__ == "__main__":
    main()
