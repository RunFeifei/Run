package com.uestc.request.handler;


import android.text.TextUtils;

import com.uestc.request.tools.OkLog;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.concurrent.TimeUnit;

import okhttp3.Connection;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.GzipSource;
import okio.Okio;

import static java.net.HttpURLConnection.HTTP_NOT_MODIFIED;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static okhttp3.internal.http.StatusLine.HTTP_CONTINUE;

public final class LoggingInterceptor implements Interceptor {
    private static final Charset UTF8 = Charset.forName("UTF-8");

    @Override
    public Response intercept(Chain chain) throws IOException {

        Request request = chain.request();

        RequestBody requestBody = request.body();
        boolean hasRequestBody = requestBody != null;

        Connection connection = chain.connection();
        Protocol protocol = connection != null ? connection.protocol() : Protocol.HTTP_1_1;

        OkLog.start("Request ↓↓↓");
        OkLog.log("Method-->" + request.method());
        OkLog.log("URL-->" + request.url());
        OkLog.log("Protocol-->" + protocol.toString());

        if (hasRequestBody) {
            if (requestBody.contentType() != null) {
                OkLog.log("Content-Type-->" + requestBody.contentType());
            }
            if (requestBody.contentLength() != -1) {
                OkLog.log("Content-Length-->" + requestBody.contentLength());
            }
            if("POST".equals(request.method())){
                StringBuilder sb = new StringBuilder();
                if (request.body() instanceof FormBody) {
                    FormBody body = (FormBody) request.body();
                    for (int i = 0; i < body.size(); i++) {
                        sb.append(body.encodedName(i) + "=" + body.encodedValue(i) + ",");
                    }
                    OkLog.log( "RequestBody :{"+sb.toString()+"}");
                }
            }

        }

        Headers requestHeaders = request.headers();
        for (int i = 0, count = requestHeaders.size(); i < count; i++) {
            String name = requestHeaders.name(i);
            if (!"Content-Type".equalsIgnoreCase(name) && !"Content-Length".equalsIgnoreCase(name)) {
                OkLog.log("Headers-->" + name + ": " + requestHeaders.value(i));
            }
        }

        if (!hasRequestBody) {
            OkLog.end("Request ↑↑↑");
        } else {
            Buffer buffer = new Buffer();
            requestBody.writeTo(buffer);

            Charset charset = UTF8;
            MediaType contentType = requestBody.contentType();
            if (contentType != null) {
                charset = contentType.charset(UTF8);
            }

            if (requestBody.contentLength() != 0 && requestBody.contentLength() < 32 * 1024 && bodyIsText(contentType)) {
                if (bodyEncodedGzip(request.headers())) {
                    buffer = decodeGzip(buffer);
                }
                OkLog.log("Headers-->" + buffer.readString(charset));
            }
            OkLog.log("Content-Length-->" + requestBody.contentLength());
            OkLog.end("Request ↑↑↑ ");
        }

        long startNs = System.nanoTime();
        Response response = chain.proceed(request);
        long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);

        ResponseBody responseBody = response.body();
        long contentLength = responseBody.contentLength();

        OkLog.start("Response ↓↓↓");
        OkLog.log("Code-->" + response.code());
        OkLog.log("Message-->" + response.message());
        OkLog.log("URL-->" + response.request().url());
        OkLog.log("TimeToken-->" + tookMs + "ms");
        OkLog.log("BodySize-->" + contentLength + "byte");
        OkLog.log("Method-->" + response.request().method());

        Headers responseHeaders = response.headers();
        for (int i = 0, count = responseHeaders.size(); i < count; i++) {
            OkLog.log(responseHeaders.name(i) + ": " + responseHeaders.value(i));
        }

        if (!hasBody(response)) {
            OkLog.end("Response ↑↑↑");
        } else {
            BufferedSource source = responseBody.source();
            source.request(Long.MAX_VALUE);
            Buffer buffer = source.buffer();

            Charset charset = UTF8;
            MediaType contentType = responseBody.contentType();
            if (contentType != null) {
                OkLog.log("ContentType-->" + contentType.toString());
                try {
                    charset = contentType.charset(UTF8);
                } catch (UnsupportedCharsetException e) {
                    OkLog.end("Response ↑↑↑");
                    return response;
                }
            }

            if (contentLength != 0 && contentLength < 32 * 1024 && bodyIsText(contentType)) {
                if (bodyEncodedGzip(response.headers())) {
                    buffer = decodeGzip(buffer);
                }
                OkLog.log("________________________________________________________");
                OkLog.log("*******************ResponseBody*************************");
                String str=buffer.clone().readString(charset);
                OkLog.log(str);
            }
            OkLog.log("ResponseBody-->" + buffer.size() + "byte");
            OkLog.end("Response ↑↑↑");
        }

        return response;
    }

    private boolean bodyEncodedGzip(Headers headers) {
        String contentEncoding = headers.get("Content-Encoding");
        return contentEncoding != null && contentEncoding.equalsIgnoreCase("gzip");
    }

    private boolean bodyIsText(MediaType contentType) {
        return contentType != null && ("text".equals(contentType.type()) || "json".equals(contentType.subtype())
                || contentType.subtype() != null && contentType.subtype().contains("form"));
    }

    private Buffer decodeGzip(Buffer buffer) throws IOException {
        GzipSource gzipSource = new GzipSource(Okio.source(buffer.clone().inputStream()));
        long count = buffer.size();
        Buffer resultBuffer = new Buffer();
        gzipSource.read(resultBuffer, count);
        gzipSource.close();
        return resultBuffer;
    }

    private boolean hasBody(Response response) {
        if (response.request().method().equals("HEAD")) {
            return false;
        }
        int responseCode = response.code();
        if ((responseCode < HTTP_CONTINUE || responseCode >= 200)
                && responseCode != HTTP_NO_CONTENT
                && responseCode != HTTP_NOT_MODIFIED) {
            return true;
        }

        return contentLength(response.headers()) != -1
                || "chunked".equalsIgnoreCase(response.header("Transfer-Encoding"));
    }

    private long contentLength(Headers headers) {
        String length = headers.get("Content-Length");
        if (TextUtils.isEmpty(length)) {
            return -1;
        }
        length = length.trim();
        try {
            return Long.parseLong(length);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

}
