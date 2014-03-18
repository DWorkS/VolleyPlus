/**
 * Copyright 2013 Ognyan Bankov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.volley.demo.misc;

import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.http.conn.ssl.SSLSocketFactory;


class SslSocketFactory extends SSLSocketFactory {
public SslSocketFactory(KeyStore keystore, String keystorePassword) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
		super(keystore, keystorePassword);
		// TODO Auto-generated constructor stub
	}

	/*    public SslSocketFactory(InputStream keyStore, String keyStorePassword) throws GeneralSecurityException {
        super(createSSLContext(keyStore, keyStorePassword), STRICT_HOSTNAME_VERIFIER);
    }

*/
    private static SSLContext createSSLContext(InputStream keyStore, String keyStorePassword) throws GeneralSecurityException {
        SSLContext sslcontext = null;
        try {
            sslcontext = SSLContext.getInstance("TLS");
            sslcontext.init(null, new TrustManager[] { new SsX509TrustManager(keyStore, keyStorePassword) }, null);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failure initializing default SSL context", e);
        } catch (KeyManagementException e) {
            throw new IllegalStateException("Failure initializing default SSL context", e);
        }

        return sslcontext;
    }
}    