package com.maortoubian.aroundme;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.maortoubian.aroundmeapi.model.GeoPt;
import com.maortoubian.aroundmeapi.model.Message;

/**
 * this is the database of the app that holds all the messages the user get
 */
public class DBHandler {

    public enum OPEN_DB_FOR { READ, WRITE }
    public enum WHO_SEND {
        ME(1), OTHER(0);
        private final int id;
        WHO_SEND(int id) { this.id=id; }
        public int getValue() { return this.id; }
    }

    public static final String _DB_NAME = "databasename";
    public static final int _DB_VERSION = 10;
    public static final String _TABLE_NAME[] = {"tblMessage", "tblPin", "tblGeo"};
    public static final String _TABLE_CREATE[]={

            "create table tblMessage ("
                    + "ID integer primary key autoincrement, "
                    + "from_mail text not null, "
                    + "to_mail text not null, "
                    + "msg text not null, "
                    + "sender integer not null, "
                    + "is_read integer not null, "
                    + "d_date text not null" +
            ");",
            "create table tblPin ("
                    + "ID integer primary key autoincrement, "
                    + "from_mail text not null, "
                    + "to_mail text not null, "
                    + "msg text not null, "
                    + "lat double not null, "
                    + "lng double not null, "
                    + "d_date text not null" +
            ");",
            "create table tblGeo ("
                    + "ID integer primary key autoincrement, "
                    + "from_mail text not null, "
                    + "to_mail text not null, "
                    + "sender integer not null, "
                    + "is_read integer not null, "
                    + "msg text not null, "
                    + "d_date text not null" +
            ");"
    };

    private Context context;
    private DBHelper dbhelper;
    private SQLiteDatabase db;

    public DBHandler(Context context) {
        this.context=context;
        dbhelper = new DBHelper(context);

    }

    public DBHandler open(OPEN_DB_FOR open_for) {
        if(open_for.READ != null)
            db = dbhelper.getReadableDatabase();
        else
            db = dbhelper.getWritableDatabase();
        return(this);
    }

    public void close() {
        dbhelper.close();
    }

