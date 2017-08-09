package net.oldev.aDictOnCopy;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Now setup the UI
        setContentView(R.layout.activity_main);

        DictionaryOnCopyService.start(getApplicationContext());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DictionaryOnCopyService.stop(getApplicationContext());
    }
}

