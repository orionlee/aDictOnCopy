package net.oldev.aDictOnCopy;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Now setup the UI
        setContentView(R.layout.activity_main);

    }

    @Override
    protected void onResume() {
        super.onResume();

        // Start the service.
        // It needs to be in onResume() rather than onCreate(), to support the workflow of
        //   launch > press notification to stop > do something else > launch again,
        // the subsequent launch will invoke onResume(), but may not invoke onCreate(), if the previous instance still exists.
        //
        DictionaryOnCopyService.startForeground(getApplicationContext());
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        // the service remains even when the activity is destroyed.
    }
}

