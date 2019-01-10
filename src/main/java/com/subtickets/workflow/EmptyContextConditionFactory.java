package com.subtickets.workflow;

import com.atlassian.jira.plugin.workflow.AbstractWorkflowPluginFactory;
import com.atlassian.jira.plugin.workflow.WorkflowPluginConditionFactory;
import com.opensymphony.workflow.loader.AbstractDescriptor;

import java.util.HashMap;
import java.util.Map;

public class EmptyContextConditionFactory extends AbstractWorkflowPluginFactory implements WorkflowPluginConditionFactory {

    protected void getVelocityParamsForInput(Map velocityParams) {
    }

    protected void getVelocityParamsForEdit(Map velocityParams, AbstractDescriptor descriptor) {
    }

    protected void getVelocityParamsForView(Map velocityParams, AbstractDescriptor descriptor) {
    }

    public Map getDescriptorParams(Map conditionParams) {
        return new HashMap();
    }
}
