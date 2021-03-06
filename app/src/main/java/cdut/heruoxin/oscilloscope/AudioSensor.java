package cdut.heruoxin.oscilloscope;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

/**
 * Created by heruoxin on 15/5/27.
 */

public class AudioSensor extends BaseSensor {

    private static final int SAMPLE_RATE_IN_HZ = 8000;
    private long mWaitTime;
    private final Callback mCallback;
    private AudioRecord mAudioRecord;
    private final Object mLock;
    private boolean isGetVoiceRun;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(
            SAMPLE_RATE_IN_HZ,
            AudioFormat.CHANNEL_IN_DEFAULT,
            AudioFormat.ENCODING_PCM_16BIT);

    public AudioSensor(long waitTime, Callback callback) {
        mCallback = callback;
        mWaitTime = waitTime;
        mLock = new Object();
        start();
    }

    private void start() {
        if (isGetVoiceRun) {
            return;
        }
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_IN_DEFAULT,
                AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);
        isGetVoiceRun = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                mAudioRecord.startRecording();
                short[] buffer = new short[BUFFER_SIZE];
                while (isGetVoiceRun) {
                    int r = mAudioRecord.read(buffer, 0, BUFFER_SIZE);
                    long v = 0;
                    for (short aBuffer : buffer) {
                        v += aBuffer * aBuffer;
                    }
                    double mean = v / (double) r;
                    double volume = 10 * Math.log10(mean);
                    if (!isPaused) {
                        mCallback.onValueChanged(volume);
                    }
                    synchronized (mLock) {
                        try {
                            mLock.wait(mWaitTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                mAudioRecord.stop();
                mAudioRecord.release();
                mAudioRecord = null;
            }
        }).start();
    }

}

