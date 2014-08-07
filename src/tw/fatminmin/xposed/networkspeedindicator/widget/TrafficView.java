package tw.fatminmin.xposed.networkspeedindicator.widget;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import tw.fatminmin.xposed.networkspeedindicator.Common;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;
import de.robv.android.xposed.XSharedPreferences;

@SuppressLint("HandlerLeak")
public class TrafficView extends TextView {

    public PositionCallback mPositionCallback = null;
    public TextView clock = null;
    
	private static final String TAG = TrafficView.class.getSimpleName();
	private DecimalFormat uploadDecimalFormat, downloadDecimalFormat;
	private static final DecimalFormat formatWithDecimal    = new DecimalFormat(" ##0.0");
	private static final DecimalFormat formatWithoutDecimal = new DecimalFormat(" ##0");
	
	private boolean mAttached;
	// TrafficStats mTrafficStats;

	long uploadSpeed, downloadSpeed;
	long totalTxBytes, totalRxBytes;
	long lastUpdateTime;
	String networkType;
	boolean networkState;

	XSharedPreferences mPref;
	int prefPosition;
	int prefForceUnit;
	int prefFontSize;
	int prefSuffix;
	int prefDisplay;
	int prefUpdateInterval;
	int prefColorMode;
	int prefColor;
	boolean prefShowUploadSpeed;
	boolean prefShowDownloadSpeed;
	boolean prefHideUnit;
	boolean prefNoSpace;
	boolean prefHideB;
	boolean prefHideInactive;
	boolean prefShowSuffix;
	String prefNetworkType;
	Set<String> prefFontStyle = new HashSet<String>();
	
	String uploadSuffix = "";
	String downloadSuffix = "";

	public TrafficView(Context context) {
		this(context, null);
		mAttached = false;
	}

