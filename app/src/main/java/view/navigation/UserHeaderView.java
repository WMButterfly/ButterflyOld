package view.navigation;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import com.windowmirror.android.R;

/**
 * View for displaying authenticated user data.
 */
public class UserHeaderView extends RelativeLayout {

    public UserHeaderView(Context context) {
        super(context);
        init();
    }

    public UserHeaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public UserHeaderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.view_user_header, this);
    }
}
