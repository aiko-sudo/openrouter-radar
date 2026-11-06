package app.openrouter.radar;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.TextWatcher;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.util.*;

public class MainActivity extends Activity {
    List<Model> all = new ArrayList<>();
    List<Model> shown = new ArrayList<>();
    String tab = "最新"; String query = "";
    LinearLayout listBox; TextView status; EditText search; FrameLayout alertSwitch; View alertThumb; TextView alertLabel; Spinner periodSpin;
    java.util.Set<String> hideOnce = new java.util.HashSet<>();
    ScrollView scrollView;
    SwipeRefreshLayout swipe;
    java.util.Map<String,String> weeklyMap = new java.util.concurrent.ConcurrentHashMap<>();
    java.util.Set<String> weeklyTried = new java.util.HashSet<>();
    volatile boolean weeklyProbing = false;
    FrameLayout root;

    java.io.File crashFile() { return new java.io.File(getFilesDir(), "crash.txt"); }
    void installCrashTrap() {
        final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            try {
                java.io.FileWriter w = new java.io.FileWriter(crashFile());
                w.write(e + "\n\n" + android.util.Log.getStackTraceString(e));
                w.close();
            } catch (Exception ignore) {}
            if (prev != null) prev.uncaughtException(t, e);
        });
    }
    void showCrashIfAny() {
        try {
            java.io.File f = crashFile();
            if (!f.exists()) return;
            String txt = new String(java.nio.file.Files.readAllBytes(f.toPath()));
            f.delete();
            reportCrash(txt);
            showOverlay("上次异常详情", txt);
        } catch (Throwable ignore) {}
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        installCrashTrap();
        try { buildUi(); load(false); }
        catch (Throwable e) {
            try {
                TextView t = new TextView(this);
                t.setText("启动出错: " + e + "\n" + android.util.Log.getStackTraceString(e));
                setContentView(t);
            } catch (Throwable ignore) {}
        }
    }

    static int dp(Context c, int d) { return (int)(d * c.getResources().getDisplayMetrics().density + 0.5f); }
    static Button compact(Button b) {
        b.setMinHeight(0); b.setMinimumHeight(0); b.setMinWidth(0); b.setMinimumWidth(0);
        b.setPadding(b.getPaddingLeft(), b.getPaddingTop(), b.getPaddingRight(), b.getPaddingBottom());
        return b;
    }
    static GradientDrawable rrect(Context c, int color, int r, int strokeColor) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp(c, r)); g.setShape(GradientDrawable.RECTANGLE);
        if (strokeColor != 0) { g.setStroke(dp(c, 1), strokeColor); }
        return g;
    }

    void buildUi() {
        root = new FrameLayout(this);
        root.setBackgroundColor(0xFFF2F2F7);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(this, 20), dp(this, 28), dp(this, 20), dp(this, 12));
        root.addView(col, new FrameLayout.LayoutParams(-1, -1));

        TextView sub = new TextView(this);
        sub.setText("OpenRouter · 最新 / 免费 / 好用模型");
        sub.setTextSize(14); sub.setTextColor(0xFF8E8E93);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-1, -2);
        subLp.setMargins(0, 0, 0, dp(this, 12));
        sub.setLayoutParams(subLp);
        col.addView(sub);

        status = new TextView(this);
        status.setText("正在加载…"); status.setTextColor(0xFF8E8E93); status.setTextSize(13);
        status.setOnLongClickListener(v -> { runSelfTest(); return true; });
        LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(-1, -2);
        stLp.setMargins(0, 0, 0, dp(this, 10));
        status.setLayoutParams(stLp);
        col.addView(status);

        search = new EditText(this);
        search.setHint("搜索模型名 / ID");
        search.setSingleLine(true);
        search.setTextSize(15);
        search.setTextColor(0xFF000000);
        search.setHintTextColor(0xFFAEAEB2);
        search.setPadding(dp(this, 16), dp(this, 5), dp(this, 16), dp(this, 5));
        search.setBackground(rrect(this, 0xFFFFFFFF, 12, 0xFFE5E5EA));
        search.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { query = s.toString().toLowerCase(); render(); }
            public void beforeTextChanged(CharSequence s,int a,int c,int d) {}
            public void onTextChanged(CharSequence s,int a,int c,int d) {}
        });
        LinearLayout.LayoutParams seLp = new LinearLayout.LayoutParams(-1, -2);
        seLp.setMargins(0, 0, 0, dp(this, 12));
        search.setLayoutParams(seLp);
        col.addView(search);

        LinearLayout seg = new LinearLayout(this);
        seg.setOrientation(LinearLayout.HORIZONTAL);
        seg.setPadding(dp(this, 2), dp(this, 2), dp(this, 2), dp(this, 2));
        seg.setBackground(rrect(this, 0xFFE5E5EA, 10, 0));
        String[] names = {"最新", "免费", "好用", "全部", "收藏"};
        for (String n : names) {
            Button btn = compact(new Button(this));
            btn.setText(n); btn.setTextSize(12); btn.setTypeface(Typeface.DEFAULT);
            btn.setTextColor(0xFF000000);
            btn.setAllCaps(false);
            btn.setBackground(rrect(this, "最新".equals(n) ? 0xFFFFFFFF : 0x00000000, 8, 0));
            btn.setElevation("最新".equals(n) ? dp(this, 2) : 0);
            btn.setPadding(0, dp(this, 2), 0, dp(this, 2));
            btn.setOnClickListener(v -> {
              try {
                tab = n;
                for (int i = 0; i < seg.getChildCount(); i++) {
                    Button ch = (Button) seg.getChildAt(i);
                    boolean on = ch.getText().toString().equals(n);
                    ch.setBackground(rrect(this, on ? 0xFFFFFFFF : 0x00000000, 8, 0));
                    ch.setElevation(on ? dp(this, 2) : 0);
                    ch.setTypeface(on ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
                }
                render();
              } catch (Throwable e) { Toast.makeText(this, "切换出错: " + e, Toast.LENGTH_LONG).show(); }
            });
            btn.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
            seg.addView(btn);
        }
        LinearLayout.LayoutParams segLp = new LinearLayout.LayoutParams(-1, -2);
        segLp.setMargins(0, 0, 0, dp(this, 12));
        seg.setLayoutParams(segLp);
        col.addView(seg);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        SharedPreferences p = getSharedPreferences("radar", Context.MODE_PRIVATE);
        boolean alertOn0 = p.getBoolean(Checker.KEY_ALERT, true);

        LinearLayout alertBox = new LinearLayout(this);
        alertBox.setOrientation(LinearLayout.HORIZONTAL);
        alertBox.setGravity(Gravity.CENTER_VERTICAL);
        alertLabel = new TextView(this);
        alertLabel.setText("新模型提醒");
        alertLabel.setTextSize(13); alertLabel.setTextColor(0xFF3A3A3C);
        alertBox.addView(alertLabel);
        alertSwitch = new FrameLayout(this);
        alertSwitch.setLayoutParams(new LinearLayout.LayoutParams(dp(this, 46), dp(this, 26)));
        LinearLayout.LayoutParams swLp = new LinearLayout.LayoutParams(dp(this, 46), dp(this, 26));
        swLp.setMargins(dp(this, 8), 0, 0, 0);
        alertSwitch.setLayoutParams(swLp);
        final FrameLayout sw = alertSwitch;
        View track = new View(this);
        track.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        sw.addView(track);
        alertThumb = new View(this);
        FrameLayout.LayoutParams thLp = new FrameLayout.LayoutParams(dp(this, 22), dp(this, 22));
        thLp.gravity = Gravity.CENTER_VERTICAL;
        thLp.setMargins(dp(this, 2), 0, 0, 0);
        alertThumb.setLayoutParams(thLp);
        sw.addView(alertThumb);
        final boolean[] alertSt = {alertOn0};
        Runnable paintSw = () -> {
            track.setBackground(rrect(this, alertSt[0] ? 0xFFAEAEB2 : 0xFFFFFFFF, 13, alertSt[0] ? 0 : 0xFFD1D1D6));
            alertThumb.setBackground(rrect(this, 0xFFFFFFFF, 11, 0xFFD1D1D6));
            FrameLayout.LayoutParams lp2 = (FrameLayout.LayoutParams) alertThumb.getLayoutParams();
            lp2.topMargin = 0; lp2.bottomMargin = 0;
            if (alertSt[0]) { lp2.gravity = Gravity.CENTER_VERTICAL | Gravity.END; lp2.leftMargin = 0; lp2.rightMargin = dp(this, 2); }
            else { lp2.gravity = Gravity.CENTER_VERTICAL | Gravity.START; lp2.leftMargin = dp(this, 2); lp2.rightMargin = 0; }
            alertThumb.setLayoutParams(lp2);
        };
        paintSw.run();
        sw.setOnClickListener(v -> {
            alertSt[0] = !alertSt[0];
            p.edit().putBoolean(Checker.KEY_ALERT, alertSt[0]).apply();
            if (alertSt[0]) Checker.schedule(this);
            paintSw.run();
            Toast.makeText(this, alertSt[0] ? "新模型提醒已开启" : "提醒已关闭", Toast.LENGTH_SHORT).show();
        });
        alertBox.addView(sw);
        row.addView(alertBox);
        periodSpin = new Spinner(this);
        periodSpin.setPadding(dp(this, 8), dp(this, 4), dp(this, 8), dp(this, 4));
        ArrayAdapter<String> pa = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, new String[]{"每2小时", "每6小时", "每24小时"}) {
            @Override public View getView(int pos, View cv, android.view.ViewGroup parent) {
                LinearLayout row = cv instanceof LinearLayout ? (LinearLayout) cv : new LinearLayout(MainActivity.this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.removeAllViews();
                TextView t = new TextView(MainActivity.this);
                t.setText(getItem(pos)); t.setTextColor(0xFF3A3A3C); t.setTextSize(13);
                t.setGravity(Gravity.CENTER_VERTICAL);
                row.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
                TextView a = new TextView(MainActivity.this);
                a.setText("▾"); a.setTextColor(0xFFAEAEB2); a.setTextSize(13);
                a.setGravity(Gravity.CENTER_VERTICAL);
                row.addView(a);
                return row;
            }
            @Override public View getDropDownView(int pos, View cv, android.view.ViewGroup parent) {
                TextView t = (TextView) super.getDropDownView(pos, cv, parent);
                t.setTextColor(0xFF000000); t.setTextSize(14); t.setBackgroundColor(0x00000000);
                t.setPadding(dp(MainActivity.this, 14), dp(MainActivity.this, 10), dp(MainActivity.this, 14), dp(MainActivity.this, 10));
                return t;
            }
        };
        pa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        periodSpin.setAdapter(pa);
        periodSpin.setBackground(rrect(this, 0xFFFFFFFF, 9, 0xFFE5E5EA));
        periodSpin.setPopupBackgroundDrawable(rrect(this, 0xFFFFFFFF, 14, 0xFFE5E5EA));
        periodSpin.setDropDownVerticalOffset(dp(this, 4));
        periodSpin.setPadding(dp(this, 10), dp(this, 4), dp(this, 10), dp(this, 4));
        long h = p.getLong(Checker.KEY_PERIOD, 6);
        periodSpin.setSelection(h == 2 ? 0 : h == 24 ? 2 : 1);
        periodSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> par, View v, int pos, long id) {
                long nh = pos == 0 ? 2 : pos == 2 ? 24 : 6;
                p.edit().putLong(Checker.KEY_PERIOD, nh).apply();
                Checker.schedule(MainActivity.this);
            }
            public void onNothingSelected(AdapterView<?> par) {}
        });
        LinearLayout.LayoutParams spinLp = new LinearLayout.LayoutParams(0, -2, 1);
        spinLp.setMargins(dp(this, 10), 0, dp(this, 10), 0);
        periodSpin.setLayoutParams(spinLp);
        row.addView(periodSpin);
        Button hiddenBtn = compact(new Button(this));
        hiddenBtn.setText("隐藏"); hiddenBtn.setAllCaps(false); hiddenBtn.setTextSize(12);
        hiddenBtn.setTextColor(0xFF8E8E93);
        hiddenBtn.setBackground(rrect(this, 0xFFFFFFFF, 9, 0xFFE5E5EA));
        hiddenBtn.setPadding(dp(this, 12), dp(this, 2), dp(this, 12), dp(this, 2));
        hiddenBtn.setOnClickListener(v -> showHiddenDialog());
        row.addView(hiddenBtn);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
        rowLp.setMargins(0, 0, 0, dp(this, 14));
        row.setLayoutParams(rowLp);
        col.addView(row);

        scrollView = new ScrollView(this);
        scrollView.setVerticalScrollBarEnabled(false);
        listBox = new LinearLayout(this);
        listBox.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(listBox);
        swipe = new SwipeRefreshLayout(this);
        swipe.addView(scrollView, new android.view.ViewGroup.LayoutParams(-1, -1));
        swipe.setOnRefreshListener(() -> load(true));
        swipe.setColorSchemeResources(android.R.color.darker_gray);
        col.addView(swipe, new LinearLayout.LayoutParams(-1, 0, 1));
        loadWeeklyCache();
        setContentView(root);
    }

    interface SheetClick { void onClick(int which); }
    Dialog sheet(String title, java.util.List<String> items, SheetClick cb) {
        Dialog d = new Dialog(this);
        d.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(this, 10), dp(this, 10), dp(this, 10), dp(this, 6));
        box.setBackground(rrect(this, 0xFFFFFFFF, 18, 0xFFE5E5EA));
        if (title != null) {
            TextView t = new TextView(this);
            t.setText(title); t.setTextSize(12); t.setTextColor(0xFF8E8E93);
            t.setPadding(dp(this, 12), dp(this, 6), dp(this, 12), dp(this, 6));
            box.addView(t);
        }
        ScrollView sc = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        sc.addView(list);
        list.setLayoutParams(new android.widget.FrameLayout.LayoutParams(-1, -2));
        java.util.List<String> show = items.size() > 40 ? items.subList(0, 40) : items;
        for (int i = 0; i < show.size(); i++) {
            final int idx = i;
            TextView it = new TextView(this);
            it.setText(show.get(i));
            it.setTextSize(15); it.setTextColor(0xFF007AFF);
            it.setPadding(dp(this, 14), dp(this, 12), dp(this, 14), dp(this, 12));
            it.setBackground(rrect(this, 0xFFF2F2F7, 10, 0));
            LinearLayout.LayoutParams itLp = new LinearLayout.LayoutParams(-1, -2);
            itLp.setMargins(0, 0, 0, dp(this, 6));
            it.setLayoutParams(itLp);
            it.setOnClickListener(v -> { d.dismiss(); cb.onClick(idx); });
            list.addView(it);
        }
        box.addView(sc, new LinearLayout.LayoutParams(-1, -2));
        TextView cancel = new TextView(this);
        cancel.setText("取消"); cancel.setTextSize(15); cancel.setTextColor(0xFF8E8E93);
        cancel.setGravity(Gravity.CENTER);
        cancel.setPadding(0, dp(this, 10), 0, dp(this, 6));
        cancel.setOnClickListener(v -> d.dismiss());
        box.addView(cancel);
        d.setContentView(box);
        d.getWindow().setBackgroundDrawable(new ColorDrawable(0x00000000));
        return d;
    }
    void reportCrash(String txt) {
        new Thread(() -> {
            try {
                java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL("http://192.168.2.221:8902/crash").openConnection();
                c.setConnectTimeout(4000); c.setReadTimeout(4000);
                c.setRequestMethod("POST");
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
                c.getOutputStream().write(txt.getBytes("UTF-8"));
                c.getOutputStream().close();
                c.getResponseCode();
            } catch (Exception ignore) {}
        }).start();
    }
    void showOverlay(String title, String txt) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(0xFFF2F2F7);
        box.setPadding(dp(this, 16), dp(this, 40), dp(this, 16), dp(this, 16));
        TextView tt = new TextView(this);
        tt.setText(title); tt.setTextSize(13); tt.setTextColor(0xFF8E8E93);
        box.addView(tt);
        ScrollView sc = new ScrollView(this);
        TextView tv = new TextView(this);
        tv.setText(txt); tv.setTextSize(12); tv.setTextColor(0xFF3A3A3C);
        tv.setTypeface(Typeface.MONOSPACE);
        sc.addView(tv);
        box.addView(sc, new LinearLayout.LayoutParams(-1, 0, 1f));
        Button cp = compact(new Button(this));
        cp.setText("复制"); cp.setAllCaps(false); cp.setTextSize(13); cp.setTextColor(0xFF007AFF);
        cp.setBackground(rrect(this, 0xFFFFFFFF, 9, 0xFFE5E5EA));
        cp.setPadding(dp(this, 12), dp(this, 6), dp(this, 12), dp(this, 6));
        cp.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("report", txt));
            Toast.makeText(this, "已复制，请粘贴发回", Toast.LENGTH_LONG).show();
        });
        Button back = compact(new Button(this));
        back.setText("继续使用"); back.setAllCaps(false); back.setTextSize(13); back.setTextColor(0xFF8E8E93);
        back.setBackground(rrect(this, 0xFFFFFFFF, 9, 0xFFE5E5EA));
        back.setPadding(dp(this, 12), dp(this, 6), dp(this, 12), dp(this, 6));
        back.setOnClickListener(v -> { buildUi(); if (all.isEmpty()) load(false); else { render(); } });
        LinearLayout rb = new LinearLayout(this);
        rb.setOrientation(LinearLayout.HORIZONTAL);
        rb.addView(cp); rb.addView(back);
        box.addView(rb);
        setContentView(box);
    }
    void runSelfTest() {
        StringBuilder log = new StringBuilder();
        try {
            log.append("模型数:").append(all.size()).append("\n");
            if (!all.isEmpty()) {
                Model m = all.get(0);
                log.append("试收藏 ").append(m.id).append("\n");
                Set<String> ss = new HashSet<>(favs());
                ss.add(m.id);
                getSharedPreferences("radar", Context.MODE_PRIVATE).edit().putStringSet("favs", ss).apply();
                log.append("收藏写入OK\n");
                log.append("试render\n");
                render();
                log.append("render OK\n");
            }
        } catch (Throwable e) {
            log.append("!!! EXCEPTION\n").append(android.util.Log.getStackTraceString(e)).append("\n");
        }
        try {
            Dialog d = sheet("自检弹窗", java.util.Arrays.asList("测试条目"), w -> {});
            d.show(); Thread.sleep(150); d.dismiss();
            log.append("弹窗 show/dismiss OK\n");
        } catch (Throwable e) {
            log.append("!!! 弹窗 EXCEPTION\n").append(android.util.Log.getStackTraceString(e)).append("\n");
        }
        log.append("== 自检结束 ==");
        showOverlay("自检结果（复制发回）", log.toString());
    }
    void setStatus(CharSequence t) { status.setText(t); status.setTextColor(0xFF3A3A3C); }
    void load(boolean manual) {
        setStatus("正在拉取 openrouter.ai/api/v1/models …");
        new Thread(() -> {
            String err = null; List<Model> fresh = null;
            try { fresh = Api.fetch(); }
            catch (Exception e) { err = e.getMessage(); }
            final List<Model> f2 = fresh; final String e2 = err;
            runOnUiThread(() -> {
                if (swipe != null) swipe.setRefreshing(false);
                if (f2 == null) { setStatus("加载失败：" + e2 + "\n下拉重试"); return; }
                all = f2;
                Checker.diff(this, f2);
                setStatus("共 " + all.size() + " 个模型 · 更新于 " + new java.text.SimpleDateFormat("MM-dd HH:mm").format(new Date()));
                render();
            });
        }).start();
    }

    Set<String> favs() { return getSharedPreferences("radar", Context.MODE_PRIVATE).getStringSet("favs", new HashSet<>()); }
    Set<String> hidden() { return getSharedPreferences("radar", Context.MODE_PRIVATE).getStringSet("hidden", new HashSet<>()); }
    void hideModel(String id, boolean forever) {
        if (forever) {
            Set<String> h = new HashSet<>(hidden()); h.add(id);
            getSharedPreferences("radar", Context.MODE_PRIVATE).edit().putStringSet("hidden", h).apply();
            Toast.makeText(this, "已加入不显示名单", Toast.LENGTH_SHORT).show();
        } else {
            hideOnce.add(id);
            Toast.makeText(this, "已隐藏此模型", Toast.LENGTH_SHORT).show();
        }
        render();
    }
    String speedText(String id) {
        String v = weeklyMap.get(id);
        if (v != null) return v;
        return weeklyTried.contains(id) ? "周 —" : "周…";
    }
    void loadWeeklyCache() {
        try {
            for (String it : getSharedPreferences("radar", Context.MODE_PRIVATE).getStringSet("weekly", new HashSet<>())) {
                int i = it.indexOf('|');
                if (i > 0) weeklyMap.put(it.substring(0, i), it.substring(i + 1));
            }
        } catch (Exception ignore) {}
    }
    void saveWeeklyCache() {
        Set<String> out = new HashSet<>();
        for (java.util.Map.Entry<String,String> e : weeklyMap.entrySet()) out.add(e.getKey() + "|" + e.getValue());
        getSharedPreferences("radar", Context.MODE_PRIVATE).edit().putStringSet("weekly", out).apply();
    }
    void probeWeekly(final List<Model> list) {
        if (weeklyProbing) return;
        final java.util.List<Model> todo = new java.util.ArrayList<>();
        for (Model m : list) { if (todo.size() >= 30) break; if (!weeklyTried.contains(m.id)) { todo.add(m); weeklyTried.add(m.id); } }
        if (todo.isEmpty()) return;
        weeklyProbing = true;
        new Thread(() -> {
            java.util.concurrent.ExecutorService ex = java.util.concurrent.Executors.newFixedThreadPool(6);
            for (final Model m : todo) ex.submit(() -> {
                String raw = Api.fetchActivity(m.id);
                long wk = raw != null ? Model.weeklyTokens(raw) : -1;
                if (wk > 0) weeklyMap.put(m.id, "周 " + Model.tokText(wk) + " tok");
            });
            ex.shutdown();
            try { ex.awaitTermination(60, java.util.concurrent.TimeUnit.SECONDS); } catch (Exception ignore) {}
            weeklyProbing = false;
            saveWeeklyCache();
            runOnUiThread(() -> { if (!all.isEmpty()) render(); });
        }).start();
    }
    void showHiddenDialog() {
        Set<String> h = hidden();
        String[] items = h.toArray(new String[0]);
        if (items.length == 0) { Toast.makeText(this, "不显示名单为空", Toast.LENGTH_SHORT).show(); return; }
        sheet("不显示名单 · 点条目恢复显示", java.util.Arrays.asList(items), w -> {
            Set<String> nh = new HashSet<>(hidden()); nh.remove(items[w]);
            getSharedPreferences("radar", Context.MODE_PRIVATE).edit().putStringSet("hidden", nh).apply();
            Toast.makeText(this, "已恢复显示", Toast.LENGTH_SHORT).show();
            render();
        }).show();
    }

    void render() {
        int keepY = scrollView != null ? scrollView.getScrollY() : 0;
        listBox.removeAllViews();
        List<Model> f = new ArrayList<>();
        for (Model m : all) {
            if (hidden().contains(m.id) || hideOnce.contains(m.id)) continue;
            if (!query.isEmpty() && !(m.name.toLowerCase().contains(query) || m.id.toLowerCase().contains(query))) continue;
            if ("免费".equals(tab) && !m.isFree()) continue;
            if ("收藏".equals(tab) && !favs().contains(m.id)) continue;
            f.add(m);
        }
        if ("最新".equals(tab)) f.sort((a, b2) -> Long.compare(b2.created, a.created));
        else if ("好用".equals(tab)) f.sort((a, b2) -> Integer.compare(Integer.parseInt(b2.score()), Integer.parseInt(a.score())));
        else if ("免费".equals(tab)) f.sort((a, b2) -> Long.compare(b2.created, a.created));
        shown = f;
        status.setText(status.getText().toString().replaceAll(" · 列表 \\d+", "") + " · 列表 " + f.size());
        if (f.isEmpty()) { TextView t = new TextView(this); t.setText("没有符合条件的模型"); listBox.addView(t); return; }
        int limit = Math.min(200, f.size());
        for (int i = 0; i < limit; i++) listBox.addView(card(f.get(i)));
        if (f.size() > limit) { TextView t = new TextView(this); t.setText("仅显示前 " + limit + " 条，缩小搜索范围"); listBox.addView(t); }
        if (scrollView != null) scrollView.post(() -> scrollView.scrollTo(0, keepY));
        probeWeekly(f);
    }

    View card(Model m) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(this, 16), dp(this, 14), dp(this, 16), dp(this, 14));
        card.setBackground(rrect(this, 0xFFFFFFFF, 16, 0xFFEDEDEF));
        card.setElevation(dp(this, 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(this, 10));
        card.setLayoutParams(lp);

        TextView nm = new TextView(this);
        nm.setText(m.name);
        nm.setTextSize(17); nm.setTypeface(Typeface.DEFAULT_BOLD); nm.setTextColor(0xFF000000);
        card.addView(nm);

        TextView price = new TextView(this);
        price.setTextSize(13); price.setTextColor(0xFF6D6D72);
        if (m.promptPrice < 0 || m.completionPrice < 0) price.setText("价格未公布 · " + speedText(m.id));
        else if (m.isFree()) price.setText("输入 免费 · 输出 免费 · " + speedText(m.id));
        else price.setText("输入 $" + String.format("%.2f", m.promptPrice * 1e6) + " · 输出 $" + String.format("%.2f", m.completionPrice * 1e6) + " /1M · " + speedText(m.id));
        price.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
        card.addView(price);

        LinearLayout meta = new LinearLayout(this);
        meta.setOrientation(LinearLayout.HORIZONTAL);
        meta.setGravity(Gravity.CENTER_VERTICAL);
        TextView badge = new TextView(this);
        badge.setText(m.isFree() ? "免费" : "付费");
        badge.setTextSize(11); badge.setTypeface(Typeface.DEFAULT_BOLD);
        badge.setTextColor(m.isFree() ? 0xFFFFFFFF : 0xFF8E8E93);
        badge.setPadding(dp(this, 8), dp(this, 2), dp(this, 8), dp(this, 2));
        badge.setBackground(rrect(this, m.isFree() ? 0xFF34C759 : 0xFFE5E5EA, 6, 0));
        meta.addView(badge);
        TextView id = new TextView(this);
        id.setText("  " + m.id + " · " + m.dateText() + (m.ctx > 0 ? " · " + (m.ctx/1000) + "k ctx" : ""));
        id.setTextColor(0xFF8E8E93); id.setTextSize(12); id.setSingleLine(true);
        LinearLayout.LayoutParams idLp = new LinearLayout.LayoutParams(0, -2, 1);
        id.setLayoutParams(idLp);
        meta.addView(id);
        LinearLayout.LayoutParams metaLp = new LinearLayout.LayoutParams(-1, -2);
        metaLp.setMargins(0, dp(this, 6), 0, dp(this, 6));
        meta.setLayoutParams(metaLp);
        card.addView(meta);

        if (!m.desc.isEmpty()) {
            TextView d = new TextView(this);
            d.setText(m.desc.length() > 200 ? m.desc.substring(0, 200) + "…" : m.desc);
            d.setTextColor(0xFF3A3A3C); d.setTextSize(14); d.setLineSpacing(0, 1.15f);
            card.addView(d);
        }
        LinearLayout btns = new LinearLayout(this);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        btns.setGravity(Gravity.END);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(-1, -2);
        btnLp.setMargins(0, dp(this, 10), 0, 0);
        btns.setLayoutParams(btnLp);
        Button copy = compact(new Button(this)); copy.setText("复制ID"); copy.setAllCaps(false);
        copy.setTextSize(12); copy.setTextColor(0xFF007AFF);
        copy.setBackground(rrect(this, 0xFFF2F2F7, 8, 0));
        copy.setPadding(dp(this, 10), dp(this, 2), dp(this, 10), dp(this, 2));
        copy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("model", m.id));
            Toast.makeText(this, "已复制 " + m.id, Toast.LENGTH_SHORT).show();
        });
        Button open = compact(new Button(this)); open.setText("详情"); open.setAllCaps(false);
        open.setTextSize(12); open.setTextColor(0xFF007AFF);
        open.setBackground(rrect(this, 0xFFF2F2F7, 8, 0));
        open.setPadding(dp(this, 10), dp(this, 2), dp(this, 10), dp(this, 2));
        open.setOnClickListener(v -> { try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://openrouter.ai/models/" + m.id))); } catch (Exception e) { Toast.makeText(this, "无法打开浏览器", Toast.LENGTH_SHORT).show(); } });
        Button fav = compact(new Button(this));
        boolean isFav = favs().contains(m.id);
        fav.setText(isFav ? "★ 已收藏" : "☆ 收藏"); fav.setAllCaps(false);
        fav.setTextSize(12); fav.setTextColor(isFav ? 0xFFFF9500 : 0xFF007AFF);
        fav.setBackground(rrect(this, 0xFFF2F2F7, 8, 0));
        fav.setPadding(dp(this, 10), dp(this, 2), dp(this, 10), dp(this, 2));
        fav.setOnClickListener(v -> {
            try {
                Set<String> s = new HashSet<>(favs());
                if (s.contains(m.id)) s.remove(m.id); else s.add(m.id);
                getSharedPreferences("radar", Context.MODE_PRIVATE).edit().putStringSet("favs", s).apply();
                render();
            } catch (Throwable e) { Toast.makeText(this, "收藏出错: " + e, Toast.LENGTH_LONG).show(); }
        });
        LinearLayout.LayoutParams cpLp = new LinearLayout.LayoutParams(-2, -2);
        cpLp.leftMargin = dp(this, 8);
        LinearLayout.LayoutParams opLp = new LinearLayout.LayoutParams(-2, -2);
        opLp.leftMargin = dp(this, 8);
        LinearLayout.LayoutParams fvLp = new LinearLayout.LayoutParams(-2, -2);
        fvLp.leftMargin = dp(this, 8);
        btns.addView(copy, cpLp); btns.addView(open, opLp); btns.addView(fav, fvLp);
        card.addView(btns);
        card.setOnLongClickListener(v -> {
            try { sheet(m.name, java.util.Arrays.asList("不显示此模型", "加入不显示名单"), w -> hideModel(m.id, w == 1)).show(); }
            catch (Throwable e) { Toast.makeText(this, "弹窗出错: " + e, Toast.LENGTH_LONG).show(); }
            return true;
        });
        return card;
    
}

}
