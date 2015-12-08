package com.example.SurfApp;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

/**
 * author      : Jaeryn
 * course      : ICS 414 - Software Engineering 2
 * date        : 2 - December - 2015
 * description : This is an application that utilizes an ambient interface. The user enters their surf location
 *               and preferred surf height (e.g. 2-8 feet) and the application icon is changed depending on the
 *               relationship between the average height (at the location) and user input. This app will run in the
 *               background and automatically update the icon daily.
 */
public class AmbientInterface extends Activity
{
    private ImageView submit_button;
    private ImageView down_button_min;
    private ImageView up_button_min;
    private ImageView down_button_max;
    private ImageView up_button_max;

    private TextView surfRange;
    private TextView location;

    // Test purposes only -----------------------------------------------------------
    private TextView test;

    private Spinner spinner;

    private View.OnClickListener submit;
    private View.OnClickListener down_min;
    private View.OnClickListener down_max;
    private View.OnClickListener up_min;
    private View.OnClickListener up_max;

    private int min_wave = 0;
    private int max_wave = 1;

    private String state;
    private String user_location;
    public int avg_surf;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);

        initializeApp();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        Intent intent = new Intent(this, AmbientService.class);
        intent.putExtra("min_wave", min_wave);
        intent.putExtra("max_wave", max_wave);
        intent.putExtra("location", user_location);
        intent.putExtra("state", state);
        startService(intent);
    }

    /**
     * method      : initializeApp
     * description : This method initializes all the Text and Image Views for the profile/settings page of the app.
     */
    private void initializeApp()
    {
        location = (TextView) findViewById(R.id.location);
        surfRange = (TextView) findViewById(R.id.surfRange);

        // Test purposes only ------------------------------------------------------------
        test = (TextView) findViewById(R.id.test);

        // button that confirms the user's settings
        submit_button = (ImageView) findViewById(R.id.submit_button);

        // buttons that modify wave height
        up_button_min = (ImageView) findViewById(R.id.up_button_min);
        down_button_min = (ImageView) findViewById(R.id.down_button_min);

        up_button_max = (ImageView) findViewById(R.id.up_button_max);
        down_button_max = (ImageView) findViewById(R.id.down_button_max);

        spinner = (Spinner) findViewById(R.id.location_spinner);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.location_array, android.R.layout.simple_spinner_item);

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);

        // submit button code below
        submit = new View.OnClickListener() {
            public void onClick(View v)
            {
                submitted();
            }
        };

        submit_button.setOnClickListener(submit);

        // Clickable image views that modify the min surf height
        down_min = new View.OnClickListener() {
            public void onClick(View v)
            {
                if(min_wave > 0)
                {
                    min_wave--;
                    surfRange.setText(String.format("%d to %d feet", min_wave, max_wave));
                }
            }
        };

        down_button_min.setOnClickListener(down_min);

        up_min = new View.OnClickListener() {
            public void onClick(View v)
            {
                if(min_wave < max_wave - 1)
                {
                    min_wave++;
                    surfRange.setText(String.format("%d to %d feet", min_wave, max_wave));
                }
            }
        };

        up_button_min.setOnClickListener(up_min);


        // Clickable image views that modify the max surf height
        down_max = new View.OnClickListener() {
            public void onClick(View v)
            {
                if(max_wave > min_wave + 1)
                {
                    max_wave--;
                    surfRange.setText(String.format("%d to %d feet", min_wave, max_wave));
                }
            }
        };

        down_button_max.setOnClickListener(down_max);

        up_max = new View.OnClickListener() {
            public void onClick(View v)
            {
                if(max_wave < 100)
                {
                    max_wave++;
                    surfRange.setText(String.format("%d to %d feet", min_wave, max_wave));
                }
            }
        };

        up_button_max.setOnClickListener(up_max);
    }

    /**
     * method      : submitted
     * description : This method calls the execute method from FetchWebsiteData which determines the average
     *               surf height and assigns it to the public variable avg_surf (int). Then, the application
     *               icon will be modified based on the relationship between avg_surf and user input.
     *               This method is invoked when the user clicks on the submit button.
     */
    private void submitted()
    {
        String package_name = getPackageName();

        user_location = spinner.getSelectedItem().toString();

        new FetchWebsiteData().execute();

        // For testing purposes only ------------------------------------------------
        test.setText(String.format("%d", avg_surf));

        if(avg_surf < min_wave)
        {
            state = ".AmbientInterface-red";

            getPackageManager().setComponentEnabledSetting(
                    new ComponentName(package_name, package_name + state),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

            getPackageManager().setComponentEnabledSetting(
                    new ComponentName(package_name, package_name + ".AmbientInterface-yellow"),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

            getPackageManager().setComponentEnabledSetting(
                    new ComponentName(package_name, package_name + ".AmbientInterface-green"),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }
        else if(avg_surf > max_wave)
        {
            state = ".AmbientInterface-yellow";

            getPackageManager().setComponentEnabledSetting(
                    new ComponentName(package_name, package_name + state),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

            getPackageManager().setComponentEnabledSetting(
                    new ComponentName(package_name, package_name + ".AmbientInterface-red"),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

            getPackageManager().setComponentEnabledSetting(
                    new ComponentName(package_name, package_name + ".AmbientInterface-green"),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }
        else if (avg_surf >= min_wave && avg_surf <= max_wave)
        {
            state = ".AmbientInterface-green";

            getPackageManager().setComponentEnabledSetting(
                    new ComponentName(package_name, package_name + state),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

            getPackageManager().setComponentEnabledSetting(
                    new ComponentName(package_name, package_name + ".AmbientInterface-red"),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

            getPackageManager().setComponentEnabledSetting(
                    new ComponentName(package_name, package_name + ".AmbientInterface-yellow"),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }
    }

    /**
     * class       : FetchWebsiteData
     * extends     : AsyncTask
     * description : This class extends AsyncTask and overrides the doInBackground method. Using the Jsoup library,
     *               it connects to the URL based on user location and sets avg_surf (average surf).
     */
    private class FetchWebsiteData extends AsyncTask<Void, Void, Void>
    {
        String str;
        int[] heights;
        int heightsCount;

        @Override
        protected Void doInBackground(Void... params)
        {
            try
            {
                Document document = Jsoup.connect("http://www.prh.noaa.gov/hnl/pages/SRF.php").get();
                Elements e = document.select("p");

                for(Element height : e)
                {
                    if(height.toString().contains("Surf along " + user_location))
                    {
                        str = height.toString();
                        String[] arr = str.split("[ \t\n]+");
                        heights = new int[arr.length];

                        heightsCount = 0;
                        for(int j = 0; j < arr.length-1; j++)
                        {
                            if(arr[j].matches("-?\\d+(\\.\\d+)?"))
                            {
                                heights[heightsCount] = Integer.parseInt(arr[j]);
                                heightsCount++;
                            }
                        }

                        avg_surf = (heights[0] + heights[1]) / 2;
                    }
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            return null;
        }
    }
}

