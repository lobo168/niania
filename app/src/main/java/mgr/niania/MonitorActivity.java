
package mgr.niania;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.telephony.gsm.SmsManager;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import org.apache.http.conn.util.InetAddressUtils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class MonitorActivity extends Activity
        implements SurfaceHolder.Callback
{
    //
    private static final String TAG = MonitorActivity.class.getSimpleName();

    private static final String WAKE_LOCK_TAG = "peepers";

    private static final String PREF_CAMERA = "camera";
    private static final int PREF_CAMERA_INDEX_DEF = 0;
    private static final String PREF_FLASH_LIGHT = "flash_light";
    private static final boolean PREF_FLASH_LIGHT_DEF = false;
    private static final String PREF_PORT = "port";
    private static final int PREF_PORT_DEF = 8080;
    private static final String PREF_JPEG_SIZE = "size";
    private static final String PREF_JPEG_QUALITY = "jpeg_quality";
    private static final int PREF_JPEG_QUALITY_DEF = 40;
    // preview sizes will always have at least one element, so this is safe
    private static final int PREF_PREVIEW_SIZE_INDEX_DEF = 0;

    private boolean mRunning = false;
    private boolean mPreviewDisplayCreated = false;
    private SurfaceHolder mPreviewDisplay = null;
    private VideoStreamer mVideoStreamer = null;

    private String mIpAddress = "";
    private int mCameraIndex = PREF_CAMERA_INDEX_DEF;
    private boolean mUseFlashLight = PREF_FLASH_LIGHT_DEF;
    private int mPort = PREF_PORT_DEF;
    private int mJpegQuality = PREF_JPEG_QUALITY_DEF;
    private int mPrevieSizeIndex = PREF_PREVIEW_SIZE_INDEX_DEF;
    private TextView mIpAddressView = null;
    private MonitorActivity.LoadPreferencesTask mLoadPreferencesTask = null;
    private SharedPreferences mPrefs = null;
    private MenuItem mSettingsMenuItem = null;
    private PowerManager.WakeLock mWakeLock = null;

    //Audio meter
    private final double MAX_REPORTABLE_AMP = 32767;
    private final double MAX_REPORTABLE_DB = 90.3087;
    private String phoneNumber = "";
    private int audio = 0;
    private boolean sms = false;
    private boolean phone = false;
    ProgressBar pb;
    private int amplitude = 0;
    private static int stepHigh = 0;


    public MonitorActivity()
    {
        super();
    } // constructor()

    //
    //final String TAG = "BabyMonitor";

    NsdManager _nsdManager;

    NsdManager.RegistrationListener _registrationListener;

    Thread _serviceThread;

    private void serviceConnection(Socket socket) throws IOException
    {
        MonitorActivity.this.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                final TextView statusText = (TextView) findViewById(R.id.textStatus);
                statusText.setText(R.string.streaming);
            }
        });

        final int frequency = 11025;
        final int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
        final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

        final int bufferSize = AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding);
        final AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                frequency, channelConfiguration,
                audioEncoding, bufferSize);

        final int byteBufferSize = bufferSize*2;
        final byte[] buffer = new byte[byteBufferSize];

        try
        {
            audioRecord.startRecording();

            final OutputStream out = socket.getOutputStream();

            socket.setSendBufferSize(byteBufferSize);
            Log.d(TAG, "Socket send buffer size: " + socket.getSendBufferSize());

            while (socket.isConnected() && Thread.currentThread().isInterrupted() == false)
            {
                double sum = 0;
                final int read = audioRecord.read(buffer, 0, bufferSize);
                out.write(buffer, 0, read);

                for (int i = 0; i < read; i++) {
                    //sum += buffer [i] * buffer [i];
                    //sum += Math.abs(buffer[i]);
					sum += buffer [i] * buffer [i];
                }

                if (read > 0) {
                    final double amplitude = sum / read;
					pb.setProgress((int) Math.sqrt(amplitude)*100);
                    //pb.setProgress((int) (MAX_REPORTABLE_DB + (20 * Math.log10(amplitude / MAX_REPORTABLE_AMP)))*100);
                    getAudioAmplitude();
                }
            }
        }
        finally
        {
            audioRecord.stop();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.i(TAG, "Baby monitor start");

        _nsdManager = (NsdManager)this.getSystemService(Context.NSD_SERVICE);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitor);

        //intenty
        pb = (ProgressBar) findViewById(R.id.progressBar);
        final Bundle b = getIntent().getExtras();
        phoneNumber = b.getString("phoneNumber");
        audio = b.getInt("audio");
        phone = b.getBoolean("phone");
        sms = b.getBoolean("sms");
        amplitude = pb.getProgress();
        //

        new MonitorActivity.LoadPreferencesTask().execute();

        mPreviewDisplay = ((SurfaceView) findViewById(R.id.camera)).getHolder();
        mPreviewDisplay.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mPreviewDisplay.addCallback(this);

        mIpAddress = tryGetIpV4Address();
        mIpAddressView = (TextView) findViewById(R.id.ip_address);
        updatePrefCacheAndUi();

        final PowerManager powerManager =
                (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
                WAKE_LOCK_TAG);
        //

        _serviceThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                while(Thread.currentThread().isInterrupted() == false)
                {
                    ServerSocket serverSocket = null;

                    try
                    {
                        // Initialize a server socket on the next available port.
                        serverSocket = new ServerSocket(0);

                        // Store the chosen port.
                        final int localPort = serverSocket.getLocalPort();

                        // Register the service so that parent devices can
                        // locate the child device
                        registerService(localPort);

                        // Wait for a parent to find us and connect
                        Socket socket = serverSocket.accept();
                        Log.i(TAG, "Connection from parent device received");

                        // We now have a client connection.
                        // Unregister so no other clients will
                        // attempt to connect
                        serverSocket.close();
                        serverSocket = null;
                        unregisterService();

                        try
                        {
                            serviceConnection(socket);
                        }
                        finally
                        {
                            socket.close();
                        }
                    }
                    catch(IOException e)
                    {
                        Log.e(TAG, "Connection failed", e);
                    }

                    // If an exception was thrown before the connection
                    // could be closed, clean it up
                    if(serverSocket != null)
                    {
                        try
                        {
                            serverSocket.close();
                        }
                        catch (IOException e)
                        {
                            Log.e(TAG, "Failed to close stray connection", e);
                        }
                        serverSocket = null;
                    }
                }
            }
        });
        _serviceThread.start();

        MonitorActivity.this.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                final TextView addressText = (TextView) findViewById(R.id.address);

                final WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
                final WifiInfo info = wifiManager.getConnectionInfo();
                final int address = info.getIpAddress();
                if(address != 0)
                {
                    @SuppressWarnings("deprecation")
                    final String ipAddress = Formatter.formatIpAddress(address);
                    addressText.setText(ipAddress);
                }
                else
                {
                    addressText.setText(R.string.wifiNotConnected);
                }
            }
        });
    }

    @Override
    protected void onDestroy()
    {
        Log.i(TAG, "Baby monitor stop");

        unregisterService();

        if(_serviceThread != null)
        {
            _serviceThread.interrupt();
            _serviceThread = null;
        }

        super.onDestroy();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        mRunning = true;
        if (mPrefs != null)
        {
            mPrefs.registerOnSharedPreferenceChangeListener(
                    mSharedPreferenceListener);
        } // if
        updatePrefCacheAndUi();
        tryStartCameraStreamer();
        mWakeLock.acquire();
    } // onResume()

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {super.onCreateOptionsMenu(menu);
        mSettingsMenuItem = menu.add(R.string.settings);
        mSettingsMenuItem.setIcon(android.R.drawable.ic_menu_manage);
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.start, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {

        if (item != mSettingsMenuItem)
        {
            return super.onOptionsItemSelected(item);
        } // if
        startActivity(new Intent(this, CameraPreferenceActivity.class));
        // sprawdzić to return true;
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void registerService(final int port)
    {
        final NsdServiceInfo serviceInfo  = new NsdServiceInfo();
        serviceInfo.setServiceName("Niania");
        serviceInfo.setServiceType("_babymonitor._tcp.");
        serviceInfo.setPort(port);

        _registrationListener = new NsdManager.RegistrationListener()
        {
            @Override
            public void onServiceRegistered(NsdServiceInfo nsdServiceInfo) {
                // Save the service name.  Android may have changed it in order to
                // resolve a conflict, so update the name you initially requested
                // with the name Android actually used.
                final String serviceName = nsdServiceInfo.getServiceName();

                Log.i(TAG, "Service name: " + serviceName);

                MonitorActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run()
                    {
                        final TextView statusText = (TextView) findViewById(R.id.textStatus);
                        statusText.setText(R.string.waitingForParent);

                        final TextView serviceText = (TextView) findViewById(R.id.textService);
                        serviceText.setText(serviceName);

                        final TextView portText = (TextView) findViewById(R.id.port);
                        portText.setText(Integer.toString(port));
                    }
                });
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode)
            {
                // Registration failed!  Put debugging code here to determine why.
                Log.e(TAG, "Registration failed: " + errorCode);
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0)
            {
                // Service has been unregistered.  This only happens when you call
                // NsdManager.unregisterService() and pass in this listener.

                Log.i(TAG, "Unregistering service");
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode)
            {
                // Unregistration failed.  Put debugging code here to determine why.

                Log.e(TAG, "Unregistration failed: " + errorCode);
            }
        };

        _nsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, _registrationListener);
    }

    /**
     * Uhregistered the service and assigns the listener
     * to null.
     */
    private void unregisterService()
    {
        if(_registrationListener != null)
        {
            Log.i(TAG, "Unregistering monitoring service");

            _nsdManager.unregisterService(_registrationListener);
            _registrationListener = null;
        }
    }
    /////

    protected void onPause()
    {
        mWakeLock.release();
        super.onPause();
        mRunning = false;
        if (mPrefs != null)
        {
            mPrefs.unregisterOnSharedPreferenceChangeListener(
                    mSharedPreferenceListener);
        } // if
        ensureCameraStreamerStopped();
    } // onPause()

    @Override
    public void surfaceChanged(final SurfaceHolder holder, final int format,
                               final int width, final int height)
    {
        // Ingored
    } // surfaceChanged(SurfaceHolder, int, int, int)

    @Override
    public void surfaceCreated(final SurfaceHolder holder)
    {
        mPreviewDisplayCreated = true;

        tryStartCameraStreamer();
    } // surfaceCreated(SurfaceHolder)

    @Override
    public void surfaceDestroyed(final SurfaceHolder holder)
    {
        mPreviewDisplayCreated = false;
        ensureCameraStreamerStopped();
    } // surfaceDestroyed(SurfaceHolder)

    private void tryStartCameraStreamer()
    {
        if (mRunning && mPreviewDisplayCreated && mPrefs != null)
        {
            mVideoStreamer = new VideoStreamer (mCameraIndex, mUseFlashLight, mPort,
                    mPrevieSizeIndex, mJpegQuality, mPreviewDisplay);
            mVideoStreamer.start();
        } // if
    } // tryStartCameraStreamer()

    private void ensureCameraStreamerStopped()
    {
        if (mVideoStreamer != null)
        {
            mVideoStreamer.stop();
            mVideoStreamer = null;
        } // if
    } // stopCameraStreamer()


    private final class LoadPreferencesTask
            extends AsyncTask<Void, Void, SharedPreferences>
    {
        private LoadPreferencesTask()
        {
            super();
        } // constructor()

        @Override
        protected SharedPreferences doInBackground(final Void... noParams)
        {
            return PreferenceManager.getDefaultSharedPreferences(
                    MonitorActivity.this);
        } // doInBackground()

        @Override
        protected void onPostExecute(final SharedPreferences prefs)
        {
            MonitorActivity.this.mPrefs = prefs;
            prefs.registerOnSharedPreferenceChangeListener(
                    mSharedPreferenceListener);
            updatePrefCacheAndUi();
            tryStartCameraStreamer();
        } // onPostExecute(SharedPreferences)


    }

    private final SharedPreferences.OnSharedPreferenceChangeListener mSharedPreferenceListener =
            new SharedPreferences.OnSharedPreferenceChangeListener()
            {
                @Override
                public void onSharedPreferenceChanged(final SharedPreferences prefs,
                                                      final String key)
                {
                    updatePrefCacheAndUi();
                } // onSharedPreferenceChanged(SharedPreferences, String)

            }; // mSharedPreferencesListener

    private final int getPrefInt(final String key, final int defValue)
    {
        // We can't just call getInt because the preference activity
        // saves everything as a string.
        try
        {
            return Integer.parseInt(mPrefs.getString(key, null /* defValue */));
        } // try
        catch (final NullPointerException e)
        {
            return defValue;
        } // catch
        catch (final NumberFormatException e)
        {
            return defValue;
        } // catch
    } // getPrefInt(String, int)

    private final void updatePrefCacheAndUi()
    {
        mCameraIndex = getPrefInt(PREF_CAMERA, PREF_CAMERA_INDEX_DEF);
        if (hasFlashLight())
        {
            if (mPrefs != null)
            {
                mUseFlashLight = mPrefs.getBoolean(PREF_FLASH_LIGHT,
                        PREF_FLASH_LIGHT_DEF);
            } // if
            else
            {
                mUseFlashLight = PREF_FLASH_LIGHT_DEF;
            } // else
        } //if
        else
        {
            mUseFlashLight = false;
        } // else

        // XXX: This validation should really be in the preferences activity.
        mPort = getPrefInt(PREF_PORT, PREF_PORT_DEF);
        // The port must be in the range [1024 65535]
        if (mPort < 1024)
        {
            mPort = 1024;
        } // if
        else if (mPort > 65535)
        {
            mPort = 65535;
        } // else if

        mPrevieSizeIndex = getPrefInt(PREF_JPEG_SIZE, PREF_PREVIEW_SIZE_INDEX_DEF);
        mJpegQuality = getPrefInt(PREF_JPEG_QUALITY, PREF_JPEG_QUALITY_DEF);
        // The JPEG quality must be in the range [0 100]
        if (mJpegQuality < 0)
        {
            mJpegQuality = 0;
        } // if
        else if (mJpegQuality > 100)
        {
            mJpegQuality = 100;
        } // else if
        mIpAddressView.setText("http://" + mIpAddress + ":" + mPort + "/");
    } // updatePrefCacheAndUi()

    private boolean hasFlashLight()
    {
        return getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA_FLASH);
    } // hasFlashLight()

    private static String tryGetIpV4Address()
    {
        try
        {
            final Enumeration<NetworkInterface> en =
                    NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements())
            {
                final NetworkInterface intf = en.nextElement();
                final Enumeration<InetAddress> enumIpAddr =
                        intf.getInetAddresses();
                while (enumIpAddr.hasMoreElements())
                {
                    final InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress())
                    {
                        final String addr = inetAddress.getHostAddress().toUpperCase();
                        if (InetAddressUtils.isIPv4Address(addr))
                        {
                            return addr;
                        }
                    } // if
                } // while
            } // for
        } // try
        catch (final Exception e)
        {
            // Ignore
        } // catch
        return null;
    } // tryGetIpV4Address()


    private void getAudioAmplitude() {

        amplitude = pb.getProgress();
        int stepLow = 0;
        String smsTxt = "Wykryty hałas w pomieszczeniu!";

        if(phone == true){
            if (audio <=50 && amplitude >= 3000){
                stepHigh ++;
                boolean isEven = ((stepHigh % 5) == 0);
                            if(isEven == true) {
                                Intent intentCall = new Intent(Intent.ACTION_CALL);
                                intentCall.setData(Uri.parse("tel:+48" + phoneNumber));
                                if (ActivityCompat.checkSelfPermission(MonitorActivity.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                    Toast.makeText(MonitorActivity.this, "No permission to call!", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                MonitorActivity.this.startActivity(intentCall);

                                //stepHigh = 0;
                            }
            }//if zagniezdzone

            else if (audio >=51 && amplitude >=2300){
                Intent intentCall = new Intent(Intent.ACTION_CALL);
                intentCall.setData(Uri.parse("tel:+48" + phoneNumber));
                if (ActivityCompat.checkSelfPermission(MonitorActivity.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MonitorActivity.this, "No permission to call!", Toast.LENGTH_SHORT).show();
                    return;
                }
                MonitorActivity.this.startActivity(intentCall);
            }//else if

        }//if1

        if (sms == true) {

            if(audio <=50 && amplitude >= 3000){
                try {
                    android.telephony.SmsManager smsManager = android.telephony.SmsManager.getDefault();
                    smsManager.sendTextMessage(phoneNumber, null, smsTxt, null, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }//if zagniezdzone

            else if (audio >=51 && amplitude >=2300){
                try {
                    android.telephony.SmsManager smsManager = android.telephony.SmsManager.getDefault();
                    smsManager.sendTextMessage(phoneNumber, null, smsTxt, null, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }//else if

        } //if1

        else {

            //nic nie rob
        }//else

    }//audioMeter funcja




}
