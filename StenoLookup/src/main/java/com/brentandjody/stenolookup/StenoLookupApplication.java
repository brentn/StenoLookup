package com.brentandjody.stenolookup;

import android.app.Application;
import android.widget.ProgressBar;

/**
 * Created by brent on 09/12/13.
 */
public class StenoLookupApplication extends Application {

    public static final String KEY_DICTIONARY_SIZE = "dictionary_size";
    public static final String KEY_DICTIONARY_FILE = "dictionary_file";

    private Dictionary mDictionary;

    @Override
    public void onCreate() {
        super.onCreate();
        mDictionary = new Dictionary(getApplicationContext());
    }

    public Dictionary getDictionary() { return mDictionary; }

}
