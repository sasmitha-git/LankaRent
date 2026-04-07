package lk.javainstitute.lankarent;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class ReminderScheduler {

    public static void schedulePaymentReminder(Context context, long timeInMillis, String title, String message) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, PaymentReminderReceiver.class);
        intent.setAction(PaymentReminderReceiver.PAYMENT_REMINDER_ACTION);
        intent.putExtra("title", title);
        intent.putExtra("message", message);

        int requestCode = (int) System.currentTimeMillis(); // Unique request code
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE);

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
    }
}