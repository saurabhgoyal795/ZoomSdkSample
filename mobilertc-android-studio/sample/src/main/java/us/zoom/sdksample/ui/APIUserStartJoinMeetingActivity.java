package us.zoom.sdksample.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import us.zoom.sdk.MeetingError;
import us.zoom.sdk.MeetingService;
import us.zoom.sdk.MeetingServiceListener;
import us.zoom.sdk.MeetingStatus;
import us.zoom.sdk.ZoomSDK;
import us.zoom.sdksample.initsdk.AuthConstants;
import us.zoom.sdksample.R;
import us.zoom.sdksample.inmeetingfunction.customizedmeetingui.MyMeetingActivity;
import us.zoom.sdksample.inmeetingfunction.customizedmeetingui.view.MeetingWindowHelper;

import us.zoom.sdksample.startjoinmeeting.joinmeetingonly.JoinMeetingHelper;


public class APIUserStartJoinMeetingActivity extends Activity implements AuthConstants, MeetingServiceListener , View.OnClickListener{

	private final static String TAG = "ZoomSDKExample";
	
	private EditText mEdtMeetingNo;
	private EditText mEdtMeetingPassword;
	private EditText mEdtVanityId;
	private View mProgressPanel;
	private Button mBtnStartMeeting;
	private Button mBtnJoinMeeting;
	private Button mBtnSettings;
	private Button mReturnMeeting;

	private final static int STYPE = MeetingService.USER_TYPE_API_USER;
	private final static String DISPLAY_NAME = "ZoomUS SDK";

