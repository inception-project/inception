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
package de.tudarmstadt.ukp.clarin.webanno.brat.page.curation.component;

import static org.uimafit.util.JCasUtil.selectCovered;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.codehaus.jackson.map.ObjectMapper;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class SentencePanel extends Panel {

	private String scriptContents = "";
	
	public class EmbedBehavior extends Behavior {
		private Component component;
		
		public void bind(Component component) {
			this.component = component;
			component.setOutputMarkupId(true);
		}
		
		public void renderHead(Component component, IHeaderResponse iHeaderResponse) {
			super.renderHead(component, iHeaderResponse);
			iHeaderResponse.renderOnLoadJavaScript(scriptContents + "Util.embed('"+component.getMarkupId()+"',collData,docData,webFontURLs);");
		}
		
		public boolean isTemporary() {
			return true;
		}
	}
	public SentencePanel(String id, Sentence sentence) {
		super(id);
		ObjectMapper mapper = new ObjectMapper();
		
		int offsetBegin = sentence.getBegin();
		
		Set<String> posDefinitions = new HashSet<String>();
		List<POS> poses = new LinkedList<POS>();
        for (Token token : selectCovered(Token.class, sentence)) {
        	posDefinitions.add(token.getPos().getPosValue());
        	poses.add(token.getPos());
        }
		
		// read PoS Tags
		Map<Object, Object> collData = new HashMap<Object, Object>();
		List<Map<String, Object>> entityTypes = new LinkedList<Map<String, Object>>();
		for (String posDefinition : posDefinitions) {
			Map<String, Object> entityType = new HashMap<String, Object>();
			entityType.put("type", posDefinition);
			entityType.put("labels", new String[]{posDefinition, posDefinition});
			entityType.put("bgColor", "#7fa2ff");
			entityType.put("borderColor", "darken");
			entityTypes.add(entityType);
		}
		collData.put("entity_types", entityTypes);
		
		// read Sentence
		Map<String, Object> docData = new HashMap<String, Object>();
		List<List<Object>> entities = new LinkedList<List<Object>>();
		Integer i = 0;
		for (POS pos : poses) {
			i += 1;
			List<Object> entity = new LinkedList<Object>();
			entity.add("T"+i);
			entity.add(pos.getPosValue());
			entity.add(new Integer[][]{{pos.getBegin() - offsetBegin, pos.getEnd() - offsetBegin}});
			entities.add(entity);
		}
		docData.put("text", sentence.getCoveredText().toString());
		docData.put("entities", entities);
		
		try {
			scriptContents += "var collData = " + mapper.writeValueAsString(collData) + ";\n";
			scriptContents += "var docData = " + mapper.writeValueAsString(docData) + ";\n";
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		add(new EmbedBehavior());
	}
	
}
