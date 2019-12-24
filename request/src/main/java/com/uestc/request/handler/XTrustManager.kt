package com.uestc.request.handler

import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

/**
 * Created by PengFeifei on 2019-12-23.
 */
class XTrustManager :X509TrustManager {

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        return emptyArray()
    }
}