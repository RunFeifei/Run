package com.uestc.request.cookie;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.CookieManager;

import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.CookieCache;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.CookiePersistor;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.uestc.request.handler.Request;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import okhttp3.Cookie;
import okhttp3.HttpUrl;

/**
 * Set-Cookie报头包含于Web服务器的响应头（ResponseHeader）中
 * Cookie报头包含在浏览器客户端请求头（ReguestHeader）中
 */
public class CookieJar extends PersistentCookieJar {

    private static CookieJar instance;
    private static SetCookieCache setCookieCache = new SetCookieCache();
    private static CookiePersist cookiePersist = new CookiePersist();

    private static final String DOMAINS_PREFERENCES = "domains";
    private static CookieManager webCookieManager;

    public static CookieJar getInstance() {
        if (instance == null) {
            webCookieManager = CookieManager.getInstance();
            instance = new CookieJar(setCookieCache, cookiePersist);
        }
        return instance;
    }

    public CookieJar(CookieCache cache, CookiePersistor persistor) {
        super(cache, persistor);
    }

    @Override
    public synchronized void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        setCookieCache.addAll(cookies);
        cookiePersist.saveAll(cookies);
        syncToManager(cookies);
    }

    private void syncToManager(List<Cookie> cookies) {
        for (Cookie cookie : cookies) {
            webCookieManager.setCookie(cookie.domain(), cookie.toString());
            SharedPreferences sharedPreferences = getSharedPreferences();
            String domains = sharedPreferences.getString(CookieJar.DOMAINS_PREFERENCES, null);
            if (TextUtils.isEmpty(domains)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    webCookieManager.flush();
                }
                return;
            }
            try {
                Gson gson = new Gson();
                Set<String> set = gson.fromJson(domains, new TypeToken<HashSet<String>>() {
                }.getType());

                if (set == null || set.size() == 0) {
                    return;
                }
                // 将sessionId保存到slSessionId
                for (String domain : set) {
                    if (cookie.name().equals("JSESSIONID")) {
                        webCookieManager.setCookie(domain, "slSessionId=" + cookie.value());
                    }
                }
            } catch (Exception e) {
                Log.e("syncToManager", "syncCookieToWebManager fail");
            }

        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webCookieManager.flush();
        }
    }


    public boolean updateWebCookie(String url) {
        String domain = url.split("//", 2)[1];
        if (TextUtils.isEmpty(domain)) {
            return false;
        }
        SharedPreferences sharedPreferences = getSharedPreferences();
        String domains = sharedPreferences.getString(CookieJar.DOMAINS_PREFERENCES, null);
        boolean needUpdate = domains == null || !domains.contains(domain);
        if (!needUpdate) {
            return false;
        }
        try {
            Gson gson = new Gson();
            Set<String> set = new HashSet<String>();
            if (TextUtils.isEmpty(domains)) {
                set.add(domain);
                String strJson = gson.toJson(set);
                sharedPreferences.edit().putString(DOMAINS_PREFERENCES, strJson).commit();
                return true;
            }
            set = gson.fromJson(domains, new TypeToken<HashSet<String>>() {
            }.getType());
            if (set == null || set.size() == 0) {
                set = new HashSet<String>();
            }
            set.add(domain);
            String strJson = gson.toJson(set);
            sharedPreferences.edit().putString(DOMAINS_PREFERENCES, strJson).commit();
            return true;
        } catch (Exception e) {
            Log.e("updateWebCookie", "updateWebCookie fail");
            return false;
        }

    }


    public synchronized boolean removeAll() {
        webCookieManager.removeAllCookie();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webCookieManager.flush();
        }
        super.clear();
        return true;
    }

    /**
     * 打印webview的cookie
     */
    public void logCookies(String url) {
        if (url != null) {
            Log.d("cookie", "logCookies : " + CookieJar.webCookieManager.getCookie(url));
        }
    }

    private SharedPreferences getSharedPreferences() {
        final Context context = Request.Companion.get().appContext;
        return context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
    }

}
