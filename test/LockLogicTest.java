package cn.dsh.alaslock;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/** 独立运行的核心逻辑自测：javac + java，不依赖 Android。 */
public class LockLogicTest {

    private static int failed = 0;

    private static Set<String> set(String... xs) {
        return new LinkedHashSet<String>(Arrays.asList(xs));
    }

    private static void eq(String name, Object got, Object want) {
        boolean ok = (got == null && want == null) || (got != null && got.equals(want));
        if (!ok) {
            failed++;
            System.out.println("FAIL " + name + "\n  got : " + got + "\n  want: " + want);
        } else {
            System.out.println("ok   " + name + " -> " + got);
        }
    }

    public static void main(String[] args) {
        eq("parse(null)", LockLogic.parse(null).size(), 0);
        eq("parse 尾随冒号", LockLogic.parse("a:b:"), set("a", "b"));
        eq("parse 真实格式", LockLogic.parse("com.foo/.Svc:com.bar/com.bar.B"),
                set("com.foo/.Svc", "com.bar/com.bar.B"));

        eq("补回缺失", LockLogic.withTargets("a:b", set("c")), "a:b:c");
        eq("已存在不重复", LockLogic.withTargets("a:b", set("b")), "a:b");
        eq("空列表补回", LockLogic.withTargets("", set("x")), "x");
        eq("null 补回", LockLogic.withTargets(null, set("x")), "x");
        eq("保留其它服务", LockLogic.withTargets("keep:other", set("mine")), "keep:other:mine");

        eq("摘掉目标", LockLogic.withoutTargets("a:b:c", set("b")), "a:c");
        eq("摘掉唯一", LockLogic.withoutTargets("a", set("a")), "");
        eq("摘掉不存在", LockLogic.withoutTargets("a", set("zz")), "a");

        eq("全部在线", LockLogic.containsAll("a:b", set("a", "b")), true);
        eq("有缺失", LockLogic.containsAll("a", set("a", "b")), false);
        eq("空目标视为在线", LockLogic.containsAll("a", set()), true);

        // 模拟一次掉线：系统把 a:b 改成只剩 a，守护补回
        String afterKill = LockLogic.withoutTargets("a:b", set("b"));
        eq("掉线后状态", afterKill, "a");
        eq("守护修复", LockLogic.withTargets(afterKill, set("b")), "a:b");


        // 决策表：谁该被拨回来
        eq("都正常", LockLogic.action("a", true, set("a"), true), LockLogic.NONE);
        eq("少了目标", LockLogic.action("a", true, set("a", "b"), true), LockLogic.LIST);
        eq("开关被关+强制", LockLogic.action("a", false, set("a"), true), LockLogic.SWITCH);
        eq("开关被关+普通", LockLogic.action("a", false, set("a"), false), LockLogic.NONE);
        eq("全没了+强制", LockLogic.action("", false, set("b"), true), LockLogic.BOTH);
        eq("全没了+普通", LockLogic.action("", false, set("b"), false), LockLogic.LIST);
        eq("null+强制", LockLogic.action(null, true, set("x"), true), LockLogic.LIST);

        System.out.println(failed == 0 ? "\nALL PASS" : "\n" + failed + " FAILED");
        System.exit(failed == 0 ? 0 : 1);
    }
}
