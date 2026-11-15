package app.perekur;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.os.Build;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

/**
 * Постоянное уведомление с обратным отсчётом и оранжевой полосой прогресса.
 * На Android 16 просится в «живые» (чип в строке состояния / Now Bar).
 * Сам себя будит раз в 5 минут, чтобы полоса двигалась, даже когда приложение закрыто.
 */
public class Live extends BroadcastReceiver {
    static final String CH = "perekur_live";
    static final int ID = 2;
    static final int ORANGE = 0xFFFF6B2C;
    static final long TICK = 5 * 60_000;

    static void start(Context c, long startAt, long endAt) {
        prefs(c).edit().putLong("start", startAt).putLong("end", endAt).apply();
        show(c);
    }

    @Override public void onReceive(Context c, Intent i) { show(c); }

    static void show(Context c) {
        long start = prefs(c).getLong("start", 0), end = prefs(c).getLong("end", 0), now = System.currentTimeMillis();
        if (end <= now) { hide(c); return; }

        NotificationManager nm = c.getSystemService(NotificationManager.class);
        NotificationChannel ch = new NotificationChannel(CH, "Отсчёт до перекура", NotificationManager.IMPORTANCE_DEFAULT);
        ch.setSound(null, null);
        ch.enableVibration(false);
        nm.createNotificationChannel(ch);

        PendingIntent open = PendingIntent.getActivity(c, 3, new Intent(c, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        String at = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(end));
        Notification.Builder b = new Notification.Builder(c, CH)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle("Следующий перекур")
                .setContentText("Можно в " + at + ". Держись!")
                .setWhen(end)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_PROGRESS)
                .setColor(ORANGE)
                .setContentIntent(open);

        int done = (int) Math.max(0, Math.min(1000, 1000 * (now - start) / Math.max(1, end - start)));
        if (Build.VERSION.SDK_INT >= 36) {
            b.setStyle(new Notification.ProgressStyle()
                    .setProgressSegments(Collections.singletonList(new Notification.ProgressStyle.Segment(1000).setColor(ORANGE)))
                    .setStyledByProgress(true)
                    .setProgressTrackerIcon(Icon.createWithResource(c, R.drawable.ic_tracker))
                    .setProgress(done));
        } else {
            b.setProgress(1000, done, false);
        }
        b.getExtras().putBoolean("android.requestPromotedOngoing", true); // Android 16 Live Update
        nm.notify(ID, b.build());

        long next = now + TICK;
        if (next < end) c.getSystemService(AlarmManager.class).setAndAllowWhileIdle(AlarmManager.RTC, next, tick(c));
    }

    static void hide(Context c) {
        c.getSystemService(NotificationManager.class).cancel(ID);
        c.getSystemService(AlarmManager.class).cancel(tick(c));
    }

    static PendingIntent tick(Context c) {
        return PendingIntent.getBroadcast(c, 4, new Intent(c, Live.class), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("live", Context.MODE_PRIVATE); }
}
