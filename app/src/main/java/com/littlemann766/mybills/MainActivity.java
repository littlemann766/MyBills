package com.littlemann766.mybills;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int FILE_CHOOSER_REQUEST = 1001;
    private static final int SAVE_BACKUP_REQUEST = 1002;
    private static final int NOTIFICATION_PERMISSION_REQUEST = 1003;
    private static final String CHANNEL_ID = "my_bills_due_dates";

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private String pendingBackupJson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createNotificationChannel();

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        webView.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");
        webView.setWebViewClient(new WebViewClient());

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(
                    WebView webView,
                    ValueCallback<Uri[]> callback,
                    FileChooserParams params) {

                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = callback;

                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_MIME_TYPES,
                        new String[]{"application/json", "text/json", "text/plain"});

                try {
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                    return true;
                } catch (Exception e) {
                    filePathCallback = null;
                    Toast.makeText(MainActivity.this,
                            "Unable to open local files.", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        });

        String url = "https://littlemann766.github.io/MyBills/?app=" + System.currentTimeMillis();
        webView.loadUrl(url);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Bill reminders", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Reminders for upcoming My Bills due dates");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    public class AndroidBridge {
        @JavascriptInterface
        public void saveBackup(final String json) {
            runOnUiThread(() -> {
                pendingBackupJson = json;
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_TITLE, "my-bills-backup.json");
                try {
                    startActivityForResult(intent, SAVE_BACKUP_REQUEST);
                } catch (Exception e) {
                    pendingBackupJson = null;
                    Toast.makeText(MainActivity.this,
                            "Unable to choose a save location.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @JavascriptInterface
        public void scheduleNotifications(final String json) {
            runOnUiThread(() -> {
                try {
                    JSONObject root = new JSONObject(json);
                    boolean enabled = root.optBoolean("enabled", false);
                    if (!enabled) {
                        cancelScheduledNotifications();
                        return;
                    }

                    if (Build.VERSION.SDK_INT >= 33 &&
                            ActivityCompat.checkSelfPermission(MainActivity.this,
                                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                                NOTIFICATION_PERMISSION_REQUEST);
                    }

                    int daysBefore = Math.max(0, Math.min(31, root.optInt("daysBefore", 3)));
                    String time = root.optString("time", "09:00");
                    JSONArray bills = root.optJSONArray("bills");
                    if (bills != null) scheduleBillAlarms(bills, daysBefore, time);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this,
                            "Could not schedule bill notifications.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void cancelScheduledNotifications() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        for (int i = 0; i < 1000; i++) {
            Intent intent = new Intent(this, ReminderReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(
                    this, 40000 + i, intent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (pi != null) {
                alarmManager.cancel(pi);
                pi.cancel();
            }
        }
    }

    private void scheduleBillAlarms(JSONArray bills, int daysBefore, String time) throws Exception {
        cancelScheduledNotifications();

        String[] hm = time.split(":");
        int hour = Integer.parseInt(hm[0]);
        int minute = hm.length > 1 ? Integer.parseInt(hm[1]) : 0;

        Calendar now = Calendar.getInstance();
        Calendar horizon = Calendar.getInstance();
        horizon.add(Calendar.MONTH, 12);

        int requestIndex = 0;
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        for (int i = 0; i < bills.length() && requestIndex < 900; i++) {
            JSONObject b = bills.getJSONObject(i);
            String name = b.optString("name", "Bill");
            double amount = b.optDouble("amount", 0);
            String due = b.optString("due", "");
            boolean recurring = b.optBoolean("recurring", false);
            String recurrence = b.optString("recurrence", "monthly");

            Date firstDate = df.parse(due);
            if (firstDate == null) continue;

            Calendar occurrence = Calendar.getInstance();
            occurrence.setTime(firstDate);

            while (occurrence.before(horizon) && requestIndex < 900) {
                Calendar fire = (Calendar) occurrence.clone();
                fire.add(Calendar.DAY_OF_MONTH, -daysBefore);
                fire.set(Calendar.HOUR_OF_DAY, hour);
                fire.set(Calendar.MINUTE, minute);
                fire.set(Calendar.SECOND, 0);
                fire.set(Calendar.MILLISECOND, 0);

                if (fire.after(now)) {
                    Intent intent = new Intent(this, ReminderReceiver.class);
                    intent.putExtra("title", name + " bill reminder");
                    intent.putExtra("text",
                            name + " — $" + String.format(Locale.US, "%.2f", amount)
                                    + " due " + df.format(occurrence.getTime()));

                    PendingIntent pi = PendingIntent.getBroadcast(
                            this, 40000 + requestIndex, intent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                    AlarmManager alarmManager =
                            (AlarmManager) getSystemService(Context.ALARM_SERVICE);

                    alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP, fire.getTimeInMillis(), pi);

                    requestIndex++;
                }

                if (!recurring) break;
                if ("weekly".equals(recurrence)) occurrence.add(Calendar.DAY_OF_MONTH, 7);
                else if ("biweekly".equals(recurrence)) occurrence.add(Calendar.DAY_OF_MONTH, 14);
                else occurrence.add(Calendar.MONTH, 1);
            }
        }

        Toast.makeText(this, "Bill notifications updated.", Toast.LENGTH_SHORT).show();
    }

    public static class ReminderReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String title = intent.getStringExtra("title");
            String text = intent.getStringExtra("text");

            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(context, CHANNEL_ID)
                            .setSmallIcon(android.R.drawable.ic_dialog_info)
                            .setContentTitle(title != null ? title : "My Bills")
                            .setContentText(text != null ? text : "You have an upcoming bill.")
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setAutoCancel(true);

            if (ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED
                    || Build.VERSION.SDK_INT < 33) {
                NotificationManagerCompat.from(context)
                        .notify((int) (System.currentTimeMillis() & 0xfffffff), builder.build());
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_CHOOSER_REQUEST) {
            if (filePathCallback == null) return;
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                results = new Uri[]{data.getData()};
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
            return;
        }

        if (requestCode == SAVE_BACKUP_REQUEST) {
            if (resultCode == RESULT_OK && data != null &&
                    data.getData() != null && pendingBackupJson != null) {
                Uri uri = data.getData();
                try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                    if (out != null) {
                        out.write(pendingBackupJson.getBytes(StandardCharsets.UTF_8));
                        out.flush();
                        Toast.makeText(this, "My Bills backup saved.",
                                Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Could not save backup.",
                            Toast.LENGTH_SHORT).show();
                }
            }
            pendingBackupJson = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
