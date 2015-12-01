package com.windowmirror.android.controller.adapter;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.windowmirror.android.R;
import com.windowmirror.android.model.Entry;

import java.util.concurrent.TimeUnit;

/**
 * @author alliecurry
 */
public class HistoryAdapter extends ArrayAdapter<Entry> {
    private static final int TEXT_MAX_LINES = 2;
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

        final String transcriptionStr = entry.getTranscription();
        if (transcriptionStr == null || transcriptionStr.isEmpty()) {
            // Transcription not found...
            viewHolder.transcription.setText(R.string.transcription_pending);
        } else {
            viewHolder.transcription.setText(transcriptionStr);
            final View.OnClickListener onClick = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (viewHolder.transcription.getMaxLines() == TEXT_MAX_LINES) {
                        viewHolder.transcription.setMaxLines(Integer.MAX_VALUE);
                    } else {
                        viewHolder.transcription.setMaxLines(TEXT_MAX_LINES);
                    }
                }
            };
            viewHolder.transcription.setOnClickListener(onClick);
            viewHolder.date.setOnClickListener(onClick);
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
