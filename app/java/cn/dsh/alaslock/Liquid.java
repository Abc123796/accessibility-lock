package cn.dsh.alaslock;

import android.animation.TimeInterpolator;
import android.content.Context;
import android.provider.Settings;
import android.view.animation.PathInterpolator;

/** 液态视觉的公共参数：配色、缓动、减弱动画检测。 */
final class Liquid {

    static final int BG = 0xFF070A0F;
    static final int TEXT = 0xFFE9EFF7;
    static final int MUTED = 0xFF8B949E;
    static final int ACCENT = 0xFF4DA3FF;
    static final int TEAL = 0xFF2BD9C4;
    static final int VIOLET = 0xFF8B7BFF;
    static final int GOOD = 0xFF3FB950;
    static final int WARN = 0xFFD29922;
    static final int BAD = 0xFFF85149;

    /** 玻璃卡片：半透明底 + 极淡描边，让背景流光透上来 */
    static final int GLASS = 0xB0121821;
    static final int GLASS_STROKE = 0x24FFFFFF;
    static final int GLASS_HI = 0xC0162030;

    /** 带一点过冲的弹簧曲线 */
    static final TimeInterpolator SPRING = new PathInterpolator(0.22f, 1.26f, 0.36f, 1f);
    static final TimeInterpolator EASE = new PathInterpolator(0.22f, 0f, 0.12f, 1f);

    private Liquid() {
    }

    static int dp(Context c, float v) {
        return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f);
    }

    /** 用户在开发者选项里关掉动画，就别再动。 */
    static boolean reduceMotion(Context c) {
        try {
            float s = Settings.Global.getFloat(c.getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE, 1f);
            return s == 0f;
        } catch (Throwable t) {
            return false;
        }
    }

    static long dur(Context c, long ms) {
        return reduceMotion(c) ? 0L : ms;
    }

    static int blend(int a, int b, float f) {
        f = Math.max(0f, Math.min(1f, f));
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF, aa = (a >>> 24) & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF, ba = (b >>> 24) & 0xFF;
        int r = (int) (ar + (br - ar) * f);
        int g = (int) (ag + (bg - ag) * f);
        int bl = (int) (ab + (bb - ab) * f);
        int al = (int) (aa + (ba - aa) * f);
        return (al << 24) | (r << 16) | (g << 8) | bl;
    }
}
