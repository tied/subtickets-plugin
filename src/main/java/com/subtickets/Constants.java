package com.subtickets;

import com.atlassian.jira.issue.operation.ScreenableIssueOperation;

import java.util.List;

import static com.atlassian.jira.issue.operation.IssueOperations.CREATE_ISSUE_OPERATION;
import static com.atlassian.jira.issue.operation.IssueOperations.EDIT_ISSUE_OPERATION;
import static com.atlassian.jira.issue.operation.IssueOperations.VIEW_ISSUE_OPERATION;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;

public class Constants {

    public static class ProjectValues {
        public static final String KEY = "NSMD";
        public static final String NAME = "New OSMD";
    }

    public static class IssueTypesNames {
        public static final String IMPROVEMENT = "Improvement";
        public static final String INCIDENT = "Incident";
        public static final String NOTIFICATION = "Notification";
        public static final String PAYMENT = "Payment";
        public static final String TASK = "Task";
        public static final String VOTING = "Voting";
        public static final String VOTING_SESSION = "Voting session";
        public static final String PAYMENT_NOTIFY = "Payment Notify";
        public static final String SUB_TASK = "Sub-task";
        public static final String VOTING_NOTIFY = "Voting Notify";
        public static final String PUBLIC = "Public";
        public static final String BOARDING = "Boarding";
        public static final String BOARDING_VALIDATION = "Boarding Validation";
    }

    public static class FieldNames {
        public static final String CONTRACTOR = "Contractor";
        public static final String ACT_START_DATE = "Actual Start Date";
        public static final String ACT_END_DATE = "Actual End Date";
        public static final String PLANNED_COSTS = "Planned Costs";
        public static final String ACTUAL_COSTS = "Actual Costs";
        public static final String FUND_TYPE = "Fund type";
        public static final String FUND_COLLECTION_MANNER = "Fund collection manner";
        public static final String ROOM = "Room";
        public static final String ROOMER = "Roomer";
        public static final String VOTE_SQUARE = "Vote Square";
        public static final String ASSIGNEE = "Assignee";
        public static final String DESCRIPTION = "Description";
        public static final String SUMMARY = "Summary";
        public static final String LABELS = "Labels";
        public static final String DUE_DATE = "Due Date";
        public static final String PRIORITY = "Priority";
        public static final String CONTRACTOR_NAME = "Contractor Name";
        public static final String CONTRACTOR_ID = "Contractor ID";
        public static final String BUSINESS_ADDRESS = "Business Address";
        public static final String BUSINESS_MAIL = "Business Mail";
        public static final String CONTRACTOR_BANK = "Contractor Bank";
        public static final String CONTRACTOR_BANK_ID = "Contractor Bank ID";
        public static final String ACCOUNT_NUMBER = "Account Number";
        public static final String PHONE = "Phone";
        public static final String EDRPOU = "EDRPOU";
    }

    public enum StatusCategoryName {

        TO_DO("To Do"), IN_PROGRESS("In progress"), DONE("Done");

        String value;

        StatusCategoryName(String value) {
            this.value = value;
        }

    }

    public enum FieldAvailability {

        VIEW(VIEW_ISSUE_OPERATION), CREATE(CREATE_ISSUE_OPERATION), EDIT(EDIT_ISSUE_OPERATION);

        private ScreenableIssueOperation operation;

        public ScreenableIssueOperation getOperation() {
            return operation;
        }

        FieldAvailability(ScreenableIssueOperation operation) {
            this.operation = operation;
        }
    }

    public static class URL {
        public static final String SERVICE_ENDPOINT = "http://localhost:3000/";

        public static final String ROOMERS = "roomers";
        public static final String CREATE_SUBS = "createsubs";
        public static final String GET_VOTING_RESULTS = "getvotingresults";
        public static final String START_VOTING_SESSION = "startvotingsession";
        public static final String BOARDING_CREATED = "boardingcreated";
        public static final String TASK_CLOSED = "taskclosed";
    }

    public static final List<String> ESTEBLISHED_FUND_TYPE_VALUE = of("Esteblished", "Плановый", "Плановий").collect(toList());
    public static final List<String> CUSTOM_FUND_TYPE_VALUE = of("Custom", "Другое", "Інше").collect(toList());
    public static final List<String> DEVELOPMENT_FUND_TYPE_VALUE = of("Development", "Развитие", "Розвиток").collect(toList());

    public static final List<String> AUTO_COMPONENT_NAME = of("Авто", "Auto").collect(toList());
    public static final List<String> DOOR_COMPONENT_NAME = of("С квартиры", "З квартири", "Door").collect(toList());
    public static final List<String> SQUARE_COMPONENT_NAME = of("С кв. метра", "З кв. метра", "Sq").collect(toList());
    public static final String UNDERGROUND_COMPONENT_NAME = "Underground";

    public static final String BLOCKED_BY_LINK_TYPE_NAME = "заблокировона";

    public static final String ROOMERS_URL = "http://localhost:3000/roomers";
    public static final String CREATE_SUBS_URL = "http://localhost:3000/createsubs";
    public static final String GET_VOTING_RESULTS_URL = "http://localhost:3000/getvotingresults";
    public static final String START_VOTING_SESSION_URL = "http://localhost:3000/startvotingsession";

    public static final String ACCOUNTANT_ROLE_NAME = "Accountant";
    public static final String ADMIN_ROLE_NAME = "Administrators";

    public static final String VOTING_TEMPLATE_FILE_NAME = "Voting template.pdf";

    public static final Float COSTS_THRESHOLD = 5000f;


}
