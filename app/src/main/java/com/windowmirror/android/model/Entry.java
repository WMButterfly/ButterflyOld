package com.windowmirror.android.model;

import java.io.Serializable;

/**
 * POJO for holding a recording and transacription data.
 * @author alliecurry
 */
public class Entry implements Serializable {
    private static final long serialVersionUID = -2365310762333427288L;

    private long timestamp;
    private long duration;
    private String audioFilePath;
    private String transcription;

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getAudioFilePath() {
        return audioFilePath;
    }

    public void setAudioFilePath(String audioFilePath) {
        this.audioFilePath = audioFilePath;
    }

    public String getTranscription() {
        return transcription;
    }

    public void setTranscription(String transcription) {
        this.transcription = transcription;
    }
}
