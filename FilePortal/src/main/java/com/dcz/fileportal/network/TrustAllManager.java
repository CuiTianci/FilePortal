package com.dcz.fileportal.network;

import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * 忽略证书校验。//todo 这绝对不是合适的“解决”问题的方式。
 * OkHttp官方已经考虑到这个Case，并给出了解决方法，即动态添加信任证书。（需要服务器证书管理员配合）
 * https://square.github.io/okhttp/https/#customizing-trusted-certificates-kt-java
 * https://android.googlesource.com/platform/external/okhttp/+/a2cab72/samples/guide/src/main/java/com/squareup/okhttp/recipes/CustomTrust.java
 */
public class TrustAllManager implements X509TrustManager {

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }

    public static SSLSocketFactory createTrustAllSSLFactory(X509TrustManager trustAllManager) {
        SSLContext sc;
        try {
            sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[]{trustAllManager}, new SecureRandom());
            return sc.getSocketFactory();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static HostnameVerifier createTrustAllHostnameVerifier() {
        return (hostname, session) -> true;
    }
}
