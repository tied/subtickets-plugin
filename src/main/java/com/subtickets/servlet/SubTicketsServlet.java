package com.subtickets.servlet;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.exception.CreateException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.IssueInputParametersImpl;
import com.atlassian.jira.issue.ModifiedValue;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.label.LabelManager;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.UserDetails;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.RequestFactory;
import com.atlassian.sal.api.net.ResponseException;
import com.subtickets.Constants;
import com.subtickets.roomers.Roomer;
import com.subtickets.roomers.Roomers;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.subtickets.Constants.AUTO_COMPONENT_NAME;
import static com.subtickets.Constants.CREATE_SUBS_URL;
import static com.subtickets.Constants.DOOR_COMPONENT_NAME;
import static com.subtickets.Constants.FieldNames.ACTUAL_COSTS;
import static com.subtickets.Constants.FieldNames.FUND_COLLECTION_MANNER;
import static com.subtickets.Constants.FieldNames.FUND_TYPE;
import static com.subtickets.Constants.FieldNames.PLANNED_COSTS;
import static com.subtickets.Constants.ROOMERS_URL;
import static com.subtickets.Constants.SQUARE_COMPONENT_NAME;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

@Named
public class SubTicketsServlet extends HttpServlet {

    private static CustomField PLANNED_COSTS_FIELD;
    private static CustomField ACTUAL_COSTS_FIELD;
    private static CustomField FUND_TYPE_FIELD;
    private static CustomField FUND_COLLECTION_MANNER_TYPE_FIELD;

    private IssueType fundPaymentIssueType;
    private IssueType fundPaymentSubIssueType;

    private ApplicationUser roomerUser;

    private IssueLinkType blockedBy;

    @ComponentImport
    private IssueService issueService;

    @ComponentImport
    private com.atlassian.sal.api.user.UserManager userManager;

    @ComponentImport
    private RequestFactory<?> requestFactory;

    @ComponentImport
    private LabelManager labelManager;

    @ComponentImport
    private SubTaskManager subTaskManager;

    private UserManager jiraUserManager;

    private IssueLinkManager issueLinkManager;

