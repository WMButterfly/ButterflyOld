package com.windowmirror.android.model;

/**
 * Enum used to keep track of transcription status.
 * @author alliecurry
 */
public enum OxfordStatus {
    NONE, // Transcription not sent.
    PENDING, // Transcription sent but has result has yet to return. May need retry after a period of time.
    SUCCESSFUL, // Transcription fully completed.
    REQUIRES_RETRY, // Transcription failed and needs retry.
    FAILED // Transcription failed and no more retry attempts are to be made.
}
