package com.windowmirror.android.controller.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CheckBox;
import com.windowmirror.android.R;
import com.windowmirror.android.util.LocalPrefs;

/**
 * Dialog for manging app settings.
 * @author alliecurry
 */
public class SettingsDialog extends DialogFragment {
    public static final String TAG = SettingsDialog.class.getSimpleName();

    private CheckBox serviceCheckBox;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.dialog_settings, container, false);
        serviceCheckBox = (CheckBox) v.findViewById(R.id.service_checkbox);
        serviceCheckBox.setChecked(LocalPrefs.getIsBackgroundService(getContext()));
        v.findViewById(R.id.save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        final Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.requestFeature(Window.FEATURE_NO_TITLE);
            window.setBackgroundDrawableResource(R.drawable.dialog_background);
        }

        setCancelable(false);

        return v;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (serviceCheckBox != null) {
            LocalPrefs.setBackgroundService(getContext(), serviceCheckBox.isChecked());
        }
        super.onDismiss(dialog);
    }
}
