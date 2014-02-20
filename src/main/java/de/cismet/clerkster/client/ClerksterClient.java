/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.clerkster.client;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthPolicy;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Level;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.MalformedURLException;
import java.net.URL;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * DOCUMENT ME!
 *
 * @author   Gilles Baatz
 * @version  $Revision$, $Date$
 */
public class ClerksterClient {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            ClerksterClient.class);

    //~ Methods ----------------------------------------------------------------

    /**
     * Sends a jar to the Clerkster service and expects a signed jar. Returns -1 if the connection could not be
     * established. Otherwise the HTTP status is returned, where 200 is successful.
     *
     * @param   username             DOCUMENT ME!
     * @param   password             DOCUMENT ME!
     * @param   urlString            DOCUMENT ME!
     * @param   input                DOCUMENT ME!
     * @param   output               DOCUMENT ME!
     * @param   ignoreSSLCertifcate  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  FileNotFoundException  DOCUMENT ME!
     * @throws  IOException            DOCUMENT ME!
     */
    public static int uploadAndReceiveJar(final String username,
            final String password,
            final String urlString,
            final File input,
            final File output,
            final boolean ignoreSSLCertifcate) throws FileNotFoundException, IOException {
        InputStream response = null;
        FileOutputStream out = null;
        PostMethod postMethod = null;
        int result = -1;
        try {
            if (ignoreSSLCertifcate) {
                // configure the SSLContext with a TrustManager
                // this ignores the every SSL certificate
                final SSLContext ctx = SSLContext.getInstance("TLS");
                ctx.init(new KeyManager[0], new TrustManager[] { new DefaultTrustManager() }, new SecureRandom());
                SSLContext.setDefault(ctx);
            }

            // create the HttpClient
            final HttpClient httpClient = new HttpClient();
            final URL url = new URL(urlString);

            // set-up the digest authentification
            final List authPrefs = new ArrayList(1);
            authPrefs.add(AuthPolicy.DIGEST);
            httpClient.getParams().setParameter(AuthPolicy.AUTH_SCHEME_PRIORITY,
                authPrefs);
            final Credentials credentials = new UsernamePasswordCredentials(username,
                    password);
            final AuthScope authScope = new AuthScope(url.getHost(),
                    url.getPort());
            httpClient.getState().setCredentials(authScope, credentials);

            // set-up the POST request. Attach the file to it.
            postMethod = new PostMethod(url.toExternalForm());
            postMethod.setDoAuthentication(true);
            postMethod.getParams().setBooleanParameter(HttpMethodParams.USE_EXPECT_CONTINUE, true);
            final Part[] parts = { new FilePart("upload", input) };
            postMethod.setRequestEntity(
                new MultipartRequestEntity(parts, postMethod.getParams()));

            // execute the POST request and evaluate its result
            result = httpClient.executeMethod(postMethod);
            if (result == 200) {
                // write the response to the output file
                response = postMethod.getResponseBodyAsStream();
                out = new FileOutputStream(output);
                IOUtils.copy(response, out);
                LOG.info("Received a signed file and writing was successful.");
            } else {
                LOG.warn("Could not receive a signed file: " + postMethod.getStatusText() + " - "
                            + postMethod.getResponseBodyAsString());
            }
        } catch (NoSuchAlgorithmException ex) {
            LOG.error(ex.getMessage(), ex);
        } catch (KeyManagementException ex) {
            LOG.error(ex.getMessage(), ex);
        } catch (MalformedURLException ex) {
            LOG.error(ex.getMessage(), ex);
        } finally {
            if (postMethod != null) {
                postMethod.releaseConnection();
            }
            if (response != null) {
                try {
                    response.close();
                } catch (IOException ex) {
                    LOG.error(ex.getMessage(), ex);
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                    LOG.error(ex.getMessage(), ex);
                }
            }
        }
        return result;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  args  DOCUMENT ME!
     */
    public static void main(final String[] args) {
        org.apache.log4j.BasicConfigurator.configure();
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.INFO);
        
        final String username = "testuser";
        final String password = "test";
        final String url = "someURL/upload";
        final File input = new File("/some/filepath");
        final File output = new File("/another/filepath");
        try {
            uploadAndReceiveJar(username, password, url, input, output, true);
        } catch (FileNotFoundException ex) {
            LOG.error(ex);
        } catch (IOException ex) {
            LOG.error(ex);
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    private static class DefaultTrustManager implements X509TrustManager {

        //~ Methods ------------------------------------------------------------

        @Override
        public void checkClientTrusted(final X509Certificate[] arg0, final String arg1) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(final X509Certificate[] arg0, final String arg1) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }
}
