package de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;

@Entity
@Table(name = "gazeteer", 
        uniqueConstraints = { @UniqueConstraint(columnNames = { "name", "recommender" }) })
public class Gazeteer
    implements Serializable
{
    private static final long serialVersionUID = 7310223920449064425L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne
    @JoinColumn(name = "recommender")
    private Recommender recommender;

    public Gazeteer()
    {
        // Required for JPA
    }
    
    public Gazeteer(String aName, Recommender aRecommender)
    {
        name = aName;
        recommender = aRecommender;
    }

    public Long getId()
    {
        return id;
    }

    public void setId(Long aId)
    {
        id = aId;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String aName)
    {
        name = aName;
    }

    public Recommender getRecommender()
    {
        return recommender;
    }

    public void setRecommender(Recommender aRecommender)
    {
        recommender = aRecommender;
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof Gazeteer)) {
            return false;
        }
        Gazeteer castOther = (Gazeteer) other;
        return new EqualsBuilder().append(name, castOther.name)
                .append(recommender, castOther.recommender).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(name).append(recommender).toHashCode();
    }
}
