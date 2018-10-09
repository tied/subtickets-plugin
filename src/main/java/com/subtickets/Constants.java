package com.subtickets;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;

public class Constants {

    public static final String PAYMENT_ISSUE_TYPE_NAME = "Payment";
    public static final String VOTING_ISSUE_TYPE_NAME = "Voting";
    public static final String VOTING_SESSION_ISSUE_TYPE_NAME = "Voting session";
    public static final String PAYMENT_SUB_ISSUE_TYPE_NAME = "Payment Notify";

    public static final String PLANNED_COSTS_FIELD_NAME = "Planned Costs";
    public static final String ACTUAL_COSTS_FIELD_NAME = "Actual Costs";
    public static final String FUND_TYPE_FIELD_NAME = "Fund type";
    public static final String FUND_COLLECTION_MANNER_FIELD_NAME = "Fund collection manner";

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
    public static final String GENERATE_VOTING_TEMPLATE_URL = "http://localhost:3000/createvotingtemplate";

    public static final String ACCOUNTANT_ROLE_NAME = "Accountant";
    public static final String ADMIN_ROLE_NAME = "Administrators";

    public static final String VOTING_TEMPLATE_FILE_NAME = "Template.pdf";

}
