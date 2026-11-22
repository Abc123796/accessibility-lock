package cn.dsh.alaslock;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/** 液态开关：滑块带挤压/回弹，轨道颜色跟着流过去。 */
public class LiquidToggle extends View {

    public interface OnChange {
        void onChanged(boolean checked);
    }

    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint knob = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF();
    private boolean checked;
    private float v = 0f;
    private OnChange listener;
    private ValueAnimator anim;

    public LiquidToggle(Context c) {
        this(c, null);
    }

    public LiquidToggle(Context c, AttributeSet a) {
        super(c, a);
        setClickable(true);
        knob.setColor(0xFFF7FAFF);
        glow.setColor(Liquid.ACCENT);
        glow.setAlpha(40);
        setPadding(0, Liquid.dp(c, 4), 0, Liquid.dp(c, 4));
    }

    public void setOnChanged(OnChange l) {
        listener = l;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean on) {
        setChecked(on, true);
    }

    public void setChecked(boolean on, boolean notify) {
        checked = on;
        float to = on ? 1f : 0f;
        if (!notify) {
            v = to;
            invalidate();
            return;
        }
        if (Liquid.reduceMotion(getContext())) {
            v = to;
            invalidate();
            return;
        }
        if (anim != null) {
            anim.cancel();
        }
        anim = ValueAnimator.ofFloat(v, to);
        anim.setDuration(340);
        anim.setInterpolator(Liquid.SPRING);
        anim.addUpdateListener(a -> {
            v = (float) a.getAnimatedValue();
            invalidate();
        });
        anim.start();
        if (listener != null) {
            listener.onChanged(checked);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_UP) {
            performClick();
        }
        return true;
    }

    @Override
    public boolean performClick() {
        setChecked(!checked);
        return true;
    }

    @Override
    protected void onMeasure(int ws, int hs) {
        int w = Liquid.dp(getContext(), 62);
        int h = Liquid.dp(getContext(), 34);
        setMeasuredDimension(resolveSize(w, ws), resolveSize(h, hs));
    }

    @Override
    protected void onDraw(Canvas cv) {
        float h = getHeight();
        float pad = getPaddingTop();
        float top = pad, bottom = h - pad;
        float r = (bottom - top) / 2f;

        // 外发光
        if (v > 0.05f && !Liquid.reduceMotion(getContext())) {
            glow.setAlpha((int) (46 * v));
            cv.drawCircle(getWidth() * 0.5f, h * 0.5f, r * 2.1f, glow);
        }

        track.setColor(Liquid.blend(0xFF2A313C, Liquid.ACCENT, v));
        box.set(0, top, getWidth(), bottom);
        cv.drawRoundRect(box, r, r, track);

        // 滑块：过冲时轻微挤压，像液滴被甩出去
        float cx = r + (getWidth() - 2 * r) * v;
        float stretch = 1f + 0.14f * (float) Math.sin(Math.PI * Math.min(1f, Math.max(0f, v)));
        float kr = (r - Liquid.dp(getContext(), 4)) * stretch;
        float kh = (r - Liquid.dp(getContext(), 4)) / stretch;
        cv.drawOval(cx - kr, h * 0.5f - kh, cx + kr, h * 0.5f + kh, knob);
    }
}
