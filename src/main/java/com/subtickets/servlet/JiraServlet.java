package com.subtickets.servlet;

import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.RequestFactory;
import com.atlassian.sal.api.net.ResponseException;

import javax.servlet.http.HttpServlet;

public abstract class JiraServlet extends HttpServlet {

    abstract protected RequestFactory getRequestFactory();

    protected void sendRequest(String url) {
        try {
            Request<?, ?> request = getRequestFactory().createRequest(Request.MethodType.POST, url);
            request.setSoTimeout(200000);
            String execute = request.execute();
            System.out.println(execute);
        } catch (ResponseException e) {
            e.printStackTrace();
        }
    }
}
