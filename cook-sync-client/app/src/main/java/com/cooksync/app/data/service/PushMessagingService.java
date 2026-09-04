package com.cooksync.app.data.service;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;

import com.cooksync.app.R;
import com.cooksync.app.data.repository.DeviceTokenRepository;
import com.cooksync.app.data.repository.impl.DeviceTokenRepositoryImp;
import com.cooksync.app.domain.ApiResult;
import com.cooksync.app.ui.home.HomeActivity;
import com.cooksync.app.util.SessionManager;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Receives incoming Firebase Cloud Messaging pushes (system-announcement broadcasts, and any
 * future push type) and shows them as a system notification, and re-registers this device's
 * token whenever FCM issues a new one. Declared in {@code AndroidManifest.xml} with the
 * {@code com.google.firebase.MESSAGING_EVENT} intent filter, which is how the OS/FCM SDK finds
 * and starts it — never constructed directly by app code.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
public class PushMessagingService extends FirebaseMessagingService {

    /** Constructed directly rather than via {@link com.cooksync.app.ui.base.ViewModelFactory}: this is a
     *  framework-instantiated {@code Service}, not an {@code Activity}/{@code ViewModel}. */
    private final DeviceTokenRepository deviceTokenRepository = new DeviceTokenRepositoryImp();

    /**
     * Registers a freshly (re)issued FCM token, but only if a session already exists — an
     * anonymous install has no account to register the token against yet. {@link HomeActivity}
     * separately registers the current token after login, covering the case where a token was
     * already issued before the user signed in.
     *
     * @param token the new FCM registration token
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        if (SessionManager.getInstance().isLoggedIn().getValue() == Boolean.TRUE) {
            deviceTokenRepository.registerDevice(token, "ANDROID", new MutableLiveData<ApiResult<Void>>());
        }
    }

    /**
     * Builds and shows a system notification for an incoming push. Only reached while the app
     * process is alive (foreground or background) — if the process isn't running, FCM shows the
     * message's {@code notification} payload automatically instead, using the default channel
     * declared via {@code AndroidManifest.xml}'s
     * {@code com.google.firebase.messaging.default_notification_channel_id} meta-data, so this
     * method and that manifest declaration must stay pointed at the same channel.
     *
     * @param message the incoming FCM message
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        RemoteMessage.Notification notification = message.getNotification();
        if (notification == null) {
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                this, getString(R.string.notification_channel_general_id))
                .setSmallIcon(R.drawable.ic_bell)
                .setContentTitle(notification.getTitle())
                .setContentText(notification.getBody())
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notification.getBody()))
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat.from(this).notify((int) System.currentTimeMillis(), builder.build());
    }
}
