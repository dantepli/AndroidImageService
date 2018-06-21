package bardan.androidimageservice;

import java.io.File;

public interface Client {
    void connect(String ipAddress, int port);
    boolean sendFile(File file);
    void disconnect();
}
