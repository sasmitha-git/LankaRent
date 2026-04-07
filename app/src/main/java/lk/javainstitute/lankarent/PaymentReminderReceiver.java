package lk.javainstitute.lankarent;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class PaymentReminderReceiver extends BroadcastReceiver {

    public static final String PAYMENT_REMINDER_ACTION = "lk.javainstitute.lankarent.PAYMENT_REMINDER";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Extract payment details from the intent
        String title = intent.getStringExtra("title");
        String message = intent.getStringExtra("message");

        // Show the notification
        showNotification(context, title, message);
    }

    private void showNotification(Context context, String title, String message) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create a notification channel (required for Android 8.0 and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "payment_reminders",
                    "Payment Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            notificationManager.createNotificationChannel(channel);
        }

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "payment_reminders")
                .setSmallIcon(R.drawable.notification) // Set your notification icon
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        // Show the notification
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}