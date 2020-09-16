package com.example.myapplication1;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.gson.Gson;
import com.smarteye.adapter.BVCU_CLIENT_TYPE;
import com.smarteye.adapter.BVCU_CmdMsgContent;
import com.smarteye.adapter.BVCU_Command;
import com.smarteye.adapter.BVCU_DialogInfo;
import com.smarteye.adapter.BVCU_DialogParam;
import com.smarteye.adapter.BVCU_EVENT_DIALOG;
import com.smarteye.adapter.BVCU_EntityInfo;
import com.smarteye.adapter.BVCU_EventCode;
import com.smarteye.adapter.BVCU_Event_DialogCmd;
import com.smarteye.adapter.BVCU_File_TransferInfos;
import com.smarteye.adapter.BVCU_MediaDir;
import com.smarteye.adapter.BVCU_Method;
import com.smarteye.adapter.BVCU_NRUCFG_ManualRecord;
import com.smarteye.adapter.BVCU_Packet;
import com.smarteye.adapter.BVCU_Result;
import com.smarteye.adapter.BVCU_ServerParam;
import com.smarteye.adapter.BVCU_SessionInfo;
import com.smarteye.adapter.BVCU_SessionParam;
import com.smarteye.adapter.BVCU_SubDev;
import com.smarteye.adapter.BVCU_SubMethod;
import com.smarteye.adapter.BVPU_MediaDir;
import com.smarteye.adapter.BVPU_ServerParam;
import com.smarteye.adapter.BVPU_VideoControl_Encode;
import com.smarteye.adapter.SAVCodec_ID;
import com.smarteye.bean.JNIMessage;
import com.smarteye.coresdk.CoreSDK;
import com.smarteye.sdk.BVAuth_EventCallback;
import com.smarteye.sdk.BVCU;
import com.smarteye.sdk.BVCU_EventCallback;
import com.smarteye.sdk.IAuth;
import com.smarteye.sdk.bean.BVAuth_Request;
import com.smarteye.sdk.bean.BVAuth_Response;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.smarteye.adapter.BVCU_MediaDir.BVCU_MEDIADIR_AUDIOSEND;
import static com.smarteye.adapter.BVCU_MediaDir.BVCU_MEDIADIR_VIDEOSEND;

