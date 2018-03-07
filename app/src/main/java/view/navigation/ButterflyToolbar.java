package view.navigation;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import com.windowmirror.android.R;

/**
 * Custom navigation toolbar displayed on main feed.
 */
public class ButterflyToolbar extends RelativeLayout {
    private View mDrawerButton;
    private View mLogo;

    public ButterflyToolbar(Context context) {
        super(context);
        init();
    }

    public ButterflyToolbar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ButterflyToolbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.view_toolbar, this);
        mDrawerButton = findViewById(R.id.drawer_button);
        mLogo = findViewById(R.id.logo);
    }

    public void setLogoListener(@Nullable OnClickListener onClickListener) {
        mLogo.setOnClickListener(onClickListener);
    }

    public void setDrawerListener(@Nullable OnClickListener onClickListener) {
        mDrawerButton.setOnClickListener(onClickListener);
    }
}
