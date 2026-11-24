package ca.yueming.docscan;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import ca.yueming.docscan.passport.ui.PassportActivity;
import ca.yueming.docscan.scan.ScanActivity;

/** Home screen: pick document scan or passport photo. */
public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_scan).setOnClickListener(v -> startActivity(new Intent(this, ScanActivity.class)));
        findViewById(R.id.btn_passport).setOnClickListener(v -> startActivity(new Intent(this, PassportActivity.class)));
        AppLog.i("Main", "started");
    }
}
