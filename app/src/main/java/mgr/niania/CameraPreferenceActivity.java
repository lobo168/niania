
package mgr.niania;

import android.hardware.Camera;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

import java.util.List;


public final class CameraPreferenceActivity extends PreferenceActivity
{
    public CameraPreferenceActivity()
    {
        super();
    } // constructor()

    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        // Camera preference
        final ListPreference cameraPreference = (ListPreference) findPreference("camera");

        setCameraPreferences(cameraPreference);

        cameraPreference.setOnPreferenceClickListener(new OnPreferenceClickListener(){
            @Override
            public boolean onPreferenceClick(Preference preference) {
                setCameraPreferences(cameraPreference);
                return false;
            }
        });

        // JPEG size preference
        final ListPreference sizePreference = (ListPreference) findPreference("size");

        setSizePreferences(sizePreference, cameraPreference);

        sizePreference.setOnPreferenceClickListener(new OnPreferenceClickListener(){
            @Override
            public boolean onPreferenceClick(Preference preference) {
                setSizePreferences(sizePreference, cameraPreference);
                return false;
            }
        });
    } // onCreate(Bundle)

    private void setCameraPreferences(final ListPreference cameraPreference)
    {
        final int numberOfCameras = Camera.getNumberOfCameras();
        final CharSequence[] entries = new CharSequence[numberOfCameras];
        final CharSequence[] entryValues = new CharSequence[numberOfCameras];
        final Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int cameraIndex = 0; cameraIndex < numberOfCameras; cameraIndex++)
        {
            Camera.getCameraInfo(cameraIndex, cameraInfo);
            String cameraFacing;
            switch (cameraInfo.facing)
            {
                case Camera.CameraInfo.CAMERA_FACING_BACK:
                    cameraFacing = "back";
                    break;
                case Camera.CameraInfo.CAMERA_FACING_FRONT:
                    cameraFacing = "front";
                    break;
                default:
                    cameraFacing = "unknown";
            } // switch

            entries[cameraIndex] = "Camara " + cameraIndex + " " + cameraFacing;
            entryValues[cameraIndex] = String.valueOf(cameraIndex);
        } //for

        cameraPreference.setEntries(entries);
        cameraPreference.setEntryValues(entryValues);

    } // setCameraPreferences(ListPreference)

    private void setSizePreferences(final ListPreference sizePreference,
                                    final ListPreference cameraPreference)
    {
        final String cameraPreferenceValue = cameraPreference.getValue();
        final int cameraIndex;
        if (cameraPreferenceValue != null)
        {
            cameraIndex = Integer.parseInt(cameraPreferenceValue);
        } // if
        else
        {
            cameraIndex = 0;
        } // else
        final Camera camera = Camera.open(cameraIndex);
        camera.setDisplayOrientation(90);
        final Camera.Parameters params = camera.getParameters();
        camera.release();

        final List<Camera.Size> supportedPreviewSizes = params.getSupportedPreviewSizes();
        CharSequence[] entries = new CharSequence[supportedPreviewSizes.size()];
        CharSequence[] entryValues = new CharSequence[supportedPreviewSizes.size()];
        for (int previewSizeIndex = 0; previewSizeIndex < supportedPreviewSizes.size();
             previewSizeIndex++)
        {
            Camera.Size supportedPreviewSize = supportedPreviewSizes.get(previewSizeIndex);
            entries[previewSizeIndex] = supportedPreviewSize.width + "x"
                                        + supportedPreviewSize.height;
            entryValues[previewSizeIndex] = String.valueOf(previewSizeIndex);
        } // for

        sizePreference.setEntries(entries);
        sizePreference.setEntryValues(entryValues);

    } // setSizePreferenceData(ListPreference)

} // class PeepersPreferenceActivity

