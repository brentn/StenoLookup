package com.brentandjody.stenolookup;

import android.app.Application;
import android.widget.ProgressBar;

/**
 * Created by brent on 09/12/13.
 */
public class StenoApplication extends Application {

    public static final String KEY_DICTIONARY_SIZE = "dictionary_size";
    public static final String KEY_DICTIONARIES = "dictionaries";

    private Dictionary mDictionary;

    @Override
    public void onCreate() {
        super.onCreate();
        mDictionary = new Dictionary(getApplicationContext());
    }

    public Dictionary getDictionary() { return mDictionary; }

}