    //saving the message
    public void saveMessage(String from,String to, String msg, WHO_SEND who_send) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Calendar cal = Calendar.getInstance();
        ContentValues content = new ContentValues();
        content.put("from_mail", from);
        content.put("to_mail", to);
        content.put("msg", msg);
        content.put("sender", who_send.getValue());
        content.put("d_date", dateFormat.format(cal.getTime()).toString());
        content.put("is_read", "0");
        long t = db.insert("tblMessage", null, content);
    }

    //saving a message that will be init for geo use and returing the id
    public Long saveGeo(String from,String to, String msg, WHO_SEND who_send) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Calendar cal = Calendar.getInstance();
        ContentValues content = new ContentValues();
        content.put("from_mail", from);
        content.put("to_mail", to);
        content.put("sender", who_send.getValue());
        content.put("msg", "Geo Message : " + msg);
        content.put("is_read", "0");
        content.put("d_date", dateFormat.format(cal.getTime()).toString());
        long id = db.insert("tblGeo", null, content);
        return id;
    }

    //saving a pin message
    public void savePin(String from , String to, String msg, double lat, double lng) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Calendar cal = Calendar.getInstance();
        ContentValues content = new ContentValues();
        content.put("from_mail", from);
        content.put("to_mail", to);
        content.put("msg", from + " : " + msg);
        content.put("lat",lat );
        content.put("lng",lng );
        content.put("d_date", dateFormat.format(cal.getTime()).toString());
        long t = db.insert("tblPin", null, content);
    }

    //returning the messages of a specific conversation between the user and friend
    public ArrayList<Message> getMessages(String to,String from) throws ParseException {
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT * FROM tblMessage where from_mail = '" + from + "' AND to_mail = '" + to + "' order by d_date ASC", null);
            if (cursor == null) {
                return (null);
            } else {
                ArrayList<Message> all_messages = new ArrayList<Message>();
                cursor.moveToFirst();
                for (int i = 0; i < cursor.getCount(); i++) {
                    Message msg = new Message();
                    msg.setFrom(from);
                    msg.setTo(cursor.getString(cursor.getColumnIndex("sender")));
                    msg.setContnet(cursor.getString(cursor.getColumnIndex("msg")));
                    msg.setDate(cursor.getString(cursor.getColumnIndex("d_date")));
                    all_messages.add(msg);
                    cursor.moveToNext();
                }
                updateReadMessageOfUser(from);
                return (all_messages);
            }
        }
        finally {
            if(cursor != null) {
                cursor.close();
            }
        }
    }

    //delete all messages from specific conversation
    public void deleteMessages(String to,String from) throws ParseException {
        db.execSQL("DELETE FROM tblMessage where from_mail = '" + from + "' AND to_mail = '" + to + "'");
    }

    //returning all pin messages for display on the map
    public ArrayList<Message> getPins() throws ParseException {
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("select * from tblPin", null);
            if (cursor == null) {
                return (null);
            }
            else {

                ArrayList<Message> all_messages = new ArrayList<Message>();
                cursor.moveToFirst();
                for (int i = 0; i < cursor.getCount(); i++) {
                    Message msg = new Message();
                    msg.setFrom(cursor.getString(cursor.getColumnIndex("from_mail")));
                    msg.setContnet(cursor.getString(cursor.getColumnIndex("msg")));
                    msg.setLocation(new GeoPt()
                                    .setLatitude((float) cursor.getDouble(cursor.getColumnIndex("lat")))
                                    .setLongitude((float) cursor.getDouble(cursor.getColumnIndex("lng")))
                    );
                    all_messages.add(msg);
                    cursor.moveToNext();
                }
                return (all_messages);
            }
        }
        finally {
            if(cursor != null) {
                cursor.close();
            }
        }
    }

    //delete wanted pin
    public void deletePin(String msg) throws ParseException {
        db.execSQL("DELETE FROM tblPin where msg = '" + msg + "'");
    }

    //delete all pins
    public void deleteAllPins() throws ParseException {
        db.execSQL("DELETE FROM tblPin");
    }

    //returning geo message
    public Message getGeo(int id) throws ParseException {
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("select * from tblGeo where ID = '" + id + "'", null);
            Message msg = new Message();
            if (cursor == null) {
                return (null);
            } else {
                cursor.moveToFirst();
                msg.setFrom(cursor.getString(cursor.getColumnIndex("from_mail")));
                msg.setContnet(cursor.getString(cursor.getColumnIndex("msg")));
                cursor.moveToNext();
            }
            return (msg);
        }
        finally {
            if(cursor != null) {
                cursor.close();
            }
        }
    }

    //counting the unread messages and returning the number of them
    public int getUnreadMessages(String myMail) {
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT ID FROM tblMessage where from_mail= '"+myMail+"' and is_read=0 ", null);
            if(cursor==null)
                return(0);
            else {
                int num = cursor.getCount();
                if (num == -1) {
                    num = 0;
                }
                return (num);
            }
        }
         finally {
            if(cursor != null) {
                cursor.close();
            }
         }
    }

    //delete all conversations messages
    public void deleteAllMessageTable() {
        db.execSQL("delete from tblMessage"); // Delete all data from tblMessage
    }

    //update all the unread messages to read
    public void updateReadMessageOfUser(String mail) {
        db.execSQL("update tblMessage SET is_read=1 where from_mail = '"+mail+"' ");
    }

    public void createSqlWithoutReturn(String query) {
        db.execSQL(query);
    }

    private static class DBHelper extends SQLiteOpenHelper {
        public DBHelper(Context context) {
            super(context, _DB_NAME, null, _DB_VERSION);
        }
        @Override
        public void onCreate(SQLiteDatabase db) {
            try {
                for(int i=0;i<_TABLE_CREATE.length;i++)
                    db.execSQL(_TABLE_CREATE[i]);
            } catch(SQLException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            for(int i=0;i<_TABLE_NAME.length;i++)
                db.execSQL("DROP TABLE IF EXISTS "+_TABLE_NAME[i]);
            onCreate(db);
        }
    }
}
