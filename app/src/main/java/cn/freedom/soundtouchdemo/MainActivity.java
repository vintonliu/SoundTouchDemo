package cn.freedom.soundtouchdemo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private Button btnStop = null;
    private Button btnStart = null;
    private EditText editTextTempo = null;
    private EditText editTextPitch = null;
    private EditText editTextSpeed = null;
    private TextView txtViewShow = null;

    AudioDeviceAndroid mDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

        mDevice = new AudioDeviceAndroid(this);

        btnStart = (Button)findViewById(R.id.btnStart);
        btnStop = (Button)findViewById(R.id.btnStop);
        btnStart.setOnClickListener(this);
        btnStop.setOnClickListener(this);

        editTextTempo = (EditText)findViewById(R.id.editTextTempo);
        editTextPitch = (EditText)findViewById(R.id.editTextPitch);
        editTextSpeed = (EditText)findViewById(R.id.editTextSpeed);

        txtViewShow = (TextView)findViewById(R.id.txtViewShow);

        String[] permissions = { Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO};

        String[] unGranted = getUngrantedPermissions(permissions);
        if (unGranted.length > 0) {
            ActivityCompat.requestPermissions(this, unGranted, PERMISSION_REQUEST_CODE);
        }

    }

    private String[] getUngrantedPermissions(String[] allPermission) {
        List<String> result = new ArrayList<String>();
        for (int i = 0; i < allPermission.length; i++) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, allPermission[i]);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                result.add(allPermission[i]);
            }
        }
        return result.toArray(new String[result.size()]);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                Toast.makeText(this, "权限申请成功", Toast.LENGTH_SHORT).show();
                break;

            default:
                break;
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnStart) {
            mDevice.SetTempoChange(Float.parseFloat(editTextTempo.getText().toString()));
            mDevice.SetPitchSemiTones(Float.parseFloat(editTextPitch.getText().toString()));
            mDevice.SetRateChange(Float.parseFloat(editTextSpeed.getText().toString()));
            mDevice.StartPlayout();
            mDevice.StartRecord();
            txtViewShow.setText(R.string.startTip);
        } else if (v.getId() == R.id.btnStop) {
            mDevice.StopRecord();
            mDevice.StopPlayout();
            txtViewShow.setText(R.string.stopTip);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.wav) {
            Intent intent = new Intent();
            intent.setClass(this, ExampleActivity.class);
            startActivity(intent);
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mDevice.Destory();
        mDevice = null;
    }
}
