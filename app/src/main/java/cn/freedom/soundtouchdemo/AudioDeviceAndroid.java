package cn.freedom.soundtouchdemo;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder.AudioSource;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.PresetReverb;
import android.os.Build;
import android.os.Environment;
import android.os.Process;
import android.util.Log;

import com.ucpaas.ucsvqe.UcsVqeConfig;
import com.ucpaas.ucsvqe.UcsVqeInterface;

import net.surina.soundtouch.SoundTouch;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by vinton on 2017/10/09,0009.
 */

public class AudioDeviceAndroid {
    private final static String TAG = "AudioDeviceAndroid";

    private final static int channels = 1;
    private final static int bitsPerSample = 16;
    private final static int bytesPerSample = channels * (bitsPerSample / 8);
    /* per 10 ms callback, 1000 / 10 */
    private final static int buffersPerSecond = 100;
    private final static int sampleRate = 16000;
    private final static int maxBytesPerBuffer = bytesPerSample * 480;
    private final static int samplesPerBuffer = sampleRate / buffersPerSecond;
    private final static int bytesPerBuffer = samplesPerBuffer * (bitsPerSample / 8);

    private AudioRecord mAudioRecord = null;
    private AudioTrack mAudioTrack = null;
    private AudioManager mAudioManager = null;
    private Context mContext = null;

    private SoundTouch mSoundTouch = null;
    private byte[] _tempBufPlay;
    private byte[] _tempBufRec;
    private int recordSizePerBuffer = 0;
    private int playSizePerBuffer = 0;

    private AudioRecordThread mRecordThread = null;
    private AudioTrackThread mTrackThread = null;

    private AcousticEchoCanceler mEchoCanceler = null;
    private PresetReverb mPresetReverb = null;
    private boolean useBuiltInAEC = false;
    private boolean usePresetReverb = true;

    private FileOutputStream mFos = null;
    private boolean isRecordFos = false;

    private class AudioRecordThread extends Thread {
        private volatile boolean keepAlive = true;
        public AudioRecordThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

