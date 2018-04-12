package com.windowmirror.android.model;

import java.io.Serializable;

/**
 * @author alliecurry
 */
public class Transcription implements Serializable {
    private static final long serialVersionUID = 1439887695255261410L;
    private static final int MAX_TRIES = 3;

    private String filePath;
    private String text;

    private OxfordStatus oxfordStatus = OxfordStatus.NONE;

    //we are creating these only transcribed for now.
    //private OxfordStatus oxfordStatus = OxfordStatus.SUCCESSFUL;

    private int oxfordTries; // Number of tries this entry has been sent to the Speech API (formally Project Oxford)

    public Transcription(final String filePath) {
        this.filePath = filePath;
    }

    public Transcription(final String filePath, final String text) {
        this.filePath = filePath;
        this.text = text;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
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
}
