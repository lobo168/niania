
package mgr.niania;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class StartActivity extends Activity
{
    final String TAG = "BabyMonitor";
    private Context myContext;
    static final int PERMISSION_ALL = 1;
    static String[] PERMISSIONS = {Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.WAKE_LOCK, Manifest.permission.CALL_PHONE, Manifest.permission.SEND_SMS};


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.i(TAG, "Baby monitor launched");
        myContext = this;
        requestPermission(hasPermissions(myContext,PERMISSIONS), PERMISSIONS);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        final Button monitorButton = (Button) findViewById(R.id.useChildDevice);
        monitorButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Log.i(TAG, "Starting up monitor");

                Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(i);
            }
        });

        final Button connectButton = (Button) findViewById(R.id.useParentDevice);
        connectButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Log.i(TAG, "Starting connection activity");

                Intent i = new Intent(getApplicationContext(), DiscoverActivity.class);
                startActivity(i);
            }
        });
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    public void requestPermission (boolean hasPermissions, String... permissions){

        if(!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }
    }
}
