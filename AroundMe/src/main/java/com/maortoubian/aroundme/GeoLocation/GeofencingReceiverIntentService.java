package com.maortoubian.aroundme.GeoLocation;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.maortoubian.aroundmeapi.model.Message;
import com.maortoubian.aroundme.ChatMainMenu;
import com.maortoubian.aroundme.DBHandler;
import com.maortoubian.aroundme.R;
import java.text.ParseException;

/*
 * this is the geolocation receiver derived that
 * will pop a notification by the specific
 * task that will be activated
 */

public class GeofencingReceiverIntentService extends
		ReceiveGeofenceTransitionBaseIntentService {

	public static final int NOTIFICATION_ID = 1;
	private NotificationManager mNotificationManager;

	//notify when geofence entered
	@Override
	protected void onEnteredGeofences(String[] geoIds) {
		Log.i("GEO", geoIds + " : just entered the area!");
		noteGeofence(geoIds);
	}

	@Override
	protected void onExitedGeofences(String[] geoIds) {}

	@Override
	protected void onError(int i) {
		Log.e(GeofencingReceiverIntentService.class.getName(), "Error: " + i);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	}

	//sending notification to the user
	private void sendNotification(String msg) {
		mNotificationManager = (NotificationManager)
				this.getSystemService(Context.NOTIFICATION_SERVICE);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, ChatMainMenu.class), 0);
		NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(this)
						.setSmallIcon(R.drawable.me)
						.setContentTitle("AroundMe")
						.setStyle(new NotificationCompat.BigTextStyle()
						.bigText(msg)).setContentText(msg);
		mBuilder.setContentIntent(contentIntent);
		mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
	}

	//adding the message to the specific conversation and mentioning its geo type
	public void noteGeofence(String[] geoIds) {
		DBHandler cdb = new DBHandler(this);
		cdb.open(DBHandler.OPEN_DB_FOR.WRITE);
		for (int index = 0; index < geoIds.length; index++)
		{
			Message geoMsg = null;
			try {
				geoMsg = cdb.getGeo(Integer.valueOf(geoIds[index]));
			}
			catch (ParseException e) {
				e.printStackTrace();
			}
			if(geoMsg != null) {
				SharedPreferences sharedPref = getSharedPreferences("userInfo",Context.MODE_PRIVATE);
				SharedPreferences.Editor editor = sharedPref.edit();
				String user_email = sharedPref.getString("email", "");
				cdb.saveMessage(geoMsg.getFrom(),user_email, geoMsg.getContnet(), DBHandler.WHO_SEND.OTHER);
				cdb.close();
				sendNotification("New Geo Msg : " + geoMsg.getContnet());
			}
		}
		cdb.close();
	}
}
