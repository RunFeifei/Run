
package com.uestc.request.cookie;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import com.franmontiel.persistentcookiejar.persistence.CookiePersistor;
import com.franmontiel.persistentcookiejar.persistence.SerializableCookie;
import com.uestc.request.handler.Request;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import okhttp3.Cookie;

@SuppressLint("CommitPrefEdits")
public class CookiePersist implements CookiePersistor {

    private static final String NAME = "CookiePersistence";


    public CookiePersist() {
    }

    @Override
    public List<Cookie> loadAll() {
        List<Cookie> cookies = new ArrayList<>(getSharedPreferences().getAll().size());

        for (Map.Entry<String, ?> entry : getSharedPreferences().getAll().entrySet()) {
            String serializedCookie = (String) entry.getValue();
            Cookie cookie = new SerializableCookie().decode(serializedCookie);
            if (cookie != null) {
                cookies.add(cookie);
            }
        }
        return cookies;
    }

    @Override
    public void saveAll(Collection<Cookie> cookies) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        for (Cookie cookie : cookies) {
            editor.putString(createCookieKey(cookie), new SerializableCookie().encode(cookie));
        }
        editor.commit();
    }

    @Override
    public void removeAll(Collection<Cookie> cookies) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        for (Cookie cookie : cookies) {
            editor.remove(createCookieKey(cookie));
        }
        editor.commit();
    }

    private static String createCookieKey(Cookie cookie) {
        return (cookie.secure() ? "https" : "http") + "://" + cookie.domain() + cookie.path() + "|" + cookie.name();
    }

    @Override
    public void clear() {
        getSharedPreferences().edit().clear().commit();
    }


    private SharedPreferences getSharedPreferences() {
        return  Request.appContext.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }
}
