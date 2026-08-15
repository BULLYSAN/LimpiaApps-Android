package com.limpiaapps.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final int NAVY = Color.rgb(11, 23, 57);
    private static final int BLUE = Color.rgb(37, 99, 235);
    private static final int GREEN = Color.rgb(22, 163, 74);
    private static final int ORANGE = Color.rgb(234, 88, 12);
    private static final int RED = Color.rgb(220, 38, 38);
    private static final int LIGHT = Color.rgb(246, 248, 252);
    private static final int MUTED = Color.rgb(100, 116, 139);

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private List<AppItem> allApps = new ArrayList<>();
    private List<AppItem> shownApps = new ArrayList<>();
    private AppListAdapter adapter;

    private EditText search;
    private Button filterRecommended;
    private Button filterReview;
    private Button filterAll;
    private TextView statusText;
    private TextView summaryRecommended;
    private TextView summaryReview;
    private TextView summaryProtected;
    private LinearLayout permissionCard;
    private ProgressBar progress;
    private ListView listView;

    private String activeFilter = "recommended";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
        scanApps();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (listView != null) scanApps();
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private View buildUi() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(LIGHT);

        page.addView(buildHeader());

        permissionCard = buildPermissionCard();
        page.addView(permissionCard);

        page.addView(buildSummary());

        search = new EditText(this);
        search.setSingleLine(true);
        search.setHint("Buscar aplicación");
        search.setTextSize(16);
        search.setPadding(dp(16), 0, dp(16), 0);
        search.setBackground(roundRect(Color.WHITE, dp(14), Color.rgb(226,232,240), 1));
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        searchLp.setMargins(dp(16), dp(10), dp(16), dp(10));
        page.addView(search, searchLp);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) { applyFilter(); }
            public void afterTextChanged(Editable e) {}
        });

        page.addView(buildFilters());

        progress = new ProgressBar(this);
        LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(dp(38), dp(38));
        pLp.gravity = Gravity.CENTER_HORIZONTAL;
        pLp.setMargins(0, dp(24), 0, dp(12));
        page.addView(progress, pLp);

        statusText = text("Analizando aplicaciones…", 14, MUTED, false);
        statusText.setGravity(Gravity.CENTER);
        page.addView(statusText, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(34)));

        listView = new ListView(this);
        listView.setDividerHeight(0);
        listView.setBackgroundColor(LIGHT);
        adapter = new AppListAdapter(this);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> showAppDetails(shownApps.get(position)));

        LinearLayout.LayoutParams listLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        listLp.setMargins(dp(8), 0, dp(8), dp(8));
        page.addView(listView, listLp);

        return page;
    }

    private View buildHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(20), dp(18), dp(20), dp(16));
        header.setBackgroundColor(NAVY);

        TextView title = text("LimpiaApps", 28, Color.WHITE, true);
        header.addView(title);

        TextView subtitle = text(
                "Detecta apps que quizá ya no necesitas y desinstálalas con seguridad.",
                14, Color.rgb(203, 213, 225), false);
        subtitle.setPadding(0, dp(4), 0, 0);
        header.addView(subtitle);

        return header;
    }

    private LinearLayout buildPermissionCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(12), dp(12), dp(12));
        card.setBackground(roundRect(Color.rgb(239, 246, 255), dp(14), Color.rgb(191,219,254), 1));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(16), dp(14), dp(16), 0);
        card.setLayoutParams(lp);

        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        words.addView(text("Mejora el análisis", 15, NAVY, true));
        words.addView(text(
                "Activa “Acceso de uso” para detectar apps que llevan semanas o meses sin abrirse.",
                13, MUTED, false),
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        card.addView(words, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button grant = smallButton("ACTIVAR", BLUE);
        grant.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                startActivity(intent);
            } catch (Exception e) {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            }
        });
        card.addView(grant);

        return card;
    }

    private View buildSummary() {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.HORIZONTAL);
        wrap.setPadding(dp(12), dp(12), dp(12), 0);
        wrap.setWeightSum(3f);

        summaryRecommended = makeStat("0", "Recomendadas", RED);
        summaryReview = makeStat("0", "Revisar", ORANGE);
        summaryProtected = makeStat("0", "Protegidas", GREEN);

        wrap.addView((View) summaryRecommended.getTag(), statLp());
        wrap.addView((View) summaryReview.getTag(), statLp());
        wrap.addView((View) summaryProtected.getTag(), statLp());

        return wrap;
    }

    private TextView makeStat(String value, String label, int color) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(8), dp(10), dp(8), dp(10));
        card.setBackground(roundRect(Color.WHITE, dp(14), Color.rgb(226,232,240), 1));

        TextView number = text(value, 23, color, true);
        number.setGravity(Gravity.CENTER);
        TextView caption = text(label, 11, MUTED, true);
        caption.setGravity(Gravity.CENTER);
        card.addView(number);
        card.addView(caption);

        number.setTag(card);
        return number;
    }

    private LinearLayout.LayoutParams statLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(70), 1f);
        lp.setMargins(dp(4), 0, dp(4), 0);
        return lp;
    }

    private View buildFilters() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(12), 0, dp(12), dp(8));

        filterRecommended = filterButton("Recomendadas", true);
        filterReview = filterButton("Revisar", false);
        filterAll = filterButton("Todas", false);

        filterRecommended.setOnClickListener(v -> { activeFilter = "recommended"; updateFilterButtons(); applyFilter(); });
        filterReview.setOnClickListener(v -> { activeFilter = "review"; updateFilterButtons(); applyFilter(); });
        filterAll.setOnClickListener(v -> { activeFilter = "all"; updateFilterButtons(); applyFilter(); });

        row.addView(filterRecommended, new LinearLayout.LayoutParams(0, dp(42), 1f));
        LinearLayout.LayoutParams middle = new LinearLayout.LayoutParams(0, dp(42), 1f);
        middle.setMargins(dp(6), 0, dp(6), 0);
        row.addView(filterReview, middle);
        row.addView(filterAll, new LinearLayout.LayoutParams(0, dp(42), 1f));
        return row;
    }

    private Button filterButton(String label, boolean selected) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(label);
        b.setTextSize(13);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        styleFilterButton(b, selected);
        return b;
    }

    private void styleFilterButton(Button b, boolean selected) {
        b.setTextColor(selected ? Color.WHITE : NAVY);
        b.setBackground(roundRect(selected ? BLUE : Color.WHITE, dp(12),
                selected ? BLUE : Color.rgb(226,232,240), 1));
    }

    private void updateFilterButtons() {
        styleFilterButton(filterRecommended, activeFilter.equals("recommended"));
        styleFilterButton(filterReview, activeFilter.equals("review"));
        styleFilterButton(filterAll, activeFilter.equals("all"));
    }

    private void scanApps() {
        if (progress == null) return;
        progress.setVisibility(View.VISIBLE);
        statusText.setVisibility(View.VISIBLE);
        statusText.setText("Analizando aplicaciones…");

        executor.submit(() -> {
            List<AppItem> result = AppScanner.scan(this);
            runOnUiThread(() -> {
                allApps = result;
                boolean access = AppScanner.hasUsageAccess(this);
                permissionCard.setVisibility(access ? View.GONE : View.VISIBLE);
                updateSummary();
                applyFilter();
                progress.setVisibility(View.GONE);
            });
        });
    }

    private void updateSummary() {
        int rec = 0, review = 0, prot = 0;
        for (AppItem a : allApps) {
            if (a.protectedApp) prot++;
            else if (a.score >= 65 && !a.keep) rec++;
            else if (a.score >= 35 && !a.keep) review++;
        }
        summaryRecommended.setText(String.valueOf(rec));
        summaryReview.setText(String.valueOf(review));
        summaryProtected.setText(String.valueOf(prot));
    }

    private void applyFilter() {
        if (adapter == null) return;
        String q = search == null ? "" : search.getText().toString().trim().toLowerCase(Locale.ROOT);

        shownApps = new ArrayList<>();
        for (AppItem a : allApps) {
            boolean matchesText = q.isEmpty()
                    || a.name.toLowerCase(Locale.ROOT).contains(q)
                    || a.packageName.toLowerCase(Locale.ROOT).contains(q);

            if (!matchesText) continue;

            boolean matchesFilter;
            switch (activeFilter) {
                case "recommended":
                    matchesFilter = !a.protectedApp && !a.keep && a.score >= 65;
                    break;
                case "review":
                    matchesFilter = !a.protectedApp && !a.keep && a.score >= 35;
                    break;
                default:
                    matchesFilter = true;
            }

            if (matchesFilter) shownApps.add(a);
        }

        adapter.notifyDataSetChanged();
        statusText.setVisibility(shownApps.isEmpty() ? View.VISIBLE : View.GONE);
        if (shownApps.isEmpty()) {
            statusText.setText(activeFilter.equals("recommended")
                    ? "No hay aplicaciones claramente recomendadas para revisar."
                    : "No se encontraron aplicaciones con este filtro.");
        }
    }

    private void showAppDetails(AppItem app) {
        ScrollView scroll = new ScrollView(this);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(22), dp(18), dp(22), dp(16));
        scroll.addView(body);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        ImageView icon = new ImageView(this);
        icon.setImageDrawable(app.icon);
        top.addView(icon, new LinearLayout.LayoutParams(dp(58), dp(58)));

        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        names.setPadding(dp(14), 0, 0, 0);
        names.addView(text(app.name, 20, NAVY, true));
        names.addView(text(app.packageName, 12, MUTED, false));
        top.addView(names, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        body.addView(top);

        TextView badge = text(app.recommendation() + " · " + app.score + "/100",
                14, recommendationColor(app), true);
        badge.setPadding(dp(12), dp(8), dp(12), dp(8));
        badge.setBackground(roundRect(withAlpha(recommendationColor(app), 24), dp(10),
                withAlpha(recommendationColor(app), 60), 1));
        LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        badgeLp.setMargins(0, dp(16), 0, dp(12));
        body.addView(badge, badgeLp);

        body.addView(text("Motivos", 16, NAVY, true));
        if (app.reasons.isEmpty()) {
            body.addView(paragraph("No hay señales importantes para recomendar su desinstalación."));
        } else {
            for (String reason : app.reasons) body.addView(paragraph("• " + reason));
        }

        body.addView(section("Información"));
        body.addView(paragraph("Instalada: " + formatDate(app.firstInstallTime)));
        body.addView(paragraph("Última actualización: " + formatDate(app.lastUpdateTime)));
        body.addView(paragraph("Último uso registrado: " +
                (app.lastUsedTime > 0 ? formatDate(app.lastUsedTime) : "sin datos")));
        body.addView(paragraph("Tamaño aproximado del APK: " + formatBytes(app.apkBytes)));
        body.addView(paragraph(app.protectedApp
                ? "Estado: aplicación del sistema. LimpiaApps bloquea su desinstalación desde esta pantalla."
                : "Estado: aplicación instalada por el usuario."));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(scroll)
                .setNegativeButton("Cerrar", null)
                .create();

        if (!app.protectedApp) {
            dialog.setButton(AlertDialog.BUTTON_NEUTRAL, app.keep ? "Quitar de mantener" : "Mantener", (d, which) -> {});
            dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Desinstalar", (d, which) -> {});
        }

        dialog.setOnShowListener(v -> {
            if (!app.protectedApp) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(RED);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn -> {
                    new AlertDialog.Builder(this)
                            .setTitle("Confirmar desinstalación")
                            .setMessage("Android mostrará ahora su pantalla oficial para desinstalar “" + app.name + "”.\n\nLimpiaApps nunca borra una app sin tu confirmación.")
                            .setNegativeButton("Cancelar", null)
                            .setPositiveButton("Continuar", (x, y) -> {
                                dialog.dismiss();
                                uninstall(app);
                            })
                            .show();
                });

                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(btn -> {
                    app.keep = !app.keep;
                    getSharedPreferences("keep_apps", Context.MODE_PRIVATE)
                            .edit().putBoolean(app.packageName, app.keep).apply();
                    ScoreEngine.score(app, AppScanner.hasUsageAccess(this));
                    dialog.dismiss();
                    updateSummary();
                    applyFilter();
                    Toast.makeText(this,
                            app.keep ? "Marcada para mantener" : "Ya puede volver a aparecer en recomendaciones",
                            Toast.LENGTH_SHORT).show();
                });
            }
        });

        dialog.show();
    }

    private void uninstall(AppItem app) {
        try {
            Intent intent = new Intent(Intent.ACTION_DELETE);
            intent.setData(Uri.parse("package:" + app.packageName));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo abrir el desinstalador.", Toast.LENGTH_LONG).show();
        }
    }

    private int recommendationColor(AppItem app) {
        if (app.protectedApp || app.keep || app.score < 35) return GREEN;
        if (app.score >= 65) return RED;
        return ORANGE;
    }

    private TextView section(String s) {
        TextView v = text(s, 16, NAVY, true);
        v.setPadding(0, dp(18), 0, dp(4));
        return v;
    }

    private TextView paragraph(String s) {
        TextView v = text(s, 14, Color.rgb(51,65,85), false);
        v.setPadding(0, dp(4), 0, dp(4));
        return v;
    }

    private Button smallButton(String label, int bg) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(12);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(Color.WHITE);
        b.setBackground(roundRect(bg, dp(10), bg, 0));
        return b;
    }

    private TextView text(String s, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private GradientDrawable roundRect(int fill, int radius, int stroke, int strokeWidthDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(radius);
        if (strokeWidthDp > 0) d.setStroke(dp(strokeWidthDp), stroke);
        return d;
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private String formatDate(long millis) {
        if (millis <= 0) return "sin datos";
        return DateFormat.getMediumDateFormat(this).format(new Date(millis));
    }

    private String formatBytes(long bytes) {
        if (bytes <= 0) return "sin datos";
        double mb = bytes / (1024.0 * 1024.0);
        if (mb >= 1024) return new DecimalFormat("0.0").format(mb / 1024.0) + " GB";
        return new DecimalFormat("0").format(mb) + " MB";
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private class AppListAdapter extends BaseAdapter {
        private final Context context;

        AppListAdapter(Context context) {
            this.context = context;
        }

        @Override public int getCount() { return shownApps.size(); }
        @Override public Object getItem(int position) { return shownApps.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            AppItem app = shownApps.get(position);

            LinearLayout card = new LinearLayout(context);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(dp(12), dp(12), dp(12), dp(12));
            card.setBackground(roundRect(Color.WHITE, dp(14), Color.rgb(226,232,240), 1));

            AbsListView.LayoutParams outer = new AbsListView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(92));
            card.setLayoutParams(outer);

            ImageView icon = new ImageView(context);
            icon.setImageDrawable(app.icon);
            card.addView(icon, new LinearLayout.LayoutParams(dp(52), dp(52)));

            LinearLayout center = new LinearLayout(context);
            center.setOrientation(LinearLayout.VERTICAL);
            center.setPadding(dp(12), 0, dp(8), 0);

            TextView name = text(app.name, 16, NAVY, true);
            name.setMaxLines(1);
            center.addView(name);

            String reason = app.reasons.isEmpty() ? app.packageName : app.reasons.get(0);
            TextView sub = text(reason, 12, MUTED, false);
            sub.setMaxLines(2);
            center.addView(sub);

            card.addView(center, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            LinearLayout right = new LinearLayout(context);
            right.setOrientation(LinearLayout.VERTICAL);
            right.setGravity(Gravity.CENTER);

            int c = recommendationColor(app);
            TextView score = text(app.protectedApp ? "🔒" : String.valueOf(app.score), 18, c, true);
            score.setGravity(Gravity.CENTER);
            right.addView(score);

            TextView label = text(app.protectedApp ? "Sistema" :
                    (app.keep ? "Mantener" : (app.score >= 65 ? "Revisar" : app.score >= 35 ? "Valorar" : "OK")),
                    10, c, true);
            label.setGravity(Gravity.CENTER);
            right.addView(label);

            card.addView(right, new LinearLayout.LayoutParams(dp(64), ViewGroup.LayoutParams.WRAP_CONTENT));

            LinearLayout shell = new LinearLayout(context);
            shell.setPadding(dp(4), dp(4), dp(4), dp(4));
            shell.addView(card, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(88)));
            return shell;
        }
    }
}
