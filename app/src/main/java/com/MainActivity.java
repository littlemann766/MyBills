package com.mybills.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
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
    private static final int REQUEST_BASE = 41000;

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private String pendingBackupJson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        webView.clearCache(true);
        webView.clearHistory();

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

        // v122: this is the ONLY Android UI source.
        webView.loadUrl("file:///android_asset/index.html");
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_REQUEST);
        }
    }

    private static Uri soundUri(String sound) {
        if ("silent".equals(sound)) return null;
        if ("alarm".equals(sound))
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if ("ringtone".equals(sound))
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    }

    private static String channelId(String sound, boolean vibrate) {
        return "my_bills_" + sound + "_" + (vibrate ? "vibrate" : "quiet");
    }

    private static void ensureChannel(Context context, String sound, boolean vibrate) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        String id = channelId(sound, vibrate);
        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm.getNotificationChannel(id) != null) return;

        NotificationChannel channel = new NotificationChannel(
                id, "My Bills alerts", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Bills and reminder notifications");
        channel.enableVibration(vibrate);
        if (vibrate) channel.setVibrationPattern(new long[]{0, 250, 150, 250});

        Uri uri = soundUri(sound);
        if (uri == null) channel.setSound(null, null);
        else channel.setSound(uri, null);

        nm.createNotificationChannel(channel);
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
        public void requestNotificationPermission() {
            runOnUiThread(() -> requestNotificationPermissionIfNeeded());
        }

        @JavascriptInterface
        public void testNotification(final String json) {
            runOnUiThread(() -> {
                try {
                    requestNotificationPermissionIfNeeded();
                    JSONObject obj = new JSONObject(json);
                    String title = obj.optString("title", "My Bills test");
                    String text = obj.optString("text", "Notifications are working.");
                    String sound = obj.optString("sound", "default");
                    boolean vibrate = obj.optBoolean("vibrate", true);
                    showNotification(MainActivity.this, title, text, sound, vibrate);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this,
                            "Could not send test notification.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @JavascriptInterface
        public void scheduleNotifications(final String json) {
            runOnUiThread(() -> {
                try {
                    JSONObject root = new JSONObject(json);
                    boolean billsEnabled = root.optBoolean("enabled", false);
                    boolean remindersEnabled = root.optBoolean("reminderEnabled", true);
                    String sound = root.optString("sound", "default");
                    boolean vibrate = root.optBoolean("vibrate", true);

                    cancelScheduledNotifications();
                    ensureChannel(MainActivity.this, sound, vibrate);

                    if (!billsEnabled && !remindersEnabled) return;

                    requestNotificationPermissionIfNeeded();

                    int daysBefore = Math.max(0, Math.min(31,
                            root.optInt("daysBefore", 3)));
                    String time = root.optString("time", "09:00");
                    int requestIndex = 0;

                    if (billsEnabled) {
                        JSONArray bills = root.optJSONArray("bills");
                        if (bills != null)
                            requestIndex = scheduleBillAlarms(
                                    bills, daysBefore, time, sound, vibrate, requestIndex);
                    }

                    if (remindersEnabled) {
                        JSONArray reminders = root.optJSONArray("reminders");
                        if (reminders != null)
                            scheduleReminderAlarms(
                                    reminders, sound, vibrate, requestIndex);
                    }

                    Toast.makeText(MainActivity.this,
                            "Notifications updated.", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this,
                            "Could not schedule notifications.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void cancelScheduledNotifications() {
        AlarmManager alarmManager =
                (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        for (int i = 0; i < 1200; i++) {
            Intent intent = new Intent(this, ReminderReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(
                    this, REQUEST_BASE + i, intent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (pi != null) {
                alarmManager.cancel(pi);
                pi.cancel();
            }
        }
    }

    private void scheduleAlarm(long whenMillis, int requestCode,
                               String title, String text,
                               String sound, boolean vibrate) {
        if (whenMillis <= System.currentTimeMillis()) return;

        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("title", title);
        intent.putExtra("text", text);
        intent.putExtra("sound", sound);
        intent.putExtra("vibrate", vibrate);

        PendingIntent pi = PendingIntent.getBroadcast(
                this, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager =
                (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, whenMillis, pi);
    }

    private int scheduleBillAlarms(JSONArray bills, int daysBefore, String time,
                                   String sound, boolean vibrate,
                                   int requestIndex) throws Exception {

        String[] hm = time.split(":");
        int hour = Integer.parseInt(hm[0]);
        int minute = hm.length > 1 ? Integer.parseInt(hm[1]) : 0;

        Calendar horizon = Calendar.getInstance();
        horizon.add(Calendar.MONTH, 12);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        for (int i = 0; i < bills.length() && requestIndex < 1000; i++) {
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

            while (occurrence.before(horizon) && requestIndex < 1000) {
                Calendar fire = (Calendar) occurrence.clone();
                fire.add(Calendar.DAY_OF_MONTH, -daysBefore);
                fire.set(Calendar.HOUR_OF_DAY, hour);
                fire.set(Calendar.MINUTE, minute);
                fire.set(Calendar.SECOND, 0);
                fire.set(Calendar.MILLISECOND, 0);

                if (fire.getTimeInMillis() > System.currentTimeMillis()) {
                    String text = name + " — $" +
                            String.format(Locale.US, "%.2f", amount) +
                            " due " + df.format(occurrence.getTime());

                    scheduleAlarm(
                            fire.getTimeInMillis(),
                            REQUEST_BASE + requestIndex,
                            name + " bill reminder",
                            text, sound, vibrate);
                    requestIndex++;
                }

                if (!recurring) break;
                if ("weekly".equals(recurrence))
                    occurrence.add(Calendar.DAY_OF_MONTH, 7);
                else if ("biweekly".equals(recurrence))
                    occurrence.add(Calendar.DAY_OF_MONTH, 14);
                else
                    occurrence.add(Calendar.MONTH, 1);
            }
        }
        return requestIndex;
    }

    private int scheduleReminderAlarms(JSONArray reminders,
                                       String sound, boolean vibrate,
                                       int requestIndex) throws Exception {
        SimpleDateFormat df =
                new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);

        for (int i = 0; i < reminders.length() && requestIndex < 1100; i++) {
            JSONObject r = reminders.getJSONObject(i);
            String name = r.optString("name", "Reminder");
            String date = r.optString("date", "");
            String time = r.optString("time", "09:00");
            String notes = r.optString("notes", "");

            Date fire = df.parse(date + " " + time);
            if (fire == null || fire.getTime() <= System.currentTimeMillis())
                continue;

            String text = notes.trim().isEmpty() ? name : notes;
            scheduleAlarm(
                    fire.getTime(),
                    REQUEST_BASE + requestIndex,
                    name, text, sound, vibrate);
            requestIndex++;
        }
        return requestIndex;
    }

    public static class ReminderReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String title = intent.getStringExtra("title");
            String text = intent.getStringExtra("text");
            String sound = intent.getStringExtra("sound");
            boolean vibrate = intent.getBooleanExtra("vibrate", true);

            if (sound == null) sound = "default";
            ensureChannel(context, sound, vibrate);
            showNotification(context,
                    title != null ? title : "My Bills",
                    text != null ? text : "You have an upcoming reminder.",
                    sound, vibrate);
        }
    }

    private static void showNotification(Context context,
                                         String title, String text,
                                         String sound, boolean vibrate) {
        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        String channel = channelId(sound, vibrate);
        ensureChannel(context, sound, vibrate);

        Intent launch = context.getPackageManager()
                .getLaunchIntentForPackage(context.getPackageName());
        PendingIntent contentIntent = null;
        if (launch != null) {
            contentIntent = PendingIntent.getActivity(
                    context, 0, launch,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        }

        Notification.Builder builder =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? new Notification.Builder(context, channel)
                        : new Notification.Builder(context);

        builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true);

        if (contentIntent != null) builder.setContentIntent(contentIntent);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Uri uri = soundUri(sound);
            if (uri != null) builder.setSound(uri);
            if (vibrate) builder.setVibrate(new long[]{0, 250, 150, 250});
        }

        if (Build.VERSION.SDK_INT < 33 ||
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        == PackageManager.PERMISSION_GRANTED) {
            nm.notify((int)(System.currentTimeMillis() & 0x7fffffff), builder.build());
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
            if (resultCode == RESULT_OK
                    && data != null
                    && data.getData() != null
                    && pendingBackupJson != null) {

                Uri uri = data.getData();
                try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                    if (out != null) {
                        out.write(pendingBackupJson.getBytes(StandardCharsets.UTF_8));
                        out.flush();
                        Toast.makeText(this,
                                "My Bills backup saved.", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(this,
                            "Could not save backup.", Toast.LENGTH_SHORT).show();
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
