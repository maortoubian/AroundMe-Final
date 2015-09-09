package com.maortoubian.aroundme;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.maortoubian.aroundmeapi.Aroundmeapi;
import com.maortoubian.aroundmeapi.model.GeoPt;
import com.maortoubian.aroundmeapi.model.User;
import com.maortoubian.aroundme.deviceinfoendpoint.Deviceinfoendpoint;
import com.maortoubian.aroundme.deviceinfoendpoint.model.DeviceInfo;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The Main Activity.
 * This activity starts up the RegisterActivity immediately, which communicates
 * with your App Engine backend using Cloud Endpoints. It also receives push
 * notifications from backend via Google Cloud Messaging (GCM).
 * Check out RegisterActivity.java for more details.
 */
public class Login extends Activity  implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    static final String TAG = "Error";

	/**
	 * Substitute you own sender ID here. This is the project number you got
	 * from the API Console, as described in "Getting Started."
	 */

    /* Is there a ConnectionResult resolution in progress? */
    private boolean mIsResolving = false;
    /* Should we automatically resolve ConnectionResults when possible? */
    private boolean mShouldResolve = false;
    /* Request code used to invoke sign in user interactions. */
    private static final int RC_SIGN_IN = 0;
    /* Client used to interact with Google APIs. */
    private GoogleApiClient mGoogleApiClient;

    /* A flag indicating that a PendingIntent is in progress and prevents
     * us from starting further intents.
     */
    private boolean mIntentInProgress;
    String SENDER_ID = "1047488186224";
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    TextView mDisplay;
    TextView loadingTv;
    Button LogInButton;
    Button signUpButton;
    Button signUpGoogle;
    LinearLayout ll;
    Location location;
    GoogleCloudMessaging gcm;
    AtomicInteger msgId = new AtomicInteger();
    SharedPreferences prefs;
    Context context;
    String regid;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .build();
        context = getApplicationContext();
        mDisplay = (TextView) findViewById(R.id.mDisplay);
        LogInButton = (Button)findViewById(R.id.registerButton);
        signUpGoogle = (Button)findViewById(R.id.signUpGoogle);
        signUpButton = (Button)findViewById(R.id.signUpButton);
        ll = (LinearLayout) findViewById(R.id.linearLayout1);
        loadingTv = (TextView) findViewById(R.id.loadText);
        EndpointApiCreator.initialize(null);
        final LocationListener mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(final Location newlocation) {
                location=newlocation;
            }
            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {}
            @Override
            public void onProviderEnabled(String s) {}
            @Override
            public void onProviderDisabled(String s) {}
        };
        LocationManager mLocManager;
        mLocManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_LOW);
        String provider = mLocManager.getBestProvider(criteria, true);
        location = mLocManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        mLocManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);
        final EditText mailEt = (EditText)findViewById(R.id.mailEt);
        final EditText passwordEt = (EditText)findViewById(R.id.passwordEt);
        SharedPreferences sharedPref = getSharedPreferences("userInfo",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        final String email = sharedPref.getString("email","");
        final String pw = sharedPref.getString("pw","");

        mailEt.setVisibility(View.INVISIBLE);
        passwordEt.setVisibility(View.INVISIBLE);
        loadingTv.setVisibility(View.VISIBLE);
        LogInButton.setVisibility(View.INVISIBLE);
        signUpButton.setVisibility(View.INVISIBLE);
        signUpButton.setVisibility(View.INVISIBLE);
        ll.setVisibility(View.INVISIBLE);

        //check if there is a user that is allready loged for passing on the login part
        if (((email != null)&&(pw != null) && (!email.equals("") && !pw.equals("")))) {

            Intent curr_intent = new Intent(Login.this, ChatMainMenu.class);
            startActivity(curr_intent);
            finish();

        } else {

            mailEt.setVisibility(View.VISIBLE);
            passwordEt.setVisibility(View.VISIBLE);
            LogInButton.setVisibility(View.VISIBLE);
            signUpButton.setVisibility(View.VISIBLE);
            ll.setVisibility(View.VISIBLE);
            loadingTv.setVisibility(View.INVISIBLE);
        }

        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(this);
            regid = getRegistrationId(context);
            if (regid.isEmpty()) {
               registerInBackground();
            }
        } else {
            Log.i(TAG, "No valid Google Play Services APK found.");
        }

        //listener for the signup activity
        signUpButton.setOnClickListener(
            new Button.OnClickListener(){
                public void onClick(View v){
                    Intent myIntent = new Intent(Login.this, SignUp.class);
                    Login.this.startActivity(myIntent);
                }
            }
        );

        //listener fot google plus login
        signUpGoogle.setOnClickListener(
                new Button.OnClickListener(){
                    public void onClick(View v){
                        if (v.getId() == R.id.signUpGoogle) {
                            onSignInClicked();
                        }
                    }
                }
        );

        //listener for the sign in activity ,
        //checks if the user axist on the server
        //if yes will pass to the main app chat activity
        LogInButton.setOnClickListener(
                new Button.OnClickListener(){
                    public void onClick(View v){
                        final String mail = mailEt.getText().toString();
                        final String pw = passwordEt.getText().toString();
                        new AsyncTask<Void,Void,Void>()
                        {
                            User u = null;
                            @Override
                            protected Void doInBackground(Void... params) {
                                u = login(mail, pw);
                                if(u!=null){
                                    Intent myIntent = new Intent(Login.this, ChatMainMenu.class);
                                    startActivity(myIntent);
                                    finish();
                                }
                                else{
                                    Login.this.runOnUiThread(new Runnable() {

                                        @Override
                                        public void run() {
                                            Toast.makeText(Login.this, "The user or password is incorrect", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                                return null;
                            }
                        }.execute();

                    }
                }
        );
	}


    public void signUp(User u) {
        try {
            Aroundmeapi api = EndpointApiCreator.getApi(Aroundmeapi.class);
            //initialize the user object with mail , password and regid.
            u.setRegistrationId(regid);
            api.register(u).execute(); // register user to db

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private User login(String mail, String pw) {
        try {
            Aroundmeapi api = EndpointApiCreator.getApi(Aroundmeapi.class);
            User u = new User();// create new user
            GeoPt GTP = new GeoPt();
            GTP.setLatitude((float) location.getLatitude());
            GTP.setLongitude((float) location.getLongitude());
            api.reportUserLocation(mail, GTP).execute();
            //initialize the user object with mail , password and regid.
            u.setMail(mail);
            u.setPassword(pw);
            u.setRegistrationId(regid);
            //check if user is registered or not , if not registered -it will return a null obj.
            User logedInUser = api.login(u.getMail(),u.getPassword(),u.getRegistrationId()).execute(); //login user(confirm with db)
            //save data to shared pref
            SharedPreferences sharedPref = getSharedPreferences("userInfo",Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("email", mail );
            editor.putString("pw", pw );
            editor.apply();
            if(logedInUser != null){
            //if the user is already registered , we did a log in , so we want to change intent
                return logedInUser;
            }
            if(logedInUser == null){
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    private void sendRegistrationIdToBackend() {
        try {
            com.maortoubian.aroundme.deviceinfoendpoint.Deviceinfoendpoint endpoint = EndpointApiCreator
                    .getApi(Deviceinfoendpoint.class);
            DeviceInfo existingInfo = endpoint.getDeviceInfo(regid).execute();
            boolean alreadyRegisteredWithEndpointServer = false;
            if (existingInfo != null
                    && regid.equals(existingInfo.getDeviceRegistrationID())) {
                alreadyRegisteredWithEndpointServer = true;
            }
            if (!alreadyRegisteredWithEndpointServer) {
				/*
				 * We are not registered as yet. Send an endpoint message
				 * containing the GCM registration id and some of the device's
				 * product information over to the backend. Then, we'll be
				 * registered.
				 */
                DeviceInfo deviceInfo = new DeviceInfo();
                endpoint.insertDeviceInfo(
                        deviceInfo
                                .setDeviceRegistrationID(regid)
                                .setTimestamp(System.currentTimeMillis())
                                .setDeviceInformation(URLEncoder
                                .encode(android.os.Build.MANUFACTURER +
                                " " + android.os.Build.PRODUCT, "UTF-8")))
                                .execute();
            }
        }
        catch (Exception e) {}

    }

   private void registerInBackground() {
        new RegistrationTask().execute();
   }

   class RegistrationTask extends AsyncTask<Void, Integer, String>{
        @Override
        protected String doInBackground(Void... params) {
            String msg = "";
            try {
                if (gcm == null) {
                    gcm = GoogleCloudMessaging.getInstance(context);
                }
                regid = gcm.register(SENDER_ID);
                msg = "Device registered, registration ID=" + regid;
                // You should send the registration ID to your server over HTTP,
                // so it can use GCM/HTTP or CCS to send messages to your app.
                // The request to your server should be authenticated if your app
                // is using accounts.
                sendRegistrationIdToBackend();
                // For this demo: we don't need to send it because the device
                // will send upstream messages to a server that echo back the
                // message using the 'from' address in the message.

                // Persist the registration ID - no need to register again.
                //  storeRegistrationId(context, regid);
            } catch (IOException ex) {
                msg = "Error :" + ex.getMessage();
                // If there is an error, don't just keep trying to register.
                // Require the user to click a button again, or perform
                // exponential back-off.
            }
            return msg;
        }
   }

   private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing registration ID is not guaranteed to work with
        // the new app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
   }

   private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGCMPreferences(context);
        int appVersion = getAppVersion(context);
        Log.i(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
   }

   private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        }
        catch (NameNotFoundException e) {
            throw new RuntimeException("Could not get package name: " + e);
        }
   }

   private SharedPreferences getGCMPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the registration ID in your app is up to you.
        return getSharedPreferences(Login.class.getSimpleName(),
                Context.MODE_PRIVATE);
   }

    @Override
    protected void onResume() {
        super.onResume();
        checkPlayServices();
    }

    //checking if google play services is avaible
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i("HELLO", "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    @Override
    public void onDisconnected() {}

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult:" + requestCode + ":" + resultCode + ":" + data);
        if (requestCode == RC_SIGN_IN) {
            // If the error resolution was not successful we should not resolve further.
            if (resultCode != RESULT_OK) {
                mShouldResolve = false;
            }
            mIsResolving = false;
            mGoogleApiClient.connect();
        }
    }

    public void onConnectionSuspended(int cause) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // Could not connect to Google Play Services.  The user needs to select an account,
        // grant permissions or resolve an error in order to sign in. Refer to the javadoc for
        // ConnectionResult to see possible error codes.
        if (!mIsResolving && mShouldResolve) {
            if (connectionResult.hasResolution()) {
                try {
                    connectionResult.startResolutionForResult(this, RC_SIGN_IN);
                    mIsResolving = true;
                } catch (IntentSender.SendIntentException e) {
                    Log.e("PLUS", "Could not resolve ConnectionResult.", e);
                    mIsResolving = false;
                    mGoogleApiClient.connect();
                }
            }
        }
    }


    //signing up with google plus if needed
    @Override
    public void onConnected(Bundle bundle) {
        // onConnected indicates that an account was selected on the device, that the selected
        // account has granted any requested permissions to our app and that we were able to
        // establish a service connection to Google Play services.
        mShouldResolve = false;
        if (Plus.PeopleApi.getCurrentPerson(mGoogleApiClient) != null) {
            final String email = Plus.AccountApi.getAccountName(mGoogleApiClient);
            final Person currentPerson = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
            final String personName = currentPerson.getDisplayName();
            final String id = currentPerson.getId();
            final String personPhoto = currentPerson.getImage().getUrl();
            new AsyncTask<Void,Void,Void>() {
                User u = null;
                @Override
                protected Void doInBackground(Void... params) {
                    u = login(email, id);
                    //save data to shared pref
                    SharedPreferences sharedPref = getSharedPreferences("userInfo",Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("email", email );
                    editor.putString("pw", id );
                    editor.apply();
                    if (u != null) {
                        //goto other intent
                        Intent myIntent = new Intent(Login.this, ChatMainMenu.class);
                        startActivity(myIntent);
                        finish();

                    } else {
                        User userForSignUp = new User();
                        userForSignUp.setImageUrl(personPhoto);
                        userForSignUp.setFullName(personName);
                        userForSignUp.setMail(email);
                        userForSignUp.setPassword(id);

                        signUp(userForSignUp);

                        Intent myIntent = new Intent(Login.this, ChatMainMenu.class);
                        startActivity(myIntent);
                        finish();
                    }
                    return null;
                }
            }.execute();
        }
        else{
            Log.i("PLUS","failed to Connect to Google+");
        }
    }

    protected void onStart() {
        super.onStart();
    }

    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    //connecting to google api for signing up with google plus
    private void onSignInClicked() {
        // User clicked the sign-in button, so begin the sign-in process and automatically
        // attempt to resolve any errors that occur.
        mShouldResolve = true;
        mGoogleApiClient.connect();
    }
}

