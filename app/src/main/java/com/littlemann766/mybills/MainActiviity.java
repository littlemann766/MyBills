package com.littlemann766.mybills;

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
import android.media.AudioAttributes;
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
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = callback;

                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/json","text/json","text/plain"});

                try {
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                    return true;
                } catch (Exception e) {
                    filePathCallback = null;
                    Toast.makeText(MainActivity.this, "Unable to open local files.", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        });

        webView.loadUrl("https://littlemann766.github.io/MyBills/?app=" + System.currentTimeMillis());
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
                    Toast.makeText(MainActivity.this, "Unable to choose a save location.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @JavascriptInterface
        public void scheduleNotifications(final String json) {
            runOnUiThread(() -> {
                try {
                    JSONObject root = new JSONObject(json);
                    boolean billEnabled = root.optBoolean("enabled", false);
                    boolean reminderEnabled = root.optBoolean("reminderEnabled", true);
                    boolean vibrate = root.optBoolean("vibrate", true);
                    String sound = root.optString("sound", "default");

                    cancelScheduledNotifications();

                    if (!billEnabled && !reminderEnabled) return;

                    if (Build.VERSION.SDK_INT >= 33 &&
                            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST);
                    }

                    int requestIndex = 0;

                    if (billEnabled) {
                        JSONArray bills = root.optJSONArray("bills");
                        if (bills != null) {
                            requestIndex = scheduleBillAlarms(
                                    bills,
                                    Math.max(0, Math.min(31, root.optInt("daysBefore", 3))),
                                    root.optString("time", "09:00"),
                                    requestIndex,
                                    vibrate,
                                    sound
                            );
                        }
                    }

                    if (reminderEnabled) {
                        JSONArray reminders = root.optJSONArray("reminders");
                        if (reminders != null) {
                            scheduleReminderAlarms(reminders, requestIndex, vibrate, sound);
                        }
                    }
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Could not schedule notifications.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private Uri soundUri(String sound) {
        if ("silent".equals(sound)) return null;
        if ("alarm".equals(sound)) return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if ("ringtone".equals(sound)) return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    }

    private String ensureChannel(String sound, boolean vibrate) {
        String channelId = "my_bills_" + sound + "_" + (vibrate ? "v" : "nv");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel existing = manager.getNotificationChannel(channelId);

            if (existing == null) {
                NotificationChannel channel = new NotificationChannel(
                        channelId,
                        "My Bills reminders",
                        NotificationManager.IMPORTANCE_DEFAULT
                );
                channel.enableVibration(vibrate);
                if (vibrate) channel.setVibrationPattern(new long[]{0,250,150,250});

                Uri uri = soundUri(sound);
                if (uri == null) {
                    channel.setSound(null, null);
                } else {
                    AudioAttributes attributes = new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .build();
                    channel.setSound(uri, attributes);
                }
                manager.createNotificationChannel(channel);
            }
        }
        return channelId;
    }

    private void cancelScheduledNotifications() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        for (int i = 0; i < 1000; i++) {
            Intent intent = new Intent(this, ReminderReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(
                    this, 40000 + i, intent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );
            if (pi != null) {
                alarmManager.cancel(pi);
                pi.cancel();
            }
        }
    }

    private int scheduleBillAlarms(JSONArray bills, int daysBefore, String time, int requestIndex, boolean vibrate, String sound) throws Exception {
        Calendar now = Calendar.getInstance();
        Calendar horizon = Calendar.getInstance();
        horizon.add(Calendar.MONTH, 12);

        String[] hm = time.split(":");
        int hour = Integer.parseInt(hm[0]);
        int minute = hm.length > 1 ? Integer.parseInt(hm[1]) : 0;
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        for (int i = 0; i < bills.length() && requestIndex < 900; i++) {
            JSONObject b = bills.getJSONObject(i);
            Date firstDate = df.parse(b.optString("due", ""));
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
                    scheduleAlarm(
                            40000 + requestIndex,
                            fire.getTimeInMillis(),
                            b.optString("name", "Bill") + " bill reminder",
                            b.optString("name", "Bill") + " — $" +
                                    String.format(Locale.US, "%.2f", b.optDouble("amount", 0)) +
                                    " due " + df.format(occurrence.getTime()),
                            vibrate,
                            sound
                    );
                    requestIndex++;
                }

                if (!b.optBoolean("recurring", false)) break;
                String recurrence = b.optString("recurrence", "monthly");
                if ("weekly".equals(recurrence)) occurrence.add(Calendar.DAY_OF_MONTH, 7);
                else if ("biweekly".equals(recurrence)) occurrence.add(Calendar.DAY_OF_MONTH, 14);
                else occurrence.add(Calendar.MONTH, 1);
            }
        }
        return requestIndex;
    }

    private int scheduleReminderAlarms(JSONArray reminders, int requestIndex, boolean vibrate, String sound) throws Exception {
        Calendar now = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        for (int i = 0; i < reminders.length() && requestIndex < 980; i++) {
            JSONObject r = reminders.getJSONObject(i);
            Date date = df.parse(r.optString("date", ""));
            if (date == null) continue;

            String[] hm = r.optString("time", "09:00").split(":");
            Calendar fire = Calendar.getInstance();
            fire.setTime(date);
            fire.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hm[0]));
            fire.set(Calendar.MINUTE, hm.length > 1 ? Integer.parseInt(hm[1]) : 0);
            fire.set(Calendar.SECOND, 0);
            fire.set(Calendar.MILLISECOND, 0);

            if (!fire.after(now)) continue;

            scheduleAlarm(
                    40000 + requestIndex,
                    fire.getTimeInMillis(),
                    r.optString("name", "Reminder"),
                    r.optString("notes", "").isEmpty() ? "My Bills reminder" : r.optString("notes", ""),
                    vibrate,
                    sound
            );
            requestIndex++;
        }
        return requestIndex;
    }

    private void scheduleAlarm(int requestCode, long when, String title, String text, boolean vibrate, String sound) {
        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("title", title);
        intent.putExtra("text", text);
        intent.putExtra("vibrate", vibrate);
        intent.putExtra("sound", sound);

        PendingIntent pi = PendingIntent.getBroadcast(
                this, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
    }

    public static class ReminderReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String title = intent.getStringExtra("title");
            String text = intent.getStringExtra("text");
            boolean vibrate = intent.getBooleanExtra("vibrate", true);
            String sound = intent.getStringExtra("sound");
            if (sound == null) sound = "default";

            Uri uri = null;
            if ("alarm".equals(sound)) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            else if ("ringtone".equals(sound)) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            else if (!"silent".equals(sound)) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            String channelId = "my_bills_" + sound + "_" + (vibrate ? "v" : "nv");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                NotificationChannel channel = manager.getNotificationChannel(channelId);
                if (channel == null) {
                    channel = new NotificationChannel(channelId, "My Bills reminders", NotificationManager.IMPORTANCE_DEFAULT);
                    channel.enableVibration(vibrate);
                    if (vibrate) channel.setVibrationPattern(new long[]{0,250,150,250});
                    if (uri == null) channel.setSound(null, null);
                    else {
                        AudioAttributes attributes = new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                                .build();
                        channel.setSound(uri, attributes);
                    }
                    manager.createNotificationChannel(channel);
                }
            }

            Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? new Notification.Builder(context, channelId)
                    : new Notification.Builder(context);

            builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title != null ? title : "My Bills")
                    .setContentText(text != null ? text : "You have a reminder.")
                    .setAutoCancel(true);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                if (vibrate) builder.setVibrate(new long[]{0,250,150,250});
                builder.setSound(uri);
            }

            if (Build.VERSION.SDK_INT < 33 ||
                    context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                manager.notify((int)(System.currentTimeMillis() & 0x0fffffff), builder.build());
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_CHOOSER_REQUEST) {
            if (filePathCallback == null) return;
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null && data.getData() != null) results = new Uri[]{data.getData()};
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
            return;
        }

        if (requestCode == SAVE_BACKUP_REQUEST) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null && pendingBackupJson != null) {
                try (OutputStream out = getContentResolver().openOutputStream(data.getData())) {
                    if (out != null) {
                        out.write(pendingBackupJson.getBytes(StandardCharsets.UTF_8));
                        out.flush();
                        Toast.makeText(this, "My Bills backup saved.", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Could not save backup.", Toast.LENGTH_SHORT).show();
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
