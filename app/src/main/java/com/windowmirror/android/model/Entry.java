package com.windowmirror.android.model;

import java.io.Serializable;
import java.util.List;

/**
 * POJO for holding a recording and transcription data.
 * @author alliecurry
 */
public class Entry implements Serializable {
    private static final long serialVersionUID = -2365310762333427288L;

    private long timestamp;
    private long duration;
    private String audioFilePath;
    private List<Transcription> transcriptions;

    private long oxfordTimestamp; // Time in milliseconds the last time this transcription was sent to Oxford

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

    public String getFullTranscription() {
        if (transcriptions == null) {
            return null;
        }
        final StringBuilder sb = new StringBuilder();
        for (final Transcription transcription : transcriptions) {
            final String text = transcription.getText();
            if (text != null && !text.isEmpty()) {
                sb.append(transcription.getText());
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }

    public long getOxfordTimestamp() {
        return oxfordTimestamp;
    }

    public void setOxfordTimestamp(long oxfordTimestamp) {
        this.oxfordTimestamp = oxfordTimestamp;
    }

    public List<Transcription> getTranscriptions() {
        return transcriptions;
    }

    public void setTranscriptions(List<Transcription> transcriptions) {
        this.transcriptions = transcriptions;
    }

    public OxfordStatus getOxfordStatus() {
        if (transcriptions == null || transcriptions.isEmpty()) {
            return OxfordStatus.FAILED;
        }

        boolean isFail = false;
        for (final Transcription transcription : transcriptions) {
            switch (transcription.getOxfordStatus()) {
                case NONE:
                case PENDING:
                    return OxfordStatus.PENDING;
                case SUCCESSFUL:
                    break;
                case REQUIRES_RETRY:
                    return OxfordStatus.REQUIRES_RETRY; // At least one transcription needs to retry
                case FAILED:
                    isFail = true;
                    break;
            }
        }
        return isFail ? OxfordStatus.FAILED : OxfordStatus.SUCCESSFUL;
    }
}
