package com.limpiaapps.android;

import android.text.format.DateUtils;

import java.util.Locale;

public final class ScoreEngine {
    private ScoreEngine() {}

    public static void score(AppItem app, boolean hasUsageAccess) {
        app.score = 0;
        app.reasons.clear();

        if (app.systemApp || app.protectedApp) {
            app.score = 0;
            app.reasons.add("Aplicación del sistema o componente protegido");
            return;
        }

        long now = System.currentTimeMillis();

        if (hasUsageAccess) {
            if (app.lastUsedTime <= 0) {
                long daysInstalled = daysBetween(app.firstInstallTime, now);
                if (daysInstalled >= 30) {
                    app.score += 55;
                    app.reasons.add("No aparece como utilizada en el historial disponible");
                } else {
                    app.score += 15;
                    app.reasons.add("Instalada recientemente y todavía sin uso registrado");
                }
            } else {
                long days = daysBetween(app.lastUsedTime, now);
                if (days >= 180) {
                    app.score += 70;
                    app.reasons.add("Sin usar desde hace " + days + " días");
                } else if (days >= 90) {
                    app.score += 55;
                    app.reasons.add("Sin usar desde hace " + days + " días");
                } else if (days >= 45) {
                    app.score += 35;
                    app.reasons.add("Poco utilizada: último uso hace " + days + " días");
                } else if (days >= 21) {
                    app.score += 15;
                    app.reasons.add("Último uso hace " + days + " días");
                }
            }
        } else {
            app.reasons.add("Concede Acceso de uso para valorar cuánto tiempo lleva sin abrirse");
        }

        long mb = app.apkBytes / (1024L * 1024L);
        if (mb >= 500) {
            app.score += 15;
            app.reasons.add("APK grande: aproximadamente " + mb + " MB");
        } else if (mb >= 200) {
            app.score += 8;
            app.reasons.add("APK de tamaño elevado: aproximadamente " + mb + " MB");
        }

        String n = app.name == null ? "" : app.name.toLowerCase(Locale.ROOT);
        String[] auxiliary = {
                "cleaner", "booster", "optimizer", "optimiser", "speed up",
                "battery saver", "ram cleaner", "phone master", "cache cleaner",
                "limpiador", "optimizador", "acelerador"
        };
        for (String token : auxiliary) {
            if (n.contains(token)) {
                app.score += 15;
                app.reasons.add("Nombre asociado a una utilidad de limpieza/optimización; conviene revisar si realmente la necesitas");
                break;
            }
        }

        if (app.keep) {
            app.score = 0;
            app.reasons.clear();
            app.reasons.add("Marcada por ti como aplicación que quieres conservar");
        }

        app.score = Math.max(0, Math.min(100, app.score));
    }

    private static long daysBetween(long from, long to) {
        if (from <= 0 || to <= from) return 0;
        return (to - from) / DateUtils.DAY_IN_MILLIS;
    }
}
