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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Named
public class SubTicketsServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(SubTicketsServlet.class);

    private static final String ROOMERS_URL = "http://5.101.122.147:8089/api/osbb/command/roomers";

    private static final String FUND_PAYMENT_ISSUE_TYPE_NAME = "Fund Payment";
    private static final String FUND_PAYMENT_SUB_ISSUE_TYPE_NAME = "Item Fund Payment";
    private static final String MONTHLY_PAYMENT_ISSUE_TYPE_NAME = "Monthly Payment";
    private static final String MONTHLY_PAYMENT_SUB_ISSUE_TYPE_NAME = "Roomer Monthly Payment";

    private static final String PLANNED_COSTS_FIELD_NAME = "plannedCosts";
    private static final String ACTUAL_COSTS_FIELD_NAME = "actualCosts";

    private static CustomField PLANNED_COSTS_FIELD;
    private static CustomField ACTUAL_COSTS_FIELD;

    private static final String AUTO_COMPONENT_NAME = "Auto";
    private static final String DOOR_COMPONENT_NAME = "Door";
    private static final String SQUARE_COMPONENT_NAME = "Square";

    private IssueType fundPaymentIssueType;
    private IssueType fundPaymentSubIssueType;
    private IssueType monthlyPaymentIssueType;
    private IssueType monthlyPaymentSubIssueType;

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

        CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
        ACTUAL_COSTS_FIELD = customFieldManager.getCustomFieldObjectByName(ACTUAL_COSTS_FIELD_NAME);
        PLANNED_COSTS_FIELD = customFieldManager.getCustomFieldObjectByName(PLANNED_COSTS_FIELD_NAME);

        Collection<IssueType> issueTypes = ComponentAccessor.getConstantsManager().getAllIssueTypeObjects();
        issueTypes.forEach(type -> {
            switch (type.getName()) {
                case (FUND_PAYMENT_ISSUE_TYPE_NAME):
                    fundPaymentIssueType = type;
                    break;
                case (FUND_PAYMENT_SUB_ISSUE_TYPE_NAME):
                    fundPaymentSubIssueType = type;
                    break;
                case (MONTHLY_PAYMENT_ISSUE_TYPE_NAME):
                    monthlyPaymentIssueType = type;
                    break;
                case (MONTHLY_PAYMENT_SUB_ISSUE_TYPE_NAME):
                    monthlyPaymentSubIssueType = type;
                    break;
            }
        });
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ApplicationUser applicationUser = jiraUserManager.getUserByKey(userManager.getRemoteUser(req).getUserKey().getStringValue());
        MutableIssue issue = issueService.getIssue(applicationUser, req.getParameter("id")).getIssue();

        try {
            if (issue.getIssueType() == monthlyPaymentIssueType) {
                createMonthlyPaymentsSubIssues(applicationUser, issue);
            } else if (issue.getIssueType() == fundPaymentIssueType) {
                createFundPaymentSubIssues(applicationUser, issue);
            }
        } catch (ResponseException e) {
            e.printStackTrace();
        }

        resp.sendRedirect(req.getHeader("referer"));
    }

    private void createMonthlyPaymentsSubIssues(ApplicationUser user, Issue issue) throws ResponseException {
        Roomers roomers = requestFactory.createRequest(Request.MethodType.GET, ROOMERS_URL).executeAndReturn(new RoomersResponseHandler());
        roomers.forEach((key, roomer) -> {
            IssueInputParameters parameters = generateIssueInputParameters(user, issue)
                    .setSummary(issue.getSummary() + " " + roomer.holderInfo);
            Issue subIssue = doCreateSubIssue(user, issue, parameters);
            if (subIssue != null) {
                setActualCosts(subIssue, roomer.amount);
                labelManager.setLabels(user, subIssue.getId(), new HashSet<>(Arrays.asList(roomer.owned_Doors)), true, true);
            }
        });
    }

    private void createFundPaymentSubIssues(ApplicationUser user, Issue issue) throws ResponseException {
        String component = new ArrayList<>(issue.getComponents()).get(0).getName();
        switch (component) {
            case AUTO_COMPONENT_NAME:
                createAutoFundPayments(user, issue);
                break;
            case DOOR_COMPONENT_NAME:
                createDoorFundPayments(user, issue);
        }
    }

    private void createAutoFundPayments(ApplicationUser user, Issue issue) throws ResponseException {
        createFundPayments(user, issue, Roomer::getAutos);

    }

    private void createDoorFundPayments(ApplicationUser user, Issue issue) throws ResponseException {
        createFundPayments(user, issue, Roomer::getDoors);

    }

    private void createFundPayments(ApplicationUser user, Issue issue, Function<Roomer, String[]> items) throws ResponseException {
        Roomers roomers = requestFactory.createRequest(Request.MethodType.GET, ROOMERS_URL).executeAndReturn(new RoomersResponseHandler());
        long itemsCount = roomers.values().stream()
                .map(items)
                .flatMap(Arrays::stream)
                .count();
        Double plannedCosts = (Double) issue.getCustomFieldValue(PLANNED_COSTS_FIELD);
        Double singlePayment = plannedCosts / itemsCount;
        roomers.values()
                .stream()
                .filter(roomer -> items.apply(roomer).length > 0)
                .forEach(roomer -> {
                    IssueInputParameters parameters = generateIssueInputParameters(user, issue)
                            .setSummary(issue.getSummary() + " - " + roomer.fio);
                    Issue subIssue = doCreateSubIssue(user, issue, parameters);
                    if (subIssue != null) {
                        String[] roomerItems = items.apply(roomer);
                        setActualCosts(subIssue, singlePayment * roomerItems.length);
                        setPlannedCosts(subIssue, plannedCosts);
                        Set<String> itemsLabels = Arrays.stream(roomerItems)
                                .map(item -> item.replaceAll(" ", "_"))
                                .collect(Collectors.toSet());
                        itemsLabels.add(getInitials(roomer.fio));
                        labelManager.setLabels(user, subIssue.getId(), itemsLabels, true, true);
                    }
                });
    }

    private IssueInputParameters generateIssueInputParameters(ApplicationUser user, Issue issue) {
        IssueType parentIssueType = issue.getIssueType();
        IssueType subIssueType = parentIssueType == fundPaymentIssueType ? fundPaymentSubIssueType : parentIssueType == monthlyPaymentIssueType ? monthlyPaymentSubIssueType : null;
        return new IssueInputParametersImpl()
                .setReporterId(user.getName())
                .setProjectId(issue.getProjectId())
                .setIssueTypeId(subIssueType.getId());
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

    private void setActualCosts(Issue issue, Double value) {
        ModifiedValue modifiedValue = new ModifiedValue<>(0.0d, value);
        ACTUAL_COSTS_FIELD.updateValue(null, issue, modifiedValue, new DefaultIssueChangeHolder());
    }

    private void setPlannedCosts(Issue issue, Double value) {
        ModifiedValue modifiedValue = new ModifiedValue<>(0.0d, value);
        PLANNED_COSTS_FIELD.updateValue(null, issue, modifiedValue, new DefaultIssueChangeHolder());
    }

    private String getInitials(String fullName) {
        String[] names = fullName.split(" ");
        String initials = Arrays.stream(names, 1, names.length)
                .map(name -> String.valueOf(name.charAt(0)))
                .collect(Collectors.joining("."));
        return names[0] + "_" + initials + ".";
    }
}