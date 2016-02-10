package com.windowmirror.android.model;

import java.io.Serializable;
import java.util.List;

/**
 * POJO for holding a recording and transcription data.
 * @author alliecurry
 */
public class Entry implements Serializable {
    private static final long serialVersionUID = -2365310762333427288L;
    private static final int MAX_TRIES = 3;

    private long timestamp;
    private long duration;
    private String audioFilePath;
    private String transcription;
    private List<String> chunks;

    private long oxfordTimestamp; // Time in milliseconds the last time this transcription was sent to Oxford
    private OxfordStatus oxfordStatus = OxfordStatus.NONE;
    private int oxfordTries; // Number of tries this entry has been sent to Project Oxford

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

    public long getOxfordTimestamp() {
        return oxfordTimestamp;
    }

    public void setOxfordTimestamp(long oxfordTimestamp) {
        this.oxfordTimestamp = oxfordTimestamp;
    }

    public OxfordStatus getOxfordStatus() {
        return oxfordStatus;
    }

    public void setOxfordStatus(OxfordStatus oxfordStatus) {
        this.oxfordStatus = oxfordStatus;
    }

    public void incrementOxfordTries() {
        ++oxfordTries;
    }

    public void updateForEmptyTranscription() {
        if (oxfordTries < MAX_TRIES) {
            oxfordStatus = OxfordStatus.REQUIRES_RETRY;
        } else {
            oxfordStatus = OxfordStatus.FAILED;
        }
    }

    public List<String> getChunks() {
        return chunks;
    }

    public void setChunks(List<String> chunks) {
        this.chunks = chunks;
    }
}
