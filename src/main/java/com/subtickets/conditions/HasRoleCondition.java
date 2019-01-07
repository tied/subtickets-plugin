package com.subtickets.conditions;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.plugin.webfragment.conditions.AbstractWebCondition;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.security.roles.ProjectRole;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.user.ApplicationUser;

public abstract class HasRoleCondition extends AbstractWebCondition {

    private ProjectRoleManager roleManager;

    private ProjectRole accountantRole;

    protected abstract String getRole();

    public HasRoleCondition() {
        roleManager = ComponentAccessor.getComponent(ProjectRoleManager.class);
        accountantRole = roleManager.getProjectRole(getRole());
    }

    @Override
    public boolean shouldDisplay(ApplicationUser user, JiraHelper jiraHelper) {
        return roleManager.isUserInProjectRole(user, accountantRole, jiraHelper.getProject());
    }

}