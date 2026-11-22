package cn.dsh.alaslock;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/** 主按钮：渐变底 + 按下时扩散的水波。 */
public class LiquidButton extends View {

    private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint wave = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF(0, 0, 0, 0);
    private final Path clip = new Path();
    private String label = "";
    private boolean danger;
    private float press;
    private ValueAnimator anim;

    public LiquidButton(Context c) {
        this(c, null);
    }

    public LiquidButton(Context c, AttributeSet a) {
        super(c, a);
        setClickable(true);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(Liquid.dp(c, 16));
    }

    public void setLabel(String s) {
        label = s == null ? "" : s;
        invalidate();
    }

    public void setDanger(boolean d) {
        danger = d;
        invalidate();
    }

    @Override
    protected void onMeasure(int ws, int hs) {
        int w = MeasureSpec.getSize(ws);
        int h = Liquid.dp(getContext(), 54);
        setMeasuredDimension(w, resolveSize(h, hs));
    }

    @Override
    protected void onDraw(Canvas cv) {
        float r = Liquid.dp(getContext(), 20);
        box.set(0, 0, getWidth(), getHeight());

        if (danger) {
            bg.setShader(null);
            bg.setColor(0x14F85149);
            cv.drawRoundRect(box, r, r, bg);
            bg.setStyle(Paint.Style.STROKE);
            bg.setStrokeWidth(Liquid.dp(getContext(), 1.2f));
            bg.setColor(0x66F85149);
            cv.drawRoundRect(box, r, r, bg);
            bg.setStyle(Paint.Style.FILL);
        } else {
            bg.setShader(new LinearGradient(0, 0, getWidth(), getHeight(),
                    0xFF6FBBFF, 0xFF2C6FD6, Shader.TileMode.CLAMP));
            cv.drawRoundRect(box, r, r, bg);
        }

        if (press > 0.01f) {
            clip.reset();
            clip.addRoundRect(box, r, r, Path.Direction.CW);
            cv.save();
            cv.clipPath(clip);
            wave.setColor(danger ? 0x2AF85149 : 0x30FFFFFF);
            float maxR = (float) Math.hypot(getWidth(), getHeight());
            cv.drawCircle(getWidth() / 2f, getHeight() / 2f, maxR * 0.62f * press, wave);
            cv.restore();
        }

        textPaint.setColor(danger ? Liquid.BAD : 0xFF061019);
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float ty = getHeight() / 2f - (fm.ascent + fm.descent) / 2f;
        cv.drawText(label, getWidth() / 2f, ty, textPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                animatePress(1f, 480);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                animatePress(0f, 260);
                break;
            default:
                break;
        }
        return super.onTouchEvent(e);
    }

    private void animatePress(float to, long ms) {
        if (Liquid.reduceMotion(getContext())) {
            press = to;
            invalidate();
            return;
        }
        if (anim != null) {
            anim.cancel();
        }
        anim = ValueAnimator.ofFloat(press, to);
        anim.setDuration(ms);
        anim.setInterpolator(Liquid.EASE);
        anim.addUpdateListener(a -> {
            press = (float) a.getAnimatedValue();
            invalidate();
        });
        anim.start();
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        super.setOnClickListener(l);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
