package es.tangrambpm.liferay;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.portlet.PortletConfig;
import javax.portlet.PortletException;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletSession;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.WindowState;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.portletbridge.ResourceException;
import org.portletbridge.portlet.BridgeAuthenticator;
import org.portletbridge.portlet.BridgeRequest;
import org.portletbridge.portlet.BridgeTransformer;
import org.portletbridge.portlet.BridgeViewPortlet;
import org.portletbridge.portlet.HttpClientCallback;
import org.portletbridge.portlet.HttpClientTemplate;
import org.portletbridge.portlet.PerPortletMemento;
import org.portletbridge.portlet.PortletBridgeContent;
import org.portletbridge.portlet.PortletBridgeMemento;

import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortalUtil;
import com.liferay.util.portlet.PortletProps;

public class BPMViewPortlet extends BridgeViewPortlet {

    private static final org.apache.commons.logging.Log log =
        org.apache.commons.logging.LogFactory.getLog(BPMViewPortlet.class);
    private HttpClientTemplate httpClientTemplate = null;
    private String mementoSessionKey = null;
    private String idParamKey = "id";
    private BridgeAuthenticator bridgeAuthenticator = null;
    private BridgeTransformer transformer = null;
    private Map<String, String> sessionvariables_cookies_map =
        new HashMap<String, String>();
    private final int SESSIONVARIABLES_MAX = 20;
    protected final int MAXURLPARAMS = 10;
    private final String SOURCEAPP_COOKIE = readProp("sourceapp.cookie",
        "proxyApp");
    private final String LANGUAGE_COOKIE = readProp("language.cookie",
            "django_language");
    protected final List<String> IGNORE_POST_PARAMETERS =  Arrays.asList("p_l_id", "p_v_l_s_g_id");

    public void init(PortletConfig portletConfig) throws PortletException
    {
        super.init(portletConfig);
        for (int i=1; i <= SESSIONVARIABLES_MAX; i++) {
            String prop = "sessionvariable" + i;
            String sessionvariable = readPropSilent(prop + ".name", "");
            String cookie = readPropSilent(prop + ".cookie", "");
            if (!sessionvariable.equals("") && !cookie.equals("")) {
                sessionvariables_cookies_map.put(sessionvariable, cookie);
            }
        }
    }

    // Read property from properties file, don't warn if not set
    public static String readPropSilent(String name, String defaultValue)
    {
        return _readProp(name, defaultValue, false);
    }

    // Read property from properties file, log, warn if not set.
    public static String readProp(String name, String defaultValue)
    {
        return _readProp(name, defaultValue, true);
    }

    // Read property from properties file, log, warn if not set.
    public static String _readProp(String name, String defaultValue, boolean show_warnings)
    {
        String value = PortletProps.get(name);
        if (value == null)
        {
            if (show_warnings)
                log.warn(name + " property not set. Using default: " + defaultValue);
            value = defaultValue;
        }
        log.info("  " + name + "=" + value);
        return value;
    }

    public static Cookie getCookie(String name, String value, HttpMethodBase method){
    	Cookie cookie;
    	String host = "";
		try {
			host = method.getURI().getHost();
		} catch (URIException e) {
			log.error(e);
		}
		try {
			value = URLEncoder.encode(value, "UTF-8");
		} catch (Exception exc) {
		}
		cookie = new Cookie(host, name, "\"" + value + "\"");
		cookie.setPath("/");
    	return cookie;
    }

    public HttpMethodBase setCookies(RenderRequest request,
            HttpMethodBase method, PortletBridgeMemento portletBridgeMemento, PerPortletMemento perPortletMemento)
    {
    	//Obtenemos el languageId de Liferay
    	String languageId = LanguageUtil.getLanguageId(request);
    	//En django solo se usan los 2 primeros caracteres del identificador del idioma Ej. es_ES, solo usa es
    	languageId = languageId.substring(0,2);
    	log.info("languageId " + ": " + languageId);

        HttpState state = perPortletMemento.getHttpState();
    	//Se agregan como cookies la aplicacion origen y el language
    	state.addCookie(getCookie(SOURCEAPP_COOKIE, "liferay", method));
    	state.addCookie(getCookie(LANGUAGE_COOKIE, languageId, method));


    	PortletSession session = request.getPortletSession();
        for (Map.Entry<String,String> me: sessionvariables_cookies_map.entrySet())
        {
            String sessionvariable = me.getKey();
            String value = (String) session.getAttribute(sessionvariable,
                PortletSession.APPLICATION_SCOPE);
            log.debug("Sessionvariable " + sessionvariable + ": " + value);
            if (value != null)
            	state.addCookie(getCookie(me.getValue(), value, method));
        }
        return method;
    }

