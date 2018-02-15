package com.subtickets.servlet;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.exception.CreateException;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.IssueInputParametersImpl;
import com.atlassian.jira.issue.ModifiedValue;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.label.LabelManager;
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.UserDetails;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.RequestFactory;
import com.atlassian.sal.api.net.ResponseException;
import com.subtickets.roomers.Roomer;
import com.subtickets.roomers.Roomers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.stream.Stream;

import static com.subtickets.Constants.ACTUAL_COSTS_FIELD_NAME;
import static com.subtickets.Constants.AUTO_COMPONENT_NAME;
import static com.subtickets.Constants.CUSTOM_FUND_TYPE_VALUE;
import static com.subtickets.Constants.DOOR_COMPONENT_NAME;
import static com.subtickets.Constants.ESTEBLISHED_FUND_TYPE_VALUE;
import static com.subtickets.Constants.FUND_TYPE_FIELD_NAME;
import static com.subtickets.Constants.PAYMENT_ISSUE_TYPE_NAME;
import static com.subtickets.Constants.PAYMENT_SUB_ISSUE_TYPE_NAME;
import static com.subtickets.Constants.FUND_COLLECTION_MANNER_FIELD_NAME;
import static com.subtickets.Constants.PLANNED_COSTS_FIELD_NAME;
import static com.subtickets.Constants.ROOMERS_URL;
import static com.subtickets.Constants.SQUARE_COMPONENT_NAME;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

@Named
public class SubTicketsServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(SubTicketsServlet.class);

    private static CustomField PLANNED_COSTS_FIELD;
    private static CustomField ACTUAL_COSTS_FIELD;
    private static CustomField FUND_TYPE_FIELD;
    private static CustomField FUND_COLLECTION_MANNER_TYPE_FIELD;

    private IssueType fundPaymentIssueType;
    private IssueType fundPaymentSubIssueType;

    private ApplicationUser roomerUser;

    @ComponentImport
    private IssueService issueService;

    private UserManager jiraUserManager;

    @ComponentImport
    private com.atlassian.sal.api.user.UserManager userManager;

    @ComponentImport
    private RequestFactory<?> requestFactory;

    @ComponentImport
    private LabelManager labelManager;

    @ComponentImport
    private SubTaskManager subTaskManager;

    @Inject
    public SubTicketsServlet(IssueService issueService, com.atlassian.sal.api.user.UserManager userManager, RequestFactory requestFactory, LabelManager labelManager,
                             SubTaskManager subTaskManager) {
        this.issueService = issueService;
        this.userManager = userManager;
        this.jiraUserManager = ComponentAccessor.getUserManager();
        this.requestFactory = requestFactory;
        this.labelManager = labelManager;
        this.subTaskManager = subTaskManager;

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

        CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
        ACTUAL_COSTS_FIELD = customFieldManager.getCustomFieldObjectByName(ACTUAL_COSTS_FIELD_NAME);
        PLANNED_COSTS_FIELD = customFieldManager.getCustomFieldObjectByName(PLANNED_COSTS_FIELD_NAME);
        FUND_TYPE_FIELD = customFieldManager.getCustomFieldObjectByName(FUND_TYPE_FIELD_NAME);
        FUND_COLLECTION_MANNER_TYPE_FIELD = customFieldManager.getCustomFieldObjectByName(FUND_COLLECTION_MANNER_FIELD_NAME);

        Collection<IssueType> issueTypes = ComponentAccessor.getConstantsManager().getAllIssueTypeObjects();
        issueTypes.forEach(type -> {
            switch (type.getName()) {
                case (PAYMENT_ISSUE_TYPE_NAME):
                    fundPaymentIssueType = type;
                    break;
                case (PAYMENT_SUB_ISSUE_TYPE_NAME):
                    fundPaymentSubIssueType = type;
                    break;
            }
        });
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ApplicationUser applicationUser = jiraUserManager.getUserByKey(userManager.getRemoteUser(req).getUserKey().getStringValue());
        MutableIssue issue = issueService.getIssue(applicationUser, req.getParameter("id")).getIssue();

        try {
            if (getFundType(issue).equals(ESTEBLISHED_FUND_TYPE_VALUE)) {
                createMonthlyPaymentsSubIssues(applicationUser, issue);
            } else if (getFundType(issue).equals(CUSTOM_FUND_TYPE_VALUE)) {
                createFundPaymentSubIssues(applicationUser, issue);
            }
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
        switch (getCollectionManner(issue)) {
            case AUTO_COMPONENT_NAME:
                createAutoFundPayments(user, issue);
                break;
            case DOOR_COMPONENT_NAME:
                createDoorFundPayments(user, issue);
                break;
            case SQUARE_COMPONENT_NAME:
                createSquareFundPayment(user, issue);
                break;
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
                        String[] derivedLabels = labels != null ? labels.apply(roomer) : new String [] {};
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
}