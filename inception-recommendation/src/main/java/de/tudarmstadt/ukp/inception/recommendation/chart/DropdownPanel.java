package de.tudarmstadt.ukp.inception.recommendation.chart;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;

public class DropdownPanel extends Panel {
 
	private static final long serialVersionUID = 4988942370126340112L;
	
	private static final List<String> GENRES = Arrays.asList("Accuracy", "Precision", "Recall", "F1");
    private static final String MID_METRIC_SELECT = "select";
    private static final String MID_METRIC_LINK = "link";

	private final AjaxLink<Void> link;

	public DropdownPanel(String aId) {
		super(aId); 

		final DropDownChoice<String> dropdown = new DropDownChoice<String>(MID_METRIC_SELECT, new Model<String>(),
				new ListModel<String>(GENRES));

		dropdown.setRequired(true);
		dropdown.setOutputMarkupId(true);
		
		dropdown.add(new AjaxFormComponentUpdatingBehavior("change") {
			private static final long serialVersionUID = -6744838136235652577L;

			protected void onUpdate(AjaxRequestTarget target) {
				send(getPage(), Broadcast.BREADTH, GENRES.get(Integer.valueOf(dropdown.getValue())) );
			}
		});

		add(dropdown);

		link = new AjaxLink<Void>(MID_METRIC_LINK) {
			private static final long serialVersionUID = 1L;
			
			boolean isDropdownVisible = false;

			@Override
			public void onClick(AjaxRequestTarget target) {

				if (isDropdownVisible) {
					Effects.hide(target, dropdown);
					target.appendJavaScript("document.getElementById('" + link.getMarkupId()
							+ "').classList.remove('fa-chevron-circle-left');");
					target.appendJavaScript("document.getElementById('" + link.getMarkupId()
							+ "').classList.add('fa-chevron-circle-right');");
					isDropdownVisible = false;

				} else {

					Effects.show(target, dropdown);
					target.appendJavaScript("document.getElementById('" + link.getMarkupId()
							+ "').classList.remove('fa-chevron-circle-right');");
					target.appendJavaScript("document.getElementById('" + link.getMarkupId()
							+ "').classList.add('fa-chevron-circle-left');");
					isDropdownVisible = true;
				}
			}
		};
		
		link.setOutputMarkupId(true);
		add(link);
	}
	private static class Effects {

		private static void hide(AjaxRequestTarget target, Component component) {
			component.add(new DisplayNoneBehavior());
			String js = "$('#" + component.getMarkupId() + "').animate({'width': '-=100'},  100); $('#"
					+ ((DropDownChoice) component).getOutputMarkupId() + "').hide();";
			target.prependJavaScript(js);
		}

		private static void show(AjaxRequestTarget target, Component component) {
			component.add(new DisplayNoneBehavior());

			String js = "$('#" + component.getMarkupId() + "').animate({'width': '+=100'},  100); $('#"
					+ ((DropDownChoice) component).getOutputMarkupId() + "').show();";
			target.prependJavaScript(js);
		}
	}

	private static class DisplayNoneBehavior extends AttributeModifier {

		private static final long serialVersionUID = 1539674355578272254L;

		private DisplayNoneBehavior() {
			super("style", Model.of("display: none"));
		}

		@Override
		public boolean isTemporary(Component component) {
			return true;
		}
	}   
}
