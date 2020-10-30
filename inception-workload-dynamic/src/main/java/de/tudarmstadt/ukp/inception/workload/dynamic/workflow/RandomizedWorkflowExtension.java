package de.tudarmstadt.ukp.inception.workload.dynamic.workflow;

import de.tudarmstadt.ukp.inception.workload.workflow.WorkflowExtension;

public class RandomizedWorkflowExtension implements WorkflowExtension
{
    public static final String RANDOMIZED_WORKFLOW = "randomized";

    @Override
    public String getLabel()
    {
        return "Randomized workflow";
    }

    @Override
    public String getId()
    {
        return RANDOMIZED_WORKFLOW;
    }
}
