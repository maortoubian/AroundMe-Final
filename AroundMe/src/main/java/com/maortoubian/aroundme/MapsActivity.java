package com.maortoubian.aroundme;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import com.maortoubian.aroundmeapi.Aroundmeapi;
import com.maortoubian.aroundmeapi.model.GeoPt;
import com.maortoubian.aroundmeapi.model.Message;
import com.maortoubian.aroundmeapi.model.UserAroundMe;
import com.maortoubian.aroundmeapi.model.UserAroundMeCollection;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapLoadedCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.plus.Plus;
import com.google.api.client.util.DateTime;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * the map activity where the user can see all his freinds and pins messages
 * and also send one or more friends geo messages or pin messages
 */

public class MapsActivity extends FragmentActivity implements
        LocationListener,
        OnMapLongClickListener,
        ConnectionCallbacks,
        OnMapLoadedCallback {

    private static final String TAG = "Error" ;
    private GoogleMap mMap;
    DBHandler cdb;
    double lat;
    double lon;
    LatLng currentLatLng;
    String user_email;
    List<UserAroundMe> usersList;
    String[] toSendArrayMails;
    String[] toSendArrayNames;
    GoogleApiClient mApiClient ;
    LocationRequest mLocationRequest;
    LocationManager mLocManager;
    Location loc;
    ImageView deleteBtn;
    Aroundmeapi api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps_2);
        cdb = new DBHandler(getApplicationContext());

        try {
            api =  EndpointApiCreator.getApi(Aroundmeapi.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        mApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .build();

        mApiClient.connect();
        SharedPreferences sharedPref = getSharedPreferences("userInfo", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        user_email = sharedPref.getString("email", "");
        setUpMapIfNeeded();

        //listener for deleting all the pins on the map
        deleteBtn = (ImageView)findViewById(R.id.deleteMsgs);
        deleteBtn.setOnClickListener(
                new Button.OnClickListener() {
                    public void onClick(View v) {
                        AlertDialog.Builder bAlert = new AlertDialog.Builder(MapsActivity.this,AlertDialog.THEME_HOLO_LIGHT);
                        bAlert.setMessage(" Delete All Pins?");
                        bAlert.setCancelable(true);
                        bAlert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                final DBHandler cdb = new DBHandler(getApplicationContext());
                                cdb.open(DBHandler.OPEN_DB_FOR.WRITE);
                                try {
                                    mMap.clear();

                                    //asynctask sending the user current location
                                    new AsyncTask<Void,Void,Void>(){
                                        @Override
                                        protected Void doInBackground(Void... params) {
                                            showAllUsers(user_email);
                                            return null;
                                        }
                                    }.execute();

                                    cdb.deleteAllPins();
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                                cdb.close();
                            }
                        });
                        bAlert.setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                        AlertDialog alert = bAlert.create();
                        alert.show();
                    }
        });

    }

    //function that send the user current location to the server
    private void sendMeLocation(String mail) {
        try {
            GeoPt GTP = new GeoPt();
            LocationManager mLocManager;
            mLocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_LOW);
            String provider = mLocManager.getBestProvider(criteria, true);
            Location location = mLocManager.getLastKnownLocation(provider);
            GTP.setLatitude((float) location.getLatitude());
            GTP.setLongitude((float) location.getLongitude());
            api.reportUserLocation(mail, GTP).execute();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    //check if the map need setup
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map))
                    .getMap();
            mMap.setOnMapLoadedCallback(this);
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                try {
                    setUpMap();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //setting up the map
    private void setUpMap() throws IOException {

        loc =  getLocation();
        if(loc!=null) {
            lat = loc.getLatitude();
            lon = loc.getLongitude();
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 20));
            CameraUpdate center = CameraUpdateFactory.newLatLng(new LatLng(lat, lon));
            mMap.moveCamera(center);
            mMap.animateCamera(CameraUpdateFactory.zoomTo(14), 2000, null);
            mMap.setOnMapLongClickListener(this);
        }

        //asynctask sending the user current location
        new AsyncTask<Void,Void,Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                sendMeLocation(user_email);
                return null;
            }
        }.execute();
    }

    //checks which provider is available for showing the map (GPS || METWORK)
    private Location getLocation(){
        Location loc = null;
        Location locGps = null;
        mLocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // getting GPS status
        boolean isGPSEnabled = mLocManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        // getting network status
        boolean isNetworkEnabled = mLocManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if(!isGPSEnabled && !isNetworkEnabled ){
            Toast.makeText(this, "no GPS or NETWORK connection", Toast.LENGTH_LONG);
        }
        else if(isGPSEnabled){
            locGps = mLocManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if(locGps != null){
                loc = mLocManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            else{
                loc = mLocManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
        }
        return loc;
    }

    //function that puts all the users on the map by theyre last location found in the server
    //and putting each user a picture of his google plus or imoji
    private void showAllUsers(String mail) {
        try {
            UserAroundMeCollection a;
            a = api.getUsersAroundMe((float) lat, (float) lon, 1000000, mail).execute();
            usersList = a.getItems();
            int size = usersList.size();
            for (int i=0 ; i<size ; i++) {

                final Float lat = usersList.get(i).getLocation().getLatitude();
                final Float lng = usersList.get(i).getLocation().getLongitude();
                final String name = usersList.get(i).getDisplayName();
                final String email = usersList.get(i).getMail();
                final String image  = usersList.get(i).getImageUrl();
                Bitmap bMapGooglePlus = null;

                if(image != null) {
                    URL url = new URL(image);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setDoInput(true);
                    conn.connect();
                    InputStream is = conn.getInputStream();
                    bMapGooglePlus = BitmapFactory.decodeStream(is);
                    final Bitmap bMapGooglePlusFinal = ImageHelper.getRoundedCornerBitmap(Bitmap.
                            createScaledBitmap(bMapGooglePlus, 70, 70, true),50);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng))
                                        .icon(BitmapDescriptorFactory.fromBitmap(bMapGooglePlusFinal))).setTitle(name);
                            }
                        });
                }
                else{
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            if ((email.equals(user_email))) {
                                mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng))
                                        .icon(BitmapDescriptorFactory.fromBitmap(IconHelper.RandomIcon(
                                                getApplicationContext(),"ME")))).setTitle("ME");
                            } else {
                                mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng))
                                        .icon(BitmapDescriptorFactory.fromBitmap(IconHelper.RandomIcon(
                                                getApplicationContext(), "other")))).setTitle(name);
                            }
                        }
                    });
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    //putting all the user pins on the map
    private void showPins() {
        Message msg;
        cdb.open(DBHandler.OPEN_DB_FOR.READ);
        ArrayList<Message> pins_from_db = null;
        try {
            pins_from_db = cdb.getPins();
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
        if (pins_from_db != null){
            for (int i = 0; i < pins_from_db.size(); i++) {
                msg = pins_from_db.get(i);
                Bitmap Pin = BitmapFactory.decodeResource(getResources(), R.drawable.pin);
                Bitmap PinScaled = Bitmap.createScaledBitmap(Pin, 70, 70, true);
                mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(msg.getLocation().getLatitude().doubleValue(),
                                msg.getLocation().getLongitude().doubleValue()))
                        .icon(BitmapDescriptorFactory.fromBitmap(PinScaled))
                        .title(msg.getContnet()));

                //setting a listener to the pin message label for deleting the clicked pin if wanted
                mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {

                    @Override
                    public void onInfoWindowClick(final Marker marker) {
                        AlertDialog.Builder bAlert = new AlertDialog.Builder(MapsActivity.this,AlertDialog.THEME_HOLO_LIGHT);
                        bAlert.setMessage(marker.getTitle());
                        final String title = marker.getTitle();
                        bAlert.setCancelable(true);
                        bAlert.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                final DBHandler cdb = new DBHandler(getApplicationContext());
                                cdb.open(DBHandler.OPEN_DB_FOR.WRITE);
                                try {
                                    cdb.deletePin(title);
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                                cdb.close();
                                marker.setVisible(false);
                            }
                        });
                        bAlert.setNegativeButton("Close", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                        AlertDialog alert = bAlert.create();
                        alert.show();
                    }
                });

            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        loc=location;
    }

    //when connected to google send user location and bring all users from the server
    @Override
    public void onConnected(Bundle bundle) {

        new AsyncTask<Void,Void,Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                sendMeLocation(user_email);
                showAllUsers(user_email);
                return null;
            }

        }.execute();
    }

    @Override
    public void onConnectionSuspended(int i) {}

    //on map longclick the user will have the abilty to pick to send pin message or
    //geo message to one or more friends
    @Override
    public void onMapLongClick(LatLng latLng) {
        currentLatLng = latLng;
        AlertDialog.Builder bAlert = new AlertDialog.Builder(MapsActivity.this,AlertDialog.THEME_HOLO_LIGHT);
        bAlert.setTitle("Pick Message Type");
        bAlert.setCancelable(true);
        bAlert.setItems(new CharSequence[]{"Pin Message", "Geo Message", "Cancel"},
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                allOrPick("PIN");
                                dialog.cancel();
                                break;
                            case 1:
                                allOrPick("GEO");
                                dialog.cancel();
                                break;
                            case 2:
                                dialog.cancel();
                        }
                    }
                });
        AlertDialog alert = bAlert.create();
        alert.show();
    }

    //send to all or pick a friends from the list of users
    public void allOrPick(final String type) {
        AlertDialog.Builder bAlert = new AlertDialog.Builder(MapsActivity.this,AlertDialog.THEME_HOLO_LIGHT);
        bAlert.setTitle("For Who To Send?");
        bAlert.setCancelable(true);
        bAlert.setItems(new CharSequence[]{"Send To All", "Pick Friends", "Cancel"},
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                sendToAll(type);
                                dialog.cancel();
                                break;
                            case 1:
                                pickFriendsDialog(type);
                                dialog.cancel();
                                break;
                            case 2:
                                dialog.cancel();
                        }
                    }
                });
        AlertDialog alert = bAlert.create();
        alert.show();
    }

    //send the message to all the users
    public void sendToAll(final String type){
        final EditText input = new EditText(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(this,AlertDialog.THEME_HOLO_LIGHT);
        builder.setView(input);
        builder.setTitle("What you want to send to all?");
        builder.setPositiveButton("Send", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                GeoPt GTP = new GeoPt();
                GTP.setLatitude((float) currentLatLng.latitude);
                GTP.setLongitude((float) currentLatLng.longitude);
                Message m = new Message();
                if (type.equals("PIN")) {
                    m.setContnet("PIN" + input.getText());
                } else {
                    m.setContnet("GEO" + input.getText());
                }
                for (int i = 0; i < usersList.size(); i++) {
                    if (!usersList.get(i).getMail().equals(user_email)) {
                        m.setTo(String.valueOf(usersList.get(i).getMail()));
                        m.setFrom(user_email);
                        m.setTimestamp(new DateTime(new Date()));
                        m.setLocation(GTP);
                        sendMessage(m);
                    }
                }
                dialog.cancel();
            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    //showing the users all the friends he can pick to send the geo or pin message
    public void pickFriendsDialog(final String type){
        // Where we track the selected items
        final ArrayList mSelectedItems = new ArrayList();
        toSendArrayMails = new String[usersList.size()];
        toSendArrayNames = new String[usersList.size()];
        final EditText input = new EditText(this);
        for(int i=0;i<usersList.size();i++){
            if(!usersList.get(i).getMail().equals(user_email)) {
                toSendArrayMails[i] = usersList.get(i).getMail();
                toSendArrayNames[i] = usersList.get(i).getDisplayName();
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this,AlertDialog.THEME_HOLO_LIGHT);
        builder.setView(input);
        // Set the dialog title
        if(type.equals("PIN")){
            builder.setTitle("Friends To Send Pin");
        }
        else{
            builder.setTitle("Friends To Send Geo");
        }
        builder.setMultiChoiceItems(toSendArrayNames, null,
                new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        if (isChecked) {
                            // If the user checked the item, add it to the selected items
                            mSelectedItems.add(toSendArrayMails[which]);
                        }
                        else if (mSelectedItems.contains(toSendArrayMails[which])) {
                            // Else, if the item is already in the array, remove it
                            mSelectedItems.remove(toSendArrayMails[which]);
                        }
                    }
                })
                // Set the action buttons
                .setPositiveButton("Send", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        GeoPt GTP = new GeoPt();
                        GTP.setLatitude((float) currentLatLng.latitude);
                        GTP.setLongitude((float) currentLatLng.longitude);
                        Message m = new Message();
                        if (type.equals("PIN")) {
                            m.setContnet("PIN" + input.getText());
                        } else {
                            m.setContnet("GEO" + input.getText());
                        }
                        if (mSelectedItems.size() == 0) {
                            Toast.makeText(MapsActivity.this, "you did not select a friend to send to", Toast.LENGTH_LONG).show();
                        } else {
                            for (int i = 0; i < mSelectedItems.size(); i++) {

                                m.setTo(String.valueOf(mSelectedItems.get(i)));
                                m.setFrom(user_email);
                                m.setTimestamp(new DateTime(new Date()));
                                m.setLocation(GTP);
                                sendMessage(m);
                            }
                            dialog.cancel();
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    };

    //sending the message bye a async class for sending few messages at a time
    public boolean sendMessage(Message m) {
        try {
            return new sendMessageClass().execute(m).get();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        catch (ExecutionException e) {
            e.printStackTrace();
        }
        return false;
    }

    //the async message class
    private class sendMessageClass extends AsyncTask<Message,Void,Boolean> {
        @Override
        protected Boolean doInBackground(Message... params) {
            try {
                api.sendMessage(params[0]).execute();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    //on map ready show pins in have some
    @Override
    public void onMapLoaded() {
        if (mMap != null) {
            showPins();
        }
    }
}

