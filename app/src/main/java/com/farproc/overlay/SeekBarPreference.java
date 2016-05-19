package com.farproc.overlay;


import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import java.util.Locale;

public class SeekBarPreference extends DialogPreference implements OnSeekBarChangeListener {
	
    private SeekBar mSeekBar;

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.seekbar_dialog);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
        
    	mBaseTitle = getDialogTitle().toString();
    }
    
    private String mBaseTitle = "";
    
    @Override
    protected void showDialog(Bundle state) {
    	final int value = getValue();
    	setDialogTitle(getDialogTitleWithPercentage(value));
    	super.showDialog(state);
    }

	private int getValue() {
		return getPersistedInt(100);
	}

    @Override
    protected void onBindDialogView(View view) {
    	final int value = getValue();
    	setDialogTitle(getDialogTitleWithPercentage(value));
        super.onBindDialogView(view);
        
        mSeekBar = (SeekBar)view.findViewById(R.id.SeekBarDialog_SeekBar);
        mSeekBar.setOnSeekBarChangeListener(this);
        mSeekBar.setProgress(value);
        
    }
    
    @Override
    protected void onDialogClosed (boolean positiveResult) {
    	super.onDialogClosed(positiveResult);
    	if(positiveResult) {
    		int value = mSeekBar.getProgress();
    		persistInt(value);
    		callChangeListener(value);
    	}
        if(mOnDialogCLosedListener != null) {
            mOnDialogCLosedListener.onDialogClosed(positiveResult);
        }
    }

    public static interface OnDialogClosedListener {
        public void onDialogClosed(boolean positiveResult);
    }

    private OnDialogClosedListener mOnDialogCLosedListener;

    public void setOnDialogClosedListener(OnDialogClosedListener l) {
        mOnDialogCLosedListener = l;
    }

    private SeekBar.OnSeekBarChangeListener mOnSeekBarChangedListener;

    public void setOnSeekbarChangedListener(SeekBar.OnSeekBarChangeListener l) {
        mOnSeekBarChangedListener = l;
    }

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		final Dialog dialog = getDialog();
		if(dialog != null) {
			dialog.setTitle(getDialogTitleWithPercentage(progress));
		}
        if(mOnSeekBarChangedListener != null) {
            mOnSeekBarChangedListener.onProgressChanged(seekBar, progress, fromUser);
        }
	}

	private String getDialogTitleWithPercentage(int value) {
		return String.format((Locale)null, "%s %d%%", mBaseTitle, value);
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
        if(mOnSeekBarChangedListener != null) {
            mOnSeekBarChangedListener.onStartTrackingTouch(seekBar);
        }
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
        if(mOnSeekBarChangedListener != null) {
            mOnSeekBarChangedListener.onStopTrackingTouch(seekBar);
        }
	}
}
