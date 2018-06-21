package bardan.androidimageservice;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class TcpClient implements Client {

    private Socket socket;

    /**
     * connects to the server.
     *
     * @param ipAddress - ip address to connect to.
     * @param port      - port to connect to.
     */
    @Override
    public void connect(String ipAddress, int port) {
        InetAddress serverAddr = null;
        this.socket = null;
        try {
            serverAddr = InetAddress.getByName(ipAddress);
        } catch (UnknownHostException e) {
            Log.e("TCP", "Invalid IP Address.", e);
            return;
        }
        try {
            this.socket = new Socket(serverAddr, port);
        } catch (IOException e) {
            Log.e("TCP", "Connection Failed.", e);
        }
    }

    /**
     * sends the name and the picture file via the tcp socket.
     *
     * @param file - picture file to send.
     * @return - true if successful.
     */
    @Override
    public boolean sendFile(File file) {
        OutputStream output;
        try {
            output = this.socket.getOutputStream();
        } catch (IOException e) {
            Log.e("TCP", "Failed opening output stream.", e);
            return false;
        }
        DataOutputStream outputStream = new DataOutputStream(output);
        byte[] name = file.getName().getBytes();
        byte[] imgBytes = convertPictureToBytes(file);
        if (imgBytes != null) {
            // length of picture
            try {
                outputStream.writeInt(name.length);
                outputStream.write(name, 0, name.length);
                outputStream.writeInt(imgBytes.length);
                outputStream.write(imgBytes, 0, imgBytes.length);
                outputStream.flush();
            } catch (IOException e) {
                Log.e("TCP", "Failed transferring file" + file.getName(), e);
                return false;
            }
        }
        return true;
    }

    /**
     * closes the socket.
     */
    @Override
    public void disconnect() {
        if (this.socket != null) {
            try {
                this.socket.close();
            } catch (IOException e) {
                Log.e("TCP", "Closing socket failed", e);
            }
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
}
