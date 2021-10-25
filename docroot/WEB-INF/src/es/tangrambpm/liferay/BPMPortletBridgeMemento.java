/*
 * Copyright 2001-2004 The Apache Software Foundation.
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
package es.tangrambpm.liferay;

import java.io.Serializable;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import javax.portlet.PortletURL;
import javax.portlet.RenderResponse;

import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.portletbridge.portlet.BridgeAuthenticator;
import org.portletbridge.portlet.BridgeRequest;
import org.portletbridge.portlet.DefaultBridgeRequest;
import org.portletbridge.portlet.PerPortletMemento;
import org.portletbridge.portlet.PortletBridgeMemento;

/**
 * @author JMcCrindle
 */
public class BPMPortletBridgeMemento implements PortletBridgeMemento, Serializable {

    /**
     * default serial version id
     */
    private static final long serialVersionUID = -5751042731400361166L;

    private Map<String, DefaultBridgeRequest> idToRequests = new ConcurrentHashMap<String, DefaultBridgeRequest>();
    private Map<String, DefaultBridgeRequest> dataToRequests = new ConcurrentHashMap<String, DefaultBridgeRequest>();
    private Map<String, PerPortletMemento> mementos = new ConcurrentHashMap<String, PerPortletMemento>();
    private final String idParamKey;
    private final BridgeAuthenticator bridgeAuthenticator;
	private AuthScope authscope = null;
	private UsernamePasswordCredentials credentials = null;
	private String saved_cookies = null;

    public BPMPortletBridgeMemento(String idParamKey, BridgeAuthenticator bridgeAuthenticator) {
        this.idParamKey = idParamKey;
        this.bridgeAuthenticator = bridgeAuthenticator;
    }

    public void setAuthScope(String host, int port, String realm) {
    	authscope = new AuthScope(host, port, realm);
    }

    public void setCredentials(String username, String password) {
    	credentials = new UsernamePasswordCredentials(username, password);
    }

    public UsernamePasswordCredentials getCredentials() {
    	return credentials;
    }

    public AuthScope getAuthScope() {
    	return authscope;
    }

    public void saveCookies(String cookies) {
    	saved_cookies = cookies;
    }

    public String getSavedCookies() {
    	return saved_cookies;
    }

    /* (non-Javadoc)
     * @see org.portletbridge.portlet.PortletBridgeMemento#getBridgeRequest(java.lang.String)
     */
    public BridgeRequest getBridgeRequest(String id) {
        return (BridgeRequest) idToRequests.get(id);
    }

    /* (non-Javadoc)
     * @see org.portletbridge.portlet.PortletBridgeMemento#getPerPortletMemento(java.lang.String)
     */
    public PerPortletMemento getPerPortletMemento(String portletId) {
        PerPortletMemento memento = (PerPortletMemento) mementos.get(portletId);
        if(memento == null) {
            memento = new BPMPerPortletMemento(bridgeAuthenticator, authscope, credentials);
            memento.getHttpState().setCredentials(authscope, credentials);
            mementos.put(portletId, memento);
        }
        return memento;
    }

    public BridgeRequest createBridgeRequest(RenderResponse response, String id, URI url) {
        PortletURL pageUrl = response.createRenderURL();
        String namespace = response.getNamespace();
        String key = namespace + pageUrl.toString() + url.toString();
        BridgeRequest request = (BridgeRequest) dataToRequests.get(key);
        if(request != null) {
            return request;
        } else {
            pageUrl.setParameter(idParamKey, id);
            DefaultBridgeRequest bridgeRequest = new DefaultBridgeRequest(id, namespace, pageUrl.toString(), url);
            idToRequests.put(id, bridgeRequest);
            dataToRequests.put(key, bridgeRequest);
            return bridgeRequest;
        }
    }

}
