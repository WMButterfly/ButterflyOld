package com.windowmirror.android.feed.detail;

import android.content.Context;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

// This class sets up the actions of the done button on the soft keyboard
class DoneOnEditorActionListener implements EditText.OnEditorActionListener{
    @Override
    public boolean onEditorAction(TextView editText, int actionId, KeyEvent event) {
        editText.setCursorVisible(true);
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            InputMethodManager inputMethodManager =
                    (InputMethodManager)editText.getContext()
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(editText.getWindowToken(), 0);
            editText.setCursorVisible(false);
            return true;
        }
        return false;
    }
}
