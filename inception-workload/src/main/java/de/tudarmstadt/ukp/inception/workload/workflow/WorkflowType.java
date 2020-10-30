package de.tudarmstadt.ukp.inception.workload.workflow;

import java.io.Serializable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class WorkflowType
    implements Serializable
{
    private static final long serialVersionUID = 6963958947605837721L;

    private final String uiName;
    private final String workflowExtensionId;

    public WorkflowType(String aWorkflowExtensionId, String aUiName)
    {
        uiName = aUiName;
        workflowExtensionId = aWorkflowExtensionId;
    }

    public String getUiName()
    {
        return uiName;
    }

    public String getWorkflowExtensionId()
    {
        return workflowExtensionId;
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof WorkflowType)) {
            return false;
        }
        WorkflowType castOther = (WorkflowType) other;
        return new EqualsBuilder().append(workflowExtensionId, castOther.workflowExtensionId)
                .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(workflowExtensionId).toHashCode();
    }
}
