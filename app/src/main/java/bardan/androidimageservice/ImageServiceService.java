package bardan.androidimageservice;

import android.app.NotificationManager;
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
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

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
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        final int notify_id = 1;
        final NotificationManager NM = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        builder.setSmallIcon(R.drawable.ic_launcher_background);
        builder.setContentTitle("Picture Transfer");
        builder.setContentText("Transfer in progress...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                File dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
                if (dcim == null) {
                    return;
                }
                File[] files = dcim.listFiles();
                List<File> pictures = new LinkedList<File>();
                int count = 0;
                for (File file : files) {
                    listAllPictures(file, pictures);
                    for (File pic : pictures) {
                        Socket socket = null;
                        try {
                            InetAddress serverAddr = InetAddress.getByName(ipAddr);
                            socket = new Socket(serverAddr, port);
                            builder.setProgress(pictures.size(), count, false);
                            if (count == pictures.size() / 2) {
                                // halfway there
                                builder.setContentText("Halfway through and going...");
                            }
                            NM.notify(notify_id, builder.build());
                            if (sendPictureViaTCP(socket, pic)) {
                                count++;
                            }
                        } catch(Exception e) {
                            Log.e("TCP", "Connection Failed", e);
                            return;
                        } finally {
                            if (socket != null) {
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e("TCP", "Error closing socket", e);
                                }
                            }
                        }
                    }
                    builder.setProgress(0, 0, false);
                    builder.setContentText("Transfer Complete");
                    NM.notify(notify_id, builder.build());
                }
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


    /**
     *
     * @param socket - socket to send through.
     * @param pic - picture to send.
     * @return - true if send was successful.
     */
    private boolean sendPictureViaTCP(Socket socket, File pic) {
        try {
            OutputStream output = socket.getOutputStream();
            DataOutputStream outputStream = new DataOutputStream(output);
            byte[] name = pic.getName().getBytes();
            byte[] imgBytes = convertPictureToBytes(pic);
            //byte[] imgBytes = readImageData(pic);
            if (imgBytes != null) {
                // length of picture
                int length = name.length;
                outputStream.writeInt(length);
                output.write(name);
                outputStream.writeInt(imgBytes.length);
                //output.write(imgBytes);
                outputStream.write(imgBytes, 0, imgBytes.length);
                output.flush();
            }
        } catch (Exception e) {
            Log.e("TCP", "Error Transferring picture" + pic.getName(), e);
            return false;
        }
        return true;
    }

    private byte[] readImageData(File pic) {
        byte[] imgData;
        try {
            FileInputStream fis = new FileInputStream(pic);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            while ((fis.read(buffer, 0, buffer.length)) != -1) {
                output.write(buffer);
            }
            imgData = output.toByteArray();
            return imgData;
        } catch (FileNotFoundException e) {
            Log.e("File", "Invalid file", e);
            return null;
        } catch (IOException e) {
            Log.e("File", "Failed reading file data", e);
            return null;
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
