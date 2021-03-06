/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
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

package com.liferay.content.targeting.rule.role;

import com.liferay.content.targeting.anonymous.users.model.AnonymousUser;
import com.liferay.content.targeting.api.model.BaseRule;
import com.liferay.content.targeting.api.model.Rule;
import com.liferay.content.targeting.model.RuleInstance;
import com.liferay.content.targeting.model.UserSegment;
import com.liferay.content.targeting.rule.categories.UserAttributesRuleCategory;
import com.liferay.content.targeting.util.ContentTargetingContextUtil;
import com.liferay.content.targeting.util.PortletKeys;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.lar.PortletDataContext;
import com.liferay.portal.kernel.lar.PortletDataException;
import com.liferay.portal.kernel.util.Constants;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Organization;
import com.liferay.portal.model.OrganizationConstants;
import com.liferay.portal.model.Role;
import com.liferay.portal.model.RoleConstants;
import com.liferay.portal.service.OrganizationLocalServiceUtil;
import com.liferay.portal.service.RoleLocalServiceUtil;
import com.liferay.portal.service.UserGroupRoleLocalServiceUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;

import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

/**
 * @author Eudaldo Alonso
 */
@Component(immediate = true, service = Rule.class)
public class OrganizationRoleRule extends BaseRule {

	@Activate
	@Override
	public void activate() {
		super.activate();
	}

	@Deactivate
	@Override
	public void deActivate() {
		super.deActivate();
	}

	@Override
	public boolean evaluate(
			HttpServletRequest request, RuleInstance ruleInstance,
			AnonymousUser anonymousUser)
		throws Exception {

		String typeSettings = ruleInstance.getTypeSettings();

		try {
			JSONObject jsonObj = JSONFactoryUtil.createJSONObject(typeSettings);

			long roleId = jsonObj.getLong("roleId");

			long organizationId = jsonObj.getLong("organizationId");

			Organization organization =
				OrganizationLocalServiceUtil.fetchOrganization(organizationId);

			if (organization == null) {
				return false;
			}

			return UserGroupRoleLocalServiceUtil.hasUserGroupRole(
				anonymousUser.getUserId(), organization.getGroupId(), roleId);
		}
		catch (JSONException e) {
		}

		return false;
	}

	@Override
	public void exportData(
			PortletDataContext portletDataContext, Element userSegmentElement,
			UserSegment userSegment, Element ruleInstanceElement,
			RuleInstance ruleInstance)
		throws Exception {

		String typeSettings = ruleInstance.getTypeSettings();

		try {
			JSONObject jsonObj = JSONFactoryUtil.createJSONObject(typeSettings);

			long organizationId = jsonObj.getLong("organizationId");

			Organization organization =
				OrganizationLocalServiceUtil.fetchOrganization(organizationId);

			if (organization == null) {
				throw new PortletDataException(
					getExportImportErrorMessage(
						userSegment, ruleInstance, Organization.class.getName(),
						String.valueOf(organizationId), Constants.EXPORT));
			}

			long roleId = jsonObj.getLong("roleId");

			Role role = RoleLocalServiceUtil.fetchRole(roleId);

			if (role == null) {
				throw new PortletDataException(
					getExportImportErrorMessage(
						userSegment, ruleInstance, Role.class.getName(),
						String.valueOf(roleId), Constants.EXPORT));
			}

			jsonObj = JSONFactoryUtil.createJSONObject();

			jsonObj.put("organizationUuid", organization.getUuid());
			jsonObj.put("roleUuid", role.getUuid());

			ruleInstance.setTypeSettings(jsonObj.toString());

			portletDataContext.addReferenceElement(
				ruleInstance, ruleInstanceElement, organization,
				PortletDataContext.REFERENCE_TYPE_WEAK, true);

			portletDataContext.addReferenceElement(
				ruleInstance, ruleInstanceElement, role,
				PortletDataContext.REFERENCE_TYPE_WEAK, true);
		}
		catch (JSONException e) {
		}
	}

	@Override
	public String getIcon() {
		return "icon-globe";
	}

	@Override
	public String getRuleCategoryKey() {
		return UserAttributesRuleCategory.KEY;
	}

	@Override
	public String getSummary(RuleInstance ruleInstance, Locale locale) {
		String typeSettings = ruleInstance.getTypeSettings();

		try {
			JSONObject jsonObj = JSONFactoryUtil.createJSONObject(typeSettings);

			long roleId = jsonObj.getLong("roleId");

			Role role = RoleLocalServiceUtil.fetchRole(roleId);

			if (role == null) {
				return StringPool.BLANK;
			}

			long organizationId = jsonObj.getLong("organizationId");

			Organization organization =
				OrganizationLocalServiceUtil.fetchOrganization(organizationId);

			if (organization == null) {
				return role.getTitle(locale);
			}

			StringBundler sb = new StringBundler();

			sb.append(role.getTitle(locale));
			sb.append(StringPool.SPACE);
			sb.append(organization.getName());

			return sb.toString();
		}
		catch (SystemException e) {
		}
		catch (JSONException e) {
		}

		return StringPool.BLANK;
	}

