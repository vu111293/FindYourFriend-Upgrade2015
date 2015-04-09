package com.sgu.findyourfriend.screen;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.sgu.findyourfriend.R;
import com.sgu.findyourfriend.adapter.FriendSwipeAdapter;
import com.sgu.findyourfriend.mgr.FriendManager;
import com.sgu.findyourfriend.mgr.IMessage;
import com.sgu.findyourfriend.mgr.MessageManager;
import com.sgu.findyourfriend.model.Friend;
import com.sgu.findyourfriend.model.Message;
import com.sgu.findyourfriend.utils.Utility;
import com.sgu.findyourfriend.view.devsmart.android.ui.HorizontalListView;

@SuppressLint("ValidFragment")
public class MessageSendFragment extends Fragment implements IMessage {

	private HorizontalListView friendsHList;
	private FriendSwipeAdapter swipeAdapter;

	private EditText editMessage;

	private int[] masks;
	private Context context;
	private Activity activity;
	private Button btnSend;
	private LinearLayout friendLayout;

	private int friendId;

	private MessageFragment mainFragment;

	public MessageSendFragment() {
	}

	public MessageSendFragment(MessageFragment mainFragment) {
		this.mainFragment = mainFragment;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		InputMethodManager imm = (InputMethodManager) getActivity()
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.showSoftInput(editMessage, InputMethodManager.SHOW_IMPLICIT);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View rootView = inflater.inflate(R.layout.fragment_send_message,
				container, false);

		friendLayout = (LinearLayout) rootView.findViewById(R.id.layoutH);

		btnSend = (Button) rootView.findViewById(R.id.btnSend);


		// init masks
		masks = new int[FriendManager.getInstance().hmMemberFriends.size()];

		editMessage = (EditText) rootView.findViewById(R.id.editMessage);

		editMessage.requestFocus();

		btnSend.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {

				InputMethodManager imm = (InputMethodManager) getActivity()
						.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(editMessage.getWindowToken(), 0);

				if (isSelectedReceiver()) {
					// send to myself
					MessageManager.getInstance().sendMessage(
							editMessage.getText().toString(), getToAddress());
					editMessage.setText("");

					// finish fragment
					getActivity().getFragmentManager().popBackStackImmediate();
					((BaseContainerFragment) getParentFragment()).popFragment();

				} else {
					Utility.showAlertDialog(getParentFragment().getActivity(),
							"", "Chọn người nhận", true);
					friendLayout.setBackgroundColor(getActivity()
							.getResources().getColor(R.color.pupors));

				}

			}
		});

		friendsHList = (HorizontalListView) rootView
				.findViewById(R.id.avatarListView);
		swipeAdapter = new FriendSwipeAdapter(getActivity(),
				R.layout.item_friend_accepted,
				FriendManager.getInstance().pureFriends);
		swipeAdapter.setShowStatus(false);

		friendsHList.setAdapter(swipeAdapter);

		friendsHList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {

				masks[position] = 1 - masks[position];
				changeSelector(position, view);
			}

		});

		// MessageManager.getInstance().setMessageListener(this);

		Bundle bundle = getArguments();
		friendId = bundle.getInt("friendId", -1);

		if (friendId >= 0) {
			Log.i("wwwwwwwwwwwwwwwwwwwwwwwww", friendId + "");

			for (int i = 0; i < swipeAdapter.getData().size(); ++i) {
				if (swipeAdapter.getItem(i).getUserInfo().getId() == friendId) {
					masks[i] = 1;
					swipeAdapter.hightLightItem(i);
					swipeAdapter.notifyDataSetChanged();
					break;
				}
			}

		}

		return rootView;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.context = activity.getApplicationContext();
		this.activity = activity;
	}

	public static void showKeyboard(Activity activity) {
		if (activity != null) {
			activity.getWindow().setSoftInputMode(
					WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		}
	}

	public static void hideKeyboard(Activity activity) {
		if (activity != null) {
			activity.getWindow().setSoftInputMode(
					WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		}
	}

	private List<Integer> getToAddress() {
		List<Integer> addrs = new ArrayList<Integer>();
		int len = swipeAdapter.getData().size();

		for (int i = 0; i < len; ++i)
			if (masks[i] == 1)
				addrs.add(swipeAdapter.getItem(i).getUserInfo().getId());

		return addrs;
	}

	private void changeSelector(int pos, View view) {
		if (masks[pos] == 0)
			view.setBackgroundColor(0x00);
		else
			view.setBackgroundColor(0xff086EBC);
	}

	private boolean isSelectedReceiver() {
		for (int i : masks) {
			if (i == 1)
				return true;
		}
		return false;
	}

	@Override
	public void addNewMessage(Message msg) {
		if (mainFragment != null)
			mainFragment.addNewMessage(msg);
	}

}
