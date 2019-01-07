package com.subtickets.conditions;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.link.IssueLink;
import com.atlassian.jira.plugin.webfragment.conditions.AbstractIssueWebCondition;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.user.ApplicationUser;

import java.util.List;

public abstract class LinkedIssuesPresent extends AbstractIssueWebCondition {

    abstract String getLinkedIssueType();

    @Override
    public boolean shouldDisplay(ApplicationUser user, Issue issue, JiraHelper jiraHelper) {
        List<IssueLink> inwardLinks = ComponentAccessor.getIssueLinkManager().getInwardLinks(issue.getId());
        IssueManager issueManager = ComponentAccessor.getIssueManager();
        return inwardLinks.stream().anyMatch(link -> issueManager.getIssueObject(link.getSourceId()).getIssueType().getName().equals(getLinkedIssueType()));
    }

}
