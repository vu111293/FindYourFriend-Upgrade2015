package com.sgu.findyourfriend.screen;

import java.util.ArrayList;
import java.util.HashMap;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.sgu.findyourfriend.R;
import com.sgu.findyourfriend.adapter.FriendSwipeAdapter;
import com.sgu.findyourfriend.mgr.FriendManager;
import com.sgu.findyourfriend.mgr.MyLocationChangeListener;
import com.sgu.findyourfriend.mgr.MyProfileManager;
import com.sgu.findyourfriend.mgr.SettingManager;
import com.sgu.findyourfriend.model.Friend;
import com.sgu.findyourfriend.model.SimpleUserAndLocation;
import com.sgu.findyourfriend.net.PostData;
import com.sgu.findyourfriend.utils.GpsPosition;
import com.sgu.findyourfriend.utils.Utility;
import com.sgu.findyourfriend.view.devsmart.android.ui.HorizontalListView;

public class MapFragment extends BaseContainerFragment implements MyLocationChangeListener {

	// point to itself
	private MapFragment mThis = this;

	private static boolean isRegister; // = false;

	private boolean isRouting;
	private boolean isHistory;

	private Context context;

	// view replace
	private static View view;
	private View inc; // include layout for horizontal friend list

	// map for app
	private GoogleMap mMap;

	private HashMap<Integer, Marker> markerManager;
	private HashMap<Integer, Marker> markerSIn; // marker Stranger and invited

	private FragmentActivity myContext;

	// view in map fragment
	private HorizontalListView avatarListView;
	private FriendSwipeAdapter swipeAdapter;

	// friend's id current
	private int fIDCurrent = -1;

	// GPS Controller
	private GpsPosition gpsPosition;

	private View viewSelected = null;

	// control button
	private Button btnMessage;
	private Button btnCall;
	private Button btnHistory;
	private Button btnUpdate;
	private Button btnRoute;
	private Button btnRequest;

	private ImageButton btnShow;
	private boolean toggleBtnValue = true;

	private ProgressBar pbOnMap;

	private boolean isMapStarted = false;

	// state marker hide/show
	private boolean isShowMarker;

	// masks check control
	private int[] maskEn = new int[6];

	private MapController mapController;

	// management
	private Resources resource;

	// bi map resource for control view icon
	private HashMap<Integer, Integer> hmToCap20;
	private HashMap<Integer, Integer> hmFromCap20;

	// vibrator
	private Vibrator vibrator;

	// update time
	private long intervalUpdateFriend;

	public static final String TAG = "MAP FRAGMENT";

	public MapFragment() {
		// init
		isMapStarted = false;
		isShowMarker = true;
		isRegister = false;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		if (view != null) {
			ViewGroup parent = (ViewGroup) view.getParent();
			if (parent != null)
				parent.removeView(view);
		}

		try {
			view = inflater.inflate(R.layout.fragment_map, container, false);
		} catch (InflateException e) {
			Log.i(TAG, e.getMessage());

		}

		afterViewCreate(view);

		return view;
	}

	private static String ID_STATE = "idSate";
	private static String ROUTING_STATE = "routingSate";
	private static String HISTORY_STATE = "historySate";

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		Log.i(TAG, "onActivityCreated");

		if (null != savedInstanceState) {
			fIDCurrent = savedInstanceState.getInt(ID_STATE);
			isRouting = savedInstanceState.getBoolean(ROUTING_STATE);
			isHistory = savedInstanceState.getBoolean(HISTORY_STATE);

			if (isRouting) {
				hideMarker(MyProfileManager.getInstance().getMineInstance(),
						FriendManager.getInstance().hmMemberFriends
								.get(fIDCurrent));
				mapController.routeTask(fIDCurrent);
			} else if (isHistory) {
				hideMarker();
				mapController.drawHistoryTask(fIDCurrent);
			}

		} else {
			isHistory = false;
			isRouting = false;
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		Log.i(TAG, "onSaveInstanceState");

		outState.putInt(ID_STATE, fIDCurrent);
		outState.putBoolean(ROUTING_STATE, isRouting);
		outState.putBoolean(HISTORY_STATE, isHistory);
	}

