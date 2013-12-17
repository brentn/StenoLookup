package com.brentandjody.stenolookup;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.ProgressBar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * Created by brent on 09/12/13.
 * implements a reverse-lookup steno dictionary, with strokes sorted by stroke-length
 */
public class Dictionary {

    private static final String[] DICTIONARY_TYPES = {".json"};
    private static final String TAG = "StenoLookup";


    private TST<Queue<String>> mDictionary = new TST<Queue<String>>();
    private SharedPreferences prefs;

    public Dictionary(Context c) {
        prefs = PreferenceManager.getDefaultSharedPreferences(c);
    }

    private OnDictionaryLoadedListener onDictionaryLoadedListener;
    public interface OnDictionaryLoadedListener {
        public void onDictionaryLoaded();
    }
    public void setOnDictionaryLoadedListener(OnDictionaryLoadedListener listener) {
        onDictionaryLoadedListener = listener;
    }

    public void load(String[] filenames, ProgressBar progressBar, int size) {
        Log.d(TAG, "loading dictionaries");
        for (String filename : filenames) {
            if (!filename.equals("null")) {
                if (filename.contains(".")) {
                    String extension = filename.substring(filename.lastIndexOf("."));
                    if (Arrays.asList(DICTIONARY_TYPES).contains(extension)) {
                        try {
                            File file = new File(filename);
                            if (!file.exists()) {
                                throw new IOException("Dictionary file could not be found.");
                            }
                        } catch (IOException e) {
                            System.err.println("Dictionary File: "+filename+" could not be found");
                        }
                    } else {
                        throw new IllegalArgumentException(extension + " is not an accepted dictionary format.");
                    }
                } else {
                    throw new IllegalArgumentException("Illegal filename:" +filename);
                }
            }
        }

        new JsonLoader(progressBar, size).execute(filenames);
    }

    public Queue<String> lookup(String english) {
        return mDictionary.get(english);
    }

    public int size() { return mDictionary.size(); }

    public void unload() {
        mDictionary = null;
        mDictionary = new TST<Queue<String>>();
    }

    private class JsonLoader extends AsyncTask<String, Integer, Long> {
        private int loaded;
        private int total_size;
        private ProgressBar progressBar;

        public JsonLoader(ProgressBar progress, int size) {
            progressBar = progress;
            total_size = size;
        }

        protected Long doInBackground(String... filenames) {
            loaded = 0;
            StrokeComparator compareByStrokeLength = new StrokeComparator();
            int update_interval = total_size/100;
            if (update_interval == 0) update_interval=1;
            String line, stroke, english;
            Queue<String> strokes;
            String[] fields;
            for (String filename : filenames) {
                if (filename == null || filename.isEmpty())
                    throw new IllegalArgumentException("Dictionary filename not provided");
                try {
                    File file = new File(filename);
                    FileReader reader = new FileReader(file);
                    BufferedReader lines = new BufferedReader(reader);
                    while ((line = lines.readLine()) != null) {
                        fields = line.split("\"");
                        if ((fields.length >= 3) && (fields[3].length() > 0)) {
                            stroke = fields[1];
                            english = fields[3];
                            strokes = mDictionary.get(english);
                            if (strokes==null)
                                strokes = new PriorityQueue<String>(3, compareByStrokeLength);
                            strokes.add(stroke);
                            mDictionary.put(english, strokes);
                            loaded++;
                            if (loaded%update_interval==0) {
                                onProgressUpdate(loaded);
                            }
                        }
                    }
                    lines.close();
                    reader.close();
                } catch (IOException e) {
                    System.err.println("Dictionary File: " + filename + " could not be found");
                }
            }
            return (long) loaded;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setMax(total_size);
            progressBar.setProgress(0);
        }

        @Override
        protected void onPostExecute(Long result) {
            super.onPostExecute(result);
            Log.d(TAG, "Finished Loading");
            int size = safeLongToInt(result);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(StenoApplication.KEY_DICTIONARY_SIZE, size);
            editor.commit();
            if (onDictionaryLoadedListener != null)
                onDictionaryLoadedListener.onDictionaryLoaded();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            progressBar.setProgress(values[0]);
        }

    }

    private static int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException
                    (l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }

    private class StrokeComparator implements Comparator<String> {

        @Override
        public int compare(String a, String b) {
            if (a==null || b==null) return 0;
            int aStrokes = countStrokes(a);
            int bStrokes = countStrokes(b);
            //first compare number of strokes
            if (aStrokes < bStrokes) return -1;
            if (aStrokes > bStrokes) return 1;
            //then compare complexity of strokes
            if (a.length() < b.length()) return -1;
            if (a.length() > b.length()) return 1;
            //otherwise consider them equal
            return 0;
        }

        private int countStrokes(String s) {
            return (s.length()-s.replace("/","").length());
        }
    }
}
