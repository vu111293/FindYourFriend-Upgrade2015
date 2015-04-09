package com.sgu.findyourfriend.mgr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.sgu.findyourfriend.R;
import com.sgu.findyourfriend.model.Friend;
import com.sgu.findyourfriend.net.PostData;

public class FriendManager {

	public static String TAG = "FRIEND MANAGER";

	private static FriendManager instance;
	private List<Friend> tempFriends;

	// hashmap store request friends
	public HashMap<Integer, Friend> hmRequestFriends;

	// hashmap store member list
	public HashMap<Integer, Friend> hmMemberFriends;

	// hashmap strangers
	public HashMap<Integer, LatLng> hmStrangers;

	// hashmap strangers
	public HashMap<Integer, Friend> hmInvited;

	// pure friends - not mine
	public ArrayList<Friend> pureFriends;

	// hashmap store drawable of friend
	public HashMap<Integer, Drawable> hmImageP;

	public Context context;

	public boolean isStop;

	public void init(Context context) {
		this.context = context;
		isStop = false;
		tempFriends = new ArrayList<Friend>();
		tempFriends = PostData.friendGetFriendList(context, MyProfileManager
				.getInstance().getMyID());

	}

	public synchronized static FriendManager getInstance() {
		if (instance == null) {
			instance = new FriendManager();
		}
		return instance;
	}

	public void loadFriend() {
		tempFriends.clear();
		// tempFriends.add(MyProfileManager.getInstance().getMineInstance());
		tempFriends.addAll(PostData.friendGetFriendList(context,
				MyProfileManager.getInstance().getMyID()));
	}

	public void setupAfterLoading() {
		if (isStop)
			return;
		Log.i("DEBUG friend size", tempFriends.size() + "");

		pureFriends.clear();
		tempFriends.add(MyProfileManager.getInstance().getMineInstance());

		hmImageP.clear();

		Iterator<Friend> it = (new ArrayList(tempFriends)).iterator();

		while (it.hasNext()) {
			Friend f = it.next();
			if (!f.getUserInfo().getAvatar().equals(""))
				hmImageP.put(f.getUserInfo().getId(),
						Drawable.createFromPath(f.getUserInfo().getAvatar()));
			else
				hmImageP.put(f.getUserInfo().getId(), context.getResources()
						.getDrawable(R.drawable.ic_no_imgprofile));
		}

		hmMemberFriends.clear();
		hmRequestFriends.clear();

		int mID = MyProfileManager.getInstance().getMyID();
		// filter request friend

		Iterator<Friend> it2 = (new ArrayList(tempFriends)).iterator();
		while (it2.hasNext()) {
			Friend f = it2.next();
			if (f.getAcceptState() == Friend.FRIEND_RELATIONSHIP
					|| f.getAcceptState() == Friend.REQUEST_SHARE
					|| f.getAcceptState() == Friend.REQUESTED_SHARE
					|| f.getAcceptState() == Friend.SHARE_RELATIONSHIP) {
				hmMemberFriends.put(f.getUserInfo().getId(), f);

				if (f.getUserInfo().getId() != mID)
					pureFriends.add(f);

			} else if (f.getAcceptState() == Friend.REQUESTED_FRIEND) {
				hmRequestFriends.put(f.getUserInfo().getId(), f);
			} else if (f.getAcceptState() == Friend.REQUEST_FRIEND) {
				hmInvited.put(f.getUserInfo().getId(), f);
			}
		}

	}