	@Override
	public void onResume() {
		super.onResume();
		// update setup map
		getmMap().setMapType(SettingManager.getInstance().getMapType());

		// check recording
		gpsPosition.setUploadMyPosition(SettingManager.getInstance()
				.isUploadMyPosition());

		// set time and accuracy update
		gpsPosition.setCheckInterval(SettingManager.getInstance()
				.getIntervalUpdatePosition());
		gpsPosition.setMinDistance(SettingManager.getInstance()
				.getAccuracyUpdatePosition());

		// set interval to update friend
		intervalUpdateFriend = SettingManager.getInstance()
				.getIntervalUpdateFriend();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		context = activity.getApplicationContext();
		myContext = (FragmentActivity) activity;

		if (!isRegister) {
			activity.registerReceiver(mHandleMessageReceiver, new IntentFilter(
					com.sgu.findyourfriend.mgr.Config.UPDATE_UI));
			activity.registerReceiver(mHandleMessageReceiver, new IntentFilter(
					com.sgu.findyourfriend.mgr.Config.MAIN_ACTION));
			isRegister = true;
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (isRegister)
			getActivity().unregisterReceiver(mHandleMessageReceiver);
	}

	// ----------------- init variable and setup methods ----------- //
	@SuppressLint("UseSparseArrays")
	private void afterViewCreate(View view) {
		// set actionbar
		View bar = getActivity().getActionBar().getCustomView();
		bar.findViewById(R.id.grpItemControl).setVisibility(View.VISIBLE);

		// get button show
		btnShow = (ImageButton) view.findViewById(R.id.btnShow);

		// get progress bar
		setPbOnMap((ProgressBar) view.findViewById(R.id.pbOnMap));
		getPbOnMap().setVisibility(View.GONE);

		// get Gps position
		gpsPosition = new GpsPosition(context, this);

		// get resources
		resource = getActivity().getResources();

		// Check if Internet present
		if (!Utility.isConnectingToInternet(getActivity())) {

			// Internet Connection is not present
			Utility.showAlertDialog(context, "Internet Connection Error",
					"Please connect to Internet connection", false);
			// stop executing code by return
			return;
		}

		// initialize
		markerManager = new HashMap<Integer, Marker>();
		markerSIn = new HashMap<Integer, Marker>();

		// setup control item hashmap
		setupForSelectionControl();

		// setup control layout and avatar list view
		inc = view.findViewById(R.id.inc_horixontal_container);

		// setup friend list
		setupFriendList();

		// setup map
		setupMap();

		// init + setup mapController
		mapController = new MapController(mThis);

		setupControlView();

		// setup send location to server
		gpsPosition.startRecording();

		// load friends position after 1 mins
		loadFriendsPosition();
		updateFriendsInfo();

		// load strangers
		loadStrangers(2000000);

		// load invited member
		loadInvitedMember();

		// vibrator setup
		vibrator = (Vibrator) context
				.getSystemService(Context.VIBRATOR_SERVICE);

		// hide control console
		hideControl();
	}

	private void loadInvitedMember() {
		// load on map
		for (int id : FriendManager.getInstance().hmInvited.keySet()) {
			createInvitedMakerOnMap(id,
					FriendManager.getInstance().hmInvited.get(id));
		}

	}

	private void loadStrangers(final double radius) {

		getmMap().addCircle(
				(new CircleOptions())
						.center(MyProfileManager.getInstance().getMyPosition())
						.radius(radius).strokeWidth(2).strokeColor(0x55ff0000));

		(new AsyncTask<Void, Void, ArrayList<SimpleUserAndLocation>>() {

			@Override
			protected ArrayList<SimpleUserAndLocation> doInBackground(
					Void... params) {

				if (FriendManager.getInstance().hmStrangers.size() == 0) {
					// try get

					return PostData
							.findInDistance(context, MyProfileManager
									.getInstance().getMyPosition().latitude,
									MyProfileManager.getInstance()
											.getMyPosition().longitude, radius);

				}
				return null;
			}

			@Override
			protected void onPostExecute(ArrayList<SimpleUserAndLocation> result) {
				if (null != result) {
					FriendManager.getInstance().hmStrangers.clear();
					for (SimpleUserAndLocation sp : result) {
						FriendManager.getInstance().hmStrangers.put(sp.getId(),
								new LatLng(sp.getLat(), sp.getLng()));

					}

				}

				// load on map
				for (int id : FriendManager.getInstance().hmStrangers.keySet()) {
					createStrangerMakerOnMap(id,
							FriendManager.getInstance().hmStrangers.get(id));
				}

			}

		}).execute();

	}

	public void tickVibrator() {
		vibrator.vibrate(100);
	}

	// ---------------- setup map ----------------------- //
	private void setupMap() {
		FragmentManager myFM = myContext.getSupportFragmentManager();
		SupportMapFragment mySpFrg = (SupportMapFragment) myFM
				.findFragmentById(R.id.mapFragment);

		setmMap(mySpFrg.getMap());
		getmMap().setMyLocationEnabled(false);

		// load setup map from setting activity
		// mMap.setMapType(SettingManager.getInstance().getMapType());

		getmMap().getUiSettings().setZoomControlsEnabled(true);
		// hide control my center
		// mMap.getUiSettings().setMyLocationButtonEnabled(false);

		// setup info window event
		getmMap().setOnInfoWindowClickListener(new OnInfoWindowClickListener() {

			@Override
			public void onInfoWindowClick(final Marker marker) {

				for (int k : markerSIn.keySet()) {
					// if ok return;

				}

				// int friendId = 0;
				for (int k : markerManager.keySet()) {
					if (markerManager.get(k).equals(marker)) {
						fIDCurrent = k;
						break;
					}
				}

				// warning
				if (fIDCurrent == -1) {
					Log.i(TAG, "id not found!");
					return;
				}

				marker.hideInfoWindow();
				final Handler mUpdateHandler = new Handler();
				Runnable mUpdateRunnable = new Runnable() {
					public void run() {
						// hide maker before display new screen
						hideMarker();

						PositionDetailFragment fragment = new PositionDetailFragment(
								mThis);

						Bundle bundle = new Bundle();
						bundle.putInt("friendId", fIDCurrent);
						bundle.putString("address",
								mapController.getAddress(marker.getPosition()));
						bundle.putDouble("myLat", MyProfileManager
								.getInstance().getMyPosition().latitude);
						bundle.putDouble("myLng", MyProfileManager
								.getInstance().getMyPosition().longitude);

						fragment.setArguments(bundle);

						hideControl();
						((BaseContainerFragment) getParentFragment())
								.replaceFragment(fragment, true);

					}
				};

				mUpdateHandler.postDelayed(mUpdateRunnable, 50);

			}
		});

		getmMap().setOnMarkerClickListener(new OnMarkerClickListener() {

			@Override
			public boolean onMarkerClick(final Marker marker) {
				Log.i(TAG, "marker on click!");

				// check marker manager
				for (int k : markerManager.keySet()) {
					if (marker.equals(markerManager.get(k))) {
						if (fIDCurrent != k) {
							changleSelectMarker(fIDCurrent, false);
							fIDCurrent = k;
							changleSelectMarker(fIDCurrent, true);

							// setup control
							swipeAdapter.hightLightItem(swipeAdapter
									.getPositionByFriendID(k));
							changeVisibilityView();
							showControl();
						}

						marker.setTitle(mapController.getAddress(FriendManager
								.getInstance().hmMemberFriends.get(k)
								.getLastLocation()));

						break;
					}
				}

				// check marker sin
				for (int k : markerSIn.keySet()) {
					if (marker.equals(markerSIn.get(k))) {
						if (FriendManager.getInstance().hmStrangers
								.containsKey(k)) {
							marker.setTitle("Kết bạn");

						} else if (FriendManager.getInstance().hmInvited
								.containsKey(k)) {
							marker.setTitle(mapController
									.getAddress(FriendManager.getInstance().hmInvited
											.get(k).getLastLocation()));
						}
						break;
					}
				}
				return false;
			}
		});

		// setup onclick on map
		getmMap().setOnMapClickListener(new OnMapClickListener() {

			@Override
			public void onMapClick(LatLng point) {
				Log.i("POS", point.latitude + " # " + point.longitude);

				swipeAdapter.unHightLightItem();
				hideControl();
				changleSelectMarker(fIDCurrent, false);
			}
		});

	}

	// ---------------- setup friend list ------------------ //
	private void setupFriendList() {
		avatarListView = (HorizontalListView) view
				.findViewById(R.id.avatarListView);

		swipeAdapter = new FriendSwipeAdapter(context,
				R.layout.item_friend_accepted, new ArrayList<Friend>(
						FriendManager.getInstance().hmMemberFriends.values()));
		avatarListView.setAdapter(swipeAdapter);

		avatarListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Log.i(TAG, "click at " + position);

				if (fIDCurrent >= 0)
					changleSelectMarker(fIDCurrent, false);

				fIDCurrent = swipeAdapter.getItem(position).getUserInfo()
						.getId();

				changleSelectMarker(fIDCurrent, true);

				// dis/en visible on control view
				changeVisibilityView();

				swipeAdapter.hightLightItem(position);
				showControl();

				if (swipeAdapter.getItem(position).getAcceptState() == Friend.SHARE_RELATIONSHIP
						&& null != swipeAdapter.getItem(position)
								.getLastLocation())
					mapController.zoomToPosition(swipeAdapter.getItem(position)
							.getLastLocation());
				else {
					Utility.showMessageOnCenter(context, "Vị trí hiện tại không có sẵn");
				}
			}
		});
	}

	// ----------------- setup control at bottom screen ----------- //
	private void setupControlView() {

		btnShow.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {

				toggleBtnValue = !toggleBtnValue;

				if (toggleBtnValue) {
					showMarker();
					btnShow.setImageDrawable(getResources().getDrawable(
							R.drawable.ic_show));
				} else {
					hideMarker();
					btnShow.setImageDrawable(getResources().getDrawable(
							R.drawable.ic_hide));
				}
			}

		});

		btnShow.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View arg0) {
				mapController.clearnMarkerHistory();
				isHistory = false;
				return true;
			}
		});

		inc = view.findViewById(R.id.inc_horixontal_container);
		btnMessage = (Button) inc.findViewById(R.id.btnMessage);
		btnCall = (Button) inc.findViewById(R.id.btnCall);
		btnHistory = (Button) inc.findViewById(R.id.btnHistory);
		btnUpdate = (Button) inc.findViewById(R.id.btnUpdate);
		btnRoute = (Button) inc.findViewById(R.id.btnRoute);
		btnRequest = (Button) inc.findViewById(R.id.btnRequest);

		btnMessage.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if (maskEn[0] == 1) {
					tickVibrator();
					hideControl();
					mapController.sendMessageTask(fIDCurrent);
				}
			}
		});

		btnCall.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (maskEn[1] == 1) {
					tickVibrator();
					hideControl();
					mapController.callTask(fIDCurrent);
				}
			}
		});

		btnHistory.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (maskEn[2] == 1
						&& null != FriendManager.getInstance().hmMemberFriends
								.get(fIDCurrent).getLastLocation()) {
					tickVibrator();
					hideControl();
					hideMarker();

					isRouting = false;
					isHistory = true;

					mapController.drawHistoryTask(fIDCurrent);
				} else {

					Utility.showMessageOnCenter(context,
							"Vị trí  hiện tại không có sẵn");
				}
			}
		});

		btnUpdate.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (maskEn[3] == 1) {
					tickVibrator();
					hideControl();
					showMarker();

					mapController.updatePositionTask(fIDCurrent);
				}
			}
		});

		btnRoute.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (maskEn[4] == 1
						&& null != FriendManager.getInstance().hmMemberFriends
								.get(fIDCurrent).getLastLocation()) {
					tickVibrator();
					hideControl();
					hideMarker(
							MyProfileManager.getInstance().getMineInstance(),
							FriendManager.getInstance().hmMemberFriends
									.get(fIDCurrent));

					isRouting = true;
					isHistory = false;

					mapController.routeTask(fIDCurrent);
				} else {
					Utility.showMessageOnCenter(context,
							"Vị trí  hiện tại không có sẵn");
				}
			}
		});

		btnRequest.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (maskEn[5] == 1) {
					tickVibrator();
					hideControl();
					showMarker();
					Friend f = FriendManager.getInstance().hmMemberFriends
							.get(fIDCurrent);

					if (f.getAcceptState() == Friend.REQUESTED_SHARE)
						mapController.acceptTask(fIDCurrent);
					else
						mapController.requestTask(fIDCurrent);

					// update ui
				}
			}
		});
	}

	// ---------- hide/show marker on map ----------------- //
	private void hideMarker() {
		Log.i(TAG, "----------------------------------------hide marker");
		for (int k : markerManager.keySet()) {
			markerManager.get(k).setVisible(false);
		}
		isShowMarker = false;
	}

	private void showMarker() {
		if (isShowMarker)
			return;
		for (int k : markerManager.keySet()) {
			markerManager.get(k).setVisible(true);
		}
		isShowMarker = true;
	}

	private void hideMarker(Friend f1, Friend f2) {
		for (int k : markerManager.keySet()) {
			markerManager.get(k).setVisible(false);
		}
		markerManager.get(f1.getUserInfo().getId()).setVisible(true);
		markerManager.get(f2.getUserInfo().getId()).setVisible(true);
		isShowMarker = false;
	}

	// ------------------ hide/show view on control console ---------- //

	@SuppressLint("UseSparseArrays")
	private void setupForSelectionControl() {
		// allways enable
		// maskEn[2] = maskEn[3] = 1;

		hmToCap20 = new HashMap<Integer, Integer>(6);
		hmToCap20.put(R.id.btnMessage, R.drawable.ic_message_cap20);
		hmToCap20.put(R.id.btnCall, R.drawable.ic_call_cap20);
		hmToCap20.put(R.id.btnRequest, R.drawable.ic_request_cap20);
		hmToCap20.put(R.id.btnRoute, R.drawable.ic_route_cap20);
		hmToCap20.put(R.id.btnHistory, R.drawable.ic_history_cap20);
		hmToCap20.put(R.id.btnUpdate, R.drawable.ic_update_cap20);

		hmFromCap20 = new HashMap<Integer, Integer>(6);
		hmFromCap20.put(R.id.btnMessage, R.drawable.ic_message);
		hmFromCap20.put(R.id.btnCall, R.drawable.ic_call);
		hmFromCap20.put(R.id.btnRequest, R.drawable.ic_request);
		hmFromCap20.put(R.id.btnRoute, R.drawable.ic_route);
		hmFromCap20.put(R.id.btnHistory, R.drawable.ic_history);
		hmFromCap20.put(R.id.btnUpdate, R.drawable.ic_update);

	}

	public void disableView(View rView, int idRaw) {
		((LinearLayout) ((ViewGroup) rView.findViewById(idRaw).getParent()))
				.getChildAt(1).setAlpha(0.2f);

		((Button) rView.findViewById(idRaw)).setBackgroundDrawable(resource
				.getDrawable(hmToCap20.get(idRaw)));
	}

	public void enableView(View rView, int idRaw) {
		((LinearLayout) ((ViewGroup) rView.findViewById(idRaw).getParent()))
				.getChildAt(1).setAlpha(1f);

		((Button) rView.findViewById(idRaw)).setBackgroundDrawable(resource
				.getDrawable(hmFromCap20.get(idRaw)));

	}

	private void changeVisibilityView() {
		if (fIDCurrent < 0)
			return;

		View rView = getView();
		// check view visiable
		// my self

		if (null == rView)
			return;

		((TextView) ((LinearLayout) ((ViewGroup) rView.findViewById(
				R.id.btnRequest).getParent())).getChildAt(1))
				.setText("yêu cầu");

		if (fIDCurrent == MyProfileManager.getInstance().getMyID()) {
			disableView(rView, R.id.btnMessage);
			maskEn[0] = 0;

			disableView(rView, R.id.btnCall);
			maskEn[1] = 0;

			enableView(rView, R.id.btnHistory);
			maskEn[2] = 1;

			enableView(rView, R.id.btnUpdate);
			maskEn[3] = 1;

			disableView(rView, R.id.btnRoute);
			maskEn[4] = 0;

			disableView(rView, R.id.btnRequest);
			maskEn[5] = 0;

		} else {

			Friend f = FriendManager.getInstance().hmMemberFriends
					.get(fIDCurrent);
			if (f.getAcceptState() == Friend.SHARE_RELATIONSHIP) {
				// full options
				enableView(rView, R.id.btnMessage);
				maskEn[0] = 1;

				enableView(rView, R.id.btnCall);
				maskEn[1] = 1;

				enableView(rView, R.id.btnHistory);
				maskEn[2] = 1;

				enableView(rView, R.id.btnUpdate);
				maskEn[3] = 1;

				enableView(rView, R.id.btnRoute);
				maskEn[4] = 1;

				disableView(rView, R.id.btnRequest);
				maskEn[5] = 0;

			} else if (f.getAcceptState() == Friend.REQUEST_SHARE) {
				// wait accept

				enableView(rView, R.id.btnMessage);
				maskEn[0] = 1;

				enableView(rView, R.id.btnCall);
				maskEn[1] = 1;

				disableView(rView, R.id.btnHistory);
				maskEn[2] = 0;

				enableView(rView, R.id.btnUpdate);
				maskEn[3] = 1;

				disableView(rView, R.id.btnRoute);
				maskEn[4] = 0;

				disableView(rView, R.id.btnRequest);
				maskEn[5] = 0;

			} else if (f.getAcceptState() == Friend.REQUESTED_SHARE) {
				// accept text

				enableView(rView, R.id.btnMessage);
				maskEn[0] = 1;

				enableView(rView, R.id.btnCall);
				maskEn[1] = 1;

				disableView(rView, R.id.btnHistory);
				maskEn[2] = 0;

				enableView(rView, R.id.btnUpdate);
				maskEn[3] = 1;

				disableView(rView, R.id.btnRoute);
				maskEn[4] = 0;

				// change text here
				enableView(rView, R.id.btnRequest);
				((TextView) ((LinearLayout) ((ViewGroup) rView.findViewById(
						R.id.btnRequest).getParent())).getChildAt(1))
						.setText("c.nhận");
				maskEn[5] = 1;

			} else {
				// friend
				enableView(rView, R.id.btnMessage);
				maskEn[0] = 1;

				enableView(rView, R.id.btnCall);
				maskEn[1] = 1;

				disableView(rView, R.id.btnHistory);
				maskEn[2] = 0;

				enableView(rView, R.id.btnUpdate);
				maskEn[3] = 1;

				disableView(rView, R.id.btnRoute);
				maskEn[4] = 0;

				enableView(rView, R.id.btnRequest);
				maskEn[5] = 1;
			}
		}
	}

	// --------------------- changle color marker --------------------- //
	private void changleSelectMarker(int idFriendCur, boolean isSelected) {
		Marker m = markerManager.get(idFriendCur);
		Friend f = FriendManager.getInstance().hmMemberFriends.get(idFriendCur);

		if (m != null) {
			if (isSelected)
				m.setIcon(combileLocationIcon(f, true));
			else
				m.setIcon(combileLocationIcon(f, false));
		}
	}

	// -------------- show/hide control console ---------------- //
	private void showControl() {
		inc.setAlpha(1.0f);
		ValueAnimator anim = ObjectAnimator.ofFloat(inc, "translationY",
				-inc.getHeight());
		anim.setDuration(500);
		anim.start();
	}

	private void hideControl() {
		ValueAnimator anim = ObjectAnimator.ofFloat(inc, "translationY",
				inc.getHeight());
		anim.setDuration(500);
		anim.start();

		if (viewSelected != null) {
			viewSelected.setBackgroundColor(0x00);
			viewSelected = null;
		}
	}

	// -------------------- should load with timer --------------- //
	private void loadFriendsPosition() {
		getmMap().clear();

		// update for friend
		for (Friend f : swipeAdapter.getData()) {
			if (f.getAcceptState() == Friend.SHARE_RELATIONSHIP
					&& null != f.getLastLocation())
				createMakerOnMap(f);
		}
	}

	private void updateFriendPosition(int friendId) {
		if (markerManager.containsKey(friendId)) {
			LatLng latlng = FriendManager.getInstance().hmMemberFriends.get(
					friendId).getLastLocation();

			markerManager.get(friendId).setPosition(latlng);

		}
	}

	private void updateAllPosition() {
		for (int k : FriendManager.getInstance().hmMemberFriends.keySet()) {
			updateFriendPosition(k);
		}
	}

	// -------------------------- update position ------------------------ //
	private void updateFriendsInfo() {
		intervalUpdateFriend = SettingManager.getInstance()
				.getIntervalUpdateFriend();
		final Handler mUpdateHandler = new Handler();
		Runnable mUpdateRunnable = new Runnable() {

			public void run() {

				(new AsyncTask<Void, Void, Void>() {

					@Override
					protected Void doInBackground(Void... params) {
						FriendManager.getInstance().loadFriend();
						return null;
					}

					@Override
					protected void onPostExecute(Void result) {
						FriendManager.getInstance().setupAfterLoading();
						// updateUI();

						Intent intentUpdate = new Intent(
								com.sgu.findyourfriend.mgr.Config.UPDATE_UI);
						intentUpdate.putExtra(
								com.sgu.findyourfriend.mgr.Config.UPDATE_TYPE,
								Utility.FRIEND);
						intentUpdate
								.putExtra(
										com.sgu.findyourfriend.mgr.Config.UPDATE_ACTION,
										Utility.RESPONSE_NO);
						context.sendBroadcast(intentUpdate);

						// Toast.makeText(getActivity(), "updateUI!",
						// Toast.LENGTH_SHORT).show();
					}

				}).execute();

				mUpdateHandler.postDelayed(this, intervalUpdateFriend);
			}
		};
		mUpdateHandler.postDelayed(mUpdateRunnable, intervalUpdateFriend);

	}

	private void updatePositionOf(Friend f) {
		Marker m = markerManager.get(f.getUserInfo().getId());
		if (m != null) {
			m.setPosition(f.getLastLocation());
			m.setTitle(mapController.getAddress(f.getLastLocation()));
		}
	}

	// ----------------------- setup iconview on map ---------------- //
	private BitmapDescriptor combileLocationIcon(final Friend f,
			boolean isSelected) {
		final Bitmap brbitmap;

		if (isSelected)
			brbitmap = BitmapFactory.decodeResource(getResources(),
					R.drawable.boder_location_selected);
		else
			brbitmap = BitmapFactory.decodeResource(getResources(),
					R.drawable.boder_location_unselected);

		return combileBorder(
				drawableToBitmap(FriendManager.getInstance().hmImageP.get(f
						.getUserInfo().getId())), brbitmap);
	}

	private BitmapDescriptor combileBorder(Bitmap bmAvatar, Bitmap brbitmap) {
		int w = bmAvatar.getWidth();
		int h = bmAvatar.getHeight();

		int newWidth = 70;
		int newHeight = 70;

		float scaleWidth = ((float) newWidth) / w;
		float scaleHeight = ((float) newHeight) / h;

		Matrix matrix = new Matrix();
		matrix.postScale(scaleWidth, scaleHeight);

		Bitmap bmAvatarResize = Bitmap.createBitmap(bmAvatar, 0, 0, w, h,
				matrix, false);
		return BitmapDescriptorFactory.fromBitmap(overlay(brbitmap,
				bmAvatarResize));
	}

	private Bitmap overlay(Bitmap bmp1, Bitmap bmp2) {
		Bitmap bmOverlay = Bitmap.createBitmap(bmp1.getWidth(),
				bmp1.getHeight(), bmp1.getConfig());
		Canvas canvas = new Canvas(bmOverlay);
		canvas.drawBitmap(bmp1, new Matrix(), null);
		canvas.drawBitmap(bmp2, (canvas.getWidth() - bmp2.getWidth()) / 2,
				(canvas.getWidth() - bmp2.getWidth()) / 2, null);
		return bmOverlay;
	}

	private Bitmap drawableToBitmap(Drawable drawable) {
		if (drawable instanceof BitmapDrawable) {
			return ((BitmapDrawable) drawable).getBitmap();
		}

		Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
				drawable.getIntrinsicHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);

		return bitmap;
	}

	// ----------------------- create map's marker ----------------- //

	private void createInvitedMakerOnMap(int id, Friend f) {
		Log.i(TAG, "create on map");
		if (null == f.getLastLocation())
			return;

		MarkerOptions options = new MarkerOptions();
		options.position(f.getLastLocation());

		options.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory
				.decodeResource(getResources(), R.drawable.ic_invited_32)));

		Marker m = getmMap().addMarker(options);
		markerSIn.put(f.getUserInfo().getId(), m);

	}

	private void createStrangerMakerOnMap(int id, LatLng latlng) {
		Log.i(TAG, "create stranger on map");

		if (markerSIn.containsKey(id))
			return;

		MarkerOptions options = new MarkerOptions();
		options.position(latlng);

		options.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory
				.decodeResource(getResources(), R.drawable.ic_stranger_32)));

		Marker m = getmMap().addMarker(options);
		markerSIn.put(id, m);
	}

	private void createMakerOnMap(Friend f) {
		Log.i(TAG, "create on map");

		MarkerOptions options = new MarkerOptions();
		options.position(f.getLastLocation());
		options.snippet(Utility.convertMicTimeToString(System
				.currentTimeMillis() - f.getUserInfo().getLastLogin().getTime())
				+ " trước - sai lệch " + f.getAccurency() + "m.");

		options.icon(combileLocationIcon(f, false));

		Marker m = getmMap().addMarker(options);
		markerManager.put(f.getUserInfo().getId(), m);
	}

	@Override
	public void onMyLocationChanged(Location location) {
		if (null == location)
			return;
		Log.i(TAG,
				"update! " + location.getLatitude() + " # "
						+ location.getLongitude());
		LatLng latlng = new LatLng(location.getLatitude(),
				location.getLongitude());
		MyProfileManager.getInstance().setMyPosition(latlng);
		Friend f = MyProfileManager.getInstance().getMineInstance();

		updatePositionOf(f);

		if (!isMapStarted) {
			mapController.zoomToPosition(location);
			isMapStarted = true;
		}

	}

	public void updatePositionTask(int fID) {
		mapController.updatePositionTask(fID);
	}

	public GoogleMap getmMap() {
		return mMap;
	}

	public void setmMap(GoogleMap mMap) {
		this.mMap = mMap;
	}

	public ProgressBar getPbOnMap() {
		return pbOnMap;
	}

	public void setPbOnMap(ProgressBar pbOnMap) {
		this.pbOnMap = pbOnMap;
	}

	// handle message
	private final BroadcastReceiver mHandleMessageReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context ctx, Intent intent) {
			String action = intent.getAction();

			if (action.equals(com.sgu.findyourfriend.mgr.Config.MAIN_ACTION)) {
				if (intent
						.hasExtra(com.sgu.findyourfriend.mgr.Config.ZOOM_POSITION_ACTION)) {
					int fID = intent
							.getIntExtra(
									com.sgu.findyourfriend.mgr.Config.ZOOM_POSITION_ACTION,
									-1);
					if (fID >= 0) {
						Friend f = FriendManager.getInstance().hmMemberFriends
								.get(fID);
						if (f.getAcceptState() == Friend.SHARE_RELATIONSHIP)
							mapController.zoomToPosition(f.getLastLocation());
						else
							Utility.showMessageOnCenter(context, "Vị trí không có sẵn");
					}
				} else if (intent
						.hasExtra(com.sgu.findyourfriend.mgr.Config.ROUTE_ACTION)) {
					hideControl();
					fIDCurrent = intent.getIntExtra(
							com.sgu.findyourfriend.mgr.Config.ROUTE_ACTION, -1);
					isRouting = true;
					isHistory = false;

					mapController.routeTask(fIDCurrent);
				} else if (intent
						.hasExtra(com.sgu.findyourfriend.mgr.Config.HISTORY_ACTION)) {

					hideControl();
					fIDCurrent = intent.getIntExtra(
							com.sgu.findyourfriend.mgr.Config.HISTORY_ACTION,
							-1);
					// showMarker();

					isRouting = false;
					isHistory = true;

					mapController.drawHistoryTask(fIDCurrent);
				}

			} else if (action
					.equals(com.sgu.findyourfriend.mgr.Config.UPDATE_UI)) {
				if (intent.getStringExtra(
						(com.sgu.findyourfriend.mgr.Config.UPDATE_TYPE))
						.equals(Utility.SHARE)) {

					swipeAdapter.notifyDataSetChanged();
					((MainActivity) myContext).notifyDataChange();
					changeVisibilityView();

				} else if (intent.getStringExtra(
						(com.sgu.findyourfriend.mgr.Config.UPDATE_TYPE))
						.equals(Utility.FRIEND)) {
					if (intent.getStringExtra(
							(com.sgu.findyourfriend.mgr.Config.UPDATE_ACTION))
							.equals(Utility.REQUEST)) {
						// update friend

					} else if (intent.getStringExtra(
							(com.sgu.findyourfriend.mgr.Config.UPDATE_ACTION))
							.equals(Utility.RESPONSE_YES)) {

						Log.i(TAG, "friend response yes");
						// add new friend to map
						for (int id : FriendManager.getInstance().hmMemberFriends
								.keySet()) {
							Friend fr = FriendManager.getInstance().hmMemberFriends
									.get(id);

							if (null != fr.getLastLocation()
									&& fr.getAcceptState() == Friend.SHARE_RELATIONSHIP
									&& !markerManager.containsKey(id)) {
								Log.i(TAG, "ok");
								createMakerOnMap(fr);
							}
						}

					} else {
						// response no

					}

					Log.i("UPDATE AFTER", "OK");
					swipeAdapter.notifyDataSetChanged();
					((MainActivity) myContext).notifyDataChange();
					changeVisibilityView();
				}
			}
		}

	};

}