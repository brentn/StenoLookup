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
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Queue;

public class MainActivity extends Activity implements Dictionary.OnDictionaryLoadedListener, TextWatcher{

    private static final int CHOOSE_DICTIONARIES_CODE = 1;
    private static final String TAG = "StenoLookup";

    private StenoApplication App;
    private Dictionary mDictionary;
    private SharedPreferences prefs;
    private EditText input;
    private TextView output;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App = ((StenoApplication) getApplication());
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        setContentView(R.layout.activity_main);
        mDictionary=App.getDictionary();
        mDictionary.setOnDictionaryLoadedListener(this);
        loadDictionary();
        output = (TextView) findViewById(R.id.output);
        input = (EditText) findViewById(R.id.input);
        input.addTextChangedListener(this);
        (findViewById(R.id.clear_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearInput();
                clearOutput();
            }
        });
        (findViewById(R.id.menu_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivityForResult(i, CHOOSE_DICTIONARIES_CODE);
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
                String dictionaries = prefs.getString(StenoApplication.KEY_DICTIONARIES, "");
                if (dictionaries.isEmpty()) {
                    Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
                    startActivityForResult(i, CHOOSE_DICTIONARIES_CODE);
                } else {
                    TextView dictname = (TextView) findViewById(R.id.dict_name);
                    dictname.setText("Dictionary file: " + dictionaries);
                    findViewById(R.id.overlay).setVisibility(View.VISIBLE);
                    findViewById(R.id.input).setVisibility(View.INVISIBLE);
                    ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
                    progressBar.setProgress(0);
                    int size = prefs.getInt(StenoApplication.KEY_DICTIONARY_SIZE, 100000);
                    mDictionary.load(dictionaries.split(":"), progressBar, size);
                }
            } else {
                Toast.makeText(this, "Dictionary media not mounted", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "SD Card not mounted, cannot load dictionary");
            }
        } else {
            unlockInput();
        }
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHOOSE_DICTIONARIES_CODE) {
            mDictionary.unload();
            loadDictionary();
        }
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
