package de.tudarmstadt.ukp.clarin.webanno.model;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "level")
public class Permissions
    implements Serializable
{
    private static final long serialVersionUID = -4218913278378355803L;

    @Id
    @GeneratedValue
    private long id;

    private String level;

    public String getLevel()
    {
        return level;
    }

    public void setLevel(String level)
    {
        this.level = level;
    }

}
