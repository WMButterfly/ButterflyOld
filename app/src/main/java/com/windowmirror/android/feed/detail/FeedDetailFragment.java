package com.windowmirror.android.feed.detail;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.windowmirror.android.R;
import com.windowmirror.android.listener.NavigationListener;
import com.windowmirror.android.model.Entry;
import com.windowmirror.android.model.OxfordStatus;

import view.ViewUtility;
import view.navigation.ButterflyToolbar;

import static android.content.Context.INPUT_METHOD_SERVICE;
import static com.windowmirror.android.model.OxfordStatus.PENDING;
import static com.windowmirror.android.model.OxfordStatus.REQUIRES_RETRY;

/**
 * Displays full transcription.
 */
public class FeedDetailFragment extends Fragment {
    private static final String KEY_ENTRY = "k_e";
    private Entry mEntry;

    private EditText mTranscriptionView;
    private TextView mDuration;
    private TextView mDate;
    private TextView mTime;
    private View mShareButton;

    private boolean mCanEdit;

    public static FeedDetailFragment create(@NonNull Entry entry) {
        FeedDetailFragment fragment = new FeedDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(KEY_ENTRY, entry);
        fragment.setArguments(args);
        return fragment;
    }


    // Method to hide the soft keyboard
    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager =
                (InputMethodManager)view.getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View layout = inflater.inflate(R.layout.fragment_feed_detail, container, false);
        if (mEntry == null && getArguments() != null) {
            mEntry = (Entry) getArguments().getSerializable(KEY_ENTRY);
        }
        mTranscriptionView = layout.findViewById(R.id.transcription);
        // Sets the onFocusChangeListener of the transcription edit text
        mTranscriptionView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (!hasFocus) {
                    hideKeyboard(view);
                }
            }
        });

        mDuration = layout.findViewById(R.id.length);
        mDate = layout.findViewById(R.id.date);
        mTime = layout.findViewById(R.id.time);
        mShareButton = layout.findViewById(R.id.share);
        displayEntry();
        mShareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onShareClick();
            }
        });
        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof NavigationListener) {
            ((NavigationListener) getActivity()).showToolbar(true);
            ((NavigationListener) getActivity()).setToolbarState(ButterflyToolbar.State.PAGE_UP);
            hideKeyboard(mTranscriptionView);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        saveEntryChanges();
        hideKeyboard(mTranscriptionView);
    }

    private void displayEntry() {
        if (mEntry == null) {
            mTranscriptionView.setText(R.string.entry_not_found);
            return;
        }
        OxfordStatus status = mEntry.getOxfordStatus();
        mCanEdit = status != PENDING && status != REQUIRES_RETRY;
        mTranscriptionView.setFocusable(mCanEdit);
        mTranscriptionView.setEnabled(mCanEdit);
        String transcriptionStr = mEntry.getFullTranscription();
        if (transcriptionStr != null && !transcriptionStr.isEmpty()) {
            mTranscriptionView.setText(transcriptionStr);
        } else {
            mTranscriptionView.setText(ViewUtility.getMessageForStatus(getContext(), mEntry.getOxfordStatus()));
        }
        mDate.setText(ViewUtility.formatDate(mEntry.getTimestamp()));
        mTime.setText(ViewUtility.formatTime(mEntry.getTimestamp()));
        mDuration.setText(ViewUtility.formatDuration(mEntry.getDuration()));
        hideKeyboard(mTranscriptionView);
    }

    private void onShareClick() {
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, mTranscriptionView.getText().toString());
        startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.share_title)));
    }



    private void saveEntryChanges() {
        if (mCanEdit) {
            mEntry.setFullTranscription(mTranscriptionView.getText().toString());
            hideKeyboard(mTranscriptionView);
        }
    }
}
