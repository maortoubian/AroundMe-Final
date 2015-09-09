package com.maortoubian.aroundme;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.maortoubian.aroundmeapi.Aroundmeapi;
import com.maortoubian.aroundmeapi.model.Message;
import com.maortoubian.aroundme.GeoLocation.GeofencingReceiverIntentService;
import com.maortoubian.aroundme.GeoLocation.SimpleGeofence;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.location.Geofence;
import java.util.ArrayList;
import java.util.List;
import static com.google.android.gms.common.api.GoogleApiClient.*;
import static com.google.android.gms.location.LocationServices.*;

/**
 * gcm intent service that gets the message then one arives and decide that to do with it
 * geo/pin/msg
 */
public class GcmIntentService extends IntentService implements ConnectionCallbacks,
        OnConnectionFailedListener, ResultCallback<Status> {

    static final String TAG = "Error";
    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;
    List<Geofence> mGeofenceList = new ArrayList<Geofence>();
    GoogleApiClient mApiClient;
    Long geoID;

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);
        SharedPreferences sharedPref = getSharedPreferences("userInfo",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        String user_email = sharedPref.getString("email", "");

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM
             * will be extended in the future with new message types, just ignore
             * any message types you're not interested in, or that you don't
             * recognize.
             */
            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                sendNotification("GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR ","");
            }
            else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
                sendNotification("Deleted messages on server: GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR ","");
            }
            else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {

                String newMatch = intent.getStringExtra("message");
                String messageId = intent.getStringExtra("newMessage");

                if (messageId != null) {
                    try {
                        Aroundmeapi api = EndpointApiCreator.getApi(Aroundmeapi.class);
                        Message m = api.getMessage(Long.parseLong(messageId)).execute();
                        String themessage = m.getContnet();
                        String fromName = m.getDisplayName();
                        String from = m.getFrom();
                        String to = m.getTo();

                        if(to.equals(user_email)){

                            //parsing the message data by the token we declared
                            //MSG/PIN/GEO
                            String type = themessage.substring(0,3);
                            themessage = themessage.substring(3);

                            //if token is MSG save it to the msgs table and notify
                            if(type.equals("MSG")) {
                                DBHandler cdb = new DBHandler(this);
                                cdb.open(DBHandler.OPEN_DB_FOR.WRITE);
                                cdb.saveMessage(from ,user_email, themessage, DBHandler.WHO_SEND.OTHER);
                                cdb.close();
                                sendNotification("New Message : " + themessage,"MSG");
                            }

                            //if token is PIN save it to the pins table and notify
                            if(type.equals("PIN")) {
                                DBHandler cdb = new DBHandler(this);
                                cdb.open(DBHandler.OPEN_DB_FOR.WRITE);
                                cdb.savePin(from, user_email, themessage,
                                        m.getLocation().getLatitude().doubleValue(),
                                        m.getLocation().getLongitude().doubleValue());
                                cdb.close();
                                sendNotification("New Pin : " + themessage,"PIN");
                            }

                            //if token is GEO save it to the geo table and notify
                            if(type.equals("GEO")) {
                                DBHandler cdb = new DBHandler(this);
                                cdb.open(DBHandler.OPEN_DB_FOR.WRITE);
                                geoID = cdb.saveGeo(from, user_email, themessage, DBHandler.WHO_SEND.OTHER);
                                cdb.close();
                                mApiClient = new GoogleApiClient.Builder(getApplicationContext())
                                        .addApi(API)
                                        .addConnectionCallbacks(this)
                                        .addOnConnectionFailedListener(this)
                                        .build();
                                SimpleGeofence geofence = new SimpleGeofence(
                                        String.valueOf(geoID),
                                        m.getLocation().getLatitude().doubleValue(),
                                        m.getLocation().getLongitude().doubleValue(),
                                        200.0f, //the default redius is 200m
                                        10000,  //the geo fence will delete it self after 10 sec
                                        Geofence.GEOFENCE_TRANSITION_ENTER);
                                mGeofenceList.add(geofence.toGeofence());
                                mApiClient.connect();
                            }
                        }
                        else{
                            Log.i(TAG,"THIS IS **NOT** USER'S EMAIL!!!");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    // Put the message into a notification and post it.
    private void sendNotification(String msg,String activityToGo) {
        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);
        PendingIntent contentIntent;
        if(activityToGo.equals("PIN")) {
            contentIntent = PendingIntent.getActivity(this, 0,
                    new Intent(this, MapsActivity.class), 0);
        }
        else {
            contentIntent = PendingIntent.getActivity(this, 0,
                    new Intent(this, ChatMainMenu.class), 0);
        }

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.me)
                        .setContentTitle("AroundMe")
                        .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(msg))
                        .setContentText(msg);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    //creating pending intent for geolocation future notification
    private PendingIntent getGeofenceTransitionPendingIntent() {
        Intent intent = new Intent(this, GeofencingReceiverIntentService.class);
        return PendingIntent.getService(this, geoID.intValue(), intent,PendingIntent.FLAG_UPDATE_CURRENT);
    }

    //after client connected,sends the geolocation pending intent to the service
    public void onConnected(Bundle arg0) {
        PendingIntent mGeofenceRequestIntent = getGeofenceTransitionPendingIntent();
        GeofencingApi.addGeofences(mApiClient, mGeofenceList,mGeofenceRequestIntent).setResultCallback(this);
    }

    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {}

    @Override
    public void onResult(Status status) {}
}


