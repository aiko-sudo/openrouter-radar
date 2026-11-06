package app.openrouter.radar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import java.util.*;

public class Checker {
    public static final String KEY_IDS = "known_ids";
    public static final String KEY_ALERT = "alert_on";
    public static final String KEY_PERIOD = "period_h";

    public static void schedule(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences("radar", Context.MODE_PRIVATE);
        if (!p.getBoolean(KEY_ALERT, true)) return;
        long h = p.getLong(KEY_PERIOD, 6);
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(ctx, CheckReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 0, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        long interval = h * 3600000L;
        long first = System.currentTimeMillis() + interval;
        try {
            if (Build.VERSION.SDK_INT >= 23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, first, pi);
            else am.setExact(AlarmManager.RTC_WAKEUP, first, pi);
        } catch (SecurityException e) {
            am.setInexactRepeating(AlarmManager.RTC_WAKEUP, first, interval, pi);
        }
    }

    public static List<Model> diff(Context ctx, List<Model> fresh) {
        SharedPreferences p = ctx.getSharedPreferences("radar", Context.MODE_PRIVATE);
        Set<String> known = p.getStringSet(KEY_IDS, new HashSet<>());
        List<Model> added = new ArrayList<>();
        Set<String> now = new HashSet<>();
        for (Model m : fresh) {
            now.add(m.id);
            if (!known.isEmpty() && !known.contains(m.id)) added.add(m);
        }
        p.edit().putStringSet(KEY_IDS, now).apply();
        return added;
    }
}