/**
 * 注意：如果对DEMO中使用到的一些java类，错误码等信息不理解
 * 请参考：http://up.besovideo.com:7780/android_sdk_bvcu_api/index.html
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
	private static final String TAG = "MainActivity";
	private static final int FILE_REQUEST_CODE = 1000;
	private static final int DOWNLOAD_FILE_FLAG = 0;
	private static final int UPLOAD_FILE_FLAG = 1;
	private static final int MESSAGE_LOGIN_SUCCESS = 100;
	private static final int MESSAGE_LOGIN_FAILED = 101;
	private static final int MESSAGE_TRANSFER_STATUS = 104;
	private static final int MESSAGE_TRANSFER_NOTHING = 105;
	private static final int MESSAGE_UPLOAD_FILE_SUCCESS = 106;
	private static final int MESSAGE_UPLOAD_FILE_FAIL = 107;
	private static final int MESSAGE_DOWNLOAD_FILE_SUCCESS = 108;
	private static final int MESSAGE_DOWNLOAD_FILE_FAIL = 109;
	private static final int MESSAGE_UPDATE_UPLOAD_FILE_PROGRESS = 110;
	private static final int MESSAGE_SHOW_TOAST_INFO = 112;        // 显示Toast信息
	private static final int MESSAGE_UPDATE_BUTTON_TEXT_DELAY = 120; // 录像时间结束重置按钮状态
	private static final String RECORDER_DIR_NAME = "Recorder";
	private static final String RECORDER_FILE_PATH = Environment.getExternalStorageDirectory() + "/" + RECORDER_DIR_NAME;
	private SurfaceView mSurfaceView;
	private int DEFAULT_WIDTH = 640;
	private int DEFAULT_HEIGHT = 480;
	private Camera mCamera;
	private int cameraIndex = 0;
	private SurfaceHolder mSurfaceHolder;
	private RecorderUtils recorderUtils;
	private LocationTools locationTools;
	private int width;
	private int height;
	private boolean sendVideoData = false;
	private Button mLoginButton, videoPreviewBtn, mUploadFileBtn, startRecordBtn;
	private LinearLayout functionLayout;
	private EditText recordTimeEdit;
	private TextView mTvTransportChannel, transferPercent, versionName;
	private boolean authFlag = false;
	private boolean isLogin = false;
	private boolean isRecord = false;
	private String mLocalFilePath;
	private String mRemoteFileName;
	private Dialog loginDialog;
	private String ipStr, portStr, usernameStr, passwordStr;
	SharedTools sharedTools;
	public static MainActivity instance;
	private int tempAvDir;
	private static final String SERIAL_NUMBER_VALUE = "auth.serialnumber";
	private static final String INNER_INFO_VALUE = "inner.info";
	private Handler mHandler = new Handler(Looper.getMainLooper()) {
		@Override
		public void handleMessage(@NonNull Message msg) {
			switch (msg.what) {
				case MESSAGE_LOGIN_SUCCESS:
					if (loginDialog != null) {
						loginDialog.cancel();
						loginDialog = null;
					}
					mLoginButton.setText(getString(R.string.logout_text));
					showToast(R.string.login_success);
					break;
				case MESSAGE_LOGIN_FAILED:
					mLoginButton.setText(getString(R.string.login_text));
					mTvTransportChannel.setText(R.string.current_transfer_text);
					break;
				case MESSAGE_TRANSFER_STATUS:
					mTvTransportChannel.setText(getString(R.string.current_transfer_text) + (String)msg.obj);
					break;
				case MESSAGE_TRANSFER_NOTHING:
					mTvTransportChannel.setText(R.string.current_transfer_text);
					break;
				case MESSAGE_DOWNLOAD_FILE_SUCCESS:
					showToast(R.string.download_success_tip);
					transferPercent.setText(getString(R.string.transferPercent) + "100%");
					break;
				case MESSAGE_DOWNLOAD_FILE_FAIL:
					showToast(R.string.download_fail_tip);
					break;
				case MESSAGE_UPLOAD_FILE_SUCCESS:
					mLocalFilePath = null;
					showToast(R.string.upload_success_tip);
					transferPercent.setText(getString(R.string.transferPercent) + "100%");
					break;
				case MESSAGE_UPLOAD_FILE_FAIL:
					showToast(R.string.upload_fail_tip);
					break;
				case MESSAGE_UPDATE_UPLOAD_FILE_PROGRESS:
					int progress = msg.arg1;
					Log.d(TAG, "progress=" + progress);
					transferPercent.setText(getString(R.string.transferPercent) + progress + "%");
					break;
				case MESSAGE_UPDATE_BUTTON_TEXT_DELAY:
					isRecord = false;
					startRecordBtn.setText(R.string.StartRecord);
					break;
				case MESSAGE_SHOW_TOAST_INFO:
					Toast.makeText(MainActivity.this, (String)msg.obj, Toast.LENGTH_SHORT).show();
					break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE,
				Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE,
				Manifest.permission.ACCESS_FINE_LOCATION}, 5);
		instance = this;
		sharedTools = new SharedTools(this);
		getSupportActionBar().hide();
		BVCU.getSDK().init(getApplicationContext());
		doAuth();
		BVCU.getSDK().setEventCallback(bvcuEventCallback); // 设置各种消息回调，不要多次设置
		setContentView(R.layout.activity_main);
		mSurfaceView = findViewById(R.id.surfaceView);
		mLoginButton = (Button) findViewById(R.id.login_button);
		videoPreviewBtn = (Button) findViewById(R.id.video_preview_button);
		mUploadFileBtn = (Button) findViewById(R.id.upload_file_button);
		startRecordBtn = (Button) findViewById(R.id.start_record_button);
		recordTimeEdit = findViewById(R.id.record_time_edit_id);
		mTvTransportChannel = (TextView) findViewById(R.id.tv_transport_channel);
		functionLayout = (LinearLayout) findViewById(R.id.function_layout_id);
		transferPercent = (TextView) findViewById(R.id.file_transport_percent);
		versionName = findViewById(R.id.version_id);
		versionName.setText("版本：" + getVersionName());
		mTvTransportChannel.setText(R.string.current_transfer_text);
		mLoginButton.setOnClickListener(this);
		videoPreviewBtn.setOnClickListener(this);
		mUploadFileBtn.setOnClickListener(this);
		startRecordBtn.setOnClickListener(this);
		mSurfaceHolder = mSurfaceView.getHolder();
		mSurfaceHolder.setKeepScreenOn(true);
		mSurfaceHolder.addCallback(surfaceHolderCallback);
		recorderUtils = new RecorderUtils();
		locationTools = new LocationTools(getApplicationContext());
//		registerLight();//需要光线传感器的自行注册(打开注释)
	}

	/**
	 * 注意：如果对DEMO中使用到的一些java类，错误码等信息不理解
	 * 请参考：http://up.besovideo.com:7780/android_sdk_bvcu_api/index.html
	 */
	private void doAuth() {
		BVAuth_Request bvAuth_request = new BVAuth_Request();
		bvAuth_request.setSzDeveloperAppID(Constant.APP_ID);
		bvAuth_request.setSzAppType(Constant.TYPE_MCP);
		bvAuth_request.setSzDeveloperRsaE(Constant.RSAE);
		bvAuth_request.setSzDeveloperRsaN(Constant.RSAN);
		String serial_num = sharedTools.getShareString(SERIAL_NUMBER_VALUE, "");
		bvAuth_request.setSzSerialNumber(serial_num);
		bvAuth_request.setSzAppDeviceID(getClientID().substring(3));
		String innerInfo = sharedTools.getShareString(INNER_INFO_VALUE, "");
		bvAuth_request.setSzInnerInfo(innerInfo);
		bvAuth_request.setUserLabel(Constant.USER_LABEL);
		bvAuth_request.setSzHardwareSN(Build.FINGERPRINT);// TODO
		BVCU.getAuth().setAuthEventCallback(bvAuthEventCallback);
		int result = BVCU.getAuth().auth(getApplicationContext(), bvAuth_request);
		Log.d(TAG, "认证方法 result ：" + result);
	}

	private BVAuth_EventCallback bvAuthEventCallback = new BVAuth_EventCallback() {
		@Override
		public void OnEvent(int iAuthResult, BVAuth_Response bvAuth_response) {
			Log.d(TAG, "AuthEventCallback------iAuthResult=" + iAuthResult +
					",authKeyStatus=" + bvAuth_response.getAuthKeyStatus() +
					",iCertType=" + bvAuth_response.getCertType() +
					",iToken=" + bvAuth_response.getToken() +
					",szSerialNumber=" + bvAuth_response.getSerialNumber() +
					",iAuthAvailableKeyCount=" + bvAuth_response.getAuthAvailableKeyCount());
			if (iAuthResult == IAuth.AUTH_Result_OK) {
				authFlag = true;
				showToastByHandler(getString(R.string.authentication_success));
				sharedTools.setShareString(SERIAL_NUMBER_VALUE, bvAuth_response.getSerialNumber());
			} else {
				authFlag = false;
				showToastByHandler(getString(R.string.authentication_failed) + " : " + iAuthResult
						+ "，认证ID为 : " + bvAuth_response.getToken() + "" + ",认证设备需联系相关商务人员");
			}
		}

		@Override
		public void OnUpdateInfo(String s) {
			Log.d(TAG, "AuthEventCallback------s=" + s);
			if (!TextUtils.isEmpty(s)) {
				sharedTools.setShareString(INNER_INFO_VALUE, s);
			}
		}
	};

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.login_button:
				if (authFlag) {
					showLoginDialog();
				} else {
					showLoginDialog();
					showToast(R.string.authentication_failed_tips);
				}
				break;
			case R.id.video_preview_button:
				if (isLogin) {
					Intent intent = new Intent(this, VideoPreviewActivity.class);
					intent.putExtra("ip", ipStr);
					startActivity(intent);
				} else {
					showToast(R.string.login_tips);
				}
				break;
			case R.id.upload_file_button:
				if (isLogin) {
					Intent fileIntent = new Intent(Intent.ACTION_GET_CONTENT);
					fileIntent.setType("*/*");
					fileIntent.addCategory(Intent.CATEGORY_OPENABLE);
					startActivityForResult(fileIntent, FILE_REQUEST_CODE);
				} else {
					showToast(R.string.login_tips);
				}
				break;
			case R.id.start_record_button:
				if (isLogin) {
					if (isRecord) {
						// 手动停止录像，移除倒计时
						mHandler.removeMessages(MESSAGE_UPDATE_BUTTON_TEXT_DELAY);
						isRecord = false;
						controlRecord(RECORD_FLAG_STOP, 0);
						startRecordBtn.setText(R.string.StartRecord);
					} else {
						isRecord = true;
						int recordLength = 2 * 60; // 默认录制两分钟
						if (!TextUtils.isEmpty(recordTimeEdit.getText()) && Integer.parseInt(recordTimeEdit.getText().toString()) >= 2) {
							recordLength = Integer.parseInt(recordTimeEdit.getText().toString()) * 60;
						} else {
							recordTimeEdit.setText("2");
						}
						controlRecord(RECORD_FLAG_START, recordLength);
						// 录像结束后重置按钮状态（在设定的录像时间基础上再加20s误差时间）
						mHandler.sendEmptyMessageDelayed(MESSAGE_UPDATE_BUTTON_TEXT_DELAY, recordLength * 1000 + 20 * 1000);
						startRecordBtn.setText(R.string.StopRecord);
					}
					// 此处限制频繁操作(1、服务器接受命令，启停录像需要时间 2、服务器为限制文件过小，至少录制5s+时长的录像)
					startRecordBtn.setClickable(false);
					startBtnTimer(startRecordBtn.getText().toString());
				} else {
					showToast(R.string.login_tips);
				}
				break;
		}
	}

	/**
	 * 登录Smarteye Server
	 *
	 * @param ip
	 * @param port
	 * @param username
	 * @param password
	 */
	private void login(String ip, int port, String username, String password) {
		BVCU_SessionParam param = new BVCU_SessionParam();
		BVCU_ServerParam serverParam = new BVCU_ServerParam();
		serverParam.szClientID = getClientID();
		serverParam.iCmdProtoType = 1;
		serverParam.szServerAddr = ip;
		serverParam.iServerPort = port;
		serverParam.szUserName = username;
		serverParam.szPassword = password;
		serverParam.szUserAgent = Constant.USER_AGENT;
		BVPU_ServerParam bvpuServerParam = new BVPU_ServerParam();
		if (serverParam.szClientID.contains("UA")) {
			param.iClientType = BVCU_CLIENT_TYPE.BVCU_CLIENT_TYPE_UA;
		} else if (serverParam.szClientID.contains("CU")) {
			param.iClientType = BVCU_CLIENT_TYPE.BVCU_CLIENT_TYPE_CU;
		} else if (serverParam.szClientID.contains("PU")) {
			param.iClientType = BVCU_CLIENT_TYPE.BVCU_CLIENT_TYPE_PU;
		}
		param.iCmdProtoType = serverParam.iCmdProtoType;
		param.iMaxChannelOpenCount = 0;
		param.iServerPort = serverParam.iServerPort;
		param.iTimeOut = 30 * 1000;
		param.szClientID = serverParam.szClientID;
		param.szPassword = serverParam.szPassword;
		param.szServerAddr = serverParam.szServerAddr;
		param.szUserAgent = serverParam.szUserAgent;
		param.szUserName = serverParam.szUserName;
		bvpuServerParam.szDeviceName = "SDK测试" + Build.MODEL;
		bvpuServerParam.iMediaDir ^= BVCU_MediaDir.BVCU_MEDIADIR_VIDEOSEND;
		bvpuServerParam.iMediaDir ^= BVCU_MediaDir.BVCU_MEDIADIR_TALKONLY;
		if (serverParam.szClientID.contains("UA")) {
			param.iClientType = BVCU_CLIENT_TYPE.BVCU_CLIENT_TYPE_UA;
			param.stEntityInfo = new BVCU_EntityInfo();
			PUDeviceInfo.initPUEntityInfo(param.stEntityInfo, bvpuServerParam);
		}
		int loginResult = BVCU.getSDK().login(param);
		Log.d(TAG, "登录方法 loginResult ：" + loginResult);
	}

	private Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
		@Override
		public void onPreviewFrame(byte[] bytes, Camera camera) {
			if (sendVideoData) {
				BVCU.getData().inputVideoData(bytes, bytes.length,
						System.currentTimeMillis() * 1000, width, height);
			}
		}
	};

	private SurfaceHolder.Callback surfaceHolderCallback = new SurfaceHolder.Callback() {
		@Override
		public void surfaceCreated(SurfaceHolder surfaceHolder) {
			Log.d(TAG, "surfaceCreated------");
			int number = Camera.getNumberOfCameras();
			if (cameraIndex > number || cameraIndex < 0)
				return;
			if (mCamera == null) {
				mCamera = Camera.open(cameraIndex);
			}
			Camera.Parameters parameters = mCamera.getParameters();
			parameters.setPreviewFormat(ImageFormat.NV21);
			setPreviewSize(parameters, DEFAULT_WIDTH, DEFAULT_HEIGHT);
			setPictureSize(parameters, DEFAULT_WIDTH, DEFAULT_HEIGHT);
			if (MainActivity.this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
				parameters.set("orientation", "portrait");
				mCamera.setDisplayOrientation(90);
			} else {
				parameters.set("orientation", "landscape");
				mCamera.setDisplayOrientation(0);
			}
			mCamera.setParameters(parameters);
			mCamera.setPreviewCallback(previewCallback);
			try {
				mCamera.setPreviewDisplay(mSurfaceHolder);
				mCamera.startPreview();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
			Log.d(TAG, "surfaceChanged------");
			if (mSurfaceHolder.getSurface() == null) {
				return;
			}
			try {
				mCamera.stopPreview();
			} catch (Exception e) {
				e.getMessage();
			}
			try {
				mCamera.setPreviewCallback(previewCallback);
				mCamera.setPreviewDisplay(mSurfaceHolder);
				mCamera.startPreview();
			} catch (Exception e) {
				e.getMessage();
			}
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
			Log.d(TAG, "surfaceDestroyed------");
			releaseCamera();
		}
	};

	String getRandomID() {
		ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
		int i3 = threadLocalRandom.nextInt(10000, 99999);
		return String.valueOf(i3);
	}

	/**
	 * 仅持久保存CU_ID的后面数字部分，前面CU_/UA_可根据需求修改
	 * @return
	 */
	String getClientID() {
		SharedPreferences sp = getSharedPreferences("config", MODE_PRIVATE);
		String ID = sp.getString("client_id", null);
		if (ID == null) {
			ID = getRandomID();
			SharedPreferences.Editor editor = sp.edit();//获得sp编辑器
			editor.putString("client_id", ID);
			editor.commit();//类似于数据库的事务，保证数据同时提交
		}
		return "UA_" + ID;
	}

	private BVCU_EventCallback bvcuEventCallback = new BVCU_EventCallback() {
		/*该Session相关的事件。函数BVCU_GetSessionInfo可以用来获得BVCU_ServerParam参数。
			iEventCode:事件码，参见Session事件
			iResult: 错误码*/
		@Override
		public void OnSessionEvent(int hSession, int iEventCode, int iResult, BVCU_SessionInfo bvcu_sessionInfo) {
			Log.d(TAG, "hSession=" + hSession + ",iEventCode=" + iEventCode + ",iResult=" + iResult);
			if (myBvcuEventCallback != null) {
				myBvcuEventCallback.OnSessionEvent(hSession, iEventCode, iResult, bvcu_sessionInfo);
			}
			if (iEventCode == BVCU_EventCode.BVCU_EVENT_SESSION_OPEN) {
				if (iResult == BVCU_Result.BVCU_RESULT_S_OK) {
					mHandler.sendEmptyMessage(MESSAGE_LOGIN_SUCCESS);
					isLogin = true;
					showToastByHandler("登录成功");
				} else {
					mHandler.sendEmptyMessage(MESSAGE_LOGIN_FAILED);
					isLogin = false;
					showToastByHandler("登录失败 ：" + iResult);
				}
			} else if (iEventCode == BVCU_EventCode.BVCU_EVENT_SESSION_CLOSE){
				if (iResult == BVCU_Result.BVCU_RESULT_S_OK) {
					showToastByHandler("注销成功");
				} else {
					showToastByHandler("注销失败");
				}
			}
			if (bvcu_sessionInfo != null) {
				Log.d(TAG, "szDomain=" + bvcu_sessionInfo.szDomain +
						",szLocalIP=" + bvcu_sessionInfo.szLocalIP +
						",szServerID=" + bvcu_sessionInfo.szServerID +
						",szServerName=" + bvcu_sessionInfo.szServerName +
						",szServerVersion=" + bvcu_sessionInfo.szServerVersion +
						",iApplierID=" + bvcu_sessionInfo.iApplierID +
						",iLocalPort=" + bvcu_sessionInfo.iLocalPort +
						",iLoginTime=" + bvcu_sessionInfo.iLoginTime +
						",iOnlineStatus=" + bvcu_sessionInfo.iOnlineStatus +
						",iReserved=" + bvcu_sessionInfo.iReserved);
			}
		}

		/*收到的Control/Query/Notify命令
		 pCommand：库内部的一个BVCU_Command对象指针。应用程序完成命令处理后，应调用SDK中的responseCmd来回复，Notify命令不需要回复
				   返回：BVCU_RESULT_S_OK表示应用程序要处理本命令，其他值表示应用程序忽略该命令，由库决定如何处理。
	   */
		@Override
		public int OnSessionCommand(int hCmd, BVCU_Command bvcu_command) {
			Log.d(TAG, "OnSessionCommand " + bvcu_command.iSubMethod);
			if (myBvcuEventCallback != null) {
				myBvcuEventCallback.OnSessionCommand(hCmd, bvcu_command);
			}
			return 0;
		}

		@Override
		public void OnPasvDialogEvent(int hDialog, int iEventCode, BVCU_Event_DialogCmd pParam) {
			Log.d(TAG, "被动 OnPasvDialogEvent hDialog ：" + hDialog + " iEventCode : " +iEventCode + " pParam : " + new Gson().toJson(pParam));
			if (myBvcuEventCallback != null) {
				myBvcuEventCallback.OnPasvDialogEvent(hDialog, iEventCode, pParam);
			}
			int channelIndex = pParam.pDialogParam.pTarget[0].iIndexMajor;
			int dir = pParam.pDialogParam.iAVStreamDir;
			Log.d(TAG, "OnPasvDialogEvent------hDialog=" + hDialog + ",iEventCode=" + iEventCode + ",channelIndex=" + channelIndex + ",dir=" + dir);
			switch (iEventCode) {
				case BVCU_EVENT_DIALOG.BVCU_EVENT_DIALOG_OPEN:
					Log.d(TAG, "DIALOG_OPEN命令");
					if (pParam.iResult == BVCU_Result.BVCU_RESULT_S_PENDING) {
					}
					break;
				case BVCU_EVENT_DIALOG.BVCU_EVENT_DIALOG_CLOSE:
					Log.d(TAG, "DIALOG_CLOSE命令");
					if (channelIndex == BVCU_SubDev.BVCU_SUBDEV_INDEXMAJOR_MIN_CHANNEL) {
						tempAvDir = 0;
						if (dir == 0) {
							sendVideoData = false;
							if (recorderUtils.isRecording()) {
								recorderUtils.stopRecorder();
							}
							mHandler.sendEmptyMessage(MESSAGE_TRANSFER_NOTHING);
						}
					}
					if (channelIndex == BVCU_SubDev.BVCU_SUBDEV_INDEXMAJOR_MIN_GPS && dir == 0) {
						locationTools.stopLocation();
					}
					break;
			}
		}

		@Override
		public int OnPasvDialogCmd(int hDialog, int iEventCode, BVCU_DialogParam pParam) {
			Log.d(TAG, "被动 OnPasvDialogCmd hDialog ：" + hDialog + " iEventCode : " + iEventCode + " pParam : " + new Gson().toJson(pParam));
			if (myBvcuEventCallback != null) {
				myBvcuEventCallback.OnPasvDialogCmd(hDialog, iEventCode, pParam);
			}
			int channelIndex = pParam.pTarget[0].iIndexMajor;
			int avDir = pParam.iAVStreamDir;
			Log.d(TAG, "OnPasvDialogCmd ------channelIndex=" + channelIndex + ",avDir=" + avDir);

			if (channelIndex >= BVCU_SubDev.BVCU_SUBDEV_INDEXMAJOR_MIN_CHANNEL && channelIndex <= BVCU_SubDev.BVCU_SUBDEV_INDEXMAJOR_MAX_CHANNEL) {
				Message message = Message.obtain();
				message.what = MESSAGE_TRANSFER_STATUS;
				message.obj = getTransferState(avDir);
				mHandler.sendMessage(message);
				/*****************************************************/
				if ((avDir & BVCU_MEDIADIR_VIDEOSEND) == BVCU_MEDIADIR_VIDEOSEND &&
						(tempAvDir & BVCU_MEDIADIR_VIDEOSEND) != BVCU_MEDIADIR_VIDEOSEND) {
					sendVideoData = true;
				} else if ((tempAvDir & BVCU_MEDIADIR_VIDEOSEND) == BVCU_MEDIADIR_VIDEOSEND
						&& (avDir & BVCU_MEDIADIR_VIDEOSEND) != BVCU_MEDIADIR_VIDEOSEND) {
					sendVideoData = false;
				}

				if ((avDir & BVCU_MEDIADIR_AUDIOSEND) == BVCU_MEDIADIR_AUDIOSEND &&
						(tempAvDir & BVCU_MEDIADIR_AUDIOSEND) != BVCU_MEDIADIR_AUDIOSEND) {
					if (!recorderUtils.isRecording()) {
						recorderUtils.startRecorder();
					}
				} else if ((avDir & BVCU_MEDIADIR_AUDIOSEND) != BVCU_MEDIADIR_AUDIOSEND &&
						(tempAvDir & BVCU_MEDIADIR_AUDIOSEND) == BVCU_MEDIADIR_AUDIOSEND) {
					if (recorderUtils.isRecording()) {
						recorderUtils.stopRecorder();
					}
				}
				/*****************************************************/
				updateParam(pParam);
				tempAvDir = avDir;
			} else if (channelIndex >= BVCU_SubDev.BVCU_SUBDEV_INDEXMAJOR_MIN_GPS && channelIndex <= BVCU_SubDev.BVCU_SUBDEV_INDEXMAJOR_MAX_GPS) {
				if (avDir == BVCU_MediaDir.BVCU_MEDIADIR_DATASEND) {
					locationTools.startLocation();
				}
			}
			return 0;
		}

		/**
		 * 作为CU时:主动Invite的回复
		 * 作为PU时:被动Invite时,返回本地回复成功/失败后的结果(比如接受被动Invite,实际并没有打开成功);以及被close时的通知
		 * @param hDialog
		 * @param iEventCode
		 * @param pParam
		 */
		@Override
		public void OnDialogEvent(int hDialog, int iEventCode, BVCU_Event_DialogCmd pParam) {
			Log.d(TAG, "主动 OnDialogEvent  hDialog : " + hDialog + " iEventCode : " + iEventCode + " pParam : " + new Gson().toJson(pParam));
			if (myBvcuEventCallback != null) {
				myBvcuEventCallback.OnDialogEvent(hDialog, iEventCode, pParam);
			}
		}

		@Override
		public void OnGetDialogInfo(int i, BVCU_DialogInfo bvcu_dialogInfo) {
			if (myBvcuEventCallback != null) {
				myBvcuEventCallback.OnGetDialogInfo(i, bvcu_dialogInfo);
			}
		}

		/**
		 *
		 * @param hCmd 命令句柄
		 * @param pCommand    命令回复实体
		 * @param iResult    命令回复错误码
		 * @return
		 */
		@Override
		public int OnCmdEvent(int hCmd, BVCU_Command pCommand, int iResult) {
			Log.d(TAG, "OnCmdEvent");
			if (myBvcuEventCallback != null) {
				myBvcuEventCallback.OnCmdEvent(hCmd, pCommand, iResult);
			}
			switch (pCommand.iMethod) {
				case BVCU_Method.BVCU_METHOD_QUERY:
					break;
				case BVCU_Method.BVCU_METHOD_CONTROL:
					if (pCommand.iSubMethod == BVCU_SubMethod.BVCU_SUBMETHOD_NRU_MANUALRECORD) {
						if (iResult == BVCU_Result.BVCU_RESULT_S_OK) {
							showToastByHandler(getString(R.string.OperationSucceeded));
						} else {
							showToastByHandler(getString(R.string.OperationFailed));
						}
					}
					Log.d(TAG, "OnCmdEvent hCmd : " + hCmd + " iResult : " + iResult + " iSubMethod : " + pCommand.iSubMethod);
					break;
				default:
					break;
			}
			return 0;
		}

		/* 调用者不可以在回调数据中对收到的数据进行处理，可以拷贝到自己内存中处理。
		   hDialog: 数据包来源Dialog
		   pPacket：音视频数据：收到的原始媒体数据；GPS数据：BVCU_PUCFG_GPSData；纯数据：组好帧后的数据
			   返回：对纯数据无意义。对音视频数据：
		   BVCU_RESULT_S_OK：pPacket被解码显示/播放。
		   BVCU_RESULT_E_FAILED：pPacket不被解码显示/播放。
		   */
		@Override
		public int DialogAfterRecv(int hDialog, BVCU_Packet pPacket) {
			Log.d(TAG, "DialogAfterRecv");
			if (myBvcuEventCallback != null) {
				myBvcuEventCallback.DialogAfterRecv(hDialog, pPacket);
			}
			return 0;
		}
        /*
        *  文件传输回调
        *  bvcu_file_transferInfos 传输文件的数量，文件的相关信息
        *  数组中的bvcu_file_transferInfos[?] -> token 与 openFileTransfer的返回值对应,用以确定文件状态对应关系
        *  -> event 见 BVCU_EVENT_DIALOG
        *  -> result 为传输结果
                1.result >= 0 && event = BVCU_EVENT_DIALOG_CLOSE 传输结束 状态下为传输成功
                2.result < 0 一律视为失败
                3.其他状态为传输中。-> infos[?].info.iTransferBytes 为已传输大小;
                bvcu_file_transferInfos[?].info.iTransferBytes 为总文件大小
                -> bvcu_file_transferInfos[?].info.stParam.bUpload // 0:下载，1:上传.
        * */

		@Override
		public void OnFileTransferInfo(BVCU_File_TransferInfos[] bvcu_file_transferInfos) {
			Log.d(TAG, "OnFileTransferInfo------bvcu_file_transferInfos.length=" + bvcu_file_transferInfos.length);
			if (myBvcuEventCallback != null) {
				myBvcuEventCallback.OnFileTransferInfo(bvcu_file_transferInfos);
			}
			if (bvcu_file_transferInfos == null || bvcu_file_transferInfos.length == 0) {
				return;
			}

			for (BVCU_File_TransferInfos infos : bvcu_file_transferInfos) {
				Log.d(TAG, "infos.event=" + infos.event + ",infos.result=" + infos.result + ",infos.token=" + infos.token +
						",infos.info.iCreateTime=" + infos.info.iCreateTime +
						",infos.info.iOnlineTime=" + infos.info.iOnlineTime +
						",infos.info.iSpeedKBpsLongTerm=" + infos.info.iSpeedKBpsLongTerm +
						",infos.info.iSpeedKBpsShortTerm=" + infos.info.iSpeedKBpsShortTerm +
						",infos.info.iTotalBytes=" + infos.info.iTotalBytes +
						",infos.info.iTransferBytes=" + infos.info.iTransferBytes);

				int percent = (int) ((float) infos.info.iTransferBytes * 100 / infos.info.iTotalBytes);
				Message message = Message.obtain();
				message.what = MESSAGE_UPDATE_UPLOAD_FILE_PROGRESS;
				message.arg1 = percent;
				mHandler.sendMessage(message);

				if (infos.result >= 0 && infos.event == BVCU_EVENT_DIALOG.BVCU_EVENT_DIALOG_CLOSE) {
					if (infos.info.stParam.bUpload == UPLOAD_FILE_FLAG) {
						mHandler.sendEmptyMessage(MESSAGE_UPLOAD_FILE_SUCCESS);
					} else if (infos.info.stParam.bUpload == DOWNLOAD_FILE_FLAG) {
						mHandler.sendEmptyMessage(MESSAGE_DOWNLOAD_FILE_SUCCESS);
					}
				} else if (infos.result < 0) {
					if (infos.info.stParam.bUpload == UPLOAD_FILE_FLAG) {
						mHandler.sendEmptyMessage(MESSAGE_UPLOAD_FILE_FAIL);
					} else if (infos.info.stParam.bUpload == DOWNLOAD_FILE_FLAG) {
						mHandler.sendEmptyMessage(MESSAGE_DOWNLOAD_FILE_FAIL);
					}
				}
			}
		}

		@Override
		public void OnElecMapAlarm(int i) {
			if (myBvcuEventCallback != null) {
				myBvcuEventCallback.OnElecMapAlarm(i);
			}
		}

		@Override
		public void OnElecMapConfigUpdate(String s, String s1) {
			if (myBvcuEventCallback != null) {
				myBvcuEventCallback.OnElecMapConfigUpdate(s, s1);
			}
		}

		/**
		 * 处理拓展通知消息
		 * @param message
		 */
		@Override
		public void OnNotifyMessage(JNIMessage message) {
			if (myBvcuEventCallback != null) {
				myBvcuEventCallback.OnNotifyMessage(message);
			}
		}
	};

	// 用于传递回调信息
	private BVCU_EventCallback myBvcuEventCallback;

	public void setBVCU_EventCallback(BVCU_EventCallback myBvcuEventCallback) {
		this.myBvcuEventCallback = myBvcuEventCallback;
	}

	@Override
	protected void onRestart() {
		super.onRestart();
	}

	private void releaseCamera() {
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.setPreviewCallback(null);
			mCamera.release();
			mCamera = null;
		}
	}

	private void updateParam(BVCU_DialogParam pParam) {
		pParam.szMyselfVideo.codec = SAVCodec_ID.SAVCODEC_ID_H264;
		pParam.szMyselfAudio.codec = SAVCodec_ID.SAVCODEC_ID_G726;
		pParam.szMyselfAudio.iBitrate = 32000;
		pParam.szMyselfAudio.iChannelCount = 1;
		pParam.szMyselfAudio.iSampleRate = 8000;
		pParam.szTargetAudio.codec = SAVCodec_ID.SAVCODEC_ID_G726;
		pParam.szTargetAudio.iBitrate = 32000;
		pParam.szTargetAudio.iChannelCount = 1;
		pParam.szTargetAudio.iSampleRate = 8000;
		pParam.szTargetAudio.eSampleFormat = 1;//SAV_SAMPLE_FMT_S16
		pParam.stEncode = getEncode(pParam.szMyselfVideo.codec);
	}

	private BVPU_VideoControl_Encode getEncode(int codec) {
		BVPU_VideoControl_Encode encode = new BVPU_VideoControl_Encode();
		encode.iBitrate = width * height * 2;
		Log.d(TAG, "getEncode iBitrate:" + encode.iBitrate);
		encode.iFramerate = 25;
		encode.iHeight = height;
		encode.iWidth = width;
		Log.d(TAG, "getEncode width:" + width + ",height:" + height);
		if (codec == SAVCodec_ID.SAVCODEC_ID_H264) {
			encode.iEncoderType = BVPU_VideoControl_Encode.ENCODER_TYPE_H264;
		} else if (codec == SAVCodec_ID.SAVCODEC_ID_H265) {
			encode.iEncoderType = BVPU_VideoControl_Encode.ENCODER_TYPE_H265;
		}
		encode.iYUVConvert = -1;
//		encode.iEncoderType = BVPU_VideoControl_Encode.ENCODER_TYPE_NULL;
		return encode;
	}

	private String getTransferState(int avDir){
		String avDesc = "";
		if (avDir != 0) {
			if ((avDir & BVPU_MediaDir.BVPU_MEDIADIR_AUDIORECV) == BVPU_MediaDir.BVPU_MEDIADIR_AUDIORECV) {
				avDesc += "音频接收 ";
			}
			if ((avDir & BVPU_MediaDir.BVPU_MEDIADIR_VIDEORECV) == BVPU_MediaDir.BVPU_MEDIADIR_VIDEORECV) {
				avDesc += "视频接收 ";
			}
			if ((avDir & BVPU_MediaDir.BVPU_MEDIADIR_AUDIOSEND) == BVPU_MediaDir.BVPU_MEDIADIR_AUDIOSEND) {
				avDesc += "音频发送 ";
			}
			if ((avDir & BVPU_MediaDir.BVPU_MEDIADIR_VIDEOSEND) == BVPU_MediaDir.BVPU_MEDIADIR_VIDEOSEND) {
				avDesc += "视频发送";
			}
		}
		return avDesc;
	}


	public void setPreviewSize(Camera.Parameters parameters, int expectWidth, int expectHeight) {
		List<Size> previewSizes = parameters.getSupportedPreviewSizes();
		Size size = calculatePerfectSize(previewSizes,
				expectWidth, expectHeight);
		width = size.width;
		height = size.height;
		Log.d(TAG, "setPreviewSize width:" + width + ",height:" + height);
		parameters.setPreviewSize(size.width, size.height);
	}


	public void setPictureSize(Camera.Parameters parameters, int expectWidth, int expectHeight) {
		List<Size> supportPcitureSizes = parameters.getSupportedPictureSizes();
		Size size = calculatePerfectSize(supportPcitureSizes,
				expectWidth, expectHeight);
		parameters.setPictureSize(size.width, size.height);
	}

	private static void sortList(List<Size> list) {
		Collections.sort(list, new Comparator<Size>() {
			@Override
			public int compare(Size pre, Size after) {
				if (pre.width > after.width) {
					return 1;
				} else if (pre.width < after.width) {
					return -1;
				}
				return 0;
			}
		});
	}

	public Size calculatePerfectSize(List<Size> sizes, int expectWidth,
									 int expectHeight) {
		sortList(sizes); // 根据宽度进行排序
		Size result = sizes.get(0);
		boolean widthOrHeight = false; // 判断存在宽或高相等的Size
		// 辗转计算宽高最接近的值
		for (Size size : sizes) {
			// 如果宽高相等，则直接返回
			if (size.width == expectWidth && size.height == expectHeight) {
				result = size;
				break;
			}
			// 仅仅是宽度相等，计算高度最接近的size
			if (size.width == expectWidth) {
				widthOrHeight = true;
				if (Math.abs(result.height - expectHeight)
						> Math.abs(size.height - expectHeight)) {
					result = size;
				}
			}
			// 高度相等，则计算宽度最接近的Size
			else if (size.height == expectHeight) {
				widthOrHeight = true;
				if (Math.abs(result.width - expectWidth)
						> Math.abs(size.width - expectWidth)) {
					result = size;
				}
			}
			// 如果之前的查找不存在宽或高相等的情况，则计算宽度和高度都最接近的期望值的Size
			else if (!widthOrHeight) {
				if (Math.abs(result.width - expectWidth)
						> Math.abs(size.width - expectWidth)
						&& Math.abs(result.height - expectHeight)
						> Math.abs(size.height - expectHeight)) {
					result = size;
				}
			}
		}
		return result;
	}

	private void showToastByHandler(String toastInfo) {
		Message message = Message.obtain();
		message.what = MESSAGE_SHOW_TOAST_INFO;
		message.obj = toastInfo;
		mHandler.sendMessage(message);
	}

	@Override
	protected void onDestroy() {
		Log.d(TAG, "onDestroy------");
		sendVideoData = false;
		isLogin = false;
		authFlag = false;
		recorderUtils.stopRecorder();
		locationTools.stopLocation();
		BVCU.getSDK().logout();
		BVCU.getSDK().deinit();
		unregisterLight();
		super.onDestroy();
		System.exit(0);
	}

	private void showLoginDialog() {
		final View loginView = LayoutInflater.from(this).inflate(R.layout.login_dialog_layout,
				null);
		loginDialog = new Dialog(this, R.style.login_dialog_style);
		loginDialog.setContentView(loginView);
		final EditText ipEdit = loginView.findViewById(R.id.ip_edit_id);
		final EditText portEdit = loginView.findViewById(R.id.port_edit_id);
		final EditText usernameEdit = loginView.findViewById(R.id.username_edit_id);
		final EditText passwordEdit = loginView.findViewById(R.id.password_edit_id);
		if (!TextUtils.isEmpty(ipStr)
				&& !TextUtils.isEmpty(portStr)
				&& !TextUtils.isEmpty(usernameStr)
				&& !TextUtils.isEmpty(passwordStr)) {
			ipEdit.setText(ipStr);
			portEdit.setText(portStr);
			usernameEdit.setText(usernameStr);
			passwordEdit.setText(passwordStr);
		}
		final Button loginBtn = loginView.findViewById(R.id.login_btn_id);
		final Button cancelBtn = loginView.findViewById(R.id.cancel_btn_id);
		if (isLogin) {
			loginBtn.setText(getString(R.string.logout_text));
		} else {
			loginBtn.setText(getString(R.string.login_text));
		}
		loginBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (loginBtn.getText().toString().equals(getString(R.string.login_text))) {
					if (TextUtils.isEmpty(ipEdit.getText().toString())
							|| TextUtils.isEmpty(portEdit.getText().toString())
							|| TextUtils.isEmpty(usernameEdit.getText().toString())
							|| TextUtils.isEmpty(passwordEdit.getText().toString())) {
						Toast.makeText(MainActivity.this, getString(R.string.LoginParamEmpty), Toast.LENGTH_SHORT).show();
						showToast(R.string.LoginParamEmpty);
						return;
					}
					ipStr = ipEdit.getText().toString();
					portStr = portEdit.getText().toString();
					usernameStr = usernameEdit.getText().toString();
					passwordStr = passwordEdit.getText().toString();
					login(ipEdit.getText().toString(),
							Integer.parseInt(portEdit.getText().toString()),
							usernameEdit.getText().toString(),
							passwordEdit.getText().toString());
				} else {
					BVCU.getSDK().logout();
					isLogin = false;
					loginBtn.setText(getString(R.string.login_text));
				}
			}
		});
		cancelBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				loginDialog.cancel();
				loginDialog = null;
			}
		});
		loginDialog.show();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		if (resultCode == Activity.RESULT_OK && requestCode == FILE_REQUEST_CODE) {
			if (data.getData() != null) {
				Uri uri = data.getData();//得到uri
				try {
					if (GetPathFromUri4kitkat.getPath(getApplicationContext(), uri).contains("/storage/emulated/0")) {
						int index = GetPathFromUri4kitkat.getPath(getApplicationContext(), uri).indexOf("/storage/emulated/0");
						mLocalFilePath = GetPathFromUri4kitkat.getPath(getApplicationContext(), uri).substring(index);
					} else {
						mLocalFilePath = GetPathFromUri4kitkat.getPath(getApplicationContext(), uri);
					}
				} catch (Exception e) {
					if (uri.getPath().contains("/storage/emulated/0")) {
						int index = uri.getPath().indexOf("/storage/emulated/0");
						mLocalFilePath = uri.getPath().substring(index);
					} else {
						mLocalFilePath = uri.getPath().substring(5);
					}
					Log.d(TAG, "get file path error : " + e.getMessage());
				}
				if (mLocalFilePath == null || TextUtils.isEmpty(mLocalFilePath)) {
					showToast(R.string.select_file_tips);
					return;
				}
				int index = mLocalFilePath.lastIndexOf("/");
				mRemoteFileName = mLocalFilePath.substring(index + 1);
				Log.d(TAG, "RemotePath : " + "/temp/" + mRemoteFileName);
				Log.d(TAG, "LocalPath : " + mLocalFilePath);
				int uploadStatus = BVCU.getSDK().openFileTransfer(UPLOAD_FILE_FLAG, "/temp/" + mRemoteFileName, mLocalFilePath, null);
				Log.d(TAG, "uploadStatus-----=" + uploadStatus);
				if (uploadStatus < 0) {
					showToast(R.string.upload_fail_tip);
				}
			}

		}
	}

	private static final int RECORD_FLAG_START = 1; // 开始录像
	private static final int RECORD_FLAG_STOP = 0; // 停止录像

	/**
	 * 启动/停止 终端NRU录像
	 * 服务器至少录制5s+时长的录像，开始录像后不要立即手动停止录像，就算立即调用停止方法，服务器也会继续录制几秒后再结束
	 * 服务器根据设定的时间自动停止录像的时间误差以分钟计，推荐至少录制2分钟，录像时长设置为>=2的整分钟
	 * @param flag 启动、停止 标志位
	 * @return
	 */
	private int controlRecord(int flag, int length) {
		BVCU_Command command = new BVCU_Command();
		command.iMethod = BVCU_Method.BVCU_METHOD_CONTROL;
		command.iSubMethod = BVCU_SubMethod.BVCU_SUBMETHOD_NRU_MANUALRECORD;
		BVCU_NRUCFG_ManualRecord nrucfgManualRecord = new BVCU_NRUCFG_ManualRecord();
		nrucfgManualRecord.szID = "PU_" + getClientID().substring(3); // 传入PU_ID
		nrucfgManualRecord.bStart = flag; // 开始/结束标志位
		nrucfgManualRecord.iLength = length; // 录像时间：秒
		nrucfgManualRecord.iMediaDir ^= BVCU_MediaDir.BVCU_MEDIADIR_VIDEORECV;
		nrucfgManualRecord.iMediaDir ^= BVCU_MediaDir.BVCU_MEDIADIR_AUDIORECV; // 服务器的媒体方向 ：视频接收 + 音频接收
		nrucfgManualRecord.iChannelIndex = 0;
		command.stMsgContent = new BVCU_CmdMsgContent();
		command.stMsgContent.iDataCount = 1;
		command.stMsgContent.pData = nrucfgManualRecord;
		command.szTargetID = "NRU_";
		int token = BVCU.getSDK().sendCmd(command);
		return token;
	}

	private void showToast(int resId) {
		Toast.makeText(MainActivity.this, getString(resId), Toast.LENGTH_SHORT).show();
	}

	private void startBtnTimer(final String btnText) {
		CountDownTimer countDownTimer = new CountDownTimer(6 * 1000, 1000) {
			@Override
			public void onTick(long millisUntilFinished) {
				startRecordBtn.setText(btnText + "(" + (millisUntilFinished / 1000 + 1) + ")");
			}

			@Override
			public void onFinish() {
				startRecordBtn.setText(btnText);
				startRecordBtn.setClickable(true);
			}
		}.start();
	}

	/**
	 * 获取版本号
	 *
	 * @return 返回版本号
	 */
	public int getVersionCode() {
		int verCode = -1;
		try {
			verCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
		return verCode;
	}

	/**
	 * 获取版本名称
	 *
	 * @return 返回版本名称
	 */
	public String getVersionName() {
		String verName = "";
		try {
			verName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
		return verName;
	}

	private Sensor lightSensor;//光线传感器
	private static SensorManager mSensorManager;//传感器管理
	private MySensorEventListener sensorEventListener;

	/**
	 * 注册光线传感器
	 * */
	private void registerLight() {
		try {
			sensorEventListener = new MySensorEventListener();
			mSensorManager = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
			lightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);//光感
			mSensorManager.registerListener(sensorEventListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);//四种传感器模式自行了解
		} catch (Exception e) {
			Log.d(TAG, "注册光线传感器失败 ：" + e.getMessage());
		}
	}

	/**
	 * 注销光线传感器
	 * */
	private void unregisterLight() {
		try {
			mSensorManager.unregisterListener(sensorEventListener);
		} catch (Exception e) {
			e.getMessage();
		}
	}

	/**
	 * 传感器监听
	 * */
	private class MySensorEventListener implements SensorEventListener {

		@Override
		public void onSensorChanged(SensorEvent event) {
			switch (event.sensor.getType()) {
				case Sensor.TYPE_LIGHT://光感
					float value = event.values[0];//自行判断光感值的上下限值
					try {
						if (value < 10) {//光感下限值(测试值)
							openIR(true);
							chooseColorEffect(false);
						}
						if (value > 80) {//光感上限值(测试值)
							openIR(false);
							chooseColorEffect(true);
						}
					} catch (Exception e) {
						return;
					}
					break;
				default:
					break;
			}
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {

		}
	}

	/**
	 * 红外开关方法
	 * */
	public static void openIR(boolean flag) {
		//不同机型红外开关方法不同
	}

	/**
	 * 相机黑白模式
	 */
	public void chooseColorEffect(boolean none) {
		Camera.Parameters parameters = null;
		if (mCamera != null) {
			try {
				parameters = mCamera.getParameters();
			} catch (Exception e) {
				Log.d(TAG, "Exception :" + e);
				return;
			}
		} else {
			return;
		}
		if (mCamera != null && parameters != null) {
			try {
				if (none){
					parameters.setColorEffect("none");
					mCamera.setParameters(parameters);
				}else {
					parameters.setColorEffect("mono");
					mCamera.setParameters(parameters);
				}
			} catch (Exception e) {
				return;
			}
		}
	}
}
