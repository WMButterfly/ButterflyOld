package com.windowmirror.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.windowmirror.android.model.service.Recording;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * POJO for holding a recording and transcription data.
 * Update: Entry now encompasses "local data" with extra tracking needed while talking to Speech API
 * (eg server data doesn't understand multi-piece transcriptions that come back in chunks)
 * while Recording Object represents the pieces associated with the WindowMirror backend.
 * A better implementation would have the server running the Speech API
 * so that client doesn't have to worry about pending transcript pieces
 * but until then, app should do it's best to track data between all services (local vs backend vs azure)
 * @author alliecurry
 */
public class Entry implements Serializable {
    private static final long serialVersionUID = -2365310762333427288L;

    private long timestamp;
    private long duration;
    private String audioFilePath;
    private List<Transcription> transcriptions;
    private String uuid;
    private long oxfordTimestamp; // Time in milliseconds the last time this transcription was sent to Oxford
    private Recording recording; // Server data associated with this entry

    public long getTimestamp() {
        if (timestamp == 0 && recording != null) {
            return recording.getTimestamp();
        }
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
            if (recording != null) {
                return recording.getTranscript();
            }
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

    public void setFullTranscription(String transcription) {
        Transcription transcription1 = new Transcription(null, transcription);
        transcription1.setOxfordStatus(OxfordStatus.SUCCESSFUL);
        transcriptions = new ArrayList<>();
        transcriptions.add(transcription1);
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
            if (recording != null) {
                // No local transcriptions but there is a recording entry ... implies past success
                return OxfordStatus.SUCCESSFUL;
            }
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

    /**
     * Create a server-ready Recording from the local data.
     * Only meant to be used when first creating the Server Object.
     */
    public Recording toRecording() {
        return new Recording.Builder()
                .date(timestamp)
                .transcript(getFullTranscription())
//                .length(duration) // FIXME have server switch length/duration field to type long
                .build();
    }

    /**
     * Creates an Entry wrapper around the given Recording
     */
    public static Entry fromRecording(@NonNull Recording recording) {
        Entry entry = new Entry();
        entry.recording = recording;
        return entry;
    }

    @Nullable
    public Recording getRecording() {
        return recording;
    }

    public void setRecording(Recording recording) {
        this.recording = recording;
        if (recording != null) {
            this.uuid = recording.getUuid();
        }
    }

    public String getUuid() {
        return uuid;
    }
}
