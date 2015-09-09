package com.maortoubian.aroundme;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.maortoubian.aroundmeapi.Aroundmeapi;
import com.maortoubian.aroundmeapi.model.User;
import com.maortoubian.aroundme.deviceinfoendpoint.Deviceinfoendpoint;
import com.maortoubian.aroundme.deviceinfoendpoint.model.DeviceInfo;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import java.io.IOException;
import java.net.URLEncoder;

/**
 * the signup activity where unregistered user can register to the app
 */

public class SignUp extends Activity {

    static final String TAG = "Error";

    String SENDER_ID = "1047488186224";
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";

    TextView mDisplay;
    GoogleCloudMessaging gcm;
    Context context;
    String regid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        mDisplay = (TextView) findViewById(R.id.mDisplay);

        EndpointApiCreator.initialize(null);

        final EditText  nameEt = (EditText)findViewById(R.id.nameEt);
        final EditText  emailEt = (EditText)findViewById(R.id.emailEt);
        final EditText  passwordEt = (EditText)findViewById(R.id.passwordEt);
        final Button    signUpButton = (Button)findViewById(R.id.signUpButton);

        context = getApplicationContext();

        if (checkPlayServices()) {

            gcm = GoogleCloudMessaging.getInstance(this);
            regid = getRegistrationId(context);

            if (regid.isEmpty()) {
                registerInBackground();
            }

            //listener for sending the user data to the server for registration
            signUpButton.setOnClickListener(
                    new Button.OnClickListener() {
                        public void onClick(View v) {
                            final String name = nameEt.getText().toString();
                            final String email = emailEt.getText().toString();
                            final String password = passwordEt.getText().toString();
                            if(name.equals("")||email.equals("")||password.equals("")){
                                Toast.makeText(SignUp.this,"Please fill all fields",Toast.LENGTH_LONG).show();
                            }else {
                                new AsyncTask<Void, Void, Void>() {

                                    @Override
                                    protected Void doInBackground(Void... params) {
                                        signup(email, password, name);
                                        return null;
                                    }
                                }.execute();
                            }
                        }
                    }
            );


        } else {
            Log.i(TAG, "No valid Google Play Services APK found.");
        }

    }

    public void signup(String mail, String pw, String name) {

            try {

                Aroundmeapi api = EndpointApiCreator.getApi(Aroundmeapi.class);
                User u = new User();// create new user
                u.setMail(mail);
                u.setPassword(pw);
                u.setFullName(name);
                u.setRegistrationId(regid);
                api.register(u).execute(); // register user to db
                SignUp.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(SignUp.this, "The user has signed up successfuly !\n" +
                                "you can login now", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
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
                                .setDeviceInformation(
                                        URLEncoder.encode(android.os.Build.MANUFACTURER
                                                        + " " + android.os.Build.PRODUCT,
                                                "UTF-8"))).execute();
            }
        } catch (Exception e) {}
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
        @Override
        protected void onPostExecute(String msg) {
           // mDisplay.append(msg + "\n");
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
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
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
}