	private boolean mbPendingStartMeeting = false;
	private boolean isResumed=false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.api_user_start_join);

		mEdtMeetingNo = (EditText)findViewById(R.id.edtMeetingNo);
		mEdtVanityId = (EditText)findViewById(R.id.edtVanityUrl);
		mEdtMeetingPassword = (EditText)findViewById(R.id.edtMeetingPassword);

		mProgressPanel = (View)findViewById(R.id.progressPanel);

		mBtnStartMeeting = (Button) findViewById(R.id.btnStartMeeting);
		mBtnStartMeeting.setOnClickListener(this);

		mBtnJoinMeeting = (Button) findViewById(R.id.btnJoinMeeting);
		mBtnJoinMeeting.setOnClickListener(this);
		mBtnSettings = findViewById(R.id.btn_settings);
		mReturnMeeting = findViewById(R.id.btn_return);
		showProgressPanel(false);
		registerListener();
	}
	
	private void registerListener() {
		MeetingService meetingService = ZoomSDK.getInstance().getMeetingService();
		if(meetingService != null) {
			meetingService.addListener(this);//register meetingServiceListener
		}
	}



	@Override
	protected void onPause() {
		super.onPause();
		isResumed=false;
	}

	@Override
	protected void onResume() {
		super.onResume();
		isResumed=true;
		refreshUI();
	}

	@Override
	protected void onDestroy() {
		ZoomSDK zoomSDK = ZoomSDK.getInstance();
		if(zoomSDK.isInitialized()) {
			MeetingService meetingService = zoomSDK.getMeetingService();
			meetingService.removeListener(this);//unregister meetingServiceListener
		}

		super.onDestroy();
	}

	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.btnStartMeeting) {
		} else if(v.getId() == R.id.btnJoinMeeting) {
			onClickBtnJoinMeeting();
		}
	}


	private void showProgressPanel(boolean show) {
		if(show) {
			mBtnStartMeeting.setVisibility(View.GONE);
			mBtnJoinMeeting.setVisibility(View.GONE);
			mProgressPanel.setVisibility(View.VISIBLE);
		} else {
			mBtnStartMeeting.setVisibility(View.VISIBLE);
			mBtnJoinMeeting.setVisibility(View.VISIBLE);
			mProgressPanel.setVisibility(View.GONE);
		}
	}

	public void onClickReturnMeeting(View view) {
		MeetingWindowHelper.getInstance().hiddenMeetingWindow(true);
		Intent intent = new Intent(this, MyMeetingActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		startActivity(intent);
	}

	public void onClickSettings(View view) {
		Intent intent = new Intent(this, MeetingSettingActivity.class);
		startActivity(intent);
	}

	public void onClickBtnJoinMeeting() {
		String meetingNo = mEdtMeetingNo.getText().toString().trim();
		String meetingPassword = mEdtMeetingPassword.getText().toString().trim();

		String vanityId = mEdtVanityId.getText().toString().trim();
		
		if(meetingNo.length() == 0 && vanityId.length() == 0) {
			Toast.makeText(this, "You need to enter a meeting number/ vanity id which you want to join.", Toast.LENGTH_LONG).show();
			return;
		}

		if(meetingNo.length() != 0 && vanityId.length() !=0) {
			Toast.makeText(this, "Both meeting number and vanity id have value,  just set one of them", Toast.LENGTH_LONG).show();
			return;
		}
		
		ZoomSDK zoomSDK = ZoomSDK.getInstance();
		
		if(!zoomSDK.isInitialized()) {
			Toast.makeText(this, "ZoomSDK has not been initialized successfully", Toast.LENGTH_LONG).show();
			return;
		}
		
		MeetingService meetingService = zoomSDK.getMeetingService();

		int ret = -1;

		ret = JoinMeetingHelper.getInstance().joinMeetingWithNumber(this, meetingNo, meetingPassword);

		Log.i(TAG, "onClickBtnJoinMeeting, ret=" + ret);
	}

	
	@Override
	public void onMeetingStatusChanged(MeetingStatus meetingStatus, int errorCode,
									   int internalErrorCode) {
		Log.i(TAG, "onMeetingStatusChanged, meetingStatus=" + meetingStatus + ", errorCode=" + errorCode
				+ ", internalErrorCode=" + internalErrorCode);
		
		if(meetingStatus == MeetingStatus.MEETING_STATUS_FAILED && errorCode == MeetingError.MEETING_ERROR_CLIENT_INCOMPATIBLE) {
			Toast.makeText(this, "Version of ZoomSDK is too low!", Toast.LENGTH_LONG).show();
		}
		
		if(mbPendingStartMeeting && meetingStatus == MeetingStatus.MEETING_STATUS_IDLE) {
			mbPendingStartMeeting = false;
		}
		if (meetingStatus == MeetingStatus.MEETING_STATUS_CONNECTING) {
			showMeetingUi();
		}
		refreshUI();
	}

	private void refreshUI() {
		MeetingStatus meetingStatus = ZoomSDK.getInstance().getMeetingService().getMeetingStatus();
		if (meetingStatus == MeetingStatus.MEETING_STATUS_CONNECTING || meetingStatus == MeetingStatus.MEETING_STATUS_INMEETING
				|| meetingStatus == MeetingStatus.MEETING_STATUS_RECONNECTING) {
			mBtnSettings.setVisibility(View.GONE);
			mReturnMeeting.setVisibility(View.VISIBLE);
		} else {
			mBtnSettings.setVisibility(View.VISIBLE);
			mReturnMeeting.setVisibility(View.GONE);
		}
		if(ZoomSDK.getInstance().getMeetingSettingsHelper().isCustomizedMeetingUIEnabled())
		{
			if (meetingStatus == MeetingStatus.MEETING_STATUS_INMEETING&&isResumed) {
				MeetingWindowHelper.getInstance().showMeetingWindow(this);
			} else {
				MeetingWindowHelper.getInstance().hiddenMeetingWindow(true);
			}
		}
	}

	private void showMeetingUi() {
		if (ZoomSDK.getInstance().getMeetingSettingsHelper().isCustomizedMeetingUIEnabled()) {
			Intent intent = new Intent(this, MyMeetingActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			this.startActivity(intent);
		}
	}

}
