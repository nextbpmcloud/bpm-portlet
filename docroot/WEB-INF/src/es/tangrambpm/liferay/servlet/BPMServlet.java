package es.tangrambpm.liferay.servlet;

import java.net.URI;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.portletbridge.portlet.BridgeFunctions;
import org.portletbridge.portlet.BridgeFunctionsFactory;
import org.portletbridge.portlet.BridgeRequest;
import org.portletbridge.portlet.ContentRewriter;
import org.portletbridge.portlet.DefaultPortletBridgeService;
import org.portletbridge.portlet.HttpClientCallback;
import org.portletbridge.portlet.PerPortletMemento;
import org.portletbridge.portlet.PortletBridgeContent;
import org.portletbridge.portlet.PortletBridgeMemento;
import org.portletbridge.portlet.PortletBridgeService;
import org.portletbridge.portlet.PortletBridgeServlet;
import org.portletbridge.portlet.PseudoRenderRequest;
import org.portletbridge.portlet.PseudoRenderResponse;
import org.portletbridge.portlet.RegexContentRewriter;
import org.portletbridge.portlet.ResourceUtil;

import es.tangrambpm.liferay.BPMHttpClientTemplate;
import es.tangrambpm.liferay.BPMPortlet;
import es.tangrambpm.liferay.BPMPortletBridgeMemento;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import javax.servlet.http.HttpSession;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.portletbridge.PortletBridgeException;
import org.portletbridge.ResourceException;

/**
 * @author jmccrindle
 * @author rickard
 */
public class BPMServlet extends PortletBridgeServlet {

    /**
     * default serial version id
     */
	private static final long serialVersionUID = 4693089559903657897L;

    public static final ResourceBundle resourceBundle = PropertyResourceBundle.getBundle("org.portletbridge.portlet.PortletBridgePortlet");
    public static final ResourceBundle portletProperties = PropertyResourceBundle.getBundle("portlet");

    
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory
            .getLog(PortletBridgeServlet.class);

    private PortletBridgeService portletBridgeService = new DefaultPortletBridgeService();

    private BPMHttpClientTemplate httpClientTemplate = new BPMHttpClientTemplate();

    private String mementoSessionKey;

    private BridgeFunctionsFactory bridgeFunctionsFactory;

	protected String[] copyResponseHeaders;
	protected String[] copyRequestHeaders;
	protected String[] copyRequestPostHeaders;
	protected String sessionTimeoutRedirectUrl = portletProperties.getString("sessionTimeoutRedirectUrl");

    /**
     * Initialise the servlet. Will throw a servlet exception if the
     * proxyBrowserSessionKey is not set.
     *
     * @see javax.servlet.GenericServlet#init()
     */
    public void init() throws ServletException {

        // get proxyBrowserSessionKey
        mementoSessionKey = this.getServletConfig().getInitParameter(
                "mementoSessionKey");

        log.debug("init(): mementoSessionKey=" + mementoSessionKey);

        if (mementoSessionKey == null) {
            throw new ServletException(resourceBundle
                    .getString("error.mementoSessionKey"));
        }

        // TODO: blow up if these aren't set.
        String cssRegex = this.getServletConfig().getInitParameter("cssRegex");
        String javascriptRegex = this.getServletConfig().getInitParameter(
                "jsRegex");

        ContentRewriter javascriptRewriter = new RegexContentRewriter(
                javascriptRegex);
        ContentRewriter cssRewriter = new RegexContentRewriter(cssRegex);

        bridgeFunctionsFactory = new BridgeFunctionsFactory(BPMPortlet.BPMIdGenerator.getInstance(), javascriptRewriter, cssRewriter);

        // TODO: blow up if these aren't set.
        copyResponseHeaders = getInitParameter("copyResponseHeaders").split(",");
        copyRequestHeaders = getInitParameter("copyRequestHeaders").split(",");
        copyRequestPostHeaders = getInitParameter("copyRequestPostHeaders").split(",");
        if (sessionTimeoutRedirectUrl == null)
        	sessionTimeoutRedirectUrl = "/";
    }