	@Override
	public void importData(
			PortletDataContext portletDataContext, UserSegment userSegment,
			RuleInstance ruleInstance)
		throws Exception {

		String typeSettings = ruleInstance.getTypeSettings();

		try {
			JSONObject jsonObj = JSONFactoryUtil.createJSONObject(typeSettings);

			String organizationUuid = jsonObj.getString("organizationUuid");

			Organization organization =
				OrganizationLocalServiceUtil.
					fetchOrganizationByUuidAndCompanyId(
						organizationUuid, portletDataContext.getCompanyId());

			if (organization == null) {
				throw new PortletDataException(
					getExportImportErrorMessage(
						userSegment, ruleInstance, Organization.class.getName(),
						organizationUuid, Constants.IMPORT));
			}

			String roleUuid = jsonObj.getString("roleUuid");

			Role role = RoleLocalServiceUtil.fetchRoleByUuidAndCompanyId(
				roleUuid, portletDataContext.getCompanyId());

			if (role == null) {
				throw new PortletDataException(
					getExportImportErrorMessage(
						userSegment, ruleInstance, Role.class.getName(),
						roleUuid, Constants.IMPORT));
			}

			jsonObj = JSONFactoryUtil.createJSONObject();

			jsonObj.put("organizationId", organization.getOrganizationId());
			jsonObj.put("roleId", role.getRoleId());

			ruleInstance.setTypeSettings(jsonObj.toString());
		}
		catch (JSONException e) {
		}
	}

	@Override
	public String processRule(
		PortletRequest request, PortletResponse response, String id,
		Map<String, String> values) {

		long roleId = GetterUtil.getLong(values.get("roleId"));
		long organizationId = GetterUtil.getLong(values.get("organizationId"));

		JSONObject jsonObj = JSONFactoryUtil.createJSONObject();

		jsonObj.put("organizationId", organizationId);
		jsonObj.put("roleId", roleId);

		return jsonObj.toString();
	}

	@Override
	protected String getFormTemplatePath() {
		return _FORM_TEMPLATE_PATH;
	}

	@Override
	protected void populateContext(
		RuleInstance ruleInstance, Map<String, Object> context,
		Map<String, String> values) {

		long roleId = 0;
		long organizationId = 0;

		if (!values.isEmpty()) {
			roleId = GetterUtil.getLong(values.get("roleId"));
			organizationId = GetterUtil.getLong(values.get("organizationId"));
		}
		else if (ruleInstance != null) {
			String typeSettings = ruleInstance.getTypeSettings();

			try {
				JSONObject jsonObj = JSONFactoryUtil.createJSONObject(
					typeSettings);

				roleId = jsonObj.getLong("roleId");
				organizationId = jsonObj.getLong("organizationId");
			}
			catch (JSONException jse) {
			}
		}

		context.put("roleId", roleId);
		context.put("organizationId", organizationId);

		Company company = (Company)context.get("company");

		List<Role> roles = new ArrayList<Role>();

		try {
			roles = RoleLocalServiceUtil.getRoles(
				company.getCompanyId(),
				new int[]{RoleConstants.TYPE_ORGANIZATION});

			Role role = RoleLocalServiceUtil.fetchRole(
				company.getCompanyId(), RoleConstants.ORGANIZATION_USER);

			List<Role> removeRoles = new ArrayList<Role>();

			removeRoles.add(role);

			roles = ListUtil.remove(roles, removeRoles);
		}
		catch (SystemException e) {
		}

		context.put("roles", roles);

		List<Organization> organizations = new ArrayList<Organization>();

		try {

			// See LPS-50218

			organizations = OrganizationLocalServiceUtil.getOrganizations(
				company.getCompanyId(),
				OrganizationConstants.ANY_PARENT_ORGANIZATION_ID);
		}
		catch (SystemException e) {
		}

		context.put("organizations", organizations);

		if ((organizations == null) || organizations.isEmpty()) {
			boolean hasUsersAdminViewPermission =
				ContentTargetingContextUtil.
					hasControlPanelPortletViewPermission(
						context, PortletKeys.USERS_ADMIN);

			if (hasUsersAdminViewPermission) {
				context.put(
					"usersAdminURL",
					ContentTargetingContextUtil.getControlPanelPortletURL(
						context, PortletKeys.USERS_ADMIN, null));
			}
		}
	}

	protected static final String _FORM_TEMPLATE_PATH =
		"templates/ct_fields_organization.ftl";

}