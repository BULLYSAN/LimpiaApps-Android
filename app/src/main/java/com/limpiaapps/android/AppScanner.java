package com.limpiaapps.android;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Process;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AppScanner {

    public static boolean hasUsageAccess(Context context) {
        try {
            AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.getPackageName()
            );
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            return false;
        }
    }

    public static List<AppItem> scan(Context context) {
        PackageManager pm = context.getPackageManager();
        boolean usageAllowed = hasUsageAccess(context);

        Map<String, Long> usage = usageAllowed
                ? loadLastUsed(context)
                : Collections.emptyMap();

        Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> launchers = pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL);
        LinkedHashMap<String, AppItem> unique = new LinkedHashMap<>();

        for (ResolveInfo ri : launchers) {
            if (ri.activityInfo == null || ri.activityInfo.applicationInfo == null) continue;

            ApplicationInfo ai = ri.activityInfo.applicationInfo;
            String pkg = ai.packageName;

            // No analizamos la propia app.
            if (context.getPackageName().equals(pkg)) continue;

            AppItem item = new AppItem();
            item.packageName = pkg;
            item.name = String.valueOf(pm.getApplicationLabel(ai));
            item.icon = ai.loadIcon(pm);
            item.systemApp = (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            item.protectedApp = item.systemApp;

            try {
                PackageInfo pi = pm.getPackageInfo(pkg, 0);
                item.firstInstallTime = pi.firstInstallTime;
                item.lastUpdateTime = pi.lastUpdateTime;
            } catch (Exception ignored) {}

            try {
                if (ai.sourceDir != null) {
                    item.apkBytes = new File(ai.sourceDir).length();
                }
            } catch (Exception ignored) {}

            Long last = usage.get(pkg);
            item.lastUsedTime = last == null ? 0L : last;

            item.keep = context.getSharedPreferences("keep_apps", Context.MODE_PRIVATE)
                    .getBoolean(pkg, false);

            ScoreEngine.score(item, usageAllowed);
            unique.put(pkg, item);
        }

        List<AppItem> result = new ArrayList<>(unique.values());
        result.sort((a, b) -> {
            int byScore = Integer.compare(b.score, a.score);
            if (byScore != 0) return byScore;
            return a.name.compareToIgnoreCase(b.name);
        });
        return result;
    }

    private static Map<String, Long> loadLastUsed(Context context) {
        HashMap<String, Long> out = new HashMap<>();
        UsageStatsManager usm =
                (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);

        long now = System.currentTimeMillis();
        long begin = now - (400L * 24L * 60L * 60L * 1000L);

        List<UsageStats> stats = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                begin,
                now
        );

        if (stats == null) return out;

        for (UsageStats s : stats) {
            if (s == null || s.getPackageName() == null) continue;
            long last = s.getLastTimeUsed();
            Long current = out.get(s.getPackageName());
            if (current == null || last > current) {
                out.put(s.getPackageName(), last);
            }
        }

        return out;
    }
}
