package cn.dsh.alaslock;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 无障碍开关字符串（ENABLED_ACCESSIBILITY_SERVICES）的解析与拼接。
 * 纯函数、不依赖 Android，方便单独测。
 */
final class LockLogic {

    private LockLogic() {
    }

    static Set<String> parse(String s) {
        Set<String> out = new LinkedHashSet<String>();
        if (s == null) {
            return out;
        }
        for (String part : s.split(":")) {
            String t = part.trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }

    static String join(Collection<String> items) {
        StringBuilder sb = new StringBuilder();
        for (String i : items) {
            if (i == null || i.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(':');
            }
            sb.append(i);
        }
        return sb.toString();
    }

    /** 在现有列表末尾补齐缺失的目标，保留原有顺序与其它服务。 */
    static String withTargets(String cur, Set<String> targets) {
        Set<String> all = parse(cur);
        all.addAll(targets);
        return join(all);
    }

    /** 从现有列表里摘掉目标。 */
    static String withoutTargets(String cur, Set<String> targets) {
        Set<String> all = parse(cur);
        all.removeAll(targets);
        return join(all);
    }

    static boolean containsAll(String cur, Set<String> targets) {
        return parse(cur).containsAll(targets);
    }

    static final int NONE = 0;
    static final int LIST = 1;
    static final int SWITCH = 2;
    static final int BOTH = LIST | SWITCH;

    /**
     * 决定这一轮要做什么：
     *   列表里缺目标 -> 补列表；总开关被关 -> 仅强制模式下拨回。
     */
    static int action(String cur, boolean accOn, Set<String> targets, boolean strict) {
        int a = NONE;
        if (!containsAll(cur, targets)) {
            a |= LIST;
        }
        if (!accOn && strict) {
            a |= SWITCH;
        }
        return a;
    }

}
