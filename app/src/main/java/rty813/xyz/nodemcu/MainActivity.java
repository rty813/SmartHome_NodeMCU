package rty813.xyz.nodemcu;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.yanzhenjie.fragment.CompatActivity;

public class MainActivity extends CompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startFragment(MainFragment.class);
    }


    @Override
    protected int fragmentLayoutId() {
        return R.id.rootlayout;
    }
}
