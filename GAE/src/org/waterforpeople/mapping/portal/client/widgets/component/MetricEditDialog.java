/*
 *  Copyright (C) 2010-2012 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo FLOW.
 *
 *  Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 *  the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 *  either version 3 of the License or any later version.
 *
 *  Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License included below for more details.
 *
 *  The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 */

package org.waterforpeople.mapping.portal.client.widgets.component;

import java.util.HashMap;
import java.util.Map;

import org.waterforpeople.mapping.app.gwt.client.survey.MetricDto;
import org.waterforpeople.mapping.app.gwt.client.util.TextConstants;

import com.gallatinsystems.framework.gwt.util.client.CompletionListener;
import com.gallatinsystems.framework.gwt.util.client.WidgetDialog;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * dialog box to display the metric editor. It is the responsibility of the
 * client code to ensure the user has the proper permissions to edit/create
 * metrics before displaying this dialog box.
 * 
 * @author Christopher Fagiani
 * 
 */
public class MetricEditDialog extends WidgetDialog implements ClickHandler,
		CompletionListener {
	private static TextConstants TEXT_CONSTANTS = GWT
			.create(TextConstants.class);
	public static final String CRITERIA_KEY = "APcriteria";
	private MetricEditWidget editWidget;
	private Button saveButton;
	private Button closeButton;

	public MetricEditDialog(CompletionListener listener, MetricDto dto) {
		super(TEXT_CONSTANTS.editMetric(), null, true, listener);
		Panel panel = new VerticalPanel();
		editWidget = new MetricEditWidget(dto, this);
		panel.add(editWidget);
		Panel buttonPanel = new HorizontalPanel();
		saveButton = new Button(TEXT_CONSTANTS.save());
		saveButton.addClickHandler(this);
		buttonPanel.add(saveButton);
		closeButton = new Button(TEXT_CONSTANTS.close());
		closeButton.addClickHandler(this);
		buttonPanel.add(closeButton);
		panel.add(buttonPanel);
		setContentWidget(panel);
	}

	@Override
	public void onClick(ClickEvent event) {
		if (event.getSource() == closeButton) {
			hide(true);
			Map<String, Object> payload = new HashMap<String, Object>();
			notifyListener(true, payload);
		}else if (event.getSource() == saveButton){
			editWidget.saveMetric();
		}
	}

	@Override
	public void operationComplete(boolean wasSuccessful,
			Map<String, Object> payload) {
		if (wasSuccessful) {
			hide(true);
			notifyListener(wasSuccessful, payload);
		}
	}
}