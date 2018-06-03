package com.windowmirror.android.feed.detail;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.KeyEvent;
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
    public void hideKeyboard(EditText editText) {
        InputMethodManager inputMethodManager =
                (InputMethodManager)editText.getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }

    // Method to hide the cursor
    public void hideCursor(EditText editText) {
        editText.setCursorVisible(false);
    }

    // Method to show the cursor
    public void showCursor(EditText editText) {
        editText.setCursorVisible(true);
    }



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View layout = inflater.inflate(R.layout.fragment_feed_detail, container, false);
        if (mEntry == null && getArguments() != null) {
            mEntry = (Entry) getArguments().getSerializable(KEY_ENTRY);
        }
        mTranscriptionView = layout.findViewById(R.id.transcription);
        // Sets the soft keyboard's return button to a done button that dismisses the keyboard
        mTranscriptionView.setRawInputType(EditorInfo.TYPE_CLASS_TEXT);
        mTranscriptionView.setImeOptions(EditorInfo.IME_ACTION_DONE);



        mTranscriptionView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (view.getId() == mTranscriptionView.getId()) {
                    showCursor(mTranscriptionView);
                }
            }
        });
        // Event handler for the done button on the soft keyboard
        // If user clicks done, the cursor along with the keyboard disappear
        mTranscriptionView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && keyCode == KeyEvent.KEYCODE_ENTER) {
                    hideKeyboard(mTranscriptionView);
                    hideCursor(mTranscriptionView);
                    return true;
                }
                return false;
            }
        });

        // Sets the onFocusChangeListener of the transcription edit text
        // if the focus changes to another view the keyboard will disappear
        mTranscriptionView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (!hasFocus) {
                    hideKeyboard(mTranscriptionView);
                    hideCursor(mTranscriptionView);
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
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        saveEntryChanges();
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
        }
    }
}
