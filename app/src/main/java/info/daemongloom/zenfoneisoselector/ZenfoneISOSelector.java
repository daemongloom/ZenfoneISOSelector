package info.daemongloom.zenfoneisoselector;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import net.rdrei.android.dirchooser.DirectoryChooserActivity;
import net.rdrei.android.dirchooser.DirectoryChooserConfig;

import java.io.File;
import java.util.ArrayList;

import eu.chainfire.libsuperuser.Shell;

/*
supolicy --live "allow shell block_device dir search"
supolicy --live "allow shell labeldfs filesystem remount"
supolicy --live "allow shell block_device dir search"
supolicy --live "allow shell usb_device dir read"
supolicy --live "allow shell capability shell sys_admin"
supolicy --live "allow shell block_device blk_file { read open ioctl rename}"
supolicy --live "allow shell labeledfs filesystem { mount remount }"
supolicy --live "allow shell shell capability sys_admin"
supolicy --live "allow init system_file:file {rename append}"
supolicy --live "allow shell proc_cpuinfo:file {mounton}"
supolicy --live "allow shell dalvikcache_data_file:file {write}"
*/
@SuppressWarnings("ALL")
public class ZenfoneISOSelector extends AppCompatActivity {
    //id for DirectoryChooser Activity
    int REQUEST_DIRECTORY = 8324;
    //Original cdrom file path on Asus Zenfone 2
    String originalCdrom= "/system/etc/cdrom_install.iso";

    @Override
    public void onBackPressed(){
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zenfone_isoselector);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (!Shell.SU.available()){
            Toast.makeText(getApplicationContext(), "SU is not available, please root your phone.", Toast.LENGTH_LONG).show();
        };

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, getResources().getString(R.string.reset_default), Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                applyNewImage(originalCdrom);
            }
        });

        ListView listView1 = (ListView) findViewById(R.id.listView);
        listView1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                String product = ((TextView) view).getText().toString();
                applyNewImage(product);
            }
        });
        SharedPreferences sharedPref = getSharedPreferences("zenfoneisoselector",0);
        String directory = sharedPref.getString("directory","");
        if (directory != ""){
            fillData(directory);
        }
        else{
            fillData("/sdcard/");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_zenfone_isoselector, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            final Intent chooserIntent = new Intent(this, DirectoryChooserActivity.class);
            SharedPreferences sharedPref = getSharedPreferences("zenfoneisoselector",0);
            String directory = sharedPref.getString("directory","");
            final DirectoryChooserConfig config = DirectoryChooserConfig.builder()
                    .newDirectoryName("New folder")
                    .allowReadOnlyDirectory(true)
                    .allowNewDirectoryNameModification(true)
                    .initialDirectory(directory)
                    .build();
            chooserIntent.putExtra(DirectoryChooserActivity.EXTRA_CONFIG, config);
            startActivityForResult(chooserIntent, REQUEST_DIRECTORY);
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData){
        if (requestCode == REQUEST_DIRECTORY) {
            if (resultCode == DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED) {
                SharedPreferences sharedPref = getSharedPreferences("zenfoneisoselector", 0);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("directory",resultData.getStringExtra(DirectoryChooserActivity.RESULT_SELECTED_DIR));
                editor.commit();
                fillData(resultData.getStringExtra(DirectoryChooserActivity.RESULT_SELECTED_DIR));
            }
        }
        super.onActivityResult(requestCode,resultCode,resultData);
    }
    public void applyNewImage(String path){
        (new SUTask()).execute(path);
    }
    public void fillData(String directory){
        File dir = new File(directory);
        File[] filelist = dir.listFiles();
        if (filelist != null) {
            ArrayList<String> ListOfFileNames = new ArrayList<String>();
            for (File file : filelist)
            {
                if (MimeTypeMap.getFileExtensionFromUrl(file.getName().toLowerCase()).equals("iso")) {
                    //ListOfFileNames.add(file.getName());
                    ListOfFileNames.add(file.getPath());
                }
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, ListOfFileNames);
            ListView listView1 = (ListView) findViewById(R.id.listView);
            listView1.setAdapter(adapter);

        } else {
            ListView listView1 = (ListView) findViewById(R.id.listView);
            listView1.setAdapter(null);
        }
    }
    private class SUTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... path) {
            // this method is executed in a background thread
            // no problem calling su here

            Shell.SU.run("mount -o remount,rw /system");
            //Let's clear current cdrom settings
            Shell.SU.run("sed -i -e 's#cdromname=.*#cdromname=\"NotExisting\"#g' /system/etc/init.cdrom.sh");
            Shell.SU.run("sh /system/etc/init.cdrom.sh");
            //And apply new one.
            Shell.SU.run("sed -i -e 's#cdromname=.*#cdromname=\""+path[0]+"\"#g' /system/etc/init.cdrom.sh");
            Shell.SU.run("sh /system/etc/init.cdrom.sh");
            Shell.SU.run("mount -o remount,ro /system");
            return null;
        }
    }
}
