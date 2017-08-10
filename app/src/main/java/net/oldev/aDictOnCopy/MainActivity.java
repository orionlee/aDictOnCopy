package net.oldev.aDictOnCopy;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Now setup the UI
        setContentView(R.layout.activity_main);

        View startCtl = findViewById(R.id.startCtl);
        startCtl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DictionaryOnCopyService.startForeground(getApplicationContext());
                MainActivity.this.finish();
            }
        });

        // Let the main activity acts as a convenient shortcut to stop the service as well
        if (DictionaryOnCopyService.isRunning()) {
            DictionaryOnCopyService.stopForeground(getApplicationContext());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // the service remains even when the activity is destroyed.
    }
}

