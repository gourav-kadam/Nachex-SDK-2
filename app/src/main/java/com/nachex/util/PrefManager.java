package com.nachex.util;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefManager {
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    Context _context;
    int PRIVATE_MODE = 0;
    private static final String PREF_NAME = "Nachex-1";
    public static final String IS_USER_LOGGED_IN = "IS_USER_LOGGED_IN";
    public static final String USER_TOKEN = "USER_TOKEN";

    public PrefManager(Context context) {
        this._context = context;
        pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }

    public static SharedPreferences.Editor getEditor(Context context) {
        SharedPreferences sharedpreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        return editor;
    }

    public static SharedPreferences getSharedPreference(Context context) {
        SharedPreferences sharedpreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sharedpreferences;
    }

    public static String getString(Context context, String name) {
        SharedPreferences sharedPreferences = getSharedPreference(context);
        return sharedPreferences.getString(name, "");
    }

    public static void setString(Context context, String name, String value) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putString(name, value);
        editor.commit();
    }
}
