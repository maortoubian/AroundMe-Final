package com.maortoubian.aroundme;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.maortoubian.aroundme.R;
/*
 * this class holds the three  HowTo fragments
 */
public class HowToActivity extends FragmentActivity {

    Button gotIt;
    Button next;
    Button prev;
    ImageView image;
    TextView text;
    int count;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_how_to);
        count = 0;
        next = (Button) findViewById(R.id.button2);
        next.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (count != 6) {
                    count++;
                    changePic(count);
                }
            }
        });

        prev = (Button) findViewById(R.id.button);
        prev.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if(count!=0) {
                    count--;
                    changePic(count);
                }
            }
        });

        image = (ImageView) findViewById(R.id.imageView2);
        image.setImageResource(R.drawable.screen_1);
    }

    public void changePic(int pic){
            switch (pic) {
                case 0:
                    image.setImageResource(R.drawable.screen_1);
                    break;
                case 1:
                    image.setImageResource(R.drawable.screen_2);
                    break;
                case 2:
                    image.setImageResource(R.drawable.screen_3);
                    break;
                case 3:
                    image.setImageResource(R.drawable.screen_4);
                    break;
                case 4:
                    image.setImageResource(R.drawable.screen_5);
                    break;
                case 5:
                    image.setImageResource(R.drawable.screen_6);
                    break;
                case 6:
                    image.setImageResource(R.drawable.screen_7);
                    break;
                default:
                    break;
            }
    }
}
