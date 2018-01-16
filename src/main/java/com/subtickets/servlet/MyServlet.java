package com.subtickets.servlet;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.IssueInputParametersImpl;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.RequestFactory;
import com.atlassian.sal.api.net.ResponseException;
import com.atlassian.sal.api.net.ReturningResponseHandler;
import com.atlassian.sal.api.user.UserProfile;
import com.shepilov.jiratutorial.roomers.Roomers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Named
public class MyServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(MyServlet.class);

    private static final String ROOMERS_URL = "http://5.101.122.147:8089/api/osbb/command/roomers";

    @ComponentImport
    private IssueService issueService;

    private UserManager jiraUserManager;

    @ComponentImport
    private com.atlassian.sal.api.user.UserManager userManager;

    @ComponentImport
    private RequestFactory<?> requestFactory;

    @Inject
    public MyServlet(IssueService issueService, com.atlassian.sal.api.user.UserManager userManager, RequestFactory requestFactory) {
        this.issueService = issueService;
        this.userManager = userManager;
        this.jiraUserManager = ComponentAccessor.getUserManager();
        this.requestFactory = requestFactory;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("------------------------------------------");
        String referer = req.getHeader("referer");
        System.out.println(referer);
        System.out.println(issueService);
        System.out.println(jiraUserManager);
        System.out.println(userManager);
        String issueKey = req.getParameter("id");
        System.out.println(issueKey);
        UserProfile user = userManager.getRemoteUser(req);
        ApplicationUser applicationUser = jiraUserManager.getUserByKey(user.getUserKey().getStringValue());
        MutableIssue issue = issueService.getIssue(applicationUser, issueKey).getIssue();
        Long issueId = issue.getId();
        System.out.println(issueId);
        resp.sendRedirect(referer);

        Request<?, ?> request = requestFactory.createRequest(Request.MethodType.GET, ROOMERS_URL);
        try {
            Roomers roomers = request.executeAndReturn(new RoomersResponseHandler());
            roomers.forEach((id, roomer) -> {
                        IssueInputParametersImpl parameters = new IssueInputParametersImpl();
                        parameters.setReporterId(applicationUser.getName());
                        parameters.setProjectId(issue.getProjectObject().getId());
                        parameters.setIssueTypeId("10100");
                        parameters.setSummary(issue.getSummary());
                        IssueService.CreateValidationResult validationResult = issueService.validateSubTaskCreate(applicationUser, issueId, parameters);
                        log.debug("Validation result", validationResult.isValid());
                        if (validationResult.isValid()) {
                            IssueService.IssueResult issueResult = issueService.create(applicationUser, validationResult);
                            issueResult.getIssue().setParentObject(issue);
                        }
                    }
            );
        } catch (ResponseException e) {
            e.printStackTrace();
        }
    }
}


class RoomersResponseHandler implements ReturningResponseHandler<com.atlassian.sal.api.net.Response, Roomers> {
    public Roomers handle(com.atlassian.sal.api.net.Response response) throws ResponseException {
        return response.getEntity(Roomers.class);
    }
}