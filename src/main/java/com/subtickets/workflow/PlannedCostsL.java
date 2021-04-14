package com.subtickets.workflow;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.workflow.condition.AbstractJiraCondition;
import com.opensymphony.module.propertyset.PropertySet;
import com.subtickets.Constants;

import java.util.Map;

import static com.subtickets.Constants.FieldNames.PLANNED_COSTS;
import static com.subtickets.JiraConfiguration.customFields;

public class PlannedCostsL extends AbstractJiraCondition {

    public boolean passesCondition(Map transientVars, Map args, PropertySet ps) {
        Issue issue = getIssue(transientVars);
        Object value = issue.getCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(customFields.get(PLANNED_COSTS).getIdAsLong()));
        return value != null && Float.valueOf(value.toString()) < Constants.COSTS_THRESHOLD;
    }

}
