package de.tudarmstadt.ukp.inception.workload.workflow;

import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.extensionpoint.ExtensionPoint;

public interface WorkflowExtensionPoint
    extends ExtensionPoint<Project, WorkflowExtension>
{
    WorkflowExtension getDefault();

    List<WorkflowType> getTypes();
}
