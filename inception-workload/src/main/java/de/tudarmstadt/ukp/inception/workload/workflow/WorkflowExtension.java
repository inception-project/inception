package de.tudarmstadt.ukp.inception.workload.workflow;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.extensionpoint.Extension;

public interface WorkflowExtension
    extends Extension<Project>
{

    default boolean accepts(Project project)
    {
        return true;
    }

    String getLabel();
}
