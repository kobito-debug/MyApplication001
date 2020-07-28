package com.websarva.wings.android.tasukete;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
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
    int user_id=1;//仮
    private DatabaseHelper helper;
    Notification notification=null;//通知

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

       LocationManager locationManager=(LocationManager)getSystemService(LOCATION_SERVICE);
       GPSLocationListener locationListener=new GPSLocationListener();
       locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,locationListener);
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
       /* NotificationManager notificationManager=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        String chID=getString(R.string.app_name);
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){
            NotificationChannel notificationChannel = new NotificationChannel(chID, chID, NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.setDescription(chID);
            notificationManager.createNotificationChannel(notificationChannel);
            notification = new Notification.Builder(this, chID).setContentTitle(getString(R.string.app_name)).setContentText("アプリ通知テスト26以上").setSmallIcon(R.drawable.icon).build();
        }else{
            notification=new Notification.Builder(this).setContentTitle(getString(R.string.app_name))
                    .setContentText("アプリ通知テスト25まで")
                    .setSmallIcon(R.drawable.icon)
                    .build();
        }
        notificationManager.notify(1,notification);*/
    }

    public void onResisterClicked(View view){
        if(helper==null)helper=new DatabaseHelper(getApplicationContext());
        if(db==null)db=helper.getWritableDatabase();
        insertData(db,user_id,latitude,longitude,date,time);
    }
    public void onEmergencyClicked(View view){
        //周辺の人を探す
        if(helper==null){
            helper=new DatabaseHelper(FirstScreen.this);
        }
        db=helper.getWritableDatabase();
        try{
            //緯度経度から距離を計算し、3㎞圏内にいるユーザーを探す
            Cursor c=db.rawQuery(
                    " with distance_name as (" +
                            "select id,name,latitude,longitude,(" +
                                "6371*acos(cos(radians(35))*cos(radians(latitude))*cos(radians(longitude)-radians(139))+sin(radians(35))*sin(radians(latitude))" +
                            "   )" +
                            ") as distance from location " +
                            "inner join user on location.user_id=user.id) select name,distance from distance_name where distance <=3;",null);
            boolean next=c.moveToFirst();
            while (next){
                user_id=c.getInt(0);
                latitude=c.getDouble(1);
                longitude=c.getDouble(2);

                next=c.moveToNext();
            }
        }finally {
            db.close();
        }
    }

    private void insertData(SQLiteDatabase db,int user_id,double latitude,double longitude,String date,String time){
        ContentValues values=new ContentValues();
        values.put("user_id",user_id);
        values.put("latitude",latitude);
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
