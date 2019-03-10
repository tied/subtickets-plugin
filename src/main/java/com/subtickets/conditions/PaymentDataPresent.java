package com.subtickets.conditions;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.user.ApplicationUser;
import com.subtickets.Constants;
import com.subtickets.JiraConfiguration;

import java.util.regex.Pattern;

public class PaymentDataPresent extends AttachmentsPresent {

    @Override
    Pattern getAttachmentNamePattern() {
        return Pattern.compile(Constants.JSON_FILE_PATTERN);
    }

    @Override
    public boolean shouldDisplay(ApplicationUser user, Issue issue, JiraHelper jiraHelper) {
        boolean esteblishedFundType = issue.getCustomFieldValue(JiraConfiguration.customFields.get(Constants.FieldNames.FUND_TYPE)).toString().equals("Esteblished");
        return !esteblishedFundType || super.shouldDisplay(user, issue, jiraHelper);
    }
}
