package com.windowmirror.android.controller.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import com.windowmirror.android.R;
import com.windowmirror.android.util.LocalPrefs;


/**
 * Fragment for manging app settings.
 * @author alliecurry
 */
public class SettingsFragment extends Fragment implements CompoundButton.OnCheckedChangeListener {
    public static final String TAG = SettingsFragment.class.getSimpleName();

    private CompoundButton serviceCheckBox;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_settings, container, false);
        serviceCheckBox = (CompoundButton) v.findViewById(R.id.service_checkbox);
        serviceCheckBox.setChecked(LocalPrefs.getIsBackgroundService(getContext()));
        serviceCheckBox.setOnCheckedChangeListener(this);
        return v;
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (serviceCheckBox != null) {
            LocalPrefs.setBackgroundService(getContext(), serviceCheckBox.isChecked());
        }
    }
}
