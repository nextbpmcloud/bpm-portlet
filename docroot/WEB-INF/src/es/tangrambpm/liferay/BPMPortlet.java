/**
 * Copyright (c) 2000-2012 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package es.tangrambpm.liferay;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.Enumeration;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.Portlet;
import javax.portlet.PortletConfig;
import javax.portlet.PortletException;
import javax.portlet.PortletMode;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.portletbridge.ResourceException;
import org.portletbridge.portlet.AltBridgeTransformer;
import org.portletbridge.portlet.BridgeAuthenticator;
import org.portletbridge.portlet.BridgeEditPortlet;
import org.portletbridge.portlet.BridgeFunctionsFactory;
import org.portletbridge.portlet.BridgeHelpPortlet;
import org.portletbridge.portlet.BridgeTransformer;
import org.portletbridge.portlet.BridgeViewPortlet;
import org.portletbridge.portlet.ContentRewriter;
import org.portletbridge.portlet.DefaultIdGenerator;
import org.portletbridge.portlet.IdGenerator;
import org.portletbridge.portlet.PortletBridgePortlet;
import org.portletbridge.portlet.PortletFunctions;
import org.portletbridge.portlet.RegexContentRewriter;
import org.portletbridge.portlet.TemplateFactory;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.ServerDetector;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;

/**
 * @author Brian Wing Shun Chan
 */
public class BPMPortlet extends PortletBridgePortlet {

    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory
            .getLog(BPMPortlet.class);

    private Portlet viewPortlet = null;
    private Portlet editPortlet = null;
    private Portlet helpPortlet = null;
    private Templates errorTemplates = null;

	@Override
	public void init() throws PortletException {
//		try {
	        
	        TemplateFactory templateFactory = new BPMTemplateFactory();
	        
	        // ResourceBundle resourceBundle = config.getResourceBundle(Locale.getDefault());
	        ResourceBundle resourceBundle = PropertyResourceBundle.getBundle("org.portletbridge.portlet.PortletBridgePortlet");

	        // initialise portlets
	        viewPortlet = createViewPortlet(resourceBundle, templateFactory);
	        editPortlet = createEditPortlet(resourceBundle, templateFactory);
	        helpPortlet = createHelpPortlet(resourceBundle, templateFactory);

	        createErrorTemplates(resourceBundle, templateFactory);

	        if(viewPortlet != null) {
	            viewPortlet.init(this.getPortletConfig());
	        }
	        if(editPortlet != null) {
	            editPortlet.init(this.getPortletConfig());
	        }
	        if(helpPortlet != null) {
	            helpPortlet.init(this.getPortletConfig());
	        }

			_enabled = true;
//		}
//		catch (Exception e) {
//			if (_log.isWarnEnabled()) {
//				_log.warn(e.getMessage());
//			}
//		}

		if (!_enabled && ServerDetector.isWebLogic() && _log.isInfoEnabled()) {
			_log.info(
				"WebProxyPortlet will not be enabled unless Liferay's " +
					"serializer.jar and xalan.jar files are copied to the " +
						"JDK's endorsed directory");
		}
	}

	@Override
	public void doView(
			RenderRequest renderRequest, RenderResponse renderResponse)
		throws IOException, PortletException {

		if (!_enabled) {
			printError(renderResponse);

			return;
		}

		PortletPreferences preferences = renderRequest.getPreferences();

		String initUrl = preferences.getValue("initUrl", StringPool.BLANK);
		
		if (Validator.isNull(initUrl)) {
			PortletRequestDispatcher portletRequestDispatcher =
				getPortletContext().getRequestDispatcher(
					"/html/portal/portlet_not_setup.jsp");

			portletRequestDispatcher.include(renderRequest, renderResponse);
		}
		else {
	        if(viewPortlet != null) {
	            viewPortlet.render(renderRequest, renderResponse);
	        } else {
	        	_log.warn("viewPortlet is null! Can't render response...");
	        }

/*			RenderResponseImpl renderResponseImpl =
				(RenderResponseImpl)renderResponse;

			StringServletResponse stringResponse = (StringServletResponse)
				renderResponseImpl.getHttpServletResponse();

			String output = stringResponse.getString();

			output = StringUtil.replace(output, "//bpmportlet/", "/bpmportlet/");

			stringResponse.setString(output);*/
		}
	}

	protected void printError(RenderResponse renderResponse)
		throws IOException {

		renderResponse.setContentType(ContentTypes.TEXT_HTML_UTF8);

		PrintWriter writer = renderResponse.getWriter();

		writer.print(
			"The BPM Portlet is not enabled due to an previuos error.");

		writer.close();
	}

