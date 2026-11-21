package cn.dsh.alaslock;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {

    private static final String PKG = "cn.dsh.alaslock";
    private static final String ADB_CMD =
            "adb shell pm grant " + PKG + " android.permission.WRITE_SECURE_SETTINGS";
    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private TextView pill;
    private TextView vAuth;
    private TextView vGuard;
    private TextView vBattery;
    private TextView vFixes;
    private LinearLayout listBox;
    private TextView primary;
    private EditText manual;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setBackgroundDrawable(new ColorDrawable(Ui.BG));
        try {
            getWindow().setStatusBarColor(Ui.BG);
            getWindow().setNavigationBarColor(Ui.BG);
        } catch (Throwable ignored) {
        }
        build();
        fillList();
        refresh();
        askNotif();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private int dp(float v) {
        return Ui.dp(this, v);
    }

    // ---------------------------------------------------------------- 界面

    private void build() {
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Ui.BG);
        sv.setFillViewport(true);

        LinearLayout root = Ui.col(this);
        root.setPadding(dp(18), dp(26), dp(18), dp(36));
        sv.addView(root, new ScrollView.LayoutParams(MATCH, WRAP));

        root.addView(Ui.tv(this, "无障碍锁", 28, Ui.TEXT, true));
        root.addView(spacer(6));
        root.addView(Ui.tv(this, "系统一关，3 秒内拨回来", 13, Ui.MUTED, false));

        pill = Ui.tv(this, "", 12, Ui.ACCENT, true);
        pill.setPadding(dp(12), dp(7), dp(12), dp(7));
        root.addView(pill, Ui.lpm(this, WRAP, WRAP, 16));

        // 状态卡
        LinearLayout card = Ui.col(this);
        card.setBackground(Ui.roundStroke(this, Ui.CARD, Ui.STROKE, 18, 1));
        card.setPadding(dp(6), dp(6), dp(6), dp(6));
        root.addView(card, Ui.lpm(this, MATCH, WRAP, 18));

        vAuth = statRow(card, "授权");
        vGuard = statRow(card, "守护");
        vBattery = statRow(card, "电池");
        vFixes = statRow(card, "恢复");

        LinearLayout strictRow = Ui.row(this);
        strictRow.setPadding(dp(12), dp(12), dp(8), dp(12));
        LinearLayout strictTexts = Ui.col(this);
        strictTexts.addView(Ui.tv(this, "强制保护", 14, Ui.TEXT, false));
        strictTexts.addView(Ui.tv(this, "手动关掉也拨回", 11, Ui.MUTED, false));
        strictRow.addView(strictTexts, Ui.weight(0));
        Switch strictSwitch = new Switch(this);
        strictSwitch.setChecked(Prefs.strict(this));
        strictSwitch.setOnCheckedChangeListener((btn, checked) -> {
            Prefs.setStrict(this, checked);
            refresh();
        });
        strictRow.addView(strictSwitch);
        card.addView(strictRow);

        // 服务列表
        TextView sec = Ui.tv(this, "要守护的服务", 14, Ui.MUTED, true);
        root.addView(sec, Ui.lpm(this, MATCH, WRAP, 24));
        listBox = Ui.col(this);
        root.addView(listBox, Ui.lpm(this, MATCH, WRAP, 6));

        // 主按钮
        primary = Ui.tv(this, "启动守护", 16, Ui.BG, true);
        primary.setGravity(Gravity.CENTER);
        primary.setPadding(0, dp(15), 0, dp(15));
        primary.setClickable(true);
        primary.setOnClickListener(v -> onPrimary());
        root.addView(primary, Ui.lpm(this, MATCH, WRAP, 22));

        // 次级操作
        link(root, "强制重连  ·  开关没关但服务死了", this::forceRebind);
        link(root, "申请电池白名单  ·  别让系统杀它", this::askBattery);
        link(root, "复制 ADB 授权命令", this::copyCmd);
        link(root, "打开系统无障碍设置", this::openA11ySettings);

        // 手动填
        TextView adv = Ui.tv(this, "找不到？手动填 包名/服务类名", 12, Ui.MUTED, false);
        root.addView(adv, Ui.lpm(this, MATCH, WRAP, 22));
        manual = new EditText(this);
        manual.setHint("com.example/.MyService");
        manual.setHintTextColor(Ui.MUTED);
        manual.setTextColor(Ui.TEXT);
        manual.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        manual.setBackground(Ui.roundStroke(this, Ui.CARD, Ui.STROKE, 12, 1));
        manual.setPadding(dp(14), dp(10), dp(14), dp(10));
        manual.setInputType(InputType.TYPE_CLASS_TEXT);
        root.addView(manual, Ui.lpm(this, MATCH, WRAP, 8));

        setContentView(sv);
    }

    private View spacer(float h) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(h)));
        return v;
    }

    private TextView statRow(LinearLayout parent, String label) {
        LinearLayout r = Ui.row(this);
        r.setPadding(dp(12), dp(11), dp(12), dp(11));
        r.addView(Ui.tv(this, label, 13, Ui.MUTED, false));
        TextView value = Ui.tv(this, "—", 13, Ui.TEXT, false);
        value.setGravity(Gravity.END);
        r.addView(value, Ui.weight(0));
        parent.addView(r);
        return value;
    }

    private void link(LinearLayout parent, String text, Runnable action) {
        TextView t = Ui.tv(this, text, 13, Ui.MUTED, false);
        t.setBackground(Ui.round(this, Ui.CARD, 12));
        t.setPadding(dp(14), dp(13), dp(14), dp(13));
        t.setClickable(true);
        t.setOnClickListener(v -> action.run());
        parent.addView(t, Ui.lpm(this, MATCH, WRAP, 8));
    }

    // ------------------------------------------------------------ 服务列表

    private void fillList() {
        listBox.removeAllViews();
        Set<String> targets = Prefs.targets(this);
        Set<String> shown = new LinkedHashSet<String>();

        String cur = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        for (String s : LockLogic.parse(cur)) {
            addRow(s, targets, shown);
        }

        try {
            AccessibilityManager am = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
            if (am != null) {
                List<android.accessibilityservice.AccessibilityServiceInfo> infos =
                        am.getInstalledAccessibilityServiceList();
                if (infos != null) {
                    for (android.accessibilityservice.AccessibilityServiceInfo info : infos) {
                        if (info.getResolveInfo() == null) {
                            continue;
                        }
                        ServiceInfo si = info.getResolveInfo().serviceInfo;
                        if (si == null) {
                            continue;
                        }
                        addRow(new ComponentName(si.packageName, si.name).flattenToString(), targets, shown);
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        for (String t : targets) {
            addRow(t, targets, shown);
        }

        if (shown.isEmpty()) {
            TextView empty = Ui.tv(this, "还没发现无障碍服务。先去目标 App 里打开它的无障碍，再回来。",
                    13, Ui.MUTED, false);
            empty.setBackground(Ui.roundStroke(this, Ui.CARD, Ui.STROKE, 14, 1));
            empty.setPadding(dp(14), dp(16), dp(14), dp(16));
            listBox.addView(empty, Ui.lpm(this, MATCH, WRAP, 4));
        }
    }

    private void addRow(String flat, Set<String> targets, Set<String> shown) {
        if (TextUtils.isEmpty(flat) || shown.contains(flat)) {
            return;
        }
        shown.add(flat);

        ComponentName cn = ComponentName.unflattenFromString(flat);
        String app = cn == null ? flat : cn.getPackageName();
        String sub = flat;
        if (cn != null) {
            try {
                CharSequence label = getPackageManager().getApplicationLabel(
                        getPackageManager().getApplicationInfo(cn.getPackageName(), 0));
                app = String.valueOf(label);
            } catch (Throwable ignored) {
            }
            sub = cn.getPackageName() + " / " + cn.getShortClassName();
        }

        boolean checked = targets.contains(flat);

        LinearLayout row = Ui.row(this);
        row.setBackground(checked
                ? Ui.roundStroke(this, Ui.CARD_HI, Ui.ACCENT, 14, 1)
                : Ui.round(this, Ui.CARD, 14));
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setClickable(true);
        row.setOnClickListener(v -> toggle(flat));

        LinearLayout texts = Ui.col(this);
        texts.addView(Ui.tv(this, app, 15, Ui.TEXT, true));
        TextView subView = Ui.tv(this, sub, 10.5f, Ui.MUTED, false);
        subView.setMaxLines(1);
        subView.setEllipsize(TextUtils.TruncateAt.END);
        texts.addView(subView);
        row.addView(texts, Ui.weight(0));

        row.addView(Ui.tv(this, checked ? "✓" : "○", 17,
                checked ? Ui.ACCENT : Ui.STROKE, true));

        listBox.addView(row, Ui.lpm(this, MATCH, WRAP, 8));
    }

    private void toggle(String flat) {
        Set<String> targets = Prefs.targets(this);
        if (!targets.remove(flat)) {
            targets.add(flat);
        }
        Prefs.setTargets(this, targets);
        fillList();
        refresh();
    }

    // -------------------------------------------------------------- 状态

    private void refresh() {
        Set<String> targets = Prefs.targets(this);
        Set<String> have = LockLogic.parse(Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES));
        boolean accOn = "1".equals(Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_ENABLED));

        int online = 0;
        for (String t : targets) {
            if (have.contains(t)) {
                online++;
            }
        }
        boolean running = Prefs.isEnabled(this) && GuardianService.running;
        boolean allOk = accOn && !targets.isEmpty() && online == targets.size();

        if (!running) {
            pill.setText("○  已停止");
            pill.setTextColor(Ui.MUTED);
            pill.setBackground(Ui.round(this, Ui.MUTED_BG, 20));
        } else if (allOk) {
            pill.setText("●  守护中 · 一切正常");
            pill.setTextColor(Ui.GOOD);
            pill.setBackground(Ui.round(this, Ui.GOOD_BG, 20));
        } else {
            pill.setText("●  守护中 · 刚修过");
            pill.setTextColor(Ui.WARN);
            pill.setBackground(Ui.round(this, Ui.WARN_BG, 20));
        }

        boolean canWrite = canWrite();
        vAuth.setText(canWrite ? "已授权" : "未授权 · 点这里复制命令");
        vAuth.setTextColor(canWrite ? Ui.GOOD : Ui.BAD);

        if (targets.isEmpty()) {
            vGuard.setText("没选服务");
            vGuard.setTextColor(Ui.MUTED);
        } else {
            vGuard.setText((running ? "运行中" : "未运行") + " · " + online + "/" + targets.size() + " 在开");
            vGuard.setTextColor(running ? (allOk ? Ui.GOOD : Ui.WARN) : Ui.MUTED);
        }

        vBattery.setText(batteryOk() ? "已忽略优化" : "没加白名单");
        vBattery.setTextColor(batteryOk() ? Ui.GOOD : Ui.WARN);

        vFixes.setText(Prefs.fixes(this) + " 次");
        vFixes.setTextColor(Ui.TEXT);

        if (running) {
            primary.setText("停止守护");
            primary.setTextColor(Ui.BAD);
            primary.setBackground(Ui.roundStroke(this, Ui.CARD, Ui.BAD, 16, 1));
        } else {
            primary.setText("启动守护");
            primary.setTextColor(Ui.BG);
            primary.setBackground(Ui.round(this, Ui.ACCENT, 16));
        }
    }

    private void onPrimary() {
        if (Prefs.isEnabled(this)) {
            stopGuard();
            return;
        }
        if (Prefs.targets(this).isEmpty()) {
            toast("先选一个要守护的服务");
            return;
        }
        if (!canWrite()) {
            showGrantDialog();
            return;
        }
        Prefs.setEnabled(this, true);
        Prefs.ensureJob(this);
        Prefs.startGuardian(this);
        toast("守护已启动");
        refresh();
    }

    private void stopGuard() {
        Prefs.setEnabled(this, false);
        Prefs.cancelJob(this);
        stopService(new Intent(this, GuardianService.class));
        toast("已停止守护");
        refresh();
    }

    // ------------------------------------------------------------ 动作

    private void forceRebind() {
        if (!Prefs.isEnabled(this)) {
            toast("先启动守护");
            return;
        }
        if (!canWrite()) {
            showGrantDialog();
            return;
        }
        Intent i = new Intent(this, GuardianService.class);
        i.putExtra("force", true);
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(i);
            } else {
                startService(i);
            }
            toast("已发送重连请求");
        } catch (Throwable t) {
            toast("发送失败：" + t.getClass().getSimpleName());
        }
        refresh();
    }

    private void askBattery() {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null && pm.isIgnoringBatteryOptimizations(getPackageName())) {
                toast("已经在电池白名单里了");
                return;
            }
            Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            i.setData(Uri.parse("package:" + getPackageName()));
            startActivity(i);
        } catch (Throwable t) {
            try {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            } catch (Throwable t2) {
                toast("请手动到 设置→电池 设成「无限制」");
            }
        }
    }

    private void copyCmd() {
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("cmd", ADB_CMD));
            }
            toast("已复制，去电脑或 Shizuku 终端执行");
        } catch (Throwable t) {
            toast(ADB_CMD);
        }
    }

    private void openA11ySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } catch (Throwable t) {
            toast("打不开，请手动进 设置→无障碍");
        }
    }

    private void showGrantDialog() {
        new AlertDialog.Builder(this)
                .setTitle("还差一步授权")
                .setMessage("授权后才能真正把开关拨回去。任意一种：\n\n"
                        + "① 电脑 USB 调试，执行：\n" + ADB_CMD + "\n\n"
                        + "② Shizuku 终端里执行同一行（去掉 adb）")
                .setPositiveButton("复制命令", (d, w) -> copyCmd())
                .setNegativeButton("知道了", null)
                .show();
    }

    private void askNotif() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
            try {
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 9);
            } catch (Throwable ignored) {
            }
        }
    }

    private boolean canWrite() {
        return checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS")
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean batteryOk() {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            return pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
        } catch (Throwable t) {
            return false;
        }
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
