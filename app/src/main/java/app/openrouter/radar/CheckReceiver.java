package app.openrouter.radar;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.List;

public class CheckReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context ctx, Intent intent) {
        new Thread(() -> {
            try {
                List<Model> fresh = Api.fetch();
                List<Model> added = Checker.diff(ctx, fresh);
                if (!added.isEmpty()) notifyNew(ctx, added);
            } catch (Exception ignored) {}
            Checker.schedule(ctx);
        }).start();
    }
    static void notifyNew(Context ctx, List<Model> added) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel("new_models", "新模型提醒", NotificationManager.IMPORTANCE_DEFAULT);
            nm.createNotificationChannel(ch);
        }
        StringBuilder sb = new StringBuilder();
        int n = Math.min(3, added.size());
        for (int i = 0; i < n; i++) sb.append(added.get(i).name).append("\n");
        if (added.size() > 3) sb.append("等共 ").append(added.size()).append(" 个");
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
            ? new Notification.Builder(ctx, "new_models")
            : new Notification.Builder(ctx);
        b.setContentTitle("发现 " + added.size() + " 个新模型")
         .setContentText(sb.toString())
         .setSmallIcon(android.R.drawable.ic_dialog_info)
         .setAutoCancel(true);
        nm.notify(1, b.build());
    }
}