    public void redirect(HttpServletResponse response, String path) {
    	response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
    	response.setHeader("Location", path);
    }
    public void redirect(HttpServletResponse response, String path, String log_text) {
    	log.warn(log_text);
    	redirect(response, path);
    }
    /**
     * url pattern should be: http://host:port/context/servlet/id
     */
    protected void doGet(final HttpServletRequest request,
            final HttpServletResponse response) throws ServletException,
            IOException {
        HttpSession session = request.getSession();
        // keepalive URL
        String path = request.getPathInfo();
        if (path == null || path.equals("/")){
            response.setContentType("text/plain");
            response.setStatus(200);
            PrintWriter writer = response.getWriter();
            writer.print("Hello");
            writer.flush();
            writer.close();
            return;
        }
        // get the id
        final String id = portletBridgeService.getIdFromRequestUri(request
                .getContextPath(), request.getRequestURI());
        // look up the data associated with that id from the session
        if (session == null) {
        	redirect(response, sessionTimeoutRedirectUrl, "No session, redirecting to session timeout page...");
//        	throw new ServletException(resourceBundle
//                    .getString("error.nosession")
//                    + ", URL=" + request.getRequestURI());
        	return;
        }
        final BPMPortletBridgeMemento memento = (BPMPortletBridgeMemento) session.getAttribute(mementoSessionKey);
        if (memento == null) {
        	// just redirect to predefined error page
        	redirect(response, sessionTimeoutRedirectUrl, "No memento, redirecting to session timeout page...");
//        	throw new ServletException(resourceBundle
//                    .getString("error.nomemento")
//                    + ", URL=" + request.getRequestURI());
        	return;
        }
        AuthScope authScope = memento.getAuthScope();
        httpClientTemplate.setAuthScope(authScope);
        httpClientTemplate.setCredentials(memento.getCredentials());

        BridgeRequest bridgeRequest = memento.getBridgeRequest(id);
        if (bridgeRequest == null) {
            throw new ServletException(resourceBundle
                    .getString("error.nobridgerequest")
                    + ", URL=" + request.getRequestURI());
        }
        final PerPortletMemento perPortletMemento = memento
                .getPerPortletMemento(bridgeRequest.getPortletId());
        if (perPortletMemento == null) {
            throw new ServletException(resourceBundle
                    .getString("error.noperportletmemento")
                    + ", URL=" + request.getRequestURI());
        }

        // go and fetch the data from the backend as appropriate
        URI url = bridgeRequest.getUrl();

        // TODO: if there is a query string, we should create a new bridge request with that query string added
        // TODO: need to clean up how we create bridge requests (i.e. gret rid of the pseudorenderresponse)
        if (request.getQueryString() != null
                && request.getQueryString().trim().length() > 0) {
            try {
                // TODO: may have to change encoding
            	// if the url already has a query string add an & instead
                String urlAsString = url.toString();
				url = new URI(urlAsString + ((url.getQuery() != null) ? '&' : '?') + request.getQueryString());
	            PseudoRenderResponse renderResponse = createRenderResponse(bridgeRequest);
	            bridgeRequest = memento.createBridgeRequest(renderResponse, BPMPortlet.BPMIdGenerator.getInstance().nextId(), url);
            } catch (URISyntaxException e) {
                throw new ServletException(e.getMessage() + ", doGet(): URL="
                        + url + ", id=" + id + ", request URI="
                        + request.getRequestURI(), e);
            }
        }

        log.debug("doGet(): URL=" + url + ", id=" + id + ", request URI="
                + request.getRequestURI());

        fetch(request, response, bridgeRequest, memento, perPortletMemento, url);

    }

    /**
     * Create a PseudoRenderResponse
     *
     * @param bridgeRequest the bridgeRequest to use
     * @return a render response
     */
	protected PseudoRenderResponse createRenderResponse(BridgeRequest bridgeRequest) {
		PseudoRenderResponse renderResponse = new PseudoRenderResponse(
		        bridgeRequest
		                .getPortletId(),
		        bridgeRequest
		                .getPageUrl(),
		        bridgeRequest
		                .getId());
		return renderResponse;
	}

