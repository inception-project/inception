package de.tudarmstadt.ukp.inception.workload.workflow;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.extensionpoint.ExtensionPoint_ImplBase;

public class WorkflowExtensionPointImpl
    extends ExtensionPoint_ImplBase<Project, WorkflowExtension>
    implements WorkflowExtensionPoint
{
    public WorkflowExtensionPointImpl(List<WorkflowExtension> aExtensions)
    {
        super(aExtensions);
    }

    @Override
    public List<WorkflowExtension> getExtensions(Project aContext)
    {
        Map<String, WorkflowExtension> byRole = new LinkedHashMap<>();
        for (WorkflowExtension extension : super.getExtensions(aContext)) {
            byRole.put(extension.getId(), extension);
        }
        return new ArrayList<>(byRole.values());
    }

    @Override
    public WorkflowExtension getDefault()
    {
        return getExtensions().get(0);
    }

    @Override
    public List<WorkflowType> getTypes()
    {
        return getExtensions().stream()
                .map(manExt -> new WorkflowType(manExt.getId(), manExt.getLabel()))
                .collect(toList());
    }
}
