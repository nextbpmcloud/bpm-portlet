package es.tangrambpm.liferay;

import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.portletbridge.portlet.BridgeAuthenticator;
import org.portletbridge.portlet.DefaultPerPortletMemento;
import org.portletbridge.portlet.SerializeableHttpState;

public class BPMPerPortletMemento extends DefaultPerPortletMemento {

	private AuthScope authscope = null;
	private UsernamePasswordCredentials credentials = null;

	public BPMPerPortletMemento(BridgeAuthenticator bridgeAuthenticator, AuthScope scope, UsernamePasswordCredentials cr) {
		super(bridgeAuthenticator);
		authscope = scope;
		credentials = cr;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -5103427167819010449L;

    public SerializeableHttpState getHttpState() {
        SerializeableHttpState state = super.getHttpState();
        state.setCredentials(authscope, credentials);
    	return state;
    }

}
