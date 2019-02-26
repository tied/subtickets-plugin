package com.subtickets;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.RequestFactory;
import com.atlassian.sal.api.net.ResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.atlassian.jira.event.type.EventType.ISSUE_CLOSED_ID;
import static com.atlassian.jira.event.type.EventType.ISSUE_CREATED_ID;
import static com.subtickets.Constants.IssueTypesNames.BOARDING;
import static com.subtickets.Constants.IssueTypesNames.IMPROVEMENT;
import static com.subtickets.Constants.IssueTypesNames.INCIDENT;
import static com.subtickets.Constants.IssueTypesNames.TASK;
import static com.subtickets.Constants.URL.BOARDING_CREATED;
import static com.subtickets.Constants.URL.SERVICE_ENDPOINT;
import static com.subtickets.Constants.URL.TASK_CLOSED;

@Component
public class IssueEventListener implements InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(IssueEventListener.class);

    @JiraImport
    private EventPublisher eventPublisher;

    @ComponentImport
    private RequestFactory requestFactory;

    @Autowired
    public IssueEventListener(EventPublisher eventPublisher, RequestFactory requestFactory) {
        this.eventPublisher = eventPublisher;
        this.requestFactory = requestFactory;
    }

    @Override
    public void destroy() {
        eventPublisher.unregister(this);
    }

    @Override
    public void afterPropertiesSet() {
        eventPublisher.register(this);
    }

    @EventListener
    public void onIssueEvent(IssueEvent issueEvent) {
        log.debug("Issue #{} event #{}", issueEvent.getIssue().getId(), issueEvent.getEventTypeId());

        Long eventTypeId = issueEvent.getEventTypeId();
        Issue issue = issueEvent.getIssue();

        Map<String, IssueType> issueTypes = JiraConfiguration.issueTypes;

        List<IssueType> issueTypesToWatchClose = Arrays.asList(issueTypes.get(TASK), issueTypes.get(IMPROVEMENT), issueTypes.get(INCIDENT));

        if (eventTypeId.equals(ISSUE_CREATED_ID) && issue.getIssueType().equals(issueTypes.get(BOARDING))) {
            sendRequest(BOARDING_CREATED + "?id=" + issue.getId());
        } else if (eventTypeId.equals(ISSUE_CLOSED_ID) && issueTypesToWatchClose.indexOf(issue.getIssueType()) >= 0) {
            sendRequest(TASK_CLOSED + "?id=" + issue.getId());
        }
    }

    private void sendRequest(String url) {
        try {
            Request<?, ?> request = requestFactory.createRequest(Request.MethodType.POST, SERVICE_ENDPOINT + url);
            request.setSoTimeout(200000);
            String execute = request.execute();
            System.out.println(execute);
        } catch (ResponseException e) {
            e.printStackTrace();
        }
    }

}
