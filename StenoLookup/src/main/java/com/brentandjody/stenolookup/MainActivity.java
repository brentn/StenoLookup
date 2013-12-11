package com.brentandjody.stenolookup;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Queue;

public class MainActivity extends Activity implements Dictionary.OnDictionaryLoadedListener, TextWatcher{

    private static final String TAG = "StenoLookup";
    private static final int FILE_SELECT_CODE = 0;

    private StenoLookupApplication App;
    private Dictionary mDictionary;
    private SharedPreferences prefs;
    private EditText input;
    private TextView output;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App = ((StenoLookupApplication) getApplication());
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        setContentView(R.layout.activity_main);
        mDictionary=App.getDictionary();
        mDictionary.setOnDictionaryLoadedListener(this);
        loadDictionary();
        output = (TextView) findViewById(R.id.output);
        input = (EditText) findViewById(R.id.input);
        input.addTextChangedListener(this);
        ((Button) findViewById(R.id.clear_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearInput();
                clearOutput();
            }
        });
        (findViewById(R.id.menu_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDictionaryFilename();
            }
        });
    }



    @Override
    public void onDictionaryLoaded() {
        unlockInput();
    }

    private void loadDictionary() {
        if (mDictionary.size() == 0 ) {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                String filename = prefs.getString(App.KEY_DICTIONARY_FILE, "");
                if (filename.isEmpty()) {
                    getDictionaryFilename();
                } else {
                    Log.d(TAG, "Storage path: "+  filename);
                    File file = new File(filename);
                    if (file.exists()) {
                        findViewById(R.id.overlay).setVisibility(View.VISIBLE);
                        findViewById(R.id.input).setVisibility(View.INVISIBLE);
                        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
                        progressBar.setProgress(0);
                        int size = prefs.getInt(StenoLookupApplication.KEY_DICTIONARY_SIZE, 100000);
                        mDictionary.load(filename, progressBar, size);
                    } else {

                        prefs.edit().putString(App.KEY_DICTIONARY_FILE, "").commit();
                        throw new RuntimeException("File not found");
                    }

                }
            } else {
                Toast.makeText(this, "Dictionary media not mounted", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "SD Card not mounted, cannot load dictionary");
            }
        } else {
            unlockInput();
        }
    }

    private void getDictionaryFilename() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a File to Upload"),
                    FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Please install a File Manager.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
        case FILE_SELECT_CODE:
        if (resultCode == RESULT_OK) {
            // Get the Uri of the selected file
            Uri uri = data.getData();
            Log.d(TAG, "File Uri: " + uri.toString());
            // Get the path
            String path = getPath(this, uri);
            Log.d(TAG, "File Path: " + path);
            prefs.edit().putString(App.KEY_DICTIONARY_FILE, path).commit();
            loadDictionary();
        }
        break;
    }
    super.onActivityResult(requestCode, resultCode, data);
}

    public static String getPath(Context context, Uri uri) {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = { "_data" };
            Cursor cursor = null;

            try {
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
                // Eat it
            }
        }
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    private void unlockInput() {
        findViewById(R.id.overlay).setVisibility(View.INVISIBLE);
        findViewById(R.id.input).setVisibility(View.VISIBLE);
    }


    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

    }

    @Override
    public void onTextChanged(CharSequence text, int i, int i2, int i3) {

    }

    @Override
    public void afterTextChanged(Editable editable) {
        clearOutput();
        if (input.getText().length() > 0)
            lookup(input.getText().toString());
    }

    private void clearInput() {
        input.setText("");
    }

    private void clearOutput() {
        output.setText("");
    }

    private void lookup(String text) {
        StringBuilder sb = new StringBuilder();
        Queue<String> result = mDictionary.lookup(text);
        if (result != null) {
            for (String s : result) {
                sb.append(s);
                sb.append(System.getProperty ("line.separator"));
            }
            output.setText(sb.toString());
        } else {
            clearOutput();
        }
    }
}
