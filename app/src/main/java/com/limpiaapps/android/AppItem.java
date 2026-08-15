package com.limpiaapps.android;

import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.List;

public class AppItem {
    public String packageName;
    public String name;
    public Drawable icon;
    public boolean systemApp;
    public boolean protectedApp;
    public boolean keep;
    public long firstInstallTime;
    public long lastUpdateTime;
    public long lastUsedTime;
    public long apkBytes;
    public int score;
    public final List<String> reasons = new ArrayList<>();

    public String recommendation() {
        if (protectedApp) return "PROTEGIDA";
        if (keep) return "MANTENER";
        if (score >= 65) return "RECOMENDADO REVISAR";
        if (score >= 35) return "REVISAR";
        return "MANTENER";
    }
}
