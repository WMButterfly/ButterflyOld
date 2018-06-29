package com.windowmirror.android.model.service;

import com.google.gson.annotations.SerializedName;

import org.joda.time.format.DateTimeFormat;

import java.io.Serializable;

import view.ViewUtility;

public final class Recording implements Serializable {
    private static final long serialVersionUID = 6545620253345768732L;

    @SerializedName("UUID")
    private String uuid;

    @SerializedName("DateCreated")
    private String date;

    @SerializedName("Name")
    private String name;

    @SerializedName("Transcript")
    private String transcript;

    @SerializedName("Length")
    private String length;

    @SerializedName("Recycle")
    private boolean recycle;

    public String getUuid() {
        return uuid;
    }

    public String getDate() {
        return date;
    }

    public String getName() {
        return name;
    }

    public String getTranscript() {
        return transcript;
    }

    public String getLength() {
        return length;
    }

    public long getTimestamp() {
        if (date == null) {
            return 0;
        }
        try {
            return ViewUtility.parseDate(date).getMillis();
        } catch (Exception e) {
            return 0;
        }
    }

    public void setTranscription(String transcription) {
        this.transcript = transcription;
    }

    public void setRecycle(boolean recycle) { this.recycle = recycle; }

    public static class Builder {
        private String name;
        private String transcript;
        private String length;
        private String date;
        private boolean recycle;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder length(String length) {
            this.length = length;
            return this;
        }

        public Builder date(long timestamp) {
            this.date = DateTimeFormat.forPattern(ViewUtility.SERVER_DATE_FORMAT).print(timestamp);
            return this;
        }

        public Builder transcript(String transcript) {
            this.transcript = transcript;
            return this;
        }

        public Builder recycle(boolean recycle) {
            this.recycle = recycle;
            return this;
        }

        public Recording build() {
            Recording recording = new Recording();
            recording.name = name;
            recording.length = length;
            recording.date = date;
            recording.transcript = transcript;
            recording.recycle = recycle;
            return recording;
        }
    }
}
