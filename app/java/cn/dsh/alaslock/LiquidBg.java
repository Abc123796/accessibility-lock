package cn.dsh.alaslock;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

/** 背景：三团缓慢漂移的柔光，营造"液态"感。 */
public class LiquidBg extends View {

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private long t0 = 0L;
    private long last = 0L;
    private boolean animate = true;

    public LiquidBg(Context c) {
        this(c, null);
    }

    public LiquidBg(Context c, AttributeSet a) {
        super(c, a);
        p.setMaskFilter(new BlurMaskFilter(Liquid.dp(c, 72), BlurMaskFilter.Blur.NORMAL));
        animate = !Liquid.reduceMotion(c);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        t0 = SystemClock.uptimeMillis();
        last = 0L;
    }

    @Override
    protected void onDraw(Canvas cv) {
        cv.drawColor(Liquid.BG);
        long now = SystemClock.uptimeMillis();
        if (t0 == 0L) {
            t0 = now;
        }
        float t = (now - t0) / 1000f;

        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0) {
            return;
        }
        float base = Math.min(w, h);

        draw(cv, w * (0.20f + 0.05f * (float) Math.sin(t * 0.23f)),
                h * (0.16f + 0.04f * (float) Math.cos(t * 0.19f)),
                base * 0.52f, Liquid.ACCENT, 0x26);
        draw(cv, w * (0.82f + 0.04f * (float) Math.cos(t * 0.17f + 1.7f)),
                h * (0.30f + 0.05f * (float) Math.sin(t * 0.21f + 1.1f)),
                base * 0.46f, Liquid.TEAL, 0x1E);
        draw(cv, w * (0.55f + 0.06f * (float) Math.sin(t * 0.13f + 3.1f)),
                h * (0.92f + 0.03f * (float) Math.cos(t * 0.16f + 2.2f)),
                base * 0.58f, Liquid.VIOLET, 0x1C);

        if (animate && now - last > 66) {
            last = now;
            postInvalidateOnAnimation();
        }
    }

    private void draw(Canvas cv, float x, float y, float r, int color, int alpha) {
        p.setShader(null);
        p.setColor((alpha << 24) | (color & 0xFFFFFF));
        p.setAlpha(alpha);
        cv.drawCircle(x, y, r, p);
    }

    @Override
    protected void onDetachedFromWindow() {
        animate = false;
        super.onDetachedFromWindow();
    }
}