            try {
                int bytesInRead;
                byte[] tempBuf = new byte[bytesPerBuffer];
                while (keepAlive) {
                    bytesInRead = mAudioRecord.read(_tempBufRec, 0, recordSizePerBuffer);

                    if (bytesInRead == recordSizePerBuffer) {
                        UcsVqeInterface.getInstance().UCSVQE_Process(_tempBufRec, bytesInRead, 10, tempBuf);
                        try {
                            if (isRecordFos) {
                                mFos.write(tempBuf, 0, bytesInRead);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        // sound touch process
                        mSoundTouch.putSamples(tempBuf, samplesPerBuffer);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                if (mAudioRecord != null) {
                    mAudioRecord.stop();
                }

                if (mAudioTrack != null) {
                    mAudioTrack.stop();
                    mAudioTrack.flush();
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }

        public void joinThread() {
            keepAlive = false;
            while (isAlive()) {
                try {
                    join(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class AudioTrackThread extends Thread {
        private volatile boolean keepAlive = true;

        public AudioTrackThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

            try {
                mAudioTrack.play();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }

            int samplesRecv;
            while(keepAlive){
                samplesRecv = mSoundTouch.receivedSamples(_tempBufPlay, samplesPerBuffer);

                if (samplesRecv > 0) {
//                    Log.i(TAG, "samplesRecv = " + samplesRecv);
                    mAudioTrack.write(_tempBufPlay, 0, samplesRecv * bytesPerSample);
                    UcsVqeInterface.getInstance().UCSVQE_FarendAnalysis(_tempBufPlay, samplesRecv * bytesPerSample);
                }

//                try {
//                    Thread.sleep(2);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
            }

            try {
                if (mAudioTrack != null) {
                    mAudioTrack.stop();
                    mAudioTrack.flush();
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }

        public void joinThread() {
            keepAlive = false;
            while (isAlive()) {
                try {
                    join(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public AudioDeviceAndroid(Context context) {
        String ver = SoundTouch.getVersionString();
        Log.i(TAG, "SoundTouch native library version = " + ver);
        mContext = context;
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        mSoundTouch = new SoundTouch();
        mSoundTouch.setSampleRate(sampleRate);
        mSoundTouch.setChannels(channels);

        mSoundTouch.setSetting(SoundTouch.SETTING_USE_QUICKSEEK, 0);
        mSoundTouch.setSetting(SoundTouch.SETTING_USE_AA_FILTER, 0);
//        mSoundTouch.setSetting(SoundTouch.SETTING_SEQUENCE_MS, 10);
//        mSoundTouch.setSetting(SoundTouch.SETTING_SEEKWINDOW_MS, 5);
//        mSoundTouch.setSetting(SoundTouch.SETTING_OVERLAP_MS, 4);

        recordSizePerBuffer = bytesPerSample * (sampleRate / buffersPerSecond);
        Log.i(TAG, "recordSizePerBuffer = " + recordSizePerBuffer);

        playSizePerBuffer = bytesPerSample * (sampleRate / buffersPerSecond);
        Log.i(TAG, "playSizePerBuffer = " + playSizePerBuffer);

        _tempBufPlay = new byte[bytesPerBuffer];
        _tempBufRec = new byte[bytesPerBuffer];
    }

    public boolean StartRecord() {
        Log.i(TAG, "StartRecord()");
        int minBufferSize = AudioRecord.getMinBufferSize(sampleRate,
                                                        AudioFormat.CHANNEL_IN_MONO,
                                                        AudioFormat.ENCODING_PCM_16BIT);
        if (minBufferSize < 2048) {
            minBufferSize *= 2;
        }

        minBufferSize *= 2;

        try {
            mAudioRecord = new AudioRecord(AudioSource.DEFAULT,
                                            sampleRate,
                                            AudioFormat.CHANNEL_IN_MONO,
                                            AudioFormat.ENCODING_PCM_16BIT,
                                            minBufferSize);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        }

        if (mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "StartRecord() failed to new AudioRecord.");
            if (mAudioRecord != null) {
                mAudioRecord.release();
                mAudioRecord = null;
            }
            return false;
        }

        if (BuiltInAECIsAvailable()) {
            mEchoCanceler = AcousticEchoCanceler.create(mAudioRecord.getAudioSessionId());
            if (mEchoCanceler != null) {
                if (mEchoCanceler.setEnabled(useBuiltInAEC) == AudioEffect.SUCCESS) {
                    AudioEffect.Descriptor descriptor = mEchoCanceler.getDescriptor();
                    Log.i(TAG, "AcousticEchoCanceler " + "name: " + descriptor.name + ", " +
                            "implementor: " + descriptor.implementor + ", " + "uuid: " +
                            descriptor.uuid);
                    Log.i(TAG, "AcousticEchoCanceler.getEnabled: " +
                            mEchoCanceler.getEnabled());
                } else {
                    Log.e(TAG, "AcousticEchoCanceler.setEnabled failed");
                }
            } else {
                Log.e(TAG, "AcousticEchoCanceler.create failed");
            }
        }

        UcsVqeConfig config = new UcsVqeConfig();
        UcsVqeInterface.getInstance().UCSVQE_Init(UcsVqeConfig.kUcsSampleRate16kHz, config);

        if (isRecordFos) {
            try {
                mFos = new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/STRecord.pcm");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        try {
            mAudioRecord.startRecording();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return false;
        }

        mRecordThread = new AudioRecordThread("AudioRecordThread");
        mRecordThread.start();
        return true;
    }

    public boolean StopRecord() {
        Log.i(TAG, "StopRecord()");

        if (mRecordThread != null) {
            mRecordThread.joinThread();
            mRecordThread = null;
        }

        if (mEchoCanceler != null) {
            mEchoCanceler.release();
            mEchoCanceler = null;
        }

        if (mAudioRecord != null) {
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;
        }

        UcsVqeInterface.getInstance().UCSVQE_Closed();

        if (isRecordFos && mFos != null) {
            try {
                mFos.close();
                mFos = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Log.i(TAG, "StopRecord() finish");
        return true;
    }

    public boolean StartPlayout() {
        Log.i(TAG, "StartPlayout()");
        int minBuffSize = AudioTrack.getMinBufferSize(sampleRate,
                                                    AudioFormat.CHANNEL_OUT_MONO,
                                                    AudioFormat.ENCODING_PCM_16BIT);

        if (minBuffSize < 6000) {
            minBuffSize *= 2;
        }

        try {
            mAudioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL,
                                        sampleRate,
                                        AudioFormat.CHANNEL_OUT_MONO,
                                        AudioFormat.ENCODING_PCM_16BIT,
                                        minBuffSize,
                                        AudioTrack.MODE_STREAM);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        }

        if (mAudioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
            Log.e(TAG, "StartPlayout() failed to new AudioTrack");
            if (mAudioTrack != null) {
                mAudioTrack.release();
                mAudioTrack = null;
            }
            return false;
        }

        mAudioManager.setSpeakerphoneOn(false);

        mPresetReverb = new PresetReverb(0, mAudioTrack.getAudioSessionId());
        mPresetReverb.setPreset(PresetReverb.PRESET_SMALLROOM);
        mPresetReverb.setEnabled(usePresetReverb);

        try {
            mAudioTrack.play();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

        mTrackThread = new AudioTrackThread("AudioTrackThread");
        mTrackThread.start();
        return true;
    }

    public void StopPlayout() {
//        mAudioManager.setSpeakerphoneOn(false);
        Log.i(TAG, "StopPlayout()");
        if (mTrackThread != null) {
            mTrackThread.joinThread();
            mTrackThread = null;
        }

        if (mAudioTrack != null) {
            mAudioTrack.release();
            mAudioTrack = null;
        }

        mSoundTouch.clear();

        Log.i(TAG, "StopPlayout() finish");
    }

    public void setTempoChange(float tempo) {
        mSoundTouch.setTempoChange(tempo);
    }

    public void setPitchSemiTones(float pitch) {
        mSoundTouch.setPitchSemiTones(pitch);
    }

    public void setRateChange(float speed) {
        mSoundTouch.setRateChange(speed);
    }

    public boolean setSetting(int settingId, int value) {
        return mSoundTouch.setSetting(settingId, value);
    }

    public int getSetting(int settingId) {
        return mSoundTouch.getSetting(settingId);
    }

    public static boolean runningOnJellyBeanOrHigher() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }

    public static boolean BuiltInAECIsAvailable() {
        // AcousticEchoCanceler was added in API level 16 (Jelly Bean).
        if (!runningOnJellyBeanOrHigher()) {
            return false;
        }
        // TODO(henrika): add black-list based on device name. We could also
        // use uuid to exclude devices but that would require a session ID from
        // an existing AudioRecord object.
         return AcousticEchoCanceler.isAvailable();
    }
}
