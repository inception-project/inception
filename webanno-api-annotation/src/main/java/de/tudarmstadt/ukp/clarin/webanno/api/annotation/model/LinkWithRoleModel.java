package de.tudarmstadt.ukp.clarin.webanno.api.annotation.model;

import java.io.Serializable;

/**
 * Represents a link with a role in the UI.
 */
public class LinkWithRoleModel
    implements Serializable
{
    private static final long serialVersionUID = 2027345278696308900L;

    public static final String CLICK_HINT = "<Click to activate>";

    public String role;
    public String label = CLICK_HINT;
    public int targetAddr = -1;
    public boolean autoCreated;

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((label == null) ? 0 : label.hashCode());
        result = prime * result + ((role == null) ? 0 : role.hashCode());
        result = prime * result + targetAddr;
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        LinkWithRoleModel other = (LinkWithRoleModel) obj;
        if (label == null) {
            if (other.label != null) {
                return false;
            }
        }
        else if (!label.equals(other.label)) {
            return false;
        }
        if (role == null) {
            if (other.role != null) {
                return false;
            }
        }
        else if (!role.equals(other.role)) {
            return false;
        }
        if (targetAddr != other.targetAddr) {
            return false;
        }
        return true;
    }

}