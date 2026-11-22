package cn.dsh.alaslock;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/**
 * 背景：三团柔光。
 * 关键：只画一次到 Bitmap，之后每帧只 drawBitmap。
 * 早期版本用 BlurMaskFilter + 软件层每帧重绘，等于每帧模糊三张全屏图，卡到不能用。
 */
public class LiquidBg extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Bitmap cache;

    public LiquidBg(Context c) {
        this(c, null);
    }

    public LiquidBg(Context c, AttributeSet a) {
        super(c, a);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        recycle();
        if (w > 0 && h > 0) {
            cache = render(w, h);
        }
    }

    private Bitmap render(int w, int h) {
        Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bm);

        // 底色渐变
        paint.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{0xFF080B11, 0xFF0D1219, 0xFF0A0E14},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        cv.drawRect(0, 0, w, h, paint);

        float base = Math.min(w, h);
        blob(cv, w * 0.18f, h * 0.14f, base * 0.60f, Liquid.ACCENT, 0x30);
        blob(cv, w * 0.84f, h * 0.26f, base * 0.52f, Liquid.TEAL, 0x24);
        blob(cv, w * 0.55f, h * 0.95f, base * 0.66f, Liquid.VIOLET, 0x20);
        return bm;
    }

    /** 用径向渐变做柔光：GPU 友好，不需要模糊滤镜 */
    private void blob(Canvas cv, float x, float y, float r, int color, int alpha) {
        int c0 = (alpha << 24) | (color & 0xFFFFFF);
        int c1 = (alpha / 3 << 24) | (color & 0xFFFFFF);
        paint.setShader(new RadialGradient(x, y, r,
                new int[]{c0, c1, 0x00000000},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        cv.drawCircle(x, y, r, paint);
        paint.setShader(null);
    }

    @Override
    protected void onDraw(Canvas cv) {
        if (cache != null && !cache.isRecycled()) {
            cv.drawBitmap(cache, 0, 0, null);
        } else {
            cv.drawColor(Liquid.BG);
        }
    }

    private void recycle() {
        if (cache != null && !cache.isRecycled()) {
            cache.recycle();
        }
        cache = null;
    }

    @Override
    protected void onDetachedFromWindow() {
        recycle();
        super.onDetachedFromWindow();
    }
}
