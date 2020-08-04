package com.websarva.wings.android.tasukete;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FirstScreen extends AppCompatActivity {

    LocationManager locationManager;
    Double latitude;
    Double longitude;
    private TextView tvLatitude;
    private TextView tvLongitude;
    String date;
    String time;
    private SQLiteDatabase db;
    int user_id = 1;//仮
    private DatabaseHelper helper;
    Notification notification = null;//通知
    Double distance;
    TextView tvRead;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        GPSLocationListener locationListener = new GPSLocationListener();
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    Activity#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for Activity#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        Button btEmergency=findViewById(R.id.btEmergency);
        btEmergency.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                onEmergencyClicked(view);
            }
        });
        Button btResister=findViewById(R.id.btResister);
        btResister.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                onResisterClicked(view);
            }
        });

        tvRead=findViewById(R.id.tvRead);

        //日時の表示
        TextView tvDate=findViewById(R.id.tvDate);
        TextView tvTime=findViewById(R.id.tvTime);
        date=getNowDate();
        time=getNowTime();
        tvDate.setText(date);
        tvTime.setText(time);

        tvLatitude=findViewById(R.id.tvLatitude);
        tvLongitude=findViewById(R.id.tvLongitude);

        //通知の発行
        String id="help_me_notification_channel";
        String name=getString(R.string.notification_channel_name);
        int importance=NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel= null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = new NotificationChannel(id,name,importance);
        }
        NotificationManager manager=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(channel);
        }
    }

    public void onResisterClicked(View view){
        if(helper==null)helper=new DatabaseHelper(getApplicationContext());
        if(db==null)db=helper.getWritableDatabase();
        insertData(db,user_id,latitude,longitude,date,time);
    }
    public void onEmergencyClicked(View view){
        //通知
        NotificationCompat.Builder builder=new NotificationCompat.Builder(FirstScreen.this,"help_me_notification_channel");
        builder.setSmallIcon(android.R.drawable.ic_dialog_info);
        builder.setContentTitle(getString(R.string.msg_notification_title_finish));
        builder.setContentText(getString(R.string.msg_notification_text_finish));
        Notification notification=builder.build();
        NotificationManager manager=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(0,notification);
        }
        
        //周辺の人を探す
        if(helper==null){
            helper=new DatabaseHelper(FirstScreen.this);
        }
        db=helper.getReadableDatabase();
        try{
            //緯度経度から距離を計算し、3㎞圏内にいるユーザーを探す
            /*Cursor c=db.rawQuery(
                            "select user_id,laitude,longitude,(" +
                                "6371*acos(cos(radians(35))*cos(radians(laitude))*cos(radians(longitude)-radians(139))+sin(radians(35))*sin(radians(laitude))" +
                            "   )" +
                            ") as distance from location;",null);*/
            Cursor c=db.rawQuery("select user_id,laitude,longitude from location;",null);
            boolean next=c.moveToFirst();
            while (next){
                user_id=c.getInt(0);
                latitude=c.getDouble(1);
                longitude=c.getDouble(2);
                //distance=c.getDouble(3);
                tvRead.setText(user_id+" | "+latitude+" | "+longitude+" | "+distance);//該当したユーザーを表示
                next=c.moveToNext();
            }
        }finally {
            db.close();
        }
    }

    public static double deg2rad(double deg){
        return deg*Math.PI/180.0;
    }

    private void insertData(SQLiteDatabase db,int user_id,double latitude,double longitude,String date,String time){
        ContentValues values=new ContentValues();
        values.put("user_id",user_id);
        values.put("laitude",latitude);
        values.put("longitude",longitude);
        values.put("date",date);
        values.put("time",time);

        db.insert("location",null,values);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){
        //ACCESS_FINE_LOCATIONに対するパーミションダイアログでかつ許可を選択したなら…
        if(requestCode == 1000 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //LocationManagerオブジェクトを取得。
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            //位置情報が更新された際のリスナオブジェクトを生成。
            GPSLocationListener locationListener = new GPSLocationListener();
            //再度ACCESS_FINE_LOCATIONの許可が下りていないかどうかのチェックをし、降りていないなら処理を中止。
            if(ActivityCompat.checkSelfPermission(FirstScreen.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            //位置情報の追跡を開始。
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }
    }

    private class GPSLocationListener implements LocationListener{
        @Override
        public void onLocationChanged(Location location){
            latitude=location.getLatitude();
            longitude=location.getLongitude();
            tvLatitude.setText(Double.toString(latitude));
            tvLongitude.setText(Double.toString(longitude));
        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        /* API 29以降非推奨

        switch (status) {
            case LocationProvider.AVAILABLE:
                Log.d("debug", "LocationProvider.AVAILABLE");
                break;
            case LocationProvider.OUT_OF_SERVICE:
                Log.d("debug", "LocationProvider.OUT_OF_SERVICE");
                break;
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                Log.d("debug", "LocationProvider.TEMPORARILY_UNAVAILABLE");
                break;
        }
        */
        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }

    public static String getNowDate(){
        final DateFormat df=new SimpleDateFormat("yyyy/MM/dd");
        final Date date=new Date(System.currentTimeMillis());
        return df.format(date);
    }

    public static String getNowTime(){
        final DateFormat df=new SimpleDateFormat("HH:mm:ss");
        final Date time=new Date(System.currentTimeMillis());
        return df.format(time);
    }
}
