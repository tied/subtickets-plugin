package com.subtickets.conditions;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.webfragment.conditions.AbstractIssueWebCondition;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.user.ApplicationUser;

import static com.subtickets.Constants.IssueTypesNames.VOTING;

public class VotingIssueTypeCondition extends AbstractIssueWebCondition {

    @Override
    public boolean shouldDisplay(ApplicationUser user, Issue issue, JiraHelper jiraHelper) {
        return issue.getIssueType().getName().equals(VOTING);
    }

}
