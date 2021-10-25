<%--
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
--%>

<%@ include file="/html/bpmportlet/init.jsp" %>

<%
String redirect = ParamUtil.getString(request, "redirect");
%>

<liferay-portlet:actionURL portletConfiguration="true" var="configurationURL" />
<style type="text/css">
    fieldset {
        width: 100%;
    }
</style>
<aui:form action="<%= configurationURL %>" method="post" name="fm">
	<aui:input name="<%= Constants.CMD %>" type="hidden" value="<%= Constants.UPDATE %>" />
	<aui:input name="redirect" type="hidden" value="<%= redirect %>" />


	<aui:fieldset title='<%= LanguageUtil.get(pageContext, "authentication") %>'>
		<aui:input cssClass="lfr-input-text-container" label="url" name="preferences--initUrl--" value="<%= initUrl %>" />
		<aui:input cssClass="lfr-use-for-all" label="queryStringPassthrough" type="checkbox"  name="preferences--queryStringPassthrough--" value="<%= queryStringPassthrough %>" />
		<%--
		<aui:input cssClass="lfr-input-text-container" label='<%= LanguageUtil.get(pageContext, "hostname") %>' name="preferences--authenticationHost--" value="<%= authenticationHost %>" />
		<aui:input cssClass="lfr-input-text-container" label='<%= LanguageUtil.get(pageContext, "port") %>' name="preferences--authenticationPort--" value="<%= authenticationPort %>" />
		<aui:input cssClass="lfr-input-text-container" label='<%= LanguageUtil.get(pageContext, "realm") %>' name="preferences--authenticationRealm--" value="<%= authenticationRealm %>" />
		<aui:input cssClass="lfr-input-text-container" label='<%= LanguageUtil.get(pageContext, "username") %>' name="preferences--authenticationUsername--" value="<%= authenticationUsername %>" />
		<aui:input cssClass="lfr-input-text-container" label='<%= LanguageUtil.get(pageContext, "password") %>' name="preferences--authenticationPassword--" value="<%= authenticationPassword %>" />
		--%>
	</aui:fieldset>

	<aui:button-row>
		<aui:button type="submit" />
	</aui:button-row>
</aui:form>

<c:if test="<%= windowState.equals(WindowState.MAXIMIZED) || windowState.equals(LiferayWindowState.POP_UP) %>">
	<aui:script>
		Liferay.Util.focusFormField(document.<portlet:namespace />fm.<portlet:namespace />initUrl);
	</aui:script>
</c:if> 
