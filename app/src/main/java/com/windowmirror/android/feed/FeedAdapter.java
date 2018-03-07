package com.windowmirror.android.feed;

import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.windowmirror.android.R;
import com.windowmirror.android.model.Entry;
import com.windowmirror.android.model.OxfordStatus;

import java.util.ArrayList;
import java.util.List;

import view.ViewUtility;

/**
 * Adapter for displays a list of recordings.
 * @author alliecurry
 */
public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.ViewHolder> {
    private static final int TEXT_MAX_LINES = 6;
    private Listener listener;
    private List<Entry> entries;

    public interface Listener {
        void onPausePlay(@NonNull Entry entry);
        boolean isEntryPlaying(@NonNull Entry entry);
        void onEntrySelected(@NonNull Entry entry);
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

    @Override
    public int getItemCount() {
        return entries == null ? 0 : entries.size();
    }

    public void addEntry(@NonNull Entry entry) {
        if (entries == null) {
            entries = new ArrayList<>();
        }
        if (!entries.contains(entry)) {
            entries.add(0, entry);
            notifyItemInserted(0);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView play;
        private TextView date;
        private TextView time;
        private TextView length;
        private TextView transcription;

        ViewHolder(final View itemView) {
            super(itemView);
            play = itemView.findViewById(R.id.play);
            date = itemView.findViewById(R.id.date);
            time = itemView.findViewById(R.id.time);
            length = itemView.findViewById(R.id.length);
            transcription = itemView.findViewById(R.id.transcription);
        }

        void bind(@NonNull final Entry entry,
                  @NonNull final Listener listener) {
            bindTranscription(entry.getFullTranscription(), entry.getOxfordStatus());
            date.setText(ViewUtility.formatDate(entry.getTimestamp()));
            time.setText(ViewUtility.formatTime(entry.getTimestamp()));
            length.setText(ViewUtility.formatDuration(entry.getDuration()));
            final boolean isPlaying = listener.isEntryPlaying(entry);
            @DrawableRes int drawable = isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
            play.setImageResource(drawable);
            play.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onPausePlay(entry);
                }
            });
            transcription.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onEntrySelected(entry);
                }
            });
        }

        private void bindTranscription(String transcriptionStr,
                                       OxfordStatus status) {
            if (transcriptionStr != null && !transcriptionStr.isEmpty()) {
                transcription.setText(transcriptionStr);
            } else {
                transcription.setText(ViewUtility.getMessageForStatus(itemView.getContext(), status));
            }
        }
    }
}
