package cn.freedom.soundtouchdemo;

import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

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