    public class BPMIdGenerator extends DefaultIdGenerator {
        public synchronized String nextId() {
            String nextId = super.nextId();
            return nextId.replace("*", "_");
        }
    }

    protected BridgeViewPortlet createViewPortlet(ResourceBundle resourceBundle, TemplateFactory templateFactory) throws PortletException {
        PortletConfig config = this.getPortletConfig();

        // get the memento session key
        String mementoSessionKey = config.getInitParameter("mementoSessionKey");
        if (mementoSessionKey == null) {
            throw new PortletException(resourceBundle
                    .getString("error.mementoSessionKey"));
        }
        // get the servlet name
        String servletName = config.getInitParameter("servletName");
        if (servletName == null) {
            throw new PortletException(resourceBundle
                    .getString("error.servletName"));
        }
        // get parserClassName
        String parserClassName = config.getInitParameter("parserClassName");
        if (parserClassName == null) {
            throw new PortletException(resourceBundle
                    .getString("error.parserClassName"));
        }
        // get authenticatorClassName
        String authenticatorClassName = config.getInitParameter("authenticatorClassName");
        if (authenticatorClassName == null) {
            throw new PortletException(resourceBundle
                    .getString("error.authenticatorClassName"));
        }
        BridgeAuthenticator bridgeAuthenticator = null;
        try {
            Class<?> authenticatorClass = Class.forName(authenticatorClassName);
            bridgeAuthenticator = (BridgeAuthenticator) authenticatorClass.newInstance();
        } catch (ClassNotFoundException e) {
            log.warn(e, e);
            throw new PortletException(resourceBundle
                    .getString("error.authenticator"));
        } catch (InstantiationException e) {
            log.warn(e, e);
            throw new PortletException(resourceBundle
                    .getString("error.authenticator"));
        } catch (IllegalAccessException e) {
            log.warn(e, e);
            throw new PortletException(resourceBundle
                    .getString("error.authenticator"));
        }
        String idParamKey = config.getInitParameter("idParamKey");
        // setup parser
        BridgeTransformer transformer = null;
        try {
            String cssRegex = config.getInitParameter("cssRegex");
            String javascriptRegex = config.getInitParameter("jsRegex");
            XMLReader parser = XMLReaderFactory.createXMLReader(parserClassName);
            for(Enumeration<?> names = config.getInitParameterNames(); names.hasMoreElements(); ) {
                String name = (String) names.nextElement();
                if(name.startsWith("parserFeature-")) {
                    parser.setFeature(name.substring("parserFeature-".length()), "true".equalsIgnoreCase(config.getInitParameter(name)));
                } else if (name.startsWith("parserProperty-")) {
                    parser.setProperty(name.substring("parserProperty-".length()), config.getInitParameter(name));
                }
            }
            IdGenerator idGenerator = BPMIdGenerator.getInstance();
            ContentRewriter javascriptRewriter = new RegexContentRewriter(javascriptRegex);
            ContentRewriter cssRewriter = new RegexContentRewriter(cssRegex);
            BridgeFunctionsFactory bridgeFunctionsFactory = new BridgeFunctionsFactory(idGenerator, javascriptRewriter, cssRewriter); 
            transformer = new AltBridgeTransformer(bridgeFunctionsFactory, templateFactory, parser, servletName);
        } catch (SAXNotRecognizedException e) {
            throw new PortletException(e);
        } catch (SAXNotSupportedException e) {
            throw new PortletException(e);
        } catch (SAXException e) {
            throw new PortletException(e);
        }

        BPMViewPortlet bridgeViewPortlet = new BPMViewPortlet();
        
        bridgeViewPortlet.setHttpClientTemplate(new BPMHttpClientTemplate());
        bridgeViewPortlet.setTransformer(transformer);
        bridgeViewPortlet.setMementoSessionKey(mementoSessionKey);
        bridgeViewPortlet.setBridgeAuthenticator(bridgeAuthenticator);
        if(idParamKey != null) {
            bridgeViewPortlet.setIdParamKey(idParamKey);
        }
        return bridgeViewPortlet;
    }
	
	private static Log _log = LogFactoryUtil.getLog(BPMPortlet.class);

	private boolean _enabled;

    protected void createErrorTemplates(ResourceBundle resourceBundle, TemplateFactory templateFactory) throws PortletException {
        // get the error stylesheet reference
        String errorStylesheet = getPortletConfig().getInitParameter("errorStylesheet");
        if (errorStylesheet == null) {
            throw new PortletException(resourceBundle
                    .getString("error.error.stylesheet"));
        }
        
        try {
            errorTemplates = templateFactory.getTemplatesFromUrl(errorStylesheet);
        } catch (ResourceException e) {
            throw new PortletException(e);
        } catch (TransformerFactoryConfigurationError e) {
            throw new PortletException(e);
        }
    }

