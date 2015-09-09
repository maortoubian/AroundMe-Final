package com.maortoubian.aroundme;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.maortoubian.aroundme.R;

import java.util.ArrayList;
import java.util.Random;

/**
 * a class that holds all the imojis for using in the main chat and map if needed
 * and returning a random imoji by demand
 */

public class IconHelper{

   static public Bitmap RandomIcon(Context c,String icon) {

       final ArrayList<Bitmap> bitmapArray = new ArrayList<Bitmap>();

       Bitmap bMap = BitmapFactory.decodeResource(c.getResources(), R.drawable.ec0);
       Bitmap bMapScaled = Bitmap.createScaledBitmap(bMap, 70, 70, true);
       bitmapArray.add(bMapScaled);
       bMap = BitmapFactory.decodeResource(c.getResources(), R.drawable.ec1);
       bMapScaled = Bitmap.createScaledBitmap(bMap, 70, 70, true);
       bitmapArray.add(bMapScaled);
       bMap = BitmapFactory.decodeResource(c.getResources(), R.drawable.ec2);
       bMapScaled = Bitmap.createScaledBitmap(bMap, 70, 70, true);
       bitmapArray.add(bMapScaled);
       bMap = BitmapFactory.decodeResource(c.getResources(), R.drawable.ec3);
       bMapScaled = Bitmap.createScaledBitmap(bMap, 70, 70, true);
       bitmapArray.add(bMapScaled);
       bMap = BitmapFactory.decodeResource(c.getResources(), R.drawable.ec4);
       bMapScaled = Bitmap.createScaledBitmap(bMap, 70, 70, true);
       bitmapArray.add(bMapScaled);
       bMap = BitmapFactory.decodeResource(c.getResources(), R.drawable.ec5);
       bMapScaled = Bitmap.createScaledBitmap(bMap, 70, 70, true);
       bitmapArray.add(bMapScaled);
       bMap = BitmapFactory.decodeResource(c.getResources(), R.drawable.ec6);
       bMapScaled = Bitmap.createScaledBitmap(bMap, 70, 70, true);
       bitmapArray.add(bMapScaled);
       bMap = BitmapFactory.decodeResource(c.getResources(), R.drawable.ec7);
       bMapScaled = Bitmap.createScaledBitmap(bMap, 70, 70, true);
       bitmapArray.add(bMapScaled);

       if (icon.equals("ME")) {
           Bitmap bMapMe = BitmapFactory.decodeResource(c.getResources(), R.drawable.me);
           final Bitmap bMapScaledMe = Bitmap.createScaledBitmap(bMapMe, 70, 70, true);
           return bMapScaledMe;
       }
       else {

           Random r = new Random();
           int i = r.nextInt(7 - 1) + 0;

           return bitmapArray.get(i);
       }

   }

}