/*******************************************************************************
 * Copyright 2012
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

/**
 * Panel, that shows the result of the curated document. This class has been
 * replaced by {@link BratCurationDocumentEditor} and not maintained anymore.
 * 
 * @deprecated
 * @author Andreas Straninger
 */
public class MergePanel extends Panel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String scriptContents = "";

	public class EmbedBehavior extends Behavior {
		private Component component;
		
		public void bind(Component component) {
			this.component = component;
			component.setOutputMarkupId(true);
		}
		
		/**
		 * start script after page has been loaded
		 */
		public void renderHead(Component component, IHeaderResponse iHeaderResponse) {
			super.renderHead(component, iHeaderResponse);
			iHeaderResponse.renderOnLoadJavaScript(scriptContents  +
					"Util.embed('"+component.getMarkupId()+"',collData,docData,webFontURLs);"
//					+"dispatcher = new Dispatcher();\n"+
//					"var urlMonitor = new URLMonitor(dispatcher);\n"+
//					"var ajax = new Ajax(dispatcher);\n"+
//					"var visualizer = new Visualizer(dispatcher, '"+component.getMarkupId()+"');\n"+
//					"var svg = visualizer.svg;\n"+
//					"var visualizerUI = new VisualizerUI(dispatcher, svg);\n"+
//					"var annotatorUI = new AnnotatorUI(dispatcher, svg);\n"+
//					"var spinner = new Spinner(dispatcher, '#spinner');\n"+
//					"var logger = new AnnotationLog(dispatcher);\n"+
//					"dispatcher.post('init');\n"
			);
		}
		
		public boolean isTemporary() {
			return true;
		}
	}
	
	public MergePanel(String id, String text) {
		super(id);
		
		add(new Label("dummy", text));
	}


}
