package com.subtickets.workflow;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.workflow.condition.AbstractJiraCondition;
import com.opensymphony.module.propertyset.PropertySet;
import com.subtickets.Constants;
import com.subtickets.Helpers;

import java.util.Map;

public class PlannedCostsGE extends AbstractJiraCondition {

    public boolean passesCondition(Map transientVars, Map args, PropertySet ps) {
        Issue issue = getIssue(transientVars);
        Object value = issue.getCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(Helpers.plannedCostsField.getIdAsLong()));
        return value != null && Float.valueOf(value.toString()) >= Constants.COSTS_THRESHOLD;
    }

}
