package lk.javainstitute.lankarent;

import android.app.Application;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

public class BroadcastReceiverApp extends Application {

    private NetworkReceiver networkReceiver;

    @Override
    public void onCreate() {
        super.onCreate();

        networkReceiver = new NetworkReceiver();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkReceiver,filter);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        if(networkReceiver != null){
            unregisterReceiver(networkReceiver);
        }
    }
}