	@SuppressLint("UseSparseArrays")
	public void setup() {
		hmStrangers = new HashMap<Integer, LatLng>();
		hmInvited = new HashMap<Integer, Friend>();

		pureFriends = new ArrayList<Friend>();
		tempFriends.add(0, MyProfileManager.getInstance().getMineInstance());

		// setup drawable icon
		hmImageP = new HashMap<Integer, Drawable>();

		Iterator<Friend> it;

		it = tempFriends.iterator();

		while (it.hasNext()) {
			Friend f = it.next();
			if (!f.getUserInfo().getAvatar().equals(""))
				hmImageP.put(f.getUserInfo().getId(),
						Drawable.createFromPath(f.getUserInfo().getAvatar()));
			else
				hmImageP.put(f.getUserInfo().getId(), context.getResources()
						.getDrawable(R.drawable.ic_no_imgprofile));
		}

		hmRequestFriends = new HashMap<Integer, Friend>();
		hmMemberFriends = new HashMap<Integer, Friend>();

		int mID = MyProfileManager.getInstance().getMyID();

		Iterator<Friend> it2 = (new ArrayList(tempFriends)).iterator();
		while (it2.hasNext()) {
			Friend f = it2.next();
			if (f.getAcceptState() == Friend.FRIEND_RELATIONSHIP
					|| f.getAcceptState() == Friend.REQUEST_SHARE
					|| f.getAcceptState() == Friend.REQUESTED_SHARE
					|| f.getAcceptState() == Friend.SHARE_RELATIONSHIP) {
				hmMemberFriends.put(f.getUserInfo().getId(), f);

				if (f.getUserInfo().getId() != mID)
					pureFriends.add(f);

			} else if (f.getAcceptState() == Friend.REQUESTED_FRIEND) {
				hmRequestFriends.put(f.getUserInfo().getId(), f);
			} else if (f.getAcceptState() == Friend.REQUEST_FRIEND) {
				hmInvited.put(f.getUserInfo().getId(), f);
			}
		}

	}

	public void updateFriend(Friend friend) {

		int key = friend.getUserInfo().getId();

		for (Friend f : tempFriends) {
			if (f.getUserInfo().getId() == key) {
				f = friend;
				break;
			}
		}

		if (hmRequestFriends.containsKey(key))
			hmRequestFriends.put(key, friend);

		if (hmMemberFriends.containsKey(key))
			hmMemberFriends.put(key, friend);

		if (hmImageP.containsKey(key))
			if (!friend.getUserInfo().getAvatar().equals(""))
				hmImageP.put(friend.getUserInfo().getId(), Drawable
						.createFromPath(friend.getUserInfo().getAvatar()));
			else
				hmImageP.put(friend.getUserInfo().getId(), context
						.getResources()
						.getDrawable(R.drawable.ic_no_imgprofile));

	}

	public void updateChangeRequestToMember(Friend fr) {
		int key = fr.getUserInfo().getId();
		hmRequestFriends.remove(key);

		hmMemberFriends.put(key, fr);

		pureFriends.add(fr);
	}

	public void removeFriendRequest(Friend fr) {
		hmRequestFriends.remove(fr.getUserInfo().getId());
	}

	public void addFriendRequest(Friend fr) {
		hmRequestFriends.put(fr.getUserInfo().getId(), fr);

		if (!fr.getUserInfo().getAvatar().equals(""))
			hmImageP.put(fr.getUserInfo().getId(),
					Drawable.createFromPath(fr.getUserInfo().getAvatar()));
		else
			hmImageP.put(fr.getUserInfo().getId(), context.getResources()
					.getDrawable(R.drawable.ic_no_imgprofile));
	}

	public void addFriendMember(Friend fr) {
		Log.i("ADDMEMBER", fr.getUserInfo().getId() + "");

		hmMemberFriends.put(fr.getUserInfo().getId(), fr);

		if (!fr.getUserInfo().getAvatar().equals(""))
			hmImageP.put(fr.getUserInfo().getId(),
					Drawable.createFromPath(fr.getUserInfo().getAvatar()));
		else
			hmImageP.put(fr.getUserInfo().getId(), context.getResources()
					.getDrawable(R.drawable.ic_no_imgprofile));

		pureFriends.add(fr);
	}

	public void updateChangeMemberFriend(Friend fr) {
		int key = fr.getUserInfo().getId();
		hmMemberFriends.remove(key);
		hmMemberFriends.put(key, fr);
	}

	public void setStop(boolean b) {
		this.isStop = b;

	}

}
