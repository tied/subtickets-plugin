package com.subtickets;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.issue.Issue;
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

import static com.atlassian.jira.event.type.EventType.ISSUE_CREATED_ID;
import static com.subtickets.Constants.IssueTypesNames.BOARDING;
import static com.subtickets.Constants.URL.BOARDING_CREATED;

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

        if (eventTypeId.equals(ISSUE_CREATED_ID) && issue.getIssueType() == JiraConfiguration.issueTypes.get(BOARDING)) {
            sendRequest(BOARDING_CREATED + "?id=" + issue.getId());
        }
    }

    private void sendRequest(String url) {
        try {
            Request<?, ?> request = requestFactory.createRequest(Request.MethodType.POST, url);
            request.setSoTimeout(200000);
            String execute = request.execute();
            System.out.println(execute);
        } catch (ResponseException e) {
            e.printStackTrace();
        }
    }

}
