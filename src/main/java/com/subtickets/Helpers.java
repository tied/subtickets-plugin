package com.subtickets;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.fields.CustomField;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import static com.subtickets.Constants.ACTUAL_COSTS_FIELD_NAME;
import static com.subtickets.Constants.FUND_COLLECTION_MANNER_FIELD_NAME;
import static com.subtickets.Constants.FUND_TYPE_FIELD_NAME;
import static com.subtickets.Constants.PLANNED_COSTS_FIELD_NAME;

@Component("homebrandedJiraEntities")
public class Helpers {

    public static CustomField plannedCostsField;
    public static CustomField actualCostsField;
    public static CustomField fundTypeField;
    public static CustomField fundCollectionMannerField;

    public Helpers() {
    }

    @PostConstruct
    private void post() {
        ComponentAccessor.getCustomFieldManager().getCustomFieldObjects().forEach(customField -> {
            switch (customField.getUntranslatedName()) {
                case ACTUAL_COSTS_FIELD_NAME:
                    actualCostsField = customField;
                    break;
                case PLANNED_COSTS_FIELD_NAME:
                    plannedCostsField = customField;
                    break;
                case FUND_TYPE_FIELD_NAME:
                    fundTypeField = customField;
                    break;
                case FUND_COLLECTION_MANNER_FIELD_NAME:
                    fundCollectionMannerField = customField;
                    break;
            }
        });

    }

}
