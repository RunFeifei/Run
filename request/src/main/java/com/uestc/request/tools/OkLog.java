package com.uestc.request.tools;

import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by PengFeifei on 17-8-2.
 */

public class OkLog {

    private static final String TAG = "OKHttp";
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final int JSON_INDENT = 4;
    private static final String SUFFIX = ".java";

    public static void log(String msg) {
        if (TextUtils.isEmpty(msg)) {
            return;
        }
        msg = msg.trim();
        String message;
        try {
            if (msg.startsWith("{")) {
                JSONObject jsonObject = new JSONObject(msg);
                message = jsonObject.toString(JSON_INDENT);
            } else if (msg.startsWith("[")) {
                JSONArray jsonArray = new JSONArray(msg);
                message = jsonArray.toString(JSON_INDENT);
            } else {
                message = msg;
            }
        } catch (JSONException e) {
            message = msg;
        }

        String[] lines = message.split(LINE_SEPARATOR);
        for (String line : lines) {
            Log.i(TAG, "║ " + line);
        }
    }

    public static void start(String hint) {
        printLine(TAG, true, hint);
        log(getStackTrace() + LINE_SEPARATOR);
    }

    public static void end(String hint) {
        printLine(TAG, false, hint);
    }

    private static void printLine(String tag, boolean isTop, String hint) {
        final String top = "╔════════ %s ══════════════════════════════════";
        final String bot = "╚════════ %s ══════════════════════════════════";
        hint = TextUtils.isEmpty(hint) ? "════════" : hint;
        String fotmated = String.format(isTop ? top : bot, hint);
        Log.i(tag, fotmated);
    }


    private static String getStackTrace() {
        final int STACK_TRACE_INDEX_4 = 4;
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        StackTraceElement targetElement = stackTrace[STACK_TRACE_INDEX_4];
        String className = targetElement.getClassName();
        String[] classNameInfo = className.split("\\.");
        if (classNameInfo.length > 0) {
            className = classNameInfo[classNameInfo.length - 1] + SUFFIX;
        }

        if (className.contains("$")) {
            className = className.split("\\$")[0] + SUFFIX;
        }

        String methodName = targetElement.getMethodName();
        int lineNumber = targetElement.getLineNumber();

        if (lineNumber < 0) {
            lineNumber = 0;
        }

        return "[ (" + className + ":" + lineNumber + ")#" + methodName + " ] ";
    }


}
