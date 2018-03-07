package view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.windowmirror.android.R;
import com.windowmirror.android.model.OxfordStatus;
import com.windowmirror.android.util.NetworkUtility;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class ViewUtility {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM d, yyyy", Locale.US);
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("h:mma", Locale.US);

    private ViewUtility() {
        throw new AssertionError();
    }

    public static String formatDate(long timestamp) {
        return DATE_FORMAT.format(new Date(timestamp));
    }

    public static String formatTime(long timestamp) {
        return TIME_FORMAT.format(new Date(timestamp));
    }

    public static String formatDuration(final long millis) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis)
                - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis));
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
                - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis));
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        return formatDurationPart(hours) +
                ":" +
                formatDurationPart(minutes) +
                ":" +
                formatDurationPart(seconds);
    }

    private static String formatDurationPart(long value) {
        return value == 0 ? "00" :
                value < 10 ? String.valueOf("0" + value) :
                        String.valueOf(value);
    }

    @StringRes
    public static int getMessageForStatus(@NonNull Context context,
                                          @NonNull OxfordStatus status) {
        switch (status) {
            case NONE:
            case PENDING:
                if (NetworkUtility.isNetworkAvailable(context)) {
                    return R.string.transcription_pending;
                } else {
                    return R.string.transcription_pending_no_network;
                }
            case SUCCESSFUL:
                return R.string.transcription_success;
            case REQUIRES_RETRY:
                if (NetworkUtility.isNetworkAvailable(context)) {
                    return R.string.transcription_retry;
                } else {
                    return R.string.transcription_retry_no_network;
                }
            case FAILED:
                return R.string.transcription_failed;
        }
        return R.string.transcription_pending;
    }
}
