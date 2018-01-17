package com.subtickets.conditions;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.plugin.webfragment.conditions.AbstractWebCondition;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.security.roles.ProjectRole;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.PluginParseException;

import java.util.Map;

public class AccountantRoleCondition extends AbstractWebCondition {

    private static final String ACCOUNTANT_ROLE_NAME = "Accountant";

    private ProjectRoleManager roleManager;

    private ProjectRole accountantRole;

    public AccountantRoleCondition() {
        roleManager = ComponentAccessor.getComponent(ProjectRoleManager.class);
        accountantRole = roleManager.getProjectRole(ACCOUNTANT_ROLE_NAME);
    }

    @Override
    public void init(Map<String, String> params) throws PluginParseException {
    }

    @Override
    public boolean shouldDisplay(ApplicationUser user, JiraHelper jiraHelper) {
        return roleManager.isUserInProjectRole(user, accountantRole, jiraHelper.getProject());
    }
}
