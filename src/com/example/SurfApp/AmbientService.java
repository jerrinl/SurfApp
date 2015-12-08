package com.example.SurfApp;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;


/**
 * Created by Q on 05-Dec-15.
 */
public class AmbientService extends Service
{
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Bundle extras = intent.getExtras();

        final String location = (String) extras.get("location");
        final String state = (String) extras.get("state");
        final int min_wave = (int) extras.get("min_wave");
        final int max_wave = (int) extras.get("max_wave");

        new Thread()
        {
            public void run()
            {
                findInBackground(min_wave, max_wave, location, state);
            }
        }.start();

        return START_NOT_STICKY;
    }

    private void findInBackground(int min_wave, int max_wave, String location, String initial_state)
    {
        String timeStamp = new SimpleDateFormat("HHmm").format(new Date());
        String package_name = getPackageName();
        String str;
        String state = initial_state;

        int[] heights;
        int heightsCount;
        int avg_surf = 0;
        int hour = Integer.parseInt(timeStamp.substring(0, 2));
        int offsetHour = (hour == 0) ? 11 : (hour < 12) ? 11 - hour : 11 - (hour % 12);
        int offsetMin = 60 - Integer.parseInt(timeStamp.substring(2));

        //TimeUnit.MINUTES.sleep(offsetMin + offsetHour*60);

        while(true)
        {
            try
            {
                Log.d("test", "before wait " + Integer.toString(offsetHour) + " " + Integer.toString(offsetMin));
                TimeUnit.MINUTES.sleep(1);
                Log.d("test", "after wait");

                Document document = Jsoup.connect("http://www.prh.noaa.gov/hnl/pages/SRF.php").get();
                Elements e = document.select("p");

                for(Element height : e)
                {
                    if(height.toString().contains("Surf along " + location))
                    {
                        str = height.toString();
                        String[] arr = str.split("[ \t\n]+");
                        heights = new int[arr.length];
                        heightsCount = 0;

                        for(int j = 0; j < arr.length-1; j++ )
                        {
                            if(arr[j].matches("-?\\d+(\\.\\d+)?"))
                            {
                                heights[heightsCount] = Integer.parseInt(arr[j]);
                                heightsCount++;
                            }
                        }

                        avg_surf = (heights[0] + heights[1]) / 2;

                        Log.d("test", Integer.toString(heights[0]));
                        Log.d("test", Integer.toString(heights[1]));
                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            if(avg_surf < min_wave && !state.equals(".AmbientInterface-red"))
            {
                getPackageManager().setComponentEnabledSetting(
                        new ComponentName(package_name, package_name + ".AmbientInterface-red"),
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

                getPackageManager().setComponentEnabledSetting(
                        new ComponentName(package_name, package_name + state),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

                state = ".AmbientInterface-red";
            }
            else if(avg_surf > max_wave && !state.equals(".AmbientInterface-yellow"))
            {
                getPackageManager().setComponentEnabledSetting(
                        new ComponentName(package_name, package_name + ".AmbientInterface-yellow"),
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

                getPackageManager().setComponentEnabledSetting(
                        new ComponentName(package_name, package_name + state),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

                state = ".AmbientInterface-yellow";
            }
            else if (avg_surf >= min_wave && avg_surf <= max_wave && !state.equals(".AmbientInterface-green"))
            {
                getPackageManager().setComponentEnabledSetting(
                        new ComponentName(package_name, package_name + ".AmbientInterface-green"),
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

                getPackageManager().setComponentEnabledSetting(
                        new ComponentName(package_name, package_name + state),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

                state = ".AmbientInterface-green";
            }

            //TimeUnit.MINUTES.sleep(720);
        }
    }
}