    private void setCredentials(PortletPreferences preferences,
            PortletBridgeMemento portletBridgeMemento) {
        String authenticationHost = preferences.getValue("authenticationHost",
            StringPool.BLANK);
        int authenticationPort = GetterUtil.getInteger(preferences.getValue(
            "authenticationPort", StringPool.BLANK), -1);
        String authenticationRealm = preferences.getValue(
            "authenticationRealm", StringPool.BLANK);
        String authenticationUsername = preferences.getValue(
            "authenticationUsername", StringPool.BLANK);
        String authenticationPassword = preferences.getValue(
            "authenticationPassword", StringPool.BLANK);
        ((BPMHttpClientTemplate) httpClientTemplate).setAuthScope(
            authenticationHost, authenticationPort, authenticationRealm);
        ((BPMHttpClientTemplate) httpClientTemplate).setCredentials(
            authenticationUsername, authenticationPassword);
        ((BPMPortletBridgeMemento) portletBridgeMemento).setAuthScope(
            authenticationHost, authenticationPort, authenticationRealm);
        ((BPMPortletBridgeMemento) portletBridgeMemento).setCredentials(
            authenticationUsername, authenticationPassword);

    }

    public void doView(final RenderRequest request,
            final RenderResponse response) throws PortletException, IOException {

        PortletPreferences preferences = request.getPreferences();

        // noop if window is minimised
        if(request.getWindowState().equals(WindowState.MINIMIZED)) {
            return;
        }

        response.setContentType("text/html");

        try {
            PortletSession session = request.getPortletSession();
            String portletId = response.getNamespace();
            PortletBridgeMemento tempMemento = (PortletBridgeMemento) session
                    .getAttribute(mementoSessionKey,
                            PortletSession.APPLICATION_SCOPE);
            if (tempMemento == null) {
                tempMemento = new BPMPortletBridgeMemento(idParamKey,
                    bridgeAuthenticator);
                session.setAttribute(mementoSessionKey, tempMemento,
                    PortletSession.APPLICATION_SCOPE);
            }
            this.setCredentials(preferences, tempMemento);
            final PortletBridgeMemento memento = tempMemento;
            final PerPortletMemento perPortletMemento = memento
                    .getPerPortletMemento(portletId);
            perPortletMemento.setPreferences(request);
            String urlId = request.getParameter(idParamKey);


            final BridgeRequest bridgeRequest;

            if(urlId != null) {
                bridgeRequest = memento
                    .getBridgeRequest(urlId);
            } else {
                // log.warn("no bridge request found for " + urlId);
                bridgeRequest = null;
            }
            ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
            HttpServletRequest httpReq = PortalUtil.getHttpServletRequest(request);
            httpReq = PortalUtil.getOriginalServletRequest(httpReq);
            if (urlId == null || bridgeRequest == null) {
                // this is the default start page for the portlet so go and
                // fetch it
                String initialUrl = perPortletMemento.getInitUrl().toString();
                if (initialUrl.contains("$tenant")) {
                    if (themeDisplay == null)
                        log.warn("themeDisplay is null, can't replace $tenant in url: " + initialUrl);
                    else {
                        String tenant = themeDisplay.getThemeSetting("tenant-slug");
                        if (tenant != null && !tenant.isEmpty()) {
                    	    initialUrl = initialUrl.replace("$tenant_slug", tenant);
                            initialUrl = initialUrl.replace("$tenant", tenant);
                    	}
                    }
                }
                initialUrl = replaceURI(initialUrl, httpReq, "processdef");
                // Legacy replacemente string
                initialUrl = replaceURI(initialUrl, httpReq, "processdef", "SolicitudReservaMinusvalidos:7:4534");
                // New parameter style replacement
                for (int i=1; i <= MAXURLPARAMS; i++) {
                    initialUrl = replaceURI(initialUrl, httpReq, "param" + i);
                }
                HttpMethodBase method;
                if (httpReq.getMethod().equals("POST")) {
                	Map<String,String[]> inputParams = httpReq.getParameterMap();
                	List<NameValuePair> requestParameters = new ArrayList<NameValuePair>();
                	for (String key : inputParams.keySet()) {
                		if (IGNORE_POST_PARAMETERS.contains(key))
                			continue;
                		String value = inputParams.get(key)[0];
                		log.info("Post Data: " + key + ": " + value);
                		requestParameters.add(new NameValuePair(key, value));
                	}
                	PostMethod m = new PostMethod(initialUrl);
                	m.setRequestBody(requestParameters.toArray(new NameValuePair[requestParameters.size()]));
            		method = m;
                }
                else {
                    if (GetterUtil.getBoolean(preferences.getValue("queryStringPassthrough", StringPool.FALSE))) {
                    	// copy query string
                    	initialUrl = addQueryString(initialUrl, PortalUtil.getCurrentURL(httpReq));
                    }
                	method = new GetMethod(initialUrl);
                }
                final URI initUrl = new URI(initialUrl);
                method.setDoAuthentication(true);
                method.addRequestHeader("bpm-portlet", "BPMViewPorlet.doView");
                String original_url = httpReq.getHeader("X-Forwarded-URL");
                if (original_url != null && !original_url.equals(""))
                	method.addRequestHeader("X-Forwarded-URL", original_url);
                this.setCookies(request, method, tempMemento, perPortletMemento);
                httpClientTemplate.service(method, perPortletMemento,
                    new HttpClientCallback() {
                        public Object doInHttpClient(int statusCode,
                            HttpMethodBase method) throws Throwable {
                            transformer.transform(memento, perPortletMemento,
                                initUrl,
                                request, response,
                                new InputStreamReader(method
                                    .getResponseBodyAsStream(),
                                    method.getResponseCharSet())
                            );
                            return null;
                        }
                    }
                );
            } else {
                PortletBridgeContent content = perPortletMemento
                    .dequeueContent(bridgeRequest.getId());
                if (content == null) {
                    // we're rerending
                    HttpMethodBase method = new GetMethod(bridgeRequest
                        .getUrl().toString());
                    method.setDoAuthentication(true);
                    String original_url = httpReq.getHeader("X-Forwarded-URL");
                    if (original_url != null && !original_url.equals(""))
                    	method.addRequestHeader("X-Forwarded-URL", original_url);
                    this.setCookies(request, method, tempMemento, perPortletMemento);
                    httpClientTemplate.service(method, perPortletMemento,
                        new HttpClientCallback() {
                            public Object doInHttpClient(int statusCode,
                                HttpMethodBase method) throws Throwable {
                                transformer.transform(
                                    memento,
                                    perPortletMemento,
                                    bridgeRequest.getUrl(),
                                    request,
                                    response,
                                    new InputStreamReader(method
                                        .getResponseBodyAsStream(),
                                        method.getResponseCharSet()));
                                    return null;
                            }
                        }
                    );
                } else {
                    // we have content, transform that
                    transformer.transform(memento, perPortletMemento,
                        bridgeRequest.getUrl(), request,
                        response, new StringReader(content.getContent()));
                }
            }
        } catch (ResourceException e) {
             log.warn("Resource Exception", e);
            // Ignore ResourceException
        } catch (Exception e) {
             log.error("General exception", e);
        }
    }

