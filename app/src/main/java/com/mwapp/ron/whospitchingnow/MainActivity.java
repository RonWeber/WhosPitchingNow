package com.mwapp.ron.whospitchingnow;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    public static final String TEAM_ID = "cin"; //If we expand to multiple teams, I'll need to change this.

    private class MakeMlbInformation extends AsyncTask<String, Void, MlbInformation> {

        @Override
        protected MlbInformation doInBackground(String... team_id) {
            try
            {
                return new MlbInformation(team_id[0]);
            }
            catch (Exception e)
            {
                return null;
            }
        }

        @Override
        protected void onPostExecute(MlbInformation result)
        {
            if (result == null)
            {
                Toast toast = Toast.makeText(getApplicationContext(), "Request Failed", Toast.LENGTH_LONG);
                toast.show();
            }
            else
            {
                TextView name = (TextView)findViewById(R.id.playerName);
                name.setText(result.getPitcherName());
                TextView era = (TextView)findViewById(R.id.era);
                era.setText("ERA: " + result.getERA());
                TextView other = (TextView)findViewById(R.id.otherStats);
                other.setText(result.getOtherStatlist());
                Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
                toolbar.setTitle("Vs. " + result.getOpposingTeam());

                LoadPicture loadPicture = new LoadPicture();
                loadPicture.execute("http://mlb.mlb.com/mlb/images/players/head_shot/" + result.getID() + ".jpg");
            }
            if (progress != null) progress.dismiss();
        }
    }

    private  class LoadPicture extends AsyncTask<String, Void, Drawable> {
        @Override
        protected Drawable doInBackground(String... fileName)
        {
            try {
                URL url = new URL(fileName[0]);
                InputStream in = url.openStream();
                BufferedInputStream stream = new BufferedInputStream(in);
                Bitmap bitmap = BitmapFactory.decodeStream(stream);
                return new BitmapDrawable(getResources(), bitmap);

            } catch (Exception e) {
                //The infamous exception nonhandler.  If we can't get a picture, bugging the user about it would only be annoying.
                return null;
            }

        }

        @Override
        protected void onPostExecute(Drawable result)
        {
            if (result != null) {
                ImageView batterPicture = (ImageView) findViewById(R.id.batterPicture);
                batterPicture.setImageDrawable(result);
            }
        }
    }

    private ProgressDialog progress;
    private MakeMlbInformation task;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    protected void onStart() {
        super.onStart();

        progress = ProgressDialog.show(this, "Loading...", "Please Wait", true, false);

        task = new MakeMlbInformation();
        task.execute(TEAM_ID);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_refresh)
        {
            if (task != null ) task.cancel(true);
            progress = ProgressDialog.show(this, "Loading...", "Please Wait", true, false);

            task = new MakeMlbInformation();
            task.execute(TEAM_ID);
        }

        return super.onOptionsItemSelected(item);
    }
}
