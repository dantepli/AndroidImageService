package bardan.androidimageservice;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    1);
        }

        Button start_btn = (Button) findViewById(R.id.start);
        Button stop_btn = (Button) findViewById(R.id.stop);

        start_btn.setEnabled(true);
        stop_btn.setEnabled(false);
    }

    public void startService(View view) {
        Intent intent = new Intent(this, ImageServiceService.class);

        Button start_btn = (Button) findViewById(R.id.start);
        Button stop_btn = (Button) findViewById(R.id.stop);

        start_btn.setEnabled(false);
        stop_btn.setEnabled(true);

        startService(intent);
    }

    public void stopService(View view) {
        Intent intent = new Intent(this, ImageServiceService.class);

        Button start_btn = (Button) findViewById(R.id.start);
        Button stop_btn = (Button) findViewById(R.id.stop);

        start_btn.setEnabled(true);
        stop_btn.setEnabled(false);

        stopService(intent);
    }
}
