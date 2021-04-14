package com.subtickets.conditions;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.plugin.webfragment.conditions.AbstractWebCondition;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.ApplicationUser;

public abstract class UserInGroupCondition extends AbstractWebCondition {

    private GroupManager groupManager;

    protected abstract String getGroupName();

    public UserInGroupCondition() {
        groupManager = ComponentAccessor.getGroupManager();
    }

    @Override
    public boolean shouldDisplay(ApplicationUser user, JiraHelper jiraHelper) {
        return groupManager.isUserInGroup(user, getGroupName());
    }

}