    /**
     * @param response
     * @param bridgeRequest
     * @param perPortletMemento
     * @param url
     * @throws ServletException
     */
    protected void fetch(final HttpServletRequest request,
            final HttpServletResponse response,
            final BridgeRequest bridgeRequest,
            final PortletBridgeMemento memento,
            final PerPortletMemento perPortletMemento, final URI url)
            throws ServletException {
        try {
            GetMethod getMethod = new GetMethod(url.toString());
            getMethod.addRequestHeader("bpm-portlet", "BPMServlet.fetch");
            copyRequestHeaders(request, getMethod);
            httpClientTemplate.service(getMethod, perPortletMemento,
                    new HttpClientCallback() {
                        public Object doInHttpClient(int statusCode,
                                HttpMethodBase method)
                                throws ResourceException, Throwable {
                            if (statusCode == HttpStatus.SC_OK) {
                                // if it's text/html then store it and redirect
                                // back to the portlet render view (portletUrl)
                            	org.apache.commons.httpclient.URI effectiveUri = method.getURI();
                            	BridgeRequest effectiveBridgeRequest = null;
                            	if(!effectiveUri.toString().equals(url.toString())) {
                    	            PseudoRenderResponse renderResponse = createRenderResponse(bridgeRequest);
                    	            effectiveBridgeRequest = memento.createBridgeRequest(renderResponse, BPMPortlet.BPMIdGenerator.getInstance().nextId(), new URI(effectiveUri.toString()));
                            	} else {
                            		effectiveBridgeRequest = bridgeRequest;
                            	}
                                String contentType = "";
                                Header contentTypeHeader = method.getResponseHeader("Content-Type");
                                if (contentTypeHeader != null)
                                    contentType = contentTypeHeader.getValue();
                                if (contentType.startsWith("text/html")) {
                                    String content = ResourceUtil.getString(
                                            method.getResponseBodyAsStream(),
                                            method.getResponseCharSet());
                                    // TODO: think about cleaning this up if we
                                    // don't get back to the render
                                    perPortletMemento.enqueueContent(
                                    		effectiveBridgeRequest.getId(),
                                            new PortletBridgeContent(url,
                                                    "get", content));
                                    // redirect
                                    // TODO: worry about this... adding the id
                                    // at the end
                                    response.sendRedirect(effectiveBridgeRequest
                                            .getPageUrl());
                                } else if (contentType.startsWith("text/javascript") ||
                                		   contentType.startsWith("text/x-javascript") ||
                                		   contentType.startsWith("application/javascript")) {
                                    // rewrite external javascript
                                    String content = ResourceUtil.getString(
                                            method.getResponseBodyAsStream(),
                                            method.getResponseCharSet());
                                    BridgeFunctions bridge = bridgeFunctionsFactory
                                            .createBridgeFunctions(
                                                    memento,
                                                    perPortletMemento,
                                                    getServletName(),
                                                    url,
                                                    new PseudoRenderRequest(request.getContextPath()),
                                                    createRenderResponse(effectiveBridgeRequest));
                                    copyResponseHeaders(response, method);
                                    PrintWriter writer = response.getWriter();
                                    writer.write(bridge.script(null, content));
                                    writer.flush();
                                } else if (contentType.startsWith("text/css")) {
                                    // rewrite external css
                                    String content = ResourceUtil.getString(
                                            method.getResponseBodyAsStream(),
                                            method.getResponseCharSet());
                                    BridgeFunctions bridge = bridgeFunctionsFactory
                                            .createBridgeFunctions(
                                                    memento,
                                                    perPortletMemento,
                                                    getServletName(),
                                                    url,
                                                    new PseudoRenderRequest(request.getContextPath()),
                                                    createRenderResponse(effectiveBridgeRequest));
                                    copyResponseHeaders(response, method);
                                    PrintWriter writer = response.getWriter();
                                    writer.write(bridge.style(null, content));
                                    writer.flush();
                                } else {
                                    // if it's anything else then stream it
                                    // back... consider stylesheets and
                                    // javascript
                                    // TODO: javascript and css rewriting
                                    copyResponseHeaders(response, method);
                                    log.trace("fetch(): returning URL=" + url
                                            + ", as stream, content type="
                                            + contentType);
                                    ResourceUtil.copy(method
                                            .getResponseBodyAsStream(),
                                            response.getOutputStream(), 4096);
                                }
                            } else {
                                // if there is a problem with the status code
                                // then return that error back
                                response.sendError(statusCode);
                            }
                            return null;
                        }
                    });
        } catch (ResourceException exc) {
        	String msg = exc.getMessage();
        	try {
                msg = MessageFormat.format(resourceBundle.getString(msg), exc.getArgs());
        	} catch (Exception exc2) {
        		// do nothing here
        	}
            throw new ServletException(msg, exc);
        }
    }

