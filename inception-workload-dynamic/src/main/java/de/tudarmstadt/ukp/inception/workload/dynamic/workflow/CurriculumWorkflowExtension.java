package de.tudarmstadt.ukp.inception.workload.dynamic.workflow;

import de.tudarmstadt.ukp.inception.workload.workflow.WorkflowExtension;

public class CurriculumWorkflowExtension implements WorkflowExtension
{
    public static final String CURRICULUM_EXTENSION = "curriculum";

    @Override
    public String getLabel()
    {
        return "Curriculum workflow";
    }

    @Override
    public String getId()
    {
        return CURRICULUM_EXTENSION;
    }
}
