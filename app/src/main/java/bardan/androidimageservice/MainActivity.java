package bardan.androidimageservice;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
