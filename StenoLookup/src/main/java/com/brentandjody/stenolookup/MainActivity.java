package com.brentandjody.stenolookup;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
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

import java.util.Queue;

public class MainActivity extends Activity implements Dictionary.OnDictionaryLoadedListener, TextWatcher{

    private static final String DICTIONARY = "dict.json";
    private static final String TAG = "StenoLookup";

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
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    @Override
    public void onDictionaryLoaded() {
        unlockInput();
    }

    private void loadDictionary() {
        //TODO:get rid of default dictionary
        if (mDictionary.size() == 0 ) {
            findViewById(R.id.overlay).setVisibility(View.VISIBLE);
            findViewById(R.id.input).setVisibility(View.INVISIBLE);
            ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
            progressBar.setProgress(0);
            int size = prefs.getInt(StenoLookupApplication.KEY_DICTIONARY_SIZE, 100000);
            mDictionary.load("dict.json", progressBar, size);
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