	public TrafficView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public TrafficView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		loadPreferences();
		updateConnectionInfo();
		updateViewVisibility();
	}
	
	public void refreshPosition() {
	    switch(prefPosition) {
        case 0:
            mPositionCallback.setLeft();
            break;
        case 1:
            mPositionCallback.setRight();
            break;
        case 2:
            mPositionCallback.setAbsoluteLeft();
            break;
        }
	}
	
	@SuppressLint("NewApi")
	public void refreshColor() {
		switch(prefColorMode) {
		case 0:
			if(clock != null) {
				setTextColor(clock.getCurrentTextColor());
			}
			else {
				// gingerbread;
				setTextColor(Color.parseColor("#33b5e5"));
			}
			break;
		case 1:
			setTextColor(prefColor);
			break;
		}
	}
	
	private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {

		@SuppressWarnings("unchecked")
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
				updateConnectionInfo();
				updateViewVisibility();
			} else if (action.equals(Common.ACTION_SETTINGS_CHANGED)) {
				Log.i(TAG, "SettingsChanged");
				if (intent.hasExtra(Common.KEY_FORCE_UNIT)) {
					prefForceUnit = intent.getIntExtra(Common.KEY_FORCE_UNIT, Common.DEF_FORCE_UNIT);
				}
				if(intent.hasExtra(Common.KEY_SHOW_UPLOAD_SPEED)) {
				    prefShowUploadSpeed = intent.getBooleanExtra(Common.KEY_SHOW_UPLOAD_SPEED, Common.DEF_SHOW_UPLOAD_SPEED);
				}
				if(intent.hasExtra(Common.KEY_SHOW_DOWNLOAD_SPEED)) {
				    prefShowDownloadSpeed = intent.getBooleanExtra(Common.KEY_SHOW_DOWNLOAD_SPEED, Common.DEF_SHOW_DOWNLOAD_SPEED);
				}
				if (intent.hasExtra(Common.KEY_HIDE_UNIT)) {
				    prefHideUnit = intent.getBooleanExtra(Common.KEY_HIDE_UNIT, Common.DEF_HIDE_UNIT);
				}
				if (intent.hasExtra(Common.KEY_NO_SPACE)) {
				    prefNoSpace = intent.getBooleanExtra(Common.KEY_NO_SPACE, Common.DEF_NO_SPACE);
				}
				if (intent.hasExtra(Common.KEY_HIDE_B)) {
				    prefHideB = intent.getBooleanExtra(Common.KEY_HIDE_B, Common.DEF_HIDE_B);
				}
				if (intent.hasExtra(Common.KEY_HIDE_INACTIVE)) {
					prefHideInactive = intent.getBooleanExtra(Common.KEY_HIDE_INACTIVE, Common.DEF_HIDE_INACTIVE);
				}
				if (intent.hasExtra(Common.KEY_SHOW_SUFFIX)) {
				    prefShowSuffix = intent.getBooleanExtra(Common.KEY_SHOW_SUFFIX, Common.DEF_SHOW_SUFFIX);
				}
				if(intent.hasExtra(Common.KEY_FONT_SIZE)) {
				    prefFontSize = intent.getIntExtra(Common.KEY_FONT_SIZE, Common.DEF_FONT_SIZE);
				}
				if(intent.hasExtra(Common.KEY_POSITION)) {
				    prefPosition = intent.getIntExtra(Common.KEY_POSITION, Common.DEF_POSITION);
				    refreshPosition();
				}
				if(intent.hasExtra(Common.KEY_SUFFIX)) {
				    prefSuffix = intent.getIntExtra(Common.KEY_SUFFIX, Common.DEF_SUFFIX);
				}
				if(intent.hasExtra(Common.KEY_NETWORK_TYPE)) {
				    prefNetworkType = intent.getStringExtra(Common.KEY_NETWORK_TYPE);
				}
				if(intent.hasExtra(Common.KEY_DISPLAY)) {
                    prefDisplay = intent.getIntExtra(Common.KEY_DISPLAY, Common.DEF_DISPLAY);
                }
				
				if(intent.hasExtra(Common.KEY_UPDATE_INTERVAL)) {
                    prefUpdateInterval = intent.getIntExtra(Common.KEY_UPDATE_INTERVAL, Common.DEF_UPDATE_INTERVAL);
                }
				if(intent.hasExtra(Common.KEY_COLOR_MODE)) {
					prefColorMode = intent.getIntExtra(Common.KEY_COLOR_MODE, Common.DEF_COLOR_MODE);
				}
				if(intent.hasExtra(Common.KEY_COLOR)) {
					prefColor = intent.getIntExtra(Common.KEY_COLOR, Common.DEF_COLOR);
				}
				if(intent.hasExtra(Common.KEY_FONT_STYLE)) {
					prefFontStyle = (Set<String>) intent.getSerializableExtra(Common.KEY_FONT_STYLE);
				}
				updateViewVisibility();
			}
		}
	};

	@SuppressLint("HandlerLeak")
    Handler mTrafficHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			long td = SystemClock.elapsedRealtime() - lastUpdateTime;
			if (td == 0) {
				return;
			}
			
			
			uploadSpeed = (TrafficStats.getTotalTxBytes() - totalTxBytes) * 1000 / td;
			downloadSpeed = (TrafficStats.getTotalRxBytes() - totalRxBytes) * 1000 / td;
			totalTxBytes = TrafficStats.getTotalTxBytes();
			totalRxBytes = TrafficStats.getTotalRxBytes();
			lastUpdateTime = SystemClock.elapsedRealtime();
			
			SpannableString spanString = new SpannableString(createText());
			if(prefFontStyle.contains("Bold")) {
				spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, spanString.length(), 0);
			}
			if(prefFontStyle.contains("Italic")) {
				spanString.setSpan(new StyleSpan(Typeface.ITALIC), 0, spanString.length(), 0);
			}
			
			setText(spanString);
			setTextSize(TypedValue.COMPLEX_UNIT_SP, prefFontSize);
			refreshColor();
			
			update();
			
			super.handleMessage(msg);
		}
	};

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();

		if (!mAttached) {
			mAttached = true;
			IntentFilter filter = new IntentFilter();
			filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
			filter.addAction(Common.ACTION_SETTINGS_CHANGED);
			getContext().registerReceiver(mIntentReceiver, filter, null, getHandler());
		}
		updateViewVisibility();
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if (mAttached) {
			getContext().unregisterReceiver(mIntentReceiver);
			mAttached = false;
		}
	}

	private void updateConnectionInfo() {
		Log.i(TAG, "updateConnectionInfo");
		ConnectivityManager connectivityManager = (ConnectivityManager) getContext().getSystemService(
				Context.CONNECTIVITY_SERVICE);

		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
		
		if (networkInfo != null) {
			networkState = networkInfo.isConnected();
			networkType = networkInfo.getTypeName().toUpperCase(Locale.ENGLISH);
			Log.i(TAG, "networkType = " + networkType);
		}else{
		    networkState = false;
		}
		else {
			networkState = false;
		}
	}

	public void updateTraffic() {
		lastUpdateTime = SystemClock.elapsedRealtime();
		totalRxBytes = TrafficStats.getTotalRxBytes();
		mTrafficHandler.sendEmptyMessage(0);
	}

	private String createText() {
		String uploadUnit, downloadUnit;
		float uploadValue, downloadValue;
		
		
		
		switch (prefForceUnit) {
		default:
		case 0:
		    
		    if (((float) uploadSpeed) / 1048576 >= 1) { // 1024 * 1024 113
                uploadValue = ((float) uploadSpeed) / 1048576f;
                uploadUnit = "M";
                uploadDecimalFormat = formatWithDecimal;
            } else if (((float) uploadSpeed) / 1024f >= 1) {
                uploadValue = ((float) uploadSpeed) / 1024f;
                uploadUnit = "K";
                uploadDecimalFormat = formatWithoutDecimal;
            } else {
                uploadValue = uploadSpeed;
                uploadUnit = "";
                uploadDecimalFormat = formatWithoutDecimal;
            }
		    
			if (((float) downloadSpeed) / 1048576 >= 1) { // 1024 * 1024 113
				downloadValue = ((float) downloadSpeed) / 1048576f;
				downloadUnit = "M";
				downloadDecimalFormat = formatWithDecimal;
			} else if (((float) downloadSpeed) / 1024f >= 1) {
				downloadValue = ((float) downloadSpeed) / 1024f;
				downloadUnit = "K";
				downloadDecimalFormat = formatWithoutDecimal;
			} else {
				downloadValue = downloadSpeed;
				downloadUnit = "";
				downloadDecimalFormat = formatWithoutDecimal;
			}
			break;
		case 1:
			downloadValue = downloadSpeed;
			uploadValue = uploadSpeed;
			uploadUnit = downloadUnit = "";
			uploadDecimalFormat = downloadDecimalFormat = formatWithoutDecimal;
			break;
		case 2:
			downloadValue = ((float) downloadSpeed) / 1024f;
			uploadValue = ((float) uploadSpeed) / 1024f;
			uploadUnit = downloadUnit = "K";
			uploadDecimalFormat = downloadDecimalFormat = formatWithoutDecimal;
			break;
		case 3:
			downloadValue = ((float) downloadSpeed) / 1048576f;
			uploadValue = ((float) uploadSpeed) / 1048576f;
			uploadUnit = downloadUnit = "M";
			uploadDecimalFormat = downloadDecimalFormat = formatWithDecimal;
			break;
		}
		
		
		String strUploadValue, strDownloadValue;
		
		if (prefHideInactive && uploadValue <= 0) {
            strUploadValue = "";
        }
        else {
            strUploadValue = uploadDecimalFormat.format(uploadValue);
        }
		if (prefHideInactive && downloadValue <= 0) {
		    strDownloadValue = "";
        }
		else {
		    strDownloadValue = downloadDecimalFormat.format(downloadValue);
		}
		
		switch(prefSuffix) {
		case 0:
		    uploadSuffix = downloadSuffix = " ";
		    break;
		case 1:
		    uploadSuffix = Common.SMALL_UP_TRIANGLE;
		    downloadSuffix = Common.SMALL_DOWN_TRIANGLE;
		    break;
		case 2:
		    uploadSuffix = Common.SMALL_UP_HOLLOW_TRIANGLE;
		    downloadSuffix = Common.SMALL_DOWN_HOLLOW_TRIANGLE;
		    break;
		}
		
		if(strUploadValue.length() > 0) {
		    if(!prefHideUnit) {
		        strUploadValue += formatUnit(uploadUnit);
		    }
		    strUploadValue += uploadSuffix;
		}
		else if (prefShowSuffix) {
			strUploadValue += uploadSuffix;
		}
		
		if(strDownloadValue.length() > 0) {
		    if(!prefHideUnit) {
		        strDownloadValue += formatUnit(downloadUnit);
		    }
		    strDownloadValue += downloadSuffix;
		}
		else if (prefShowSuffix) {
			strDownloadValue += downloadSuffix;
		}
		
		boolean showInExactPosition = (prefShowUploadSpeed && prefShowDownloadSpeed);
		
		if(!prefShowUploadSpeed) {
		    strUploadValue = "";
		}
		if(!prefShowDownloadSpeed) {
		    strDownloadValue = "";
		}
		
		String delimeter = "";
		if(prefDisplay == 0) {
		    delimeter = "\n";
		}
		else {
		    delimeter = " ";
		    showInExactPosition = false; //irrelevant in one-line mode
		}
		
		String ret = "";
		if((showInExactPosition) || (strUploadValue.length() > 0 && strDownloadValue.length() > 0)) {
		    ret = strUploadValue + delimeter + strDownloadValue; 
		}
		else {
		    ret = strUploadValue + strDownloadValue;
		}
		return ret;
	}
	
	private String formatUnit(String unit) {
		if (!prefHideB) {
			unit += "B";
		}
		if ((!prefNoSpace) && (unit.length() > 0)) {
			unit = " " + unit;
		}
		return unit;
	}

	public void update() {
		mTrafficHandler.removeCallbacks(mRunnable);
		mTrafficHandler.postDelayed(mRunnable, prefUpdateInterval);
	}

	Runnable mRunnable = new Runnable() {
		@Override
		public void run() {
			mTrafficHandler.sendEmptyMessage(0);
		}
	};
	
	private boolean isCorrectNetworkType() {
	    if(prefNetworkType == null || prefNetworkType.equals("both")) {
	        return true;
	    }
	    else {
	        return prefNetworkType.contains(networkType);
	    }
	}
	
	private void updateViewVisibility() {
		if (networkState && isCorrectNetworkType()) {
			if (mAttached) {
				updateTraffic();
			}
			setVisibility(View.VISIBLE);
		} else {
			setVisibility(View.GONE);
		}
		
	}

	private void loadPreferences() {
		mPref = new XSharedPreferences(Common.PKG_NAME);
		prefForceUnit = Common.getPrefInt(mPref, Common.KEY_FORCE_UNIT, Common.DEF_FORCE_UNIT);
		prefShowUploadSpeed = mPref.getBoolean(Common.KEY_SHOW_UPLOAD_SPEED, Common.DEF_SHOW_UPLOAD_SPEED);
		prefShowDownloadSpeed = mPref.getBoolean(Common.KEY_SHOW_DOWNLOAD_SPEED, Common.DEF_SHOW_DOWNLOAD_SPEED);
		prefHideUnit = mPref.getBoolean(Common.KEY_HIDE_UNIT, Common.DEF_HIDE_UNIT);
		prefNoSpace = mPref.getBoolean(Common.KEY_NO_SPACE, Common.DEF_NO_SPACE);
		prefHideB = mPref.getBoolean(Common.KEY_HIDE_B, Common.DEF_HIDE_B);
		prefHideInactive = mPref.getBoolean(Common.KEY_HIDE_INACTIVE, Common.DEF_HIDE_INACTIVE);
		prefShowSuffix = mPref.getBoolean(Common.KEY_SHOW_SUFFIX, Common.DEF_SHOW_SUFFIX);
		prefFontSize = Common.getPrefInt(mPref, Common.KEY_FONT_SIZE, Common.DEF_FONT_SIZE);
		prefPosition = Common.getPrefInt(mPref, Common.KEY_POSITION, Common.DEF_POSITION);
		prefSuffix = Common.getPrefInt(mPref, Common.KEY_SUFFIX, Common.DEF_SUFFIX);
		prefNetworkType = mPref.getString(Common.KEY_NETWORK_TYPE, Common.DEF_NETWORK_TYPE);
		prefDisplay = Common.getPrefInt(mPref, Common.KEY_DISPLAY, Common.DEF_DISPLAY);
		prefUpdateInterval = Common.getPrefInt(mPref, Common.KEY_UPDATE_INTERVAL, Common.DEF_UPDATE_INTERVAL);
		prefColorMode = Common.getPrefInt(mPref, Common.KEY_COLOR_MODE	, Common.DEF_COLOR_MODE);
		prefColor = mPref.getInt(Common.KEY_COLOR, Common.DEF_COLOR);
		prefFontStyle = mPref.getStringSet(Common.KEY_FONT_STYLE, Common.DEF_FONT_STYLE);
	}
}
