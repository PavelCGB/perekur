package app.perekur;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Environment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

/** Проверяет последний релиз на GitHub (тег вида v2 = versionCode 2) и предлагает обновиться. */
class Updater {
    static final String REPO = "PavelCGB/perekur";

    static void check(Activity a) {
        new Thread(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL("https://api.github.com/repos/" + REPO + "/releases/latest").openConnection();
                c.setRequestProperty("Accept", "application/vnd.github+json");
                c.setConnectTimeout(8000); c.setReadTimeout(8000);
                if (c.getResponseCode() != 200) return;
                String body;
                try (InputStream in = c.getInputStream(); Scanner s = new Scanner(in, "UTF-8").useDelimiter("\\A")) { body = s.hasNext() ? s.next() : ""; }
                JSONObject rel = new JSONObject(body);
                long latest = Long.parseLong(rel.getString("tag_name").replaceAll("\\D", ""));
                long mine = a.getPackageManager().getPackageInfo(a.getPackageName(), 0).getLongVersionCode();
                if (latest <= mine) return;
                String apk = null;
                JSONArray assets = rel.getJSONArray("assets");
                for (int i = 0; i < assets.length(); i++) {
                    String u = assets.getJSONObject(i).getString("browser_download_url");
                    if (u.endsWith(".apk")) apk = u;
                }
                if (apk == null) return;
                String url = apk, notes = rel.optString("body", "");
                a.runOnUiThread(() -> {
                    if (a.isFinishing()) return;
                    new AlertDialog.Builder(a)
                            .setTitle("Есть новая версия " + rel.optString("name", "v" + latest))
                            .setMessage(notes.isEmpty() ? "Скачать и установить обновление?" : notes)
                            .setPositiveButton("Обновить", (d, w) -> download(a, url, latest))
                            .setNegativeButton("Позже", null)
                            .show();
                });
            } catch (Exception ignored) { } // нет сети или GitHub недоступен — просто работаем дальше
        }).start();
    }

    static void download(Activity a, String url, long version) {
        DownloadManager dm = a.getSystemService(DownloadManager.class);
        String name = "Perekur-v" + version + ".apk";
        DownloadManager.Request r = new DownloadManager.Request(Uri.parse(url))
                .setTitle("Перекур — обновление")
                .setMimeType("application/vnd.android.package-archive")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(a, Environment.DIRECTORY_DOWNLOADS, name);
        long id = dm.enqueue(r);
        a.registerReceiver(new BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent i) {
                if (i.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) != id) return;
                c.unregisterReceiver(this);
                Uri file = dm.getUriForDownloadedFile(id);
                if (file == null) return;
                a.startActivity(new Intent(Intent.ACTION_VIEW)
                        .setDataAndType(file, "application/vnd.android.package-archive")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        }, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED);
    }
}