    protected BridgeEditPortlet createEditPortlet(ResourceBundle resourceBundle, TemplateFactory templateFactory) throws PortletException {
        PortletConfig config = this.getPortletConfig();

        // get the edit stylesheet reference
        String editStylesheet = config.getInitParameter("editStylesheet");
        if (editStylesheet == null) {
            throw new PortletException(resourceBundle
                    .getString("error.edit.stylesheet"));
        }

        BridgeEditPortlet bridgeEditPortlet = new BridgeEditPortlet();
        try {
            bridgeEditPortlet.setTemplates(templateFactory.getTemplatesFromUrl(editStylesheet));
        } catch (ResourceException e) {
            throw new PortletException(e);
        } catch (TransformerFactoryConfigurationError e) {
            throw new PortletException(e);
        }
        return bridgeEditPortlet;
    }
    
    protected BridgeHelpPortlet createHelpPortlet(ResourceBundle resourceBundle, TemplateFactory templateFactory) throws PortletException {
        PortletConfig config = this.getPortletConfig();

        // get the help stylesheet reference
        String editStylesheet = config.getInitParameter("helpStylesheet");
        if (editStylesheet == null) {
            throw new PortletException(resourceBundle
                    .getString("error.help.stylesheet"));
        }

        BridgeHelpPortlet bridgeHelpPortlet = new BridgeHelpPortlet();
        try {
            bridgeHelpPortlet.setTemplates(templateFactory.getTemplatesFromUrl(editStylesheet));
        } catch (ResourceException e) {
            throw new PortletException(e);
        } catch (TransformerFactoryConfigurationError e) {
            throw new PortletException(e);
        }
        return bridgeHelpPortlet;
    }
    
    
    /* (non-Javadoc)
     * @see javax.portlet.GenericPortlet#render(javax.portlet.RenderRequest, javax.portlet.RenderResponse)
     */
    public void render(RenderRequest request, RenderResponse response)
            throws PortletException, IOException {
        try {
            super.render(request, response);
        } catch (Throwable exception) {
            // getPortletConfig().getPortletContext().log(exception.getMessage(), exception);
            // using this instead because pluto doesn't seem to print out portletcontext logs
            log.warn(exception, exception);
            response.setContentType("text/html");
            try {
                Transformer transformer = errorTemplates.newTransformer();
                transformer.setParameter("portlet", new PortletFunctions(request, response));
                transformer.setParameter("exception", exception);
                transformer.transform(new StreamSource(new StringReader("<xml/>")), new StreamResult(response.getWriter()));
            } catch (TransformerConfigurationException e) {
                throw new PortletException(e);
            } catch (TransformerException e) {
                throw new PortletException(e);
            } catch (IOException e) {
                throw new PortletException(e);
            }
        }
    }

    
    /* (non-Javadoc)
     * @see javax.portlet.GenericPortlet#doEdit(javax.portlet.RenderRequest, javax.portlet.RenderResponse)
     */
    protected void doEdit(RenderRequest request, RenderResponse response)
            throws PortletException, IOException {
        if(editPortlet != null) {
            editPortlet.render(request, response);
        }
    }
    
    /* (non-Javadoc)
     * @see javax.portlet.GenericPortlet#doHelp(javax.portlet.RenderRequest, javax.portlet.RenderResponse)
     */
    protected void doHelp(RenderRequest request, RenderResponse response)
            throws PortletException, IOException {
        if(helpPortlet != null) {
            helpPortlet.render(request, response);
        }
    }
    
    /* (non-Javadoc)
     * @see javax.portlet.GenericPortlet#processAction(javax.portlet.ActionRequest, javax.portlet.ActionResponse)
     */
    public void processAction(ActionRequest request, ActionResponse response)
            throws PortletException, IOException {
        PortletMode portletMode = request.getPortletMode();
        if(portletMode.equals(PortletMode.VIEW)) {
            viewPortlet.processAction(request, response);
        } else if(portletMode.equals(PortletMode.EDIT)) {
            editPortlet.processAction(request, response);
        } else if (portletMode.equals(PortletMode.HELP)) {
            helpPortlet.processAction(request, response);
        }
    }

	public Portlet getEditPortlet() {
		return editPortlet;
	}

	public void setEditPortlet(Portlet editPortlet) {
		this.editPortlet = editPortlet;
	}

	public Portlet getHelpPortlet() {
		return helpPortlet;
	}

	public void setHelpPortlet(Portlet helpPortlet) {
		this.helpPortlet = helpPortlet;
	}

	public Portlet getViewPortlet() {
		return viewPortlet;
	}

	public void setViewPortlet(Portlet viewPortlet) {
		this.viewPortlet = viewPortlet;
	}

}