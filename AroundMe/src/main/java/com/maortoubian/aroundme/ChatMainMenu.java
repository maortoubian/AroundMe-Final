package com.maortoubian.aroundme;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import com.maortoubian.aroundmeapi.Aroundmeapi;
import com.maortoubian.aroundmeapi.model.Message;
import com.maortoubian.aroundmeapi.model.UserAroundMe;
import com.maortoubian.aroundmeapi.model.UserAroundMeCollection;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * this is the app main activity where the user can see all his friends that registered to the app
 * new messages will be shown here on the right side of the user how sent theme
 */
public class ChatMainMenu extends Activity {

    public final static String EXTRA_MESSAGE = "MESSAGE";
    private ListView lv;
    ArrayAdapter<String> arrayAdapter;
    List<UserAroundMe> usersList;
    public ArrayList<RowUser> CustomListViewValuesArr = new ArrayList<RowUser>();
    private ListView list;
    private UserListAdapter adapter;
    String user_email;
    int usersListSize;
    int newMsgCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);
        ImageView logOutBtn = (ImageView) findViewById(R.id.logOutBtn);
        ImageView mapBtn = (ImageView) findViewById(R.id.mapButton);
        lv = (ListView) findViewById(R.id.usersLv);
        list = (ListView) findViewById(R.id.usersLv);
        Resources res = getResources();
        adapter = new UserListAdapter(ChatMainMenu.this, CustomListViewValuesArr, res);
        list.setAdapter(adapter);
        List<String> arrList = new ArrayList<String>();
        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, arrList);
        lv.setAdapter(arrayAdapter);
        Intent intent = getIntent();
        String value = intent.getStringExtra("key");
        SharedPreferences sharedPref = getSharedPreferences("userInfo", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        user_email = sharedPref.getString("email", "");
        newMsgCount = 0;

        //how to to use will be pop at the first enterance to the app
        sharedPref.getBoolean("firstUse", true);
        Log.i("maor", "first:" + sharedPref.getBoolean("firstUse", true));
        if (sharedPref.getBoolean("firstUse", true)) {
            editor.putBoolean("firstUse", false);
            editor.commit();
            howTo();
        }


        //asynctask for getting the list of users from the server
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                showUsers(user_email);
                return null;
            }
        }.execute();

        //listener for opening the more options menu
        logOutBtn.setOnClickListener(
                new Button.OnClickListener() {
                    public void onClick(View v) {
                        AlertDialog.Builder settingAlert = new AlertDialog.Builder(ChatMainMenu.this,AlertDialog.THEME_HOLO_LIGHT);
                        settingAlert.setTitle("settings");
                        settingAlert.setCancelable(true);
                        settingAlert.setItems(new CharSequence[]{"Delete All Messages", "How To Use", "About Us", "Log Out", "Cancel"},
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        switch (which) {
                                            case 0:
                                                //delete all msgs
                                                deleteAllMsgs();
                                                break;
                                            case 1:
                                                //how to use the app tutorial
                                                howTo();
                                                break;
                                            case 2:
                                                //about us section
                                                aboutUs();
                                                break;
                                            case 3:
                                                //login out from the specific user
                                                logOutMenu();
                                                break;
                                            case 4:
                                                dialog.cancel();
                                                break;
                                        }
                                    }
                                });
                        AlertDialog alert = settingAlert.create();
                        alert.show();
                    }
        });

        //listener for going to the map activity
        mapBtn.setOnClickListener(
                new Button.OnClickListener() {
                    public void onClick(View v) {
                        Intent curr_intent = new Intent(ChatMainMenu.this, MapsActivity.class);
                        ChatMainMenu.this.startActivity(curr_intent);
                    }
                }
        );
    }

    //function that opens the tutorial for the app usage
    public void howTo(){
        Intent intentHowTo = new Intent(ChatMainMenu.this, HowToActivity.class);
        startActivity(intentHowTo);
    }

    //function that init the log out
    public void logOutMenu(){
        SharedPreferences sharedPref = getSharedPreferences("userInfo", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("email", "");
        editor.putString("pw", "");
        editor.apply();
        AlertDialog.Builder bAlert = new AlertDialog.Builder(this,AlertDialog.THEME_HOLO_LIGHT);
        bAlert.setMessage("Are you sure \n you want to LogOut?");
        bAlert.setCancelable(true);
        bAlert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                Intent curr_intent = new Intent(ChatMainMenu.this, Login.class);
                startActivity(curr_intent);
                finish();
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

    //function for deleting all the user conversation from the db and views
    public void deleteAllMsgs(){
        AlertDialog.Builder bAlert = new AlertDialog.Builder(this,AlertDialog.THEME_HOLO_LIGHT);
        bAlert.setMessage("Are you sure \n you want to delete all messages?");
        bAlert.setCancelable(true);
        bAlert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                DBHandler cdb = new DBHandler(getApplicationContext());
                cdb.open(DBHandler.OPEN_DB_FOR.WRITE);
                cdb.deleteAllMessageTable();
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

    //function of the about us
    public void aboutUs(){
        AlertDialog.Builder bAlert = new AlertDialog.Builder(this,AlertDialog.THEME_HOLO_LIGHT);
        bAlert.setTitle("AROUND ME");
        bAlert.setMessage("was made as part of an Android course at " +
                "SHENKAR software engineering department\n\n" +
                "Developers:\nMaor Toubian\nYoni Nezer\nYaron Israeli");
        bAlert.setCancelable(true);
        bAlert.setNegativeButton("Close", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog alert = bAlert.create();
        alert.show();
    }

    //open the wanted user chat activity by clicking the user name
    public void onItemClick(int Pos) {
        int size = usersList.size();
        UserAroundMe[] arr = new UserAroundMe[size + 1];
        usersList.toArray(arr);
        for (int i = 0; i < size; i++) {
            if (arr[i].getDisplayName() == CustomListViewValuesArr.get(Pos).getTitle()) {
                String mail = arr[i].getMail();
                String name = arr[i].getDisplayName();
                RowUser tempRow = (RowUser) CustomListViewValuesArr.get(Pos);
                Intent intent = new Intent(this, Chat.class);
                intent.putExtra(EXTRA_MESSAGE, Integer.toString(tempRow.getId()));
                intent.putExtra("item", mail);
                intent.putExtra("name", name);
                startActivity(intent);
            }
        }
    }

    //will add to the ui all the users rows with google plus pictures or imojis
    public void showUsers(String user_email) {
        try {
            CustomListViewValuesArr.clear();

            Aroundmeapi api = EndpointApiCreator.getApi(Aroundmeapi.class);
            UserAroundMeCollection a;

            a = api.getAllUsers(user_email).execute();
            usersList = a.getItems();
            usersListSize = usersList.size();
            UserAroundMe[] arr = new UserAroundMe[usersListSize + 1];
            usersList.toArray(arr);

            for (int i = 0; i < usersListSize; i++) {
                final String name = arr[i].getDisplayName();
                final String email = arr[i].getMail();
                final String image = arr[i].getImageUrl();
                Bitmap bMapGooglePlus = null;
                if (!email.equals(user_email)) {
                    if (image != null) {
                        URL url = new URL(image);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setDoInput(true);
                        conn.connect();
                        InputStream is = conn.getInputStream();
                        bMapGooglePlus = BitmapFactory.decodeStream(is);
                        final Bitmap bMapGooglePlusFinal =
                                ImageHelper.getRoundedCornerBitmap
                                        (Bitmap.createScaledBitmap(bMapGooglePlus, 70, 70, true), 50);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                final DBHandler cdb = new DBHandler(getApplicationContext());
                                cdb.open(DBHandler.OPEN_DB_FOR.READ);
                                int unReadmsgCount = cdb.getUnreadMessages(email);
                                RowUser r = new RowUser();
                                r.setTitle(name);
                                r.setIcon(bMapGooglePlusFinal);
                                if (unReadmsgCount != 0) {
                                    r.setnewMsg(String.valueOf(unReadmsgCount));
                                    CustomListViewValuesArr.add(0, r);
                                }
                                else {
                                    r.setnewMsg("");
                                    CustomListViewValuesArr.add(r);
                                }
                                list.setAdapter(adapter);
                                cdb.close();
                            }
                        });
                    }
                    else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                final DBHandler cdb = new DBHandler(getApplicationContext());
                                cdb.open(DBHandler.OPEN_DB_FOR.READ);
                                int unReadmsgCount = cdb.getUnreadMessages(email);
                                RowUser r = new RowUser();
                                r.setTitle(name);
                                r.setIcon(IconHelper.RandomIcon(getApplicationContext(), "other"));
                                if (unReadmsgCount != 0) {
                                    r.setnewMsg(String.valueOf(unReadmsgCount));
                                    CustomListViewValuesArr.add(0, r);
                                }
                                else {
                                    r.setnewMsg("");
                                    CustomListViewValuesArr.add(r);
                                }
                                list.setAdapter(adapter);
                                cdb.close();
                            }
                        });
                    }
                }
            }
            checkNewMsgs();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    //checking constantly for new messages to arrive to show the user
    //and update the new messages number if needed to the new messages num or none
    private void checkNewMsgs() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
            if (CustomListViewValuesArr.size() != 0) {
                int size = usersList.size();
                if (size != 0) {
                    UserAroundMe[] arr = new UserAroundMe[size + 1];
                    usersList.toArray(arr);
                    final DBHandler cdb = new DBHandler(getApplicationContext());
                    cdb.open(DBHandler.OPEN_DB_FOR.READ);
                    for (int i = 0; i < (CustomListViewValuesArr.size() - 1); i++) {
                        final String email = arr[i].getMail();
                        int unReadmsgCount = cdb.getUnreadMessages(email);
                        if (unReadmsgCount > 0) {
                            for (int j = 0; j < (CustomListViewValuesArr.size() - 1); j++) {
                                if (CustomListViewValuesArr.get(j).getTitle().equals(arr[i].getDisplayName())) {
                                    RowUser r = CustomListViewValuesArr.get(j);
                                    if (r.getnewMsg().equals("")) {
                                        r.setnewMsg("0");
                                    }
                                    if (Integer.valueOf(r.getnewMsg()) == unReadmsgCount) {
                                    } else {
                                        r.setnewMsg(String.valueOf(unReadmsgCount));
                                        CustomListViewValuesArr.remove(j);
                                        CustomListViewValuesArr.add(0, r);
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                //stuff that updates ui
                                                adapter.notifyDataSetChanged();
                                            }
                                        });
                                    }
                                }
                            }
                        }
                        if (unReadmsgCount == 0) {
                            for (int j = 0; j < (CustomListViewValuesArr.size() - 1); j++) {
                                if (CustomListViewValuesArr.get(j).getTitle().equals(arr[i].getDisplayName())) {
                                    CustomListViewValuesArr.get(j).setnewMsg("");
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            adapter.notifyDataSetChanged();
                                        }
                                    });
                                }
                            }
                        }
                    }
                    cdb.close();
                }
            }
            }
        }, 0, 2000);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}