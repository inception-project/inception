package de.tudarmstadt.ukp.inception.workload.dynamic.workflow;

import de.tudarmstadt.ukp.inception.workload.workflow.WorkflowExtension;

public class DefaultWorkflowExtension implements WorkflowExtension
{
    public static final String DEFAULT_WORKFLOW = "default";

    @Override
    public String getLabel()
    {
        return "Default workflow";
    }

    @Override
    public String getId()
    {
        return DEFAULT_WORKFLOW;
    }
}
