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
import java.util.HashSet;

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

    private CustomFieldManager customFieldManager;

    @Inject
    public SubTicketsServlet(IssueService issueService, com.atlassian.sal.api.user.UserManager userManager, RequestFactory requestFactory, LabelManager labelManager,
                             SubTaskManager subTaskManager) {
        this.issueService = issueService;
        this.userManager = userManager;
        this.jiraUserManager = ComponentAccessor.getUserManager();
        this.requestFactory = requestFactory;
        this.labelManager = labelManager;
        this.subTaskManager = subTaskManager;
        this.customFieldManager = ComponentAccessor.getCustomFieldManager();
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
                    monthlyPaymentSubIssueType= type;
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
                createFundPaymentSubIssues();
            }
        } catch (ResponseException e) {
            e.printStackTrace();
        }

        resp.sendRedirect(req.getHeader("referer"));
    }

    private void createMonthlyPaymentsSubIssues(ApplicationUser user, Issue parentIssue) throws ResponseException {
        Roomers roomers = requestFactory.createRequest(Request.MethodType.GET, ROOMERS_URL).executeAndReturn(new RoomersResponseHandler());
        roomers.forEach((key, roomer) -> {
            IssueInputParameters parameters = generateIssueInputParameters(user, parentIssue)
            .setSummary(parentIssue.getSummary() + " " + roomer.holderInfo);
            Issue issue = doCreateSubIssue(user, parentIssue, parameters);
            if (issue != null) {
                setActualCosts(issue, roomer.amount.doubleValue());
                labelManager.setLabels(user, issue.getId(), new HashSet<>(Arrays.asList(roomer.owned_Doors)), true, true);
            }
        });
    }

    private void createFundPaymentSubIssues() {

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
        CustomField customField = customFieldManager.getCustomFieldObjectByName(ACTUAL_COSTS_FIELD_NAME);
        ModifiedValue modifiedValue = new ModifiedValue<>(0.0d, value);
        customField.updateValue(null, issue, modifiedValue, new DefaultIssueChangeHolder());
    }
}