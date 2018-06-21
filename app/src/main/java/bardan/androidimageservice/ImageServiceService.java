package bardan.androidimageservice;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import android.widget.Toast;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class ImageServiceService extends Service {

    private static final String ipAddr = "10.0.2.2";
    private static final int port = 8001;
    private boolean transferFlag = false;

    public ImageServiceService() {
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        final IntentFilter wifiFilter = new IntentFilter();
        wifiFilter.addAction("android.net.wifi.supplicant.CONNECTION_CHANGE");
        wifiFilter.addAction("android.net.wifi.STATE_CHANGE");
        BroadcastReceiver wifiBroadcast = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (networkInfo != null) {
                    if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                        // get the different network states
                        if (networkInfo.getState() == NetworkInfo.State.CONNECTED) {
                            startImageTransfer(context);
                        }
                    }
                }
            }
        };
        this.registerReceiver(wifiBroadcast, wifiFilter);
    }

    /**
     * starts the image transfer from the DCIM folder.
     */
    private void startImageTransfer(Context context) {
        if (transferFlag) {
            // already transferring
            return;
        } else {
            transferFlag = true;
        }

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "default");
        final int notify_id = 1;
        final NotificationManager NM =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        builder.setSmallIcon(R.drawable.ic_launcher_background);
        builder.setContentTitle("Picture Transfer");
        builder.setContentText("Transfer in progress...");

        NotificationChannel channel = new NotificationChannel("default",
                "progress", NotificationManager.IMPORTANCE_DEFAULT);
        if (NM != null) {
            NM.createNotificationChannel(channel);
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                Client client = new TcpClient();
                client.connect(ipAddr, port);
                File dcim = new File(Environment.getExternalStoragePublicDirectory(Environment.
                        DIRECTORY_DCIM), "Camera");
                List<File> pictures = new LinkedList<>();
                int count = 0;
                listAllPictures(dcim, pictures);
                for (File pic : pictures) {
                    builder.setProgress(pictures.size(), count, false);
                    if (count == pictures.size() / 2) {
                        // halfway there
                        builder.setContentText("Halfway through and going...");
                    }
                    NM.notify(notify_id, builder.build());
                    if (client.sendFile(pic)) {
                        count++;
                    }
                }
                builder.setProgress(0, 0, false);
                builder.setContentText("Transfer Complete");
                NM.notify(notify_id, builder.build());
                transferFlag = false;
                client.disconnect();
            }
        }).start();

    }

    /**
     * if file is a directory, scans the files in the folder and sub-folders
     * and adds them to the list.
     * o.w, adds the file to the list.
     *
     * @param file     - folder or file.
     * @param pictures - list of pictures to add.
     */
    private void listAllPictures(File file, List<File> pictures) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                listAllPictures(f, pictures);
            }
        } else {
            pictures.add(file);
        }
    }

    public int onStartCommand(Intent intent, int flag, int startId) {
        Toast.makeText(this, "Service Starting...", Toast.LENGTH_SHORT).show();
        return START_STICKY;
    }

    public void onDestroy() {
        Toast.makeText(this, "Service Ending...", Toast.LENGTH_SHORT).show();
    }
}
