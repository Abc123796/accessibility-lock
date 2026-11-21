package cn.dsh.alaslock;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** 统一的颜色、尺寸和小控件工厂，避免到处写魔法数字。 */
final class Ui {

    static final int BG = 0xFF0B0E13;
    static final int CARD = 0xFF161B22;
    static final int CARD_HI = 0xFF1B2430;
    static final int STROKE = 0xFF2A313C;
    static final int TEXT = 0xFFE6EDF3;
    static final int MUTED = 0xFF8B949E;
    static final int ACCENT = 0xFF4DA3FF;
    static final int GOOD = 0xFF3FB950;
    static final int WARN = 0xFFD29922;
    static final int BAD = 0xFFF85149;

    static final int ACCENT_BG = 0x1F4DA3FF;
    static final int GOOD_BG = 0x1F3FB950;
    static final int WARN_BG = 0x1FD29922;
    static final int MUTED_BG = 0x148B949E;
    static final int BAD_BG = 0x1FF85149;

    private Ui() {
    }

    static int dp(Context c, float v) {
        return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f);
    }

    static GradientDrawable round(Context c, int fill, float radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(c, radiusDp));
        return d;
    }

    static GradientDrawable roundStroke(Context c, int fill, int stroke, float radiusDp, float wDp) {
        GradientDrawable d = round(c, fill, radiusDp);
        d.setStroke(dp(c, wDp), stroke);
        return d;
    }

    static TextView tv(Context c, CharSequence s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) {
            t.setTypeface(Typeface.DEFAULT_BOLD);
        }
        return t;
    }

    static LinearLayout col(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    static LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    static LinearLayout.LayoutParams lpm(Context c, int w, int h, float topDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.topMargin = dp(c, topDp);
        return p;
    }

    static LinearLayout.LayoutParams weight(int w) {
        return new LinearLayout.LayoutParams(w, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }

    static void gap(Context c, LinearLayout parent, float dp) {
        View v = new View(c);
        parent.addView(v, new LinearLayout.LayoutParams(1, dp(c, dp)));
    }
}