    protected void doPost(final HttpServletRequest request,
            final HttpServletResponse response) throws ServletException,
            IOException {
        // get the id
        final String id = portletBridgeService.getIdFromRequestUri(request
                .getContextPath(), request.getRequestURI());
        // look up the data associated with that id from the session
        HttpSession session = request.getSession();
        if (session == null) {
            throw new ServletException(resourceBundle
                    .getString("error.nosession"));
        }
        final BPMPortletBridgeMemento memento = (BPMPortletBridgeMemento) session
                .getAttribute(mementoSessionKey);
        if (memento == null) {
            throw new ServletException(resourceBundle
                    .getString("error.nomemento"));
        }
        httpClientTemplate.setAuthScope(memento.getAuthScope());
        httpClientTemplate.setCredentials(memento.getCredentials());
        final BridgeRequest bridgeRequest = memento.getBridgeRequest(id);
        if (bridgeRequest == null) {
            throw new ServletException(resourceBundle
                    .getString("error.nobridgerequest"));
        }
        final PerPortletMemento perPortletMemento = memento
                .getPerPortletMemento(bridgeRequest.getPortletId());
        if (perPortletMemento == null) {
            throw new ServletException(resourceBundle
                    .getString("error.noperportletmemento"));
        }

        // go and fetch the data from the backend as appropriate
        final URI url = bridgeRequest.getUrl();

        log.debug("doPost(): URL=" + url);

        try {
            PostMethod postMethod = new PostMethod(url.toString());
            postMethod.addRequestHeader("bpm-portlet", "BPMServlet.doPost");
            postMethod.setRequestEntity(new InputStreamRequestEntity(request
                    .getInputStream()));
            copyRequestHeaders(request, postMethod);
            httpClientTemplate.service(postMethod, perPortletMemento,
                    new HttpClientCallback() {
                        public Object doInHttpClient(int statusCode,
                                HttpMethodBase method)
                                throws ResourceException, Throwable {
                            if (statusCode == HttpStatus.SC_OK) {
                                // if it's text/html then store it and redirect
                                // back to the portlet render view (portletUrl)
                                String contentType = "";
                                Header contentTypeHeader = method.getResponseHeader("Content-Type");
                                if (contentTypeHeader != null)
                                    contentType = contentTypeHeader.getValue();
                                if (contentType.startsWith("text/html")) {
                                    String content = ResourceUtil.getString(
                                            method.getResponseBodyAsStream(),
                                            method.getResponseCharSet());
                                    // TODO: think about cleaning this up if we
                                    // don't get back to the render
                                    perPortletMemento.enqueueContent(
                                            bridgeRequest.getId(),
                                            new PortletBridgeContent(url,
                                                    "post", content));
                                    // redirect
                                    // TODO: worry about this... adding the id
                                    // at the end
                                    log.debug("doPost(): doing response.sendRedirect to URL="
                                              + bridgeRequest.getPageUrl());

                                    response.sendRedirect(bridgeRequest.getPageUrl());
                                } else {
                                    // if it's anything else then stream it
                                    // back... consider stylesheets and
                                    // javascript
                                    // TODO: javascript and css rewriting
                                    copyResponseHeaders(response, method);
                                    ResourceUtil.copy(method
                                            .getResponseBodyAsStream(),
                                            response.getOutputStream(), 4096);
                                }
                            } else if (statusCode == HttpStatus.SC_MOVED_TEMPORARILY) {
                                Header locationHeader = method.getResponseHeader("Location");
                                if(locationHeader != null) {
                                    URI redirectUrl = new URI(locationHeader.getValue().trim());
                                    log.debug("redirecting to [" + redirectUrl + "]");
                                    PseudoRenderResponse renderResponse = createRenderResponse(bridgeRequest);
                                    BridgeRequest updatedBridgeRequest = memento.createBridgeRequest(renderResponse, BPMPortlet.BPMIdGenerator.getInstance().nextId(), redirectUrl);
                                    fetch(request, response, updatedBridgeRequest,
                                        memento, perPortletMemento, redirectUrl);

                                } else {
                                	throw new PortletBridgeException("error.missingLocation");
                                }
                            } else {
                                // if there is a problem with the status code
                                // then return that error back
                                response.sendError(statusCode);
                            }
                            return null;
                        }
                    });
        } catch (ResourceException resourceException) {
            String format = MessageFormat.format(resourceBundle
                    .getString(resourceException.getMessage()),
                    resourceException.getArgs());
            throw new ServletException(format, resourceException);
        }
    }

    public void setPortletBridgeService(
            PortletBridgeService portletBridgeService) {
        this.portletBridgeService = portletBridgeService;
    }

    public void setHttpClientTemplate(BPMHttpClientTemplate httpClientTemplate) {
        this.httpClientTemplate = httpClientTemplate;
    }

    protected void copyRequestHeaders(HttpServletRequest request,
            HttpMethodBase method) {
    	// headers we do not copy for now: Accept-Encoding, Cache-Control,
    	// Connection, Accept-Charset, Cookie (!)
    	for (String header: copyRequestHeaders){
        	String headervalue = request.getHeader(header);
            if (headervalue != null && !headervalue.equals(""))
            {
            	method.addRequestHeader(header, headervalue);
            }
    	}
    	if (method.getName().equals("POST")) {
	    	for (String header: copyRequestPostHeaders){
	        	String headervalue = request.getHeader(header);
	            if (headervalue != null && !headervalue.equals(""))
	            {
	            	method.addRequestHeader(header, headervalue);
	            }
	    	}
    	}
    }

    protected void copyResponseHeaders(HttpServletResponse response,
            HttpMethodBase method) {
    	for (String header: copyResponseHeaders){
    		Header h = method.getResponseHeader(header);
    		if (h == null)
    			continue;
        	String headervalue = h.getValue();
        	if (headervalue == null || headervalue.equals(""))
        		continue;
            response.setHeader(header, headervalue);
    	}
    }
}
