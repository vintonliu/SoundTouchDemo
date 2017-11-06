////////////////////////////////////////////////////////////////////////////////
///
/// Example class that invokes native SoundTouch routines through the JNI
/// interface.
///
/// Author        : Copyright (c) Olli Parviainen
/// Author e-mail : oparviai 'at' iki.fi
/// WWW           : http://www.surina.net
///
////////////////////////////////////////////////////////////////////////////////
//
// $Id: SoundTouch.java 211 2015-05-15 00:07:10Z oparviai $
//
////////////////////////////////////////////////////////////////////////////////

package net.surina.soundtouch;

import android.util.Log;

import java.nio.ByteBuffer;

public final class SoundTouch {
    //
    // Available setting IDs for the 'setSetting' & 'get_setting' functions:

    /// Enable/disable anti-alias filter in pitch transposer (0 = disable)
    final public static int SETTING_USE_AA_FILTER = 0;

    /// Pitch transposer anti-alias filter length (8 .. 128 taps, default = 32)
    final public static int SETTING_AA_FILTER_LENGTH = 1;

    /// Enable/disable quick seeking algorithm in tempo changer routine
    /// (enabling quick seeking lowers CPU utilization but causes a minor sound
    ///  quality compromising)
    final public static int SETTING_USE_QUICKSEEK = 2;

    /// Time-stretch algorithm single processing sequence length in milliseconds. This determines
    /// to how long sequences the original sound is chopped in the time-stretch algorithm.
    /// See "STTypes.h" or README for more information.
    final public static int SETTING_SEQUENCE_MS = 3;

    /// Time-stretch algorithm seeking window length in milliseconds for algorithm that finds the
    /// best possible overlapping location. This determines from how wide window the algorithm
    /// may look for an optimal joining location when mixing the sound sequences back together.
    /// See "STTypes.h" or README for more information.
    final public static int SETTING_SEEKWINDOW_MS = 4;

    /// Time-stretch algorithm overlap length in milliseconds. When the chopped sound sequences
    /// are mixed back together, to form a continuous sound stream, this parameter defines over
    /// how long period the two consecutive sequences are let to overlap each other.
    /// See "STTypes.h" or README for more information.
    final public static int SETTING_OVERLAP_MS = 5;

    // Native interface function that returns SoundTouch version string.
    // This invokes the native c++ routine defined in "soundtouch-jni.cpp".
    public native final static String getVersionString();

    private native final void setTempo(long handle, float tempo);

    // -50 ~ 100%
    private native final void setTempoChange(long handle, float tempo);

    // Sets pitch change in semi-tones compared to the original pitch
    // (-12 .. +12)
    private native final void setPitchSemiTones(long handle, float pitch);

    private native final void setRate(long handle, float rate);

    private native final void setRateChange(long handle, float rate);

    private native final boolean setSetting(long handle, int settingId, int value);

    private native final int getSetting(long handle, int settingId);

    private native final int processFile(long handle, String inputFile, String outputFile);

    public native final static String getErrorString();

    private native final static long newInstance();

    private native final void deleteInstance(long handle);

    private native final void cachePlayDirectBufferAddress(ByteBuffer byteBuffer);

    private native final void cacheRecordDirectBufferAddress(ByteBuffer byteBuffer);

    /// Returns number of samples currently processed
    private native final int numSamples(long handle);

    private native final int receiveSamples(long handle, int maxSamples);

    private native final void putSamples(long handle, int numSamples);

    private native final void clear(long handle);

    private native final void setSampleRate(long handle, int sampleRate);

    private native final void setChannels(long handle, int channels);

    long handle = 0;

    private final static int maxBufferSize = 1280;
    ByteBuffer playBuffer = null;
    ByteBuffer recBuffer = null;


    public SoundTouch() {
        handle = newInstance();
        playBuffer = ByteBuffer.allocateDirect(maxBufferSize);
        recBuffer = ByteBuffer.allocateDirect(maxBufferSize);
        cachePlayDirectBufferAddress(playBuffer);
        cacheRecordDirectBufferAddress(recBuffer);
    }

    public void close() {
        deleteInstance(handle);
        handle = 0;
    }

    public void setTempo(float tempo) {
        setTempo(handle, tempo);
    }

    public void setTempoChange(float tempo) {
        setTempoChange(handle, tempo);
    }

    public void setPitchSemiTones(float pitch) {
        setPitchSemiTones(handle, pitch);
    }

    public void setRate(float speed) {
        setRate(handle, speed);
    }

    public void setRateChange(float rateChange) {
        setRateChange(handle, rateChange);
    }

    public void setSampleRate(int sampleRate) {
        setSampleRate(handle, sampleRate);
    }

    public void setChannels(int channels) {
        setChannels(handle, channels);
    }

    public boolean setSetting(int settingId, int value) {
        return setSetting(handle, settingId, value);
    }

    public int getSetting(int settingId) {
        return getSetting(handle, settingId);
    }

    public int processFile(String inputFile, String outputFile) {
        return processFile(handle, inputFile, outputFile);
    }

    public void putSamples(byte[] recBuf, int numSamples) {
        recBuffer.rewind();
        recBuffer.put(recBuf);
        putSamples(handle, numSamples);
    }

    public int numSamples() {
        return numSamples(handle);
    }

    public int receivedSamples(byte[] playBuf, int maxSamples) {
        playBuffer.rewind();
        int samples = receiveSamples(handle, maxSamples);
        if (samples > 0) {
            playBuffer.get(playBuf);
            return samples;
        }
        return 0;
    }

    public void clear() {
        clear(handle);
    }

    // Load the native library upon startup
    static {
        System.loadLibrary("soundtouch");
    }
}
