package bardan.androidimageservice;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class ImageServiceService extends Service {

    private BroadcastReceiver wifiBroadcast;
    private static final String ipAddr = "10.0.2.2";
    private static final int port = 8080;

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
        this.wifiBroadcast = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (networkInfo != null) {
                    if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                        // get the different network states
                        if (networkInfo.getState() == NetworkInfo.State.CONNECTED) {
                            startImageTransfer();
                        }
                    }
                }
            }
        };
        this.registerReceiver(this.wifiBroadcast, wifiFilter);
    }

    /**
     * starts the image transfer from the DCIM folder.
     */
    private void startImageTransfer() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                File dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
                if (dcim == null) {
                    return;
                }
                File[] pics = dcim.listFiles();
                int count = 0;
                if (pics != null) {
                    for (File pic : pics) {
                        sendPictureViaTCP(pic);
                    }
                }
            }
        }).start();

    }

    /**
     * Sends the picture via TCP to the service.
     *
     * @param pic - picture file to send.
     */
    private void sendPictureViaTCP(File pic) {
        try {
            InetAddress serverAddr = InetAddress.getByName(ipAddr);
            Socket socket = new Socket(serverAddr, port);
            OutputStream output = socket.getOutputStream();
            FileInputStream fis = new FileInputStream(pic);
            byte[] imgBytes = convertPictureToBytes(pic);
            output.write(imgBytes);
            output.flush();
        } catch (Exception e) {
            Log.e("TCP", "Error Transferring picture" + pic.getName(), e);
        }
    }

    /**
     * @param pic - picture to convert.
     * @return - byte array if file is found, null o.w.
     */
    @Nullable
    private byte[] convertPictureToBytes(File pic) {
        FileInputStream fis;
        try {
            fis = new FileInputStream(pic);
        } catch (FileNotFoundException e) {
            return null;
        }
        Bitmap bm = BitmapFactory.decodeStream(fis);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 70, stream);
        return stream.toByteArray();
    }

    public int onStartCommand(Intent intent, int flag, int startId) {
        Toast.makeText(this, "Service Starting...", Toast.LENGTH_SHORT).show();
        return START_STICKY;
    }

    public void onDestroy() {
        Toast.makeText(this, "Service Ending...", Toast.LENGTH_SHORT).show();
    }
}