    @Inject
    public SubTicketsServlet(IssueService issueService, com.atlassian.sal.api.user.UserManager userManager, RequestFactory requestFactory, LabelManager labelManager,
                             SubTaskManager subTaskManager) {
        this.issueService = issueService;
        this.userManager = userManager;
        this.jiraUserManager = ComponentAccessor.getUserManager();
        this.requestFactory = requestFactory;
        this.labelManager = labelManager;
        this.subTaskManager = subTaskManager;
        issueLinkManager = ComponentAccessor.getIssueLinkManager();

//        blockedBy = ComponentAccessor.getComponent(IssueLinkTypeManager.class)
//                .getIssueLinkTypesByInwardDescription(BLOCKED_BY_LINK_TYPE_NAME).stream().findFirst().get();

        if (this.jiraUserManager.getUserByName("Roomer") != null) {
            roomerUser = this.jiraUserManager.getUserByName("Roomer");
        } else if (this.jiraUserManager.getUserByName("roomer") != null) {
            roomerUser = this.jiraUserManager.getUserByName("roomer");
        } else {
            try {
                roomerUser = this.jiraUserManager.createUser(new UserDetails("Roomer", "Roomer"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        ComponentAccessor.getCustomFieldManager().getCustomFieldObjects().forEach(customField -> {
            switch (customField.getUntranslatedName()) {
                case ACTUAL_COSTS:
                    ACTUAL_COSTS_FIELD = customField;
                    break;
                case PLANNED_COSTS:
                    PLANNED_COSTS_FIELD = customField;
                    break;
                case FUND_TYPE:
                    FUND_TYPE_FIELD = customField;
                    break;
                case FUND_COLLECTION_MANNER:
                    FUND_COLLECTION_MANNER_TYPE_FIELD = customField;
                    break;
            }
        });

        Collection<IssueType> issueTypes = ComponentAccessor.getConstantsManager().getAllIssueTypeObjects();
        issueTypes.forEach(type -> {
            switch (type.getName()) {
                case (Constants.IssueTypesNames.PAYMENT):
                    fundPaymentIssueType = type;
                    break;
                case (Constants.IssueTypesNames.PAYMENT_NOTIFY):
                    fundPaymentSubIssueType = type;
                    break;
            }
        });
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ApplicationUser applicationUser = jiraUserManager.getUserByKey(userManager.getRemoteUser(req).getUserKey().getStringValue());
        String issueId = req.getParameter("id");
        MutableIssue issue = issueService.getIssue(applicationUser, issueId).getIssue();

        try {
//            String fundType = getFundType(issue);
//            Predicate<String> candidateMatch = getCandidateMatch(fundType);
//            if (ESTEBLISHED_FUND_TYPE_VALUE.stream().anyMatch(candidateMatch)) {
//                createMonthlyPaymentsSubIssues(applicationUser, issue);
//            } else if (CUSTOM_FUND_TYPE_VALUE.stream().anyMatch(candidateMatch)) {
//                createFundPaymentSubIssues(applicationUser, issue);
//            }
            Request<?, ?> request = requestFactory.createRequest(Request.MethodType.POST, CREATE_SUBS_URL + "?id=" + issueId);
            request.setSoTimeout(200000);
            String execute = request.execute();
            System.out.println(execute);
        } catch (ResponseException e) {
            e.printStackTrace();
        }

        resp.sendRedirect(req.getHeader("referer"));
    }

    private void createMonthlyPaymentsSubIssues(ApplicationUser user, Issue issue) throws ResponseException {
        getRoomers().forEach((key, roomer) -> {
            IssueInputParameters parameters = generateIssueInputParameters(user, issue)
                    .setSummary(issue.getSummary() + " - " + roomer.holderInfo);
            Issue subIssue = doCreateSubIssue(user, issue, parameters);
            if (subIssue != null) {
                setPlannedCosts(subIssue, roomer.amount);
                addLabels(user, subIssue, roomer.owned_Doors);
            }
        });
    }

    private void createFundPaymentSubIssues(ApplicationUser user, Issue issue) throws ResponseException {
        String collectionManner = getCollectionManner(issue);
        Predicate<String> candidateMatch = getCandidateMatch(collectionManner);
        if (AUTO_COMPONENT_NAME.stream().anyMatch(candidateMatch)) {
            createAutoFundPayments(user, issue);
        } else if (DOOR_COMPONENT_NAME.stream().anyMatch(candidateMatch)) {
            createDoorFundPayments(user, issue);
        } else if (SQUARE_COMPONENT_NAME.stream().anyMatch(candidateMatch)) {
            createSquareFundPayment(user, issue);
        }
    }

    private void createAutoFundPayments(ApplicationUser user, Issue issue) throws ResponseException {
        createFundPayments(user, issue, Roomer::getAutosToCount, Roomer::getAutos);
    }

    private void createDoorFundPayments(ApplicationUser user, Issue issue) throws ResponseException {
        createFundPayments(user, issue, Roomer::getDoorsToCount, Roomer::getDoors);
    }

    private void createSquareFundPayment(ApplicationUser user, Issue issue) throws ResponseException {
        createFundPayments(user, issue, Roomer::getSquareToCount, null);
    }

    private void createFundPayments(ApplicationUser user, Issue issue, Function<Roomer, Number> items, Function<Roomer, String[]> labels) throws ResponseException {
        Roomers roomers = getRoomers();
        Double itemsCount = roomers.values().stream()
                .map(items)
                .mapToDouble(Number::doubleValue)
                .sum();
        Double plannedCosts = getPlannedCosts(issue);
        Double singlePayment = (Math.ceil((plannedCosts / itemsCount) * 100)) / 100.0;
        roomers.values().stream()
                .filter(roomer -> items.apply(roomer).doubleValue() > 0)
                .forEach(roomer -> {
                    IssueInputParameters parameters = generateIssueInputParameters(user, issue)
                            .setSummary(issue.getSummary() + " - " + roomer.fio);
                    Issue subIssue = doCreateSubIssue(user, issue, parameters);
                    if (subIssue != null) {
                        setPlannedCosts(subIssue, singlePayment * items.apply(roomer).doubleValue());
                        String[] derivedLabels = labels != null ? labels.apply(roomer) : new String[]{};
                        addLabels(user, subIssue, Stream.concat(Arrays.stream(derivedLabels), Stream.of(getInitials(roomer.fio))).collect(toSet()));
                    }
                });
    }

    private IssueInputParameters generateIssueInputParameters(ApplicationUser user, Issue issue) {
        return new IssueInputParametersImpl()
                .setAssigneeId(roomerUser.getName())
                .setReporterId(user.getName())
                .setProjectId(issue.getProjectId())
                .setIssueTypeId(fundPaymentSubIssueType.getId());
    }

    private Issue doCreateSubIssue(ApplicationUser user, Issue parentIssue, IssueInputParameters parameters) {
        IssueService.CreateValidationResult validationResult = issueService.validateSubTaskCreate(user, parentIssue.getId(), parameters);
        if (validationResult.isValid()) {
            IssueService.IssueResult issueResult = issueService.create(user, validationResult);
            MutableIssue subIssue = issueResult.getIssue();
            try {
                subTaskManager.createSubTaskIssueLink(parentIssue, subIssue, user);
                issueLinkManager.createIssueLink(subIssue.getId(), parentIssue.getId(), blockedBy.getId(), 0L, user);
            } catch (CreateException e) {
                e.printStackTrace();
            }
            return subIssue;
        }
        return null;
    }

    private String getCollectionManner(Issue issue) {
        return issue.getCustomFieldValue(FUND_COLLECTION_MANNER_TYPE_FIELD).toString();
    }

    private void setActualCosts(Issue issue, Double value) {
        ModifiedValue modifiedValue = new ModifiedValue<>(0.0d, value);
        PLANNED_COSTS_FIELD.updateValue(null, issue, modifiedValue, new DefaultIssueChangeHolder());
    }

    private void setPlannedCosts(Issue issue, Double value) {
        ModifiedValue modifiedValue = new ModifiedValue<>(0.0d, value);
        PLANNED_COSTS_FIELD.updateValue(null, issue, modifiedValue, new DefaultIssueChangeHolder());
    }

    private Double getPlannedCosts(Issue issue) {
        return (Double) issue.getCustomFieldValue(PLANNED_COSTS_FIELD);
    }

    private String getFundType(Issue issue) {
        return issue.getCustomFieldValue(FUND_TYPE_FIELD).toString();
    }

    private String getInitials(String fullName) {
        String[] names = fullName.split(" ");
        String initials = Arrays.stream(names, 1, names.length)
                .map(name -> String.valueOf(name.charAt(0)))
                .collect(joining("."));
        return names[0] + "_" + initials + ".";
    }

    private Roomers getRoomers() throws ResponseException {
        return requestFactory.createRequest(Request.MethodType.GET, ROOMERS_URL).executeAndReturn(new RoomersResponseHandler());
    }

    private void addLabels(ApplicationUser user, Issue issue, Collection<String> labels) {
        Set<String> labelsSet = labels.stream().map(label -> label.replaceAll(" ", "_")).collect(toSet());
        labelManager.setLabels(user, issue.getId(), labelsSet, true, true);
    }

    private void addLabels(ApplicationUser user, Issue issue, String... labels) {
        addLabels(user, issue, Stream.of(labels).collect(toSet()));
    }

    private Predicate<String> getCandidateMatch(String target) {
        return candidate -> candidate.equals(target);
    }
}