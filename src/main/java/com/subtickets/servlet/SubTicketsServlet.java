package com.subtickets.servlet;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.exception.CreateException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.IssueInputParametersImpl;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.label.LabelManager;
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
import java.util.List;
import java.util.stream.Collectors;

@Named
public class SubTicketsServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(SubTicketsServlet.class);

    private static final String ROOMERS_URL = "http://5.101.122.147:8089/api/osbb/command/roomers";

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

    private IssueType fundIssueType;

    private IssueType monthlyPaymentType;

    @Inject
    public SubTicketsServlet(IssueService issueService, com.atlassian.sal.api.user.UserManager userManager, RequestFactory requestFactory, LabelManager labelManager,
                             SubTaskManager subTaskManager) {
        this.issueService = issueService;
        this.userManager = userManager;
        this.jiraUserManager = ComponentAccessor.getUserManager();
        this.requestFactory = requestFactory;
        this.labelManager = labelManager;
        this.subTaskManager = subTaskManager;
        Collection<IssueType> issueTypes = ComponentAccessor.getConstantsManager().getAllIssueTypeObjects();
        issueTypes.stream().filter(issueType -> issueType.getName().equals("Fund"))
                .findFirst()
                .ifPresent(issueType -> fundIssueType = issueType);
        issueTypes.stream().filter(issueType -> issueType.getName().equals("Monthly Payment"))
                .findFirst()
                .ifPresent(issueType -> monthlyPaymentType = issueType);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ApplicationUser applicationUser = jiraUserManager.getUserByKey(userManager.getRemoteUser(req).getUserKey().getStringValue());
        MutableIssue issue = issueService.getIssue(applicationUser, req.getParameter("id")).getIssue();

        try {
            if (issue.getIssueType() == monthlyPaymentType) {
                createMonthlyPaymentsSubIssues(applicationUser, issue);
            } else if (issue.getIssueType() == fundIssueType) {
                createFundPaymentSubIssues();
            }
        } catch (ResponseException e) {
            e.printStackTrace();
        }

        resp.sendRedirect(req.getHeader("referer"));
    }

    private void createMonthlyPaymentsSubIssues(ApplicationUser user, Issue parentIssue) throws ResponseException {
        Roomers roomers = requestFactory.createRequest(Request.MethodType.GET, ROOMERS_URL).executeAndReturn(new RoomersResponseHandler());
        List<String> doors = roomers.values()
                .stream()
                .map(roomer -> roomer.owned_Doors)
                .flatMap(Arrays::stream)
                .distinct()
                .collect(Collectors.toList());
        doors.forEach(door -> {
            IssueInputParameters parameters = generateIssueInputParameters(user, parentIssue)
                    .setSummary(parentIssue.getSummary() + " " + door);
            Issue subIssue = doCreateSubIssue(user, parentIssue, parameters);
            if (subIssue != null) {
                labelManager.addLabel(user, subIssue.getId(), door, true);
            }
        });
    }

    private void createFundPaymentSubIssues() {

    }

    private IssueInputParameters generateIssueInputParameters(ApplicationUser user, Issue issue) {
        return new IssueInputParametersImpl()
                .setReporterId(user.getName())
                .setProjectId(issue.getProjectId())
                .setIssueTypeId(issue.getIssueTypeId());
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
}