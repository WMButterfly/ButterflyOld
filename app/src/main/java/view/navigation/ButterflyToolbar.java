package view.navigation;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import com.windowmirror.android.R;

/**
 * Custom navigation toolbar displayed on main feed.
 */
public class ButterflyToolbar extends Toolbar {
    private ImageView mDrawerButton;
    private View mLogo;

    private OnClickListener mOnMenuClick;
    private OnClickListener mOnBackClick;

    private State mState = State.DEFAULT;

    public enum State {
        DEFAULT, // shows menu button
        PAGE_UP // shows back button
    }

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
        mDrawerButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mState == null) {
                    return;
                }
                switch (mState) {
                    case DEFAULT:
                        if (mOnMenuClick != null) {
                            mOnMenuClick.onClick(v);
                        }
                        break;
                    case PAGE_UP:
                        if (mOnBackClick != null) {
                            mOnBackClick.onClick(v);
                        }
                        break;
                }
            }
        });
    }

    public void setLogoListener(@Nullable OnClickListener onClickListener) {
        mLogo.setOnClickListener(onClickListener);
    }

    public void setMenuListener(@Nullable OnClickListener onClickListener) {
        mOnMenuClick = onClickListener;
    }

    public void setBackListener(@Nullable OnClickListener onClickListener) {
        mOnBackClick = onClickListener;
    }

    public void setState(@NonNull State state) {
        mState = state;
        switch (state) {
            case DEFAULT:
                mDrawerButton.setImageResource(R.drawable.ic_menu);
                break;
            case PAGE_UP:
                mDrawerButton.setImageResource(R.drawable.ic_back);
                break;
        }
    }
}
