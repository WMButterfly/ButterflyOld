package com.windowmirror.android.feed;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.windowmirror.android.R;
import com.windowmirror.android.model.Entry;
import com.windowmirror.android.model.OxfordStatus;
import com.windowmirror.android.util.NetworkUtility;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Adapter for displays a list of recordings.
 * @author alliecurry
 */
public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.ViewHolder> {
    private static final int TEXT_MAX_LINES = 6;
    private static final long MAX_RELATIVE_TIME = 21600000;
    private static final SimpleDateFormat LONG_FORMAT = new SimpleDateFormat("MMM d yyyy h:mm a", Locale.US);
    private Listener listener;
    private List<Entry> entries;

    public interface Listener {
        void onPausePlay(final Entry entry);
        boolean isEntryPlaying(final Entry entry);
    }

    FeedAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void setEntries(List<Entry> entries) {
        this.entries = entries;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_history, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final Entry entry = entries.get(position);
        holder.bind(entry, listener);
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

    @Override
    public int getItemCount() {
        return entries == null ? 0 : entries.size();
    }

    public void addEntry(@NonNull Entry entry) {
        if (entries == null) {
            entries = new ArrayList<>();
        }
        entries.add(0, entry);
        notifyItemInserted(0);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView play;
        private TextView date;
        private TextView time;
        private TextView length;
        private EditText transcription;

        ViewHolder(final View itemView) {
            super(itemView);
            play = itemView.findViewById(R.id.play);
            date = itemView.findViewById(R.id.date);
            time = itemView.findViewById(R.id.time);
            length = itemView.findViewById(R.id.length);
            transcription = itemView.findViewById(R.id.transcription);

            transcription.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    transcription.setMaxLines(TEXT_MAX_LINES);
                    transcription.setSelection(0, transcription.length());
                    ClipboardManager clipboard = (ClipboardManager)
                            itemView.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Butterfly",
                            transcription.getText());
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(itemView.getContext(), "Text copied to clipboard", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
            });
        }

        void bind(@NonNull final Entry entry,
                  @NonNull final Listener listener) {
            final Context context = itemView.getContext();
            bindTranscription(entry.getFullTranscription(), entry.getOxfordStatus());
            final CharSequence dateStr = getRelativeTime(context, entry.getTimestamp());
            date.setText(dateStr);
            length.setText(formatDuration(entry.getDuration()));
            final boolean isPlaying = listener.isEntryPlaying(entry);
            @DrawableRes int drawable = isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
            play.setImageResource(drawable);
            play.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onPausePlay(entry);
                }
            });
        }

        private void bindTranscription(String transcriptionStr,
                                       OxfordStatus status) {
            if (transcriptionStr != null && !transcriptionStr.isEmpty()) {
                transcription.setText(transcriptionStr);
                final View.OnClickListener onClick = new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (transcription.getMaxLines() == TEXT_MAX_LINES) {
                            transcription.setMaxLines(Integer.MAX_VALUE);
                        } else {
                            transcription.setMaxLines(TEXT_MAX_LINES);
                        }
                        transcription.setSelection(0);
                    }
                };
                transcription.setOnClickListener(onClick);
                date.setOnClickListener(onClick);
            } else {
                switch (status) {
                    case NONE:
                    case PENDING:
                        if (NetworkUtility.isNetworkAvailable(itemView.getContext())) {
                            transcription.setText(R.string.transcription_pending);
                        } else {
                            transcription.setText(R.string.transcription_pending_no_network);
                        }
                        break;
                    case SUCCESSFUL:
                        transcription.setText(R.string.transcription_success);
                        break;
                    case REQUIRES_RETRY:
                        if (NetworkUtility.isNetworkAvailable(itemView.getContext())) {
                            transcription.setText(R.string.transcription_retry);
                        } else {
                            transcription.setText(R.string.transcription_retry_no_network);
                        }
                        break;
                    case FAILED:
                        transcription.setText(R.string.transcription_failed);
                        break;
                }
            }
        }
    }
}
