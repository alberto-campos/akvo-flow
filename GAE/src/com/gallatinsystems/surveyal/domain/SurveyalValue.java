package com.gallatinsystems.surveyal.domain;

import java.util.Date;

import javax.jdo.annotations.PersistenceCapable;

import com.gallatinsystems.framework.domain.BaseDomain;

/**
 * domain structure to represent the value of an surveyed metric for one
 * surveyed locale at a given point in time. If the survey question used to
 * gather this value was mapped to a metric, the metricId and metricName will be
 * populated (otherwise, they are null). Similarly, the valueType can be either
 * NUMERIC or STRING and the numericValue or stringValue will be populated
 * accordingly.
 * 
 * @author Christopher Fagiani
 * 
 */
@PersistenceCapable
public class SurveyalValue extends BaseDomain {
	private static final long serialVersionUID = 861240917705953798L;
	public static final String STRING_VAL_TYPE = "STRING";
	public static final String NUM_VAL_TYPE = "NUMERIC";

	private Integer year;
	private Integer day;
	private Integer month;
	private Date collectionDate;
	private Long surveyedLocaleId;
	private Long surveyInstanceId;
	private String questionText;
	private Long surveyQuestionId;
	private String metricName;
	private String metricGroup;
	private Long metricId;
	private Double score;
	private String valueType;
	private String stringValue;
	private Double numericValue;
	private String organization;
	private String countryCode;
	private String sublevel1;
	private String sublevel2;
	private String sublevel3;
	private String sublevel4;
	private String sublevel5;
	private String sublevel6;
	private String localeType;
	private String systemIdentifier;

	public Long getSurveyedLocaleId() {
		return surveyedLocaleId;
	}

	public void setSurveyedLocaleId(Long surveyedLocaleId) {
		this.surveyedLocaleId = surveyedLocaleId;
	}

	public String getMetricGroup() {
		return metricGroup;
	}

	public void setMetricGroup(String metricGroup) {
		this.metricGroup = metricGroup;
	}

	public String getSystemIdentifier() {
		return systemIdentifier;
	}

	public void setSystemIdentifier(String systemIdentifier) {
		this.systemIdentifier = systemIdentifier;
	}

	public String getCountryCode() {
		return countryCode;
	}

	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}

	public String getSublevel1() {
		return sublevel1;
	}

	public void setSublevel1(String sublevel1) {
		this.sublevel1 = sublevel1;
	}

	public String getSublevel2() {
		return sublevel2;
	}

	public void setSublevel2(String sublevel2) {
		this.sublevel2 = sublevel2;
	}

	public String getSublevel3() {
		return sublevel3;
	}

	public void setSublevel3(String sublevel3) {
		this.sublevel3 = sublevel3;
	}

	public String getSublevel4() {
		return sublevel4;
	}

	public void setSublevel4(String sublevel4) {
		this.sublevel4 = sublevel4;
	}

	public String getSublevel5() {
		return sublevel5;
	}

	public void setSublevel5(String sublevel5) {
		this.sublevel5 = sublevel5;
	}

	public String getSublevel6() {
		return sublevel6;
	}

	public void setSublevel6(String sublevel6) {
		this.sublevel6 = sublevel6;
	}

	public String getLocaleType() {
		return localeType;
	}

	public void setLocaleType(String localeType) {
		this.localeType = localeType;
	}

	public Integer getYear() {
		return year;
	}

	public void setYear(Integer year) {
		this.year = year;
	}

	public Integer getDay() {
		return day;
	}

	public void setDay(Integer day) {
		this.day = day;
	}

	public Integer getMonth() {
		return month;
	}

	public void setMonth(Integer month) {
		this.month = month;
	}

	public Date getCollectionDate() {
		return collectionDate;
	}

	public void setCollectionDate(Date collectionDate) {
		this.collectionDate = collectionDate;
	}

	public Long getSurveyInstanceId() {
		return surveyInstanceId;
	}

	public void setSurveyInstanceId(Long surveyInstanceId) {
		this.surveyInstanceId = surveyInstanceId;
	}

	public String getQuestionText() {
		return questionText;
	}

	public void setQuestionText(String questionText) {
		this.questionText = questionText;
	}

	public Long getSurveyQuestionId() {
		return surveyQuestionId;
	}

	public void setSurveyQuestionId(Long surveyQuestionId) {
		this.surveyQuestionId = surveyQuestionId;
	}

	public String getMetricName() {
		return metricName;
	}

	public void setMetricName(String metricName) {
		this.metricName = metricName;
	}

	public Long getMetricId() {
		return metricId;
	}

	public void setMetricId(Long metricId) {
		this.metricId = metricId;
	}

	public Double getScore() {
		return score;
	}

	public void setScore(Double score) {
		this.score = score;
	}

	public String getValueType() {
		return valueType;
	}

	public void setValueType(String valueType) {
		this.valueType = valueType;
	}

	public String getStringValue() {
		return stringValue;
	}

	public void setStringValue(String stringValue) {
		this.stringValue = stringValue;
	}

	public Double getNumericValue() {
		return numericValue;
	}

	public void setNumericValue(Double numericValue) {
		this.numericValue = numericValue;
	}

	public String getOrganization() {
		return organization;
	}

	public void setOrganization(String organization) {
		this.organization = organization;
	}

}