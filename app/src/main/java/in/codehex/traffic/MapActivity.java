package in.codehex.traffic;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

public class MapActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        Toast.makeText(MapActivity.this, getIntent().getStringExtra("position"), Toast.LENGTH_SHORT).show();
    }
}
