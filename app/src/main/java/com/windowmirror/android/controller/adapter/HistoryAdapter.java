package com.windowmirror.android.controller.adapter;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.windowmirror.android.R;
import com.windowmirror.android.model.Entry;
import com.windowmirror.android.util.NetworkUtility;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * @author alliecurry
 */
public class HistoryAdapter extends ArrayAdapter<Entry> {
    private static final int TEXT_MAX_LINES = 2;
    private static final long MAX_RELATIVE_TIME = 21600000;
    private static final SimpleDateFormat LONG_FORMAT = new SimpleDateFormat("MMM d yyyy h:mm a", Locale.US);
    private Listener listener;

    public HistoryAdapter(final Context context, final Listener listener) {
        super(context, 0);
        this.listener = listener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final View row;
        final ViewHolder viewHolder;

        if (convertView == null) {
            row = LayoutInflater.from(getContext()).inflate(R.layout.list_item_history, parent, false);
            viewHolder = initViewHolder(row);
            row.setTag(viewHolder);
        } else {
            row = convertView;
            viewHolder = (ViewHolder) row.getTag();
        }

        final Entry entry = getItem(position);

        final CharSequence dateStr = getRelativeTime(getContext(), entry.getTimestamp());
        viewHolder.date.setText(dateStr);

        viewHolder.transcription.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                viewHolder.transcription.setMaxLines(TEXT_MAX_LINES);
                if (viewHolder.transcription instanceof EditText) {
                    ((EditText) viewHolder.transcription).setSelection(0,
                            viewHolder.transcription.length());
                }
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                        getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("WindowMirror",
                        viewHolder.transcription.getText());
                clipboard.setPrimaryClip(clip);

                Toast.makeText(getContext(), "Text copied to clipboard", Toast.LENGTH_SHORT).show();

                return true;
            }
        });

        final String transcriptionStr = entry.getTranscription();
        if (transcriptionStr != null && !transcriptionStr.isEmpty()) {
            viewHolder.transcription.setText(transcriptionStr);
            final View.OnClickListener onClick = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (viewHolder.transcription.getMaxLines() == TEXT_MAX_LINES) {
                        viewHolder.transcription.setMaxLines(Integer.MAX_VALUE);
                    } else {
                        viewHolder.transcription.setMaxLines(TEXT_MAX_LINES);
                    }
                    row.invalidate();
                    if (viewHolder.transcription instanceof EditText) {
                        ((EditText) viewHolder.transcription).setSelection(0);
                    }
                }
            };
            viewHolder.transcription.setOnClickListener(onClick);
            viewHolder.date.setOnClickListener(onClick);
        } else {
            switch (entry.getOxfordStatus()) {
                case NONE:
                case PENDING:
                    if (NetworkUtility.isNetworkAvailable(getContext())) {
                        viewHolder.transcription.setText(R.string.transcription_pending);
                    } else {
                        viewHolder.transcription.setText(R.string.transcription_pending_no_network);
                    }
                    break;
                case SUCCESSFUL:
                    viewHolder.transcription.setText(R.string.transcription_success);
                    break;
                case REQUIRES_RETRY:
                    if (NetworkUtility.isNetworkAvailable(getContext())) {
                        viewHolder.transcription.setText(R.string.transcription_retry);
                    } else {
                        viewHolder.transcription.setText(R.string.transcription_retry_no_network);
                    }
                    break;
                case FAILED:
                    viewHolder.transcription.setText(R.string.transcription_failed);
                    break;
            }
        }


        final boolean isPlaying = listener.isEntryPlaying(entry);
        final int drawableTop = isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
        viewHolder.play.setCompoundDrawablesWithIntrinsicBounds(0, drawableTop, 0, 0);
        viewHolder.play.setText(formatDuration(entry.getDuration()));
        viewHolder.play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onPausePlay(entry);
            }
        });

        return row;
    }

    /** @return a formatted / user-friendly version of the given time relative to the current time. */
    private static CharSequence getRelativeTime(final Context c, final long millis) {
        final long now = System.currentTimeMillis();
        final long difference = now - millis;

        if (difference > MAX_RELATIVE_TIME) {
            return LONG_FORMAT.format(new Date(millis));
        }

        return (difference >= 0 && difference <= DateUtils.MINUTE_IN_MILLIS) ?
                c.getString(R.string.now) :
                DateUtils.getRelativeTimeSpanString(
                        millis,
                        now,
                        DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE);
    }

    private static String formatDuration(final long millis) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis)
                - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis));
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
                - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis));
        long hours = TimeUnit.MILLISECONDS.toHours(millis);

        StringBuilder b = new StringBuilder();
        if (hours > 0) {
            b.append(String.valueOf(hours));
            b.append(":");
        }
        b.append(minutes == 0 ? "00" : minutes < 10 ? String.valueOf("0" + minutes) :
                String.valueOf(minutes));
        b.append(":");
        b.append(seconds == 0 ? "00" : seconds < 10 ? String.valueOf("0" + seconds) :
                String.valueOf(seconds));
        return b.toString();
    }

    private static ViewHolder initViewHolder(final View row) {
        final ViewHolder viewHolder = new ViewHolder();
        viewHolder.play = (TextView) row.findViewById(R.id.play);
        viewHolder.date = (TextView) row.findViewById(R.id.date);
        viewHolder.transcription = (TextView) row.findViewById(R.id.transcription);
        return viewHolder;
    }

    private static class ViewHolder {
        private TextView play;
        private TextView date;
        private TextView transcription;
    }

    public interface Listener {
        void onPausePlay(final Entry entry);
        boolean isEntryPlaying(final Entry entry);
    }
}
