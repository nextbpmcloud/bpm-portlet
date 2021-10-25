<%@ include file="/html/portlet/init.jsp" %>


<%
PortletPreferences preferences = renderRequest.getPreferences();

String portletResource = ParamUtil.getString(request, "portletResource");

if (Validator.isNotNull(portletResource)) {
	preferences = PortletPreferencesFactoryUtil.getPortletSetup(request, portletResource);
}

String initUrl = preferences.getValue("initUrl", StringPool.BLANK);
boolean queryStringPassthrough  = GetterUtil.getBoolean(preferences.getValue("queryStringPassthrough", StringPool.TRUE));
String authenticationHost = preferences.getValue("authenticationHost", StringPool.BLANK);
String authenticationPort = preferences.getValue("authenticationPort", StringPool.BLANK);
String authenticationRealm = preferences.getValue("authenticationRealm", StringPool.BLANK);
String authenticationUsername = preferences.getValue("authenticationUsername", StringPool.BLANK);
String authenticationPassword = preferences.getValue("authenticationPassword", StringPool.BLANK);
String scope = preferences.getValue("scope", StringPool.BLANK);
String proxyHost = preferences.getValue("proxyHost", StringPool.BLANK);
int proxyPort = GetterUtil.getInteger(preferences.getValue("proxyPort", null), -1);
String proxyAuthentication = preferences.getValue("proxyAuthentication", StringPool.BLANK);
String proxyAuthenticationUsername = preferences.getValue("proxyAuthenticationUsername", StringPool.BLANK);
String proxyAuthenticationPassword = preferences.getValue("proxyAuthenticationPassword", StringPool.BLANK);
String proxyAuthenticationHost = preferences.getValue("proxyAuthenticationHost", StringPool.BLANK);
String proxyAuthenticationDomain = preferences.getValue("proxyAuthenticationDomain", StringPool.BLANK);
String stylesheet = preferences.getValue("stylesheet", StringPool.BLANK);
%> 
