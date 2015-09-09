package com.maortoubian.aroundme;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.maortoubian.aroundmeapi.Aroundmeapi;
import com.maortoubian.aroundmeapi.model.Message;

import java.text.ParseException;
import java.util.ArrayList;

/**
 * this activity holds the chat between the user and another person
 */

public class Chat extends Activity {

    private ArrayList<String> arrayListToDo;
    private ArrayAdapter<String> arrayAdapterToDo;
    Thread t;
    boolean gotMsgs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_chat);

        arrayListToDo = new ArrayList<String>();
        arrayAdapterToDo = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,arrayListToDo);

        final ListView listViewToDo = (ListView) findViewById(R.id.listViewToDo);
        listViewToDo.setAdapter(arrayAdapterToDo);
        TextView name = (TextView) findViewById(R.id.textView);
        final EditText sendEt = (EditText)findViewById(R.id.sendEt);
        Button sendBtn = (Button)findViewById(R.id.sendBtn);
        ImageView mapMsgBtn = (ImageView)findViewById(R.id.chatMapMsg);
        ImageView deleteBtn = (ImageView)findViewById(R.id.deleteMsgs);
        registerForContextMenu(listViewToDo);
        SharedPreferences sharedPref = getSharedPreferences("userInfo",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        final String myMail = sharedPref.getString("email","");
        final ArrayList<Msg> listMessage = new ArrayList<Msg>();
        Intent chatIntent = getIntent();
        final String userToSendMsg = chatIntent.getStringExtra("item");
        final String userToSendName = chatIntent.getStringExtra("name");
        name.setText(userToSendName);

        //thread that will keep the ui updated with the convesation rows
        t = new Thread() {
            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(500);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                DBHandler cdb = new DBHandler(getApplicationContext());
                                cdb.open(DBHandler.OPEN_DB_FOR.READ);
                                arrayAdapterToDo.clear();
                                ArrayList<Message> msgs_from_db = null;
                                try {
                                    msgs_from_db = cdb.getMessages(myMail,userToSendMsg);
                                }
                                catch (ParseException e) {
                                    e.printStackTrace();
                                }
                                Message msg;
                                if (msgs_from_db.size() == 0 && gotMsgs) {
                                    listMessage.clear();
                                    arrayAdapterToDo.setNotifyOnChange(true);
                                    hideKeyboard();
                                    gotMsgs = false;
                                }
                                else if( (msgs_from_db.size()) > (listMessage.size())){
                                    gotMsgs = true;
                                    listMessage.clear();
                                    if (msgs_from_db == null) {
                                        Toast.makeText(getApplicationContext(), "No msg got from db..", Toast.LENGTH_LONG).show();
                                    }
                                    else {
                                        for (int i = 0; i < msgs_from_db.size(); i++) {
                                            msg = msgs_from_db.get(i);
                                            if (msg.getTo().equals("1")) {
                                                listMessage.add(new Msg(myMail, msg.getContnet(), msg.getDate(), "1"));
                                            }
                                            else {
                                                listMessage.add(new Msg(userToSendMsg, msg.getContnet(), msg.getDate(), "0"));
                                            }
                                        }
                                    }
                                    MessagesListAdapter adapter = new MessagesListAdapter(getApplicationContext(), listMessage);
                                    listViewToDo.setAdapter(adapter);
                                    listViewToDo.setSelection(adapter.getCount() - 1);
                                    arrayAdapterToDo.setNotifyOnChange(true);
                                    cdb.close();
                                }
                            }
                        });
                    }
                } catch (InterruptedException e) {}
            }
        };
        t.start();

        //listener to te send button for sending new message
        sendBtn.setOnClickListener(
            new Button.OnClickListener() {
                public void onClick(View v) {
                        final String msg = sendEt.getText().toString();
                        Message m = new Message();
                        m.setContnet("MSG" + msg);
                        m.setTo(userToSendMsg);
                        m.setFrom(myMail);
                        sendMessage(m);
                        sendEt.setText("");
                        //save msg to db and to adapter
                        DBHandler cdb = new DBHandler(getApplication());
                        cdb.open(DBHandler.OPEN_DB_FOR.WRITE);
                        cdb.saveMessage(userToSendMsg, myMail, msg, DBHandler.WHO_SEND.ME);
                        cdb.close();
                        arrayAdapterToDo.add(myMail + " : " + msg);
                }
            }
        );

        //listener to go to the map activity
        mapMsgBtn.setOnClickListener(
                new Button.OnClickListener() {
                    public void onClick(View v) {
                        Intent curr_intent = new Intent(Chat.this, MapsActivity.class);
                        Chat.this.startActivity(curr_intent);
                    }
                }
        );

        //listener to delete the specific conversation data
        deleteBtn.setOnClickListener(
                new Button.OnClickListener() {
                    public void onClick(View v) {
                        AlertDialog.Builder bAlert = new AlertDialog.Builder(Chat.this,AlertDialog.THEME_HOLO_LIGHT);
                        bAlert.setMessage(" Delete this conversation?");
                        bAlert.setCancelable(true);
                        bAlert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                final DBHandler cdb = new DBHandler(getApplicationContext());
                                cdb.open(DBHandler.OPEN_DB_FOR.WRITE);
                                try {
                                    cdb.deleteMessages(myMail,userToSendMsg);
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

    //asynctask for sending the message
    private void sendMessage(final Message m)
    {
        new AsyncTask<Void,Void,Void>()
        {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    Aroundmeapi api =  EndpointApiCreator.getApi(Aroundmeapi.class);
                    api.sendMessage(m).execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();
    }

    //hiding the keyboard
    private void hideKeyboard() {
        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        //stop thread from running thr chat update
        t.interrupt();
    };

}
