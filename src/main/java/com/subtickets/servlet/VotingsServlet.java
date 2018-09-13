package com.subtickets.servlet;

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.net.RequestFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.subtickets.Constants.CREATE_SUBS_URL;

@Named
public class VotingsServlet extends JiraServlet {

    @ComponentImport
    private RequestFactory<?> requestFactory;

    @Inject
    public VotingsServlet(RequestFactory requestFactory) {
        this.requestFactory = requestFactory;
    }

    @Override
    protected RequestFactory getRequestFactory() {
        return requestFactory;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String issueId = req.getParameter("id");
        sendRequest(CREATE_SUBS_URL + "?id=" + issueId);
        System.out.println(issueId);
        resp.sendRedirect(req.getHeader("referer"));
    }


}