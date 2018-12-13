package de.tudarmstadt.ukp.inception.externalsearch.event;

import org.springframework.context.ApplicationEvent;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;

public class ExternalSearchQueryEvent extends ApplicationEvent {

  private static final long serialVersionUID = 2911869258080097719L;

  private final Project project;
  private final String user;
  private final String query;

  public ExternalSearchQueryEvent(Object aSource, Project aProject, String aUser, String aQuery) {
    super(aSource);

    project = aProject;
    user = aUser;
    query = aQuery;
  }

  public String getUser() {
    return user;
  }

  public Project getProject() {
    return project;
  }

  public String getQuery() {
    return query;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("ExternalSearchQueryEvent [project=");
    builder.append(project);
    builder.append(", user=");
    builder.append(user);
    builder.append(", query=");
    builder.append(query);
    builder.append("]");
    return builder.toString();
  }
}
