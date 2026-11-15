package app.perekur;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Insets;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class MainActivity extends Activity {
    private WebView web;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        web = new WebView(this);
        web.setBackgroundColor(0xFF0E0C0B);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        web.addJavascriptInterface(new Bridge(), "Android");
        // Android 15+ рисует под системными панелями — отодвигаем контент
        web.setOnApplyWindowInsetsListener((v, in) -> {
            Insets b = in.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            v.setPadding(b.left, b.top, b.right, b.bottom);
            return WindowInsets.CONSUMED;
        });
        setContentView(web);
        getWindow().getDecorView().setBackgroundColor(0xFF0E0C0B);
        web.loadUrl("file:///android_asset/index.html");

        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        Updater.check(this, false);
    }

    static PendingIntent alarmIntent(Context c) {
        return PendingIntent.getBroadcast(c, 0, new Intent(c, AlarmReceiver.class), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    class Bridge {
        @JavascriptInterface
        public void schedule(double at) {
            AlarmManager am = getSystemService(AlarmManager.class);
            PendingIntent show = PendingIntent.getActivity(MainActivity.this, 1, new Intent(MainActivity.this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
            if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms())
                am.setAlarmClock(new AlarmManager.AlarmClockInfo((long) at, show), alarmIntent(MainActivity.this));
            else
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, (long) at, alarmIntent(MainActivity.this));
        }

        @JavascriptInterface
        public String version() {
            try { return getPackageManager().getPackageInfo(getPackageName(), 0).versionName; } catch (Exception e) { return ""; }
        }

        @JavascriptInterface
        public void checkUpdate() { Updater.check(MainActivity.this, true); }

        @JavascriptInterface
        public void cancel() {
            getSystemService(AlarmManager.class).cancel(alarmIntent(MainActivity.this));
        }

        @JavascriptInterface
        public void keepScreen(boolean on) {
            runOnUiThread(() -> {
                if (on) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            });
        }
    }
}
