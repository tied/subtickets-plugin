package com.subtickets.conditions;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.webfragment.conditions.AbstractIssueWebCondition;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.user.ApplicationUser;

import java.util.regex.Pattern;

public abstract class AttachmentsPresent extends AbstractIssueWebCondition {

    abstract Pattern getAttachmentNamePattern();

    @Override
    public boolean shouldDisplay(ApplicationUser user, Issue issue, JiraHelper jiraHelper) {
        return ComponentAccessor.getAttachmentManager().getAttachments(issue).stream().anyMatch(attachment -> getAttachmentNamePattern().matcher(attachment.getFilename()).matches());
    }

}
