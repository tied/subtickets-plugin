package com.subtickets.servlet;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.exception.CreateException;
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
import com.atlassian.sal.api.user.UserProfile;
import com.subtickets.roomers.Roomers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;

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

    private Request<?, ?> roomersRequest;

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
        this.roomersRequest = requestFactory.createRequest(Request.MethodType.GET, ROOMERS_URL);
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
        String referer = req.getHeader("referer");
        String issueKey = req.getParameter("id");
        UserProfile user = userManager.getRemoteUser(req);
        ApplicationUser applicationUser = jiraUserManager.getUserByKey(user.getUserKey().getStringValue());
        MutableIssue issue = issueService.getIssue(applicationUser, issueKey).getIssue();

        try {
            Roomers roomers = roomersRequest.executeAndReturn(new RoomersResponseHandler());
            roomers.forEach((id, roomer) -> {
                        IssueInputParametersImpl parameters = new IssueInputParametersImpl();
                        parameters.setReporterId(applicationUser.getName());
                        parameters.setProjectId(issue.getProjectObject().getId());
                        parameters.setIssueTypeId(issue.getIssueTypeId());
                        parameters.setSummary(issue.getSummary());
                        IssueService.CreateValidationResult validationResult = issueService.validateSubTaskCreate(applicationUser, issue.getId(), parameters);
                        if (validationResult.isValid()) {
                            IssueService.IssueResult issueResult = issueService.create(applicationUser, validationResult);
                            MutableIssue subTask = issueResult.getIssue();
                            labelManager.addLabel(applicationUser, subTask.getId(), roomer.owned_Doors[0], true);
                            try {
                                subTaskManager.createSubTaskIssueLink(issue, subTask, applicationUser);
                            } catch (CreateException e) {
                                e.printStackTrace();
                            }
                        }
                    }
            );
        } catch (ResponseException e) {
            e.printStackTrace();
        }
        resp.sendRedirect(referer);
    }
}