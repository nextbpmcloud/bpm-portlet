package es.tangrambpm.liferay;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.portletbridge.ResourceException;
import org.portletbridge.portlet.HttpClientCallback;
import org.portletbridge.portlet.HttpClientState;
import org.portletbridge.portlet.HttpClientTemplate;

public class BPMHttpClientTemplate implements HttpClientTemplate {

	private AuthScope authscope = null;
    private static final org.apache.commons.logging.Log log =
            org.apache.commons.logging.LogFactory.getLog(BPMViewPortlet.class);
	private UsernamePasswordCredentials credentials = null;
    private HttpClient httpClient = null;
    private static boolean initialized=false;
    private static boolean singleCookieHeader ;
    private static String contentCharset;
    private static int connectionTimeout;
    private static int socketTimeout;
    private static int maxRedirects;
    private static boolean staleCheck;
    private static boolean httpProtocolWarnExtraInput;

    private void readParams(){
    	if (!initialized) {
	        singleCookieHeader = Boolean.parseBoolean(BPMViewPortlet.readProp("http.protocol.single-cookie-header", "true"));
	        contentCharset = BPMViewPortlet.readProp("http.protocol.content-charset", "UTF-8");
	        connectionTimeout = Integer.parseInt(BPMViewPortlet.readProp("http.connection.timeout", "10000"));
	        socketTimeout = Integer.parseInt(BPMViewPortlet.readProp("http.socket.timeout", "60000"));
	        staleCheck = Boolean.parseBoolean(BPMViewPortlet.readProp("http.connection.stalecheck", "true"));
            httpProtocolWarnExtraInput = Boolean.parseBoolean(BPMViewPortlet.readProp("http.protocol.warn-extra-input", "true"));
	        initialized = true;
	        maxRedirects = Integer.parseInt(BPMViewPortlet.readProp("http.protocol.max-redirects", "20"));
    	}
    }

    public BPMHttpClientTemplate() {

    	class MyConnectionManager extends MultiThreadedHttpConnectionManager {
    		public MyConnectionManager() {
    	    	this.setMaxConnectionsPerHost(100);
    	    	this.setMaxTotalConnections(100);
    		}
    		public void releaseConnection(HttpConnection conn) {
    			conn.close();
    			super.releaseConnection(conn);

    		}

    	}

    	readParams();
    	MyConnectionManager cm = new MyConnectionManager();
    	httpClient = new HttpClient(cm);
        httpClient.getParams().setParameter("http.protocol.single-cookie-header", singleCookieHeader);
        httpClient.getParams().setParameter("http.protocol.content-charset", contentCharset);
        httpClient.getParams().setParameter("http.connection.timeout", connectionTimeout);
        httpClient.getParams().setParameter("http.socket.timeout", socketTimeout);
        httpClient.getParams().setParameter("http.protocol.warn-extra-input", httpProtocolWarnExtraInput);
        httpClient.getParams().setParameter("http.protocol.version", HttpVersion.HTTP_1_0);
        httpClient.getParams().setParameter("http.connection.stalecheck", staleCheck);
        httpClient.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
        httpClient.getParams().setParameter("http.protocol.allow-circular-redirects", true);
        httpClient.getParams().setParameter("http.protocol.max-redirects", maxRedirects);
    }

    public void setAuthScope(String host, int port, String realm) {
    	authscope = new AuthScope(host, port, realm);
    }

    public void setCredentials(String username, String password) {
    	credentials = new UsernamePasswordCredentials(username, password);
    }

    public void setAuthScope(AuthScope scope) {
    	authscope = scope;
    }

    public void setCredentials(UsernamePasswordCredentials crd) {
    	credentials = crd;
    }

    public Object service(HttpMethodBase method, HttpClientState state, HttpClientCallback callback) throws ResourceException {
    	String url = "";
    	try {
    		url = method.getURI().toString();
    	} catch (Exception exc){
    		url = "unknown";
    	}
    	try {
    		log.debug("Try to get: " + url);
            HostConfiguration hostConfiguration = new HostConfiguration();
            if(state.getProxyHost() != null && state.getProxyHost().trim().length() > 0) {
                hostConfiguration.setProxy(state.getProxyHost(), state.getProxyPort());
            }
            hostConfiguration.setHost(method.getURI());
            HttpState httpState = state.getHttpState();
            httpState.setCredentials(authscope, credentials);
            int statusCode = httpClient.executeMethod(hostConfiguration, method, httpState);
            return callback.doInHttpClient(statusCode, method);
        } catch (Throwable e) {
        	log.error("connect to: " + url + ", " + e.toString());
        	throw new ResourceException("Connection Error");
        } finally {
            method.releaseConnection();
        }
    }
}
