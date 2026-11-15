package app.perekur;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;

public class AlarmReceiver extends BroadcastReceiver {
    static final String CH = "perekur_ready";

    @Override
    public void onReceive(Context c, Intent i) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        NotificationChannel ch = new NotificationChannel(CH, "Можно курить", NotificationManager.IMPORTANCE_HIGH);
        ch.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build());
        ch.enableVibration(true);
        ch.setVibrationPattern(new long[]{0, 500, 200, 500, 200, 900});
        nm.createNotificationChannel(ch);

        Live.hide(c); // отсчёт закончен — убираем живое уведомление
        PendingIntent open = PendingIntent.getActivity(c, 2, new Intent(c, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        nm.notify(1, new Notification.Builder(c, CH)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle("Молодец, ты продержался!")
                .setContentText("Можешь выйти перекурить.")
                .setCategory(Notification.CATEGORY_ALARM)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build());
    }
}
