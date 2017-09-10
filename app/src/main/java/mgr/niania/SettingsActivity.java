package mgr.niania;


import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FilterQueryProvider;
import android.widget.SeekBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

/**
 * Created by Lobo on 2017-01-07.
 */

public class SettingsActivity extends Activity {

    private SeekBar seekBar;
    private TextView seekBarValue;
    private CheckBox checkBoxSms;
    private CheckBox checkBoxPhone;
    private AutoCompleteTextView contactsView;



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setContentView(R.layout.activity_settings);

        SeekBar seekBar = (SeekBar) findViewById(R.id.seekbar);
        final TextView seekBarValue = (TextView) findViewById(R.id.seekbar_value);
        CheckBox checkBoxSms = (CheckBox) findViewById(R.id.checkboxSMS);
        CheckBox checkBoxPhone = (CheckBox) findViewById(R.id.checkboxCall);
        contactsView = (AutoCompleteTextView) findViewById(R.id.telefon);

        //autoComplete();
        //getPreferences();

        //AutoCompleteTextView phoneNumber = (AutoCompleteTextView) findViewById(R.id.telefon);
        /*contactsView.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent i = new Intent(getApplicationContext(), MonitorActivity.class);
                i.putExtra("phoneNumber", Integer.parseInt(contactsView.getText().toString()));
                startActivity(i);
            }
        });
*/

        final Button settingsButton = (Button) findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                getIntents();
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                // TODO Auto-generated method stub
                seekBarValue.setText(String.valueOf(progress)); //+ "%");
              /*  Intent i = new Intent(getApplicationContext(), MonitorActivity.class);
                i.putExtra("audio", seekBarValue.getText().toString());
                startActivity(i);
              */
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }
        });

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume(){
        getPreferences();
        super.onResume();
    }

    public void onPause(){
        SeekBar seekBar = (SeekBar) findViewById(R.id.seekbar);
        AutoCompleteTextView contactsView = (AutoCompleteTextView) findViewById(R.id.telefon);
        saveState(seekBar);
        saveState(contactsView);
        super.onPause();
    }





    public void autoComplete(){

        AutoCompleteTextView actv = (AutoCompleteTextView) findViewById(R.id.telefon);
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_2, null,
                new String[] {ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER},
                new int[] {android.R.id.text1, android.R.id.text2, },
                0);
        FilterQueryProvider provider = new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence constraint) {
                if (constraint == null) {
                    return null;
                }
                return getContentResolver().query(Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI, constraint.toString()), null, null, null, null);
            }
        };
        adapter.setFilterQueryProvider(provider);
        adapter.setStringConversionColumn(0);
        actv.setAdapter(adapter);
    }

    public void saveState(View view){
        SeekBar seekBar = (SeekBar) findViewById(R.id.seekbar);
        AutoCompleteTextView contactsView = (AutoCompleteTextView) findViewById(R.id.telefon);
        CheckBox checkBoxSms = (CheckBox) findViewById(R.id.checkboxSMS);
        CheckBox checkBoxPhone = (CheckBox) findViewById(R.id.checkboxCall);
        String input = contactsView.getText().toString();
        boolean checked;
        boolean checked2;
        int seekbar = seekBar.getProgress();
        if(checkBoxSms.isChecked()){
            checked = true;
        }

        else{
            checked = false;
        }
        if(checkBoxPhone.isChecked()){
            checked2 = true;
        }

        else{
            checked2 = false;
        }

        switch(view.getId()) {
            case R.id.checkboxSMS:
                SharedPreferences sp = this.getSharedPreferences("state1", SettingsActivity.MODE_PRIVATE);
                SharedPreferences.Editor editor = sp.edit();
                editor.clear();
                editor.putBoolean("checkBoxsms", checked).apply();
                break;
            case R.id.checkboxCall:
                SharedPreferences sp2 = this.getSharedPreferences("state2", SettingsActivity.MODE_PRIVATE);
                SharedPreferences.Editor editor2 = sp2.edit();
                editor2.clear();
                editor2.putBoolean("checkBoxphone", checked2).apply();
                break;
            case R.id.telefon:
                SharedPreferences sp3 = this.getSharedPreferences("stateLong", SettingsActivity.MODE_PRIVATE);
                SharedPreferences.Editor editor3 = sp3.edit();
                editor3.clear();
                editor3.putLong("Editbox", Long.parseLong(input)).commit();
                break;
            case R.id.seekbar:
                SharedPreferences sp4 = this.getSharedPreferences("SeekbarPref", SettingsActivity.MODE_PRIVATE);
                SharedPreferences.Editor editor4 = sp4.edit();
                editor4.clear();
                editor4.putInt("Seekbar", seekbar).commit();
                break;

        }
    }

    public void getPreferences(){
        CheckBox checkBoxSms = (CheckBox) findViewById(R.id.checkboxSMS);
        CheckBox checkBoxPhone = (CheckBox) findViewById(R.id.checkboxCall);
        AutoCompleteTextView contactsView = (AutoCompleteTextView) findViewById(R.id.telefon);
        SeekBar seekBar = (SeekBar) findViewById(R.id.seekbar);
        final TextView seekBarValue = (TextView) findViewById(R.id.seekbar_value);

        SharedPreferences sp = getSharedPreferences("state1", SettingsActivity.MODE_PRIVATE);
        boolean checked = sp.getBoolean("checkBoxsms", false);
        checkBoxSms.setChecked(checked);
        SharedPreferences sp2 = getSharedPreferences("state2", SettingsActivity.MODE_PRIVATE);
        boolean checked2 = sp2.getBoolean("checkBoxphone", false);
        checkBoxPhone.setChecked(checked2);
        SharedPreferences sp3 = getSharedPreferences("stateLong", SettingsActivity.MODE_PRIVATE);
        long telefon = sp3.getLong("Editbox", 0);
        contactsView.setText(Long.toString(telefon));
        SharedPreferences sp4 = getSharedPreferences("SeekbarPref", SettingsActivity.MODE_PRIVATE);
        int seekbar = sp4.getInt("Seekbar", 50);
        seekBar.setProgress(seekbar);
        seekBarValue.setText(Integer.toString(seekbar));
    }

    public void getIntents(){

        contactsView = (AutoCompleteTextView) findViewById(R.id.telefon);
        TextView seekBarValue = (TextView) findViewById(R.id.seekbar_value);
        checkBoxSms = (CheckBox) findViewById(R.id.checkboxSMS);
        checkBoxPhone = (CheckBox) findViewById(R.id.checkboxCall);


        String phoneNumber = contactsView.getText().toString();
        Intent i = new Intent(SettingsActivity.this, MonitorActivity.class);
        i.putExtra("phoneNumber", phoneNumber);
        int audio = Integer.parseInt(seekBarValue.getText().toString());
        i.putExtra("audio", audio);
        i.putExtra("sms", checkBoxSms.isChecked());
        i.putExtra("phone", checkBoxPhone.isChecked());
        startActivity(i);



        /*Intent i = new Intent(OpcjeActivity.this, MonitorActivity.class);
        i.putExtra("phoneNumber", contactsView.getText().toString());
        startActivity(i);

        Intent i2 = new Intent(OpcjeActivity.this, MonitorActivity.class);
        i2.putExtra("audio", seekBarValue.getText().toString());
        startActivity(i2);
        */
    }

}