    /* Replace string with string in URI */
    public String replaceURI(String url, String pattern, String replacement) {
    	url = url.replace(pattern, replacement);
    	return url;
    }
    public String addQueryString(String url, String qs) {
    	if (url.contains("?"))
    		url += "&";
		else
			url += "?";
    	if (qs.contains("?"))
			qs = qs.split("\\?")[1];
    	url += qs;
    	return url;
    }
    public String addQueryString(String url, List<NameValuePair> params) {
    	if (url.contains("?"))
    		url += "&";
		else
			url += "?";
    	for (NameValuePair nvp: params) {
    		String key = nvp.getName();
    		String value = nvp.getValue();
    		if (value != null && !value.isEmpty())
    			url += key + "=" + value;
    	}
    	return url;
    }

    /* Replace pattern in URI with parameter gotton from request */
    public String replaceURI(String uri, HttpServletRequest req, String paramname, String targetPattern) {
        String paramvalue = req.getParameter(paramname);
        if (paramvalue == null)
            paramvalue = "";
        try {
			paramvalue = URLEncoder.encode(paramvalue, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// ignore
		}
        return replaceURI(uri, targetPattern, paramvalue);
    }

    /* Replace parameter name pattern in URI with parameter gotten from request */
    public String replaceURI(String uri, HttpServletRequest req, String paramname) {
       return replaceURI(uri, req, paramname, "$" + paramname);
    }

    public void setMementoSessionKey(String mementoSessionKey) {
        this.mementoSessionKey = mementoSessionKey;
    }

    public void setHttpClientTemplate(HttpClientTemplate httpClientTemplate) {
        this.httpClientTemplate = httpClientTemplate;
    }

    public void setTransformer(BridgeTransformer transformer) {
        this.transformer = transformer;
    }

    public void setIdParamKey(String idParamKey) {
        this.idParamKey = idParamKey;
    }

    public void setBridgeAuthenticator(
            BridgeAuthenticator bridgeAuthenticator) {
        this.bridgeAuthenticator = bridgeAuthenticator;
    }

}
