package com.subtickets;

import java.util.List;

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
    }

    public static class FieldNames {
        public static final String PLANNED_COSTS = "Planned Costs";
        public static final String ACTUAL_COSTS = "Actual Costs";
        public static final String FUND_TYPE = "Fund type";
        public static final String FUND_COLLECTION_MANNER = "Fund collection manner";
        public static final String ROOM = "Room";
        public static final String ROOMER = "Roomer";
        public static final String VOTE_SQUARE = "Vote Square";
    }

    public enum StatusCategoryName {

        TO_DO("To Do"), IN_PROGRESS("In progress"), DONE("Done");

        String value;

        StatusCategoryName(String value) {
            this.value = value;
        }

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
