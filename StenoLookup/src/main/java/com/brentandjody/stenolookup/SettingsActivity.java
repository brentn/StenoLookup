package com.brentandjody.stenolookup;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends PreferenceActivity {

    private static final String TAG = "StenoLookup";
    private static final int FILE_SELECT_CODE = 0;
    private SharedPreferences prefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        addPreferencesFromResource(R.xml.dictionaries);
        listDictionaries();
    }

    @Override
    protected void onStop() {
        super.onStop();
        String result = "";
        PreferenceCategory dict_cat = (PreferenceCategory) findPreference("dictionaries");
        EditTextPreference p;
        for (int i = 0; i<(dict_cat.getPreferenceCount()-1); i++) { //don't count the "add" entry
            p = (EditTextPreference) dict_cat.getPreference(i);
            if (!p.getSummary().equals("null"))
               result += ((EditTextPreference) dict_cat.getPreference(i)).getSummary()+":";
        }
        if (result.length() > 1)
            result = result.substring(0,result.length()-1);
        prefs.edit().putString(StenoApplication.KEY_DICTIONARIES, result).commit();
    }

    private void listDictionaries() {
        PreferenceCategory dict_cat = (PreferenceCategory) findPreference("dictionaries");
        dict_cat.removeAll();
        String dictionaries = prefs.getString(StenoApplication.KEY_DICTIONARIES, "");
        EditTextPreference p;
        int i = 0;
        for (String dictionary : dictionaries.split(":")) {
            if (!dictionary.trim().isEmpty()) {
                p = new EditTextPreference(this);
                p.setText(dictionary.substring(dictionary.lastIndexOf("/") + 1));
                p.setSummary(dictionary);
                p.setKey("dictionary_"+i);
                removeDictHandler(p);
                dict_cat.addPreference(p);
                i++;
            }
        }
        p = new EditTextPreference(this);
        p.setIcon(R.drawable.ic_menu_add);
        p.setKey("dictionary_"+i);
        addDictHandler(p);
        dict_cat.addPreference(p);
    }

    private void addDictHandler(EditTextPreference p) {
        p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                try {
                    startActivityForResult(
                            Intent.createChooser(intent, "Select your .json dictionary file"),
                            FILE_SELECT_CODE);
                } catch (android.content.ActivityNotFoundException ex) {
                    // Potentially direct the user to the Market with a Dialog
                    Toast.makeText(getApplicationContext(), "Please install a File Manager.", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });
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
                    String path = getPath(getApplicationContext(), uri);
                    Log.d(TAG, "File Path: " + path);
                    String oldlist = prefs.getString(StenoApplication.KEY_DICTIONARIES,"");
                    if (!oldlist.isEmpty()) oldlist+=":";
                    prefs.edit().putString(StenoApplication.KEY_DICTIONARIES, oldlist+path).commit();
                    listDictionaries();
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void removeDictHandler(EditTextPreference p) {
        p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                ((EditTextPreference) preference).setSummary("");
                ((EditTextPreference) preference).setText("");
                ((EditTextPreference) preference).setEnabled(false);
                return true;
            }
        });
    }

    public static String getPath(Context context, Uri uri) {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = { "_data" };
            Cursor cursor;

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



}