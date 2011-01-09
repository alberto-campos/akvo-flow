package org.waterforpeople.mapping.portal.client.widgets.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.waterforpeople.mapping.app.gwt.client.survey.OptionContainerDto;
import org.waterforpeople.mapping.app.gwt.client.survey.QuestionDto;
import org.waterforpeople.mapping.app.gwt.client.survey.QuestionGroupDto;
import org.waterforpeople.mapping.app.gwt.client.survey.QuestionOptionDto;
import org.waterforpeople.mapping.app.gwt.client.survey.SurveyService;
import org.waterforpeople.mapping.app.gwt.client.survey.SurveyServiceAsync;
import org.waterforpeople.mapping.app.gwt.client.survey.QuestionDto.QuestionType;

import com.gallatinsystems.framework.gwt.wizard.client.CompletionListener;
import com.gallatinsystems.framework.gwt.wizard.client.ContextAware;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class QuestionEditWidget extends Composite implements ContextAware,
		ChangeHandler, ClickHandler {

	private static final String INPUT_LABEL_CSS = "input-label";
	private static final String REORDER_BUTTON_CSS = "reorder-button";
	private static final String DEFAULT_BOX_WIDTH = "300px";
	private static final String SELECT_TXT = "Select...";
	private VerticalPanel panel;
	private TextArea questionTextArea;
	private ListBox questionTypeSelector;
	private TextArea tooltipArea;
	private TextBox validationRuleBox;
	private CheckBox mandatoryBox;
	private CheckBox dependentBox;
	private ListBox dependentQuestionSelector;
	private ListBox dependentAnswerSelector;
	private CaptionPanel dependencyPanel;
	private Grid dependencyGrid;

	private CheckBox allowOtherBox;
	private CheckBox allowMultipleBox;
	private Button addOptionButton;
	private CaptionPanel optionPanel;
	private VerticalPanel optionContent;
	private FlexTable optionTable;
	private SurveyServiceAsync surveyService;
	private Map<String, Object> bundle;
	private QuestionDto currentQuestion;
	private Map<Long, List<QuestionDto>> optionQuestions;

	public QuestionEditWidget() {
		surveyService = GWT.create(SurveyService.class);
		optionQuestions = new HashMap<Long, List<QuestionDto>>();
		installWidgets();
		initWidget(panel);
	}

	private void installWidgets() {
		panel = new VerticalPanel();
		questionTextArea = new TextArea();
		questionTextArea.setWidth(DEFAULT_BOX_WIDTH);
		tooltipArea = new TextArea();
		tooltipArea.setWidth(DEFAULT_BOX_WIDTH);
		validationRuleBox = new TextBox();
		mandatoryBox = new CheckBox();
		dependentBox = new CheckBox();
		dependentBox.addClickHandler(this);
		questionTypeSelector = new ListBox();
		questionTypeSelector.addItem("Free Text",
				QuestionDto.QuestionType.FREE_TEXT.toString());
		questionTypeSelector.addItem("Option", QuestionDto.QuestionType.OPTION
				.toString());
		questionTypeSelector.addItem("Number", QuestionDto.QuestionType.NUMBER
				.toString());
		questionTypeSelector.addItem("Geo", QuestionDto.QuestionType.GEO
				.toString());
		questionTypeSelector.addItem("Photo", QuestionDto.QuestionType.PHOTO
				.toString());
		questionTypeSelector.addItem("Video", QuestionDto.QuestionType.VIDEO
				.toString());
		questionTypeSelector.addItem("Strength",
				QuestionDto.QuestionType.STRENGTH.toString());
		questionTypeSelector.addChangeHandler(this);
		CaptionPanel basePanel = new CaptionPanel("Question Basics:");

		Grid grid = new Grid(7, 2);
		basePanel.add(grid);

		installRow("Question Text", questionTextArea, grid, 0);
		installRow("Question Type", questionTypeSelector, grid, 1);
		installRow("Tooltip", tooltipArea, grid, 2);
		installRow("Validation Rule", validationRuleBox, grid, 3);
		installRow("Mandatory", mandatoryBox, grid, 4);
		installRow("Dependent", dependentBox, grid, 5);

		dependencyPanel = new CaptionPanel("Dependency Details:");
		dependentQuestionSelector = new ListBox();
		dependentQuestionSelector.setWidth(DEFAULT_BOX_WIDTH);
		dependentQuestionSelector.addChangeHandler(this);
		dependentQuestionSelector.addItem(SELECT_TXT);

		dependentAnswerSelector = new ListBox();
		dependentAnswerSelector.addItem(SELECT_TXT);
		dependentAnswerSelector.setWidth(DEFAULT_BOX_WIDTH);

		dependencyGrid = new Grid(2, 2);
		dependencyPanel.add(dependencyGrid);
		installRow(null, dependencyPanel, grid, 6, 1);
		dependencyPanel.setVisible(false);
		installRow("Question", dependentQuestionSelector, dependencyGrid, 0);
		installRow("Response", dependentAnswerSelector, dependencyGrid, 1);

		panel.add(basePanel);

		allowMultipleBox = new CheckBox();
		allowOtherBox = new CheckBox();
		addOptionButton = new Button("Add Option");
		addOptionButton.addClickHandler(this);
		optionPanel = new CaptionPanel("Option Details:");
		optionContent = new VerticalPanel();
		optionPanel.add(optionContent);
		optionTable = new FlexTable();
		Grid optGrid = new Grid(2, 4);
		installRow("Allow Multiple", allowMultipleBox, optGrid, 0, 0);
		installRow("Allow 'Other'", allowOtherBox, optGrid, 0, 2);

		optionContent.add(optGrid);
		optionContent.add(optionTable);
		optionContent.add(addOptionButton);
		optionPanel.setVisible(false);
		panel.add(optionPanel);
	}

	private void installRow(String labelText, Widget widget, Grid parent,
			int row) {
		installRow(labelText, widget, parent, row, 0);
	}

	private void installRow(String labelText, Widget widget, Grid parent,
			int row, int colOffset) {
		if (labelText != null) {
			Label label = new Label();
			label.setStylePrimaryName(INPUT_LABEL_CSS);
			label.setText(labelText);
			parent.setWidget(row, colOffset, label);
			parent.setWidget(row, colOffset + 1, widget);

		} else {
			parent.setWidget(row, colOffset, widget);
		}
	}

	private void populateFields() {
		questionTextArea.setText(currentQuestion.getText());
		tooltipArea.setText(currentQuestion.getTip());
		if (currentQuestion.getType() != null) {
			for (int i = 0; i < questionTypeSelector.getItemCount(); i++) {
				if (currentQuestion.getType().toString().equals(
						questionTypeSelector.getValue(i))) {
					questionTypeSelector.setSelectedIndex(i);
					break;
				}
			}
		}
		if (currentQuestion.getMandatoryFlag() != null) {
			mandatoryBox.setValue(currentQuestion.getMandatoryFlag());
		}
		if (currentQuestion.getQuestionDependency() != null) {
			dependentBox.setValue(true);
			loadDependencyList();
		}
		if (QuestionDto.QuestionType.OPTION == currentQuestion.getType()) {
			loadOptions();
		}

	}

	private void loadOptions() {
		if (QuestionDto.QuestionType.OPTION == currentQuestion.getType()
				&& (currentQuestion.getOptionContainerDto() == null || currentQuestion
						.getOptionContainerDto().getOptionsList() == null)) {
			optionPanel.setVisible(true);
			showLoading(optionPanel, "Loading options...");
			surveyService.loadQuestionDetails(currentQuestion.getKeyId(),
					new AsyncCallback<QuestionDto>() {

						@Override
						public void onFailure(Throwable caught) {
							showContent(optionPanel, new Label("Error"));
						}

						@Override
						public void onSuccess(QuestionDto result) {
							showContent(optionPanel, optionContent);
							currentQuestion = result;
							populateOptions(currentQuestion
									.getOptionContainerDto());
						}
					});
		} else {
			populateOptions(currentQuestion.getOptionContainerDto());
		}
	}

	private void populateOptions(OptionContainerDto optionContainer) {
		optionPanel.setVisible(true);
		// wipe out any old values
		optionTable.clear(true);
		if (optionContainer != null && optionContainer.getOptionsList() != null) {
			for (QuestionOptionDto opt : optionContainer.getOptionsList()) {
				installOptionRow(opt);
			}
		}
	}

	private void installOptionRow(QuestionOptionDto opt) {
		final int row = optionTable.getRowCount();
		optionTable.insertRow(row);
		TextBox optText = new TextBox();
		optionTable.setWidget(row, 0, optText);
		HorizontalPanel bp = new HorizontalPanel();
		final Image moveUp = new Image("/images/greenuparrow.png");
		final Image moveDown = new Image("/images/greendownarrow.png");

		ClickHandler reorderClickHandler = new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				int increment = 0;
				ArrayList<QuestionOptionDto> optList = currentQuestion
						.getOptionContainerDto().getOptionsList();
				if (event.getSource() == moveUp && row > 0) {
					increment = -1;
				} else if (event.getSource() == moveDown
						&& row < optList.size() - 1) {
					increment = 1;
				}
				if (increment != 0) {
					QuestionOptionDto targetOpt = optList.get(row + increment);
					QuestionOptionDto movingOpt = optList.get(row);
					optList.set(row + increment, movingOpt);
					optList.set(row,targetOpt);
					targetOpt.setOrder(targetOpt.getOrder()-increment);
					movingOpt.setOrder(movingOpt.getOrder()+increment);
					//now update the UI
					((TextBox)(optionTable.getWidget(row, 0))).setText(targetOpt.getText());
					((TextBox)(optionTable.getWidget(row+increment, 0))).setText(movingOpt.getText());
				}
			}
		};

		moveUp.setStylePrimaryName(REORDER_BUTTON_CSS);
		moveUp.addClickHandler(reorderClickHandler);

		moveDown.setStylePrimaryName(REORDER_BUTTON_CSS);
		moveDown.addClickHandler(reorderClickHandler);
		bp.add(moveUp);
		bp.add(moveDown);
		optionTable.setWidget(row, 1, bp);
		Button deleteButton = new Button("Remove");
		optionTable.setWidget(row, 2, deleteButton);
		if (opt != null) {
			optText.setText(opt.getText());
			if(opt.getOrder() == null){
				opt.setOrder(row);
			}
		} else {
			if (currentQuestion.getOptionContainerDto() == null) {
				currentQuestion.setOptionContainerDto(new OptionContainerDto());
			}
			if (currentQuestion.getOptionContainerDto().getOptionsList() == null) {
				currentQuestion.getOptionContainerDto().setOptionsList(
						new ArrayList<QuestionOptionDto>());
			}
			QuestionOptionDto dto = new QuestionOptionDto();
			dto.setOrder(row);
			currentQuestion.getOptionContainerDto().getOptionsList().add(dto);

		}
	}

	private void showLoading(HasWidgets container, String labelText) {
		Label l = new Label(labelText);
		container.clear();
		container.add(l);
	}

	private void showContent(HasWidgets container, Widget content) {
		container.clear();
		container.add(content);
	}

	private void loadDependencyList() {
		dependencyPanel.setVisible(true);
		if (optionQuestions != null
				&& optionQuestions.get(currentQuestion.getSurveyId()) != null) {
			populateDependencySelection(currentQuestion, optionQuestions
					.get(currentQuestion.getSurveyId()));
		} else {
			showLoading(dependencyPanel, "Loading...");
			surveyService.listSurveyQuestionByType(currentQuestion
					.getSurveyId(), QuestionType.OPTION,
					new AsyncCallback<QuestionDto[]>() {

						@Override
						public void onFailure(Throwable caught) {
							showContent(dependencyPanel, new Label(
									"Error loading questions"));
						}

						@Override
						public void onSuccess(QuestionDto[] result) {
							if (result != null) {
								List<QuestionDto> questionList = Arrays
										.asList(result);
								optionQuestions.put(currentQuestion
										.getSurveyId(), questionList);
								getContextBundle()
										.put(
												BundleConstants.OPTION_QUESTION_LIST_KEY,
												optionQuestions);

								populateDependencySelection(currentQuestion,
										questionList);
							}
							showContent(dependencyPanel, dependencyGrid);
						}
					});
		}
	}

	private void populateDependencySelection(QuestionDto currentQuestion,
			List<QuestionDto> questionList) {
		dependencyPanel.setVisible(true);
		if (questionList != null) {
			String selectedQId = null;
			for (int i = 0; i < questionList.size(); i++) {
				QuestionDto q = questionList.get(i);
				dependentQuestionSelector.addItem(q.getText(), q.getKeyId()
						.toString());
				if (currentQuestion != null
						&& currentQuestion.getQuestionDependency() != null
						&& currentQuestion.getQuestionDependency()
								.getQuestionId().equals(q.getKeyId())) {
					dependentQuestionSelector.setSelectedIndex(i + 1);
					selectedQId = q.getKeyId().toString();
				}
			}
			if (selectedQId != null) {
				loadDependentQuestionAnswers(selectedQId);
			}
		}
	}

	private void loadDependentQuestionAnswers(String questionId) {
		final List<QuestionDto> questionList = optionQuestions
				.get(currentQuestion.getSurveyId());
		QuestionDto question = null;
		if (questionList != null) {
			for (QuestionDto q : questionList) {
				if (q.getKeyId().toString().equals(questionId)) {
					question = q;
				}
			}
		}
		if (question != null) {
			if (question.getOptionContainerDto() != null
					&& question.getOptionContainerDto().getOptionsList() != null) {
				populateDependencyAnswers(currentQuestion, question
						.getOptionContainerDto().getOptionsList());
			} else {
				showLoading(dependencyPanel, "Loading");
				// if the option container is null, we probably have not
				// yet loaded the question details. so do it now
				surveyService.loadQuestionDetails(question.getKeyId(),
						new AsyncCallback<QuestionDto>() {
							@Override
							public void onSuccess(QuestionDto result) {

								if (questionList != null) {
									// update the option container of the cached
									// result so we have it for next time
									int idx = questionList.indexOf(result);
									if (idx >= 0) {
										questionList
												.get(idx)
												.setOptionContainerDto(
														result
																.getOptionContainerDto());
									}
								}
								if (result.getOptionContainerDto() != null) {
									populateDependencyAnswers(currentQuestion,
											result.getOptionContainerDto()
													.getOptionsList());
								}
								showContent(dependencyPanel, dependencyGrid);
							}

							@Override
							public void onFailure(Throwable caught) {
								showContent(dependencyPanel, dependencyGrid);
								Window.alert("Could not load answers");
							}
						});
			}
		}
	}

	private void populateDependencyAnswers(QuestionDto currentQuestion,
			List<QuestionOptionDto> options) {
		// first, clear out the existing data
		dependentAnswerSelector.clear();
		// now add the "select" item
		dependentAnswerSelector.addItem(SELECT_TXT);
		if (options != null) {
			for (int i = 0; i < options.size(); i++) {
				dependentAnswerSelector.addItem(options.get(i).getText(),
						options.get(i).getKeyId().toString());
				if (currentQuestion != null
						&& currentQuestion.getQuestionDependency() != null
						&& options.get(i).getText().equals(
								currentQuestion.getQuestionDependency()
										.getAnswerValue())) {
					dependentAnswerSelector.setSelectedIndex(i + 1);
				}
			}
		}
	}

	@Override
	public Map<String, Object> getContextBundle() {
		if (bundle == null) {
			bundle = new HashMap<String, Object>();
		}
		bundle.put(BundleConstants.QUESTION_KEY, currentQuestion);
		return bundle;
	}

	@Override
	public void persistContext(final CompletionListener listener) {
		surveyService.saveQuestion(currentQuestion, currentQuestion
				.getQuestionGroupId(), new AsyncCallback<QuestionDto>() {

			@Override
			public void onSuccess(QuestionDto result) {
				currentQuestion = result;
				if (listener != null) {
					listener.operationComplete(true, getContextBundle());
				}
			}

			@Override
			public void onFailure(Throwable caught) {
				if (listener != null) {
					listener.operationComplete(false, getContextBundle());
				}
			}
		});

	}

	@Override
	@SuppressWarnings("unchecked")
	public void setContextBundle(Map<String, Object> bundle) {
		this.bundle = bundle;
		currentQuestion = (QuestionDto) bundle
				.get(BundleConstants.QUESTION_KEY);
		optionQuestions = (Map<Long, List<QuestionDto>>) bundle
				.get(BundleConstants.OPTION_QUESTION_LIST_KEY);
		if (optionQuestions == null) {
			optionQuestions = new HashMap<Long, List<QuestionDto>>();
		}
		if (currentQuestion != null) {
			populateFields();
		} else {
			currentQuestion = new QuestionDto();
			QuestionGroupDto currentGroup = (QuestionGroupDto) bundle
					.get(BundleConstants.QUESTION_GROUP_KEY);
			currentQuestion.setSurveyId(currentGroup.getSurveyId());
			currentQuestion.setPath(currentGroup.getPath() + "/"
					+ currentGroup.getCode());
		}
	}

	@Override
	public void onChange(ChangeEvent event) {
		if (event.getSource() == questionTypeSelector) {
			if (QuestionDto.QuestionType.OPTION.toString().equals(
					questionTypeSelector.getValue(questionTypeSelector
							.getSelectedIndex()))) {
				optionPanel.setVisible(true);
				loadOptions();
			} else {
				optionPanel.setVisible(false);
			}
		} else if (event.getSource() == dependentQuestionSelector) {
			int index = dependentQuestionSelector.getSelectedIndex();
			if (index > 0) {
				loadDependentQuestionAnswers(dependentQuestionSelector
						.getValue(index));
			} else {
				populateDependencyAnswers(currentQuestion, null);
			}
		}
	}

	@Override
	public void onClick(ClickEvent event) {
		if (event.getSource() == dependentBox) {
			if (dependentBox.getValue()) {
				dependencyPanel.setVisible(true);
				loadDependencyList();
			} else {
				dependencyPanel.setVisible(false);
			}
		} else if (event.getSource() == addOptionButton) {
			installOptionRow(null);
		}
	}
}