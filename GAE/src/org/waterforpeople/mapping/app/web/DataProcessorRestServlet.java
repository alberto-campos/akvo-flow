/*
 *  Copyright (C) 2010-2013 Stichting Akvo (Akvo Foundation)
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

package org.waterforpeople.mapping.app.web;

import java.io.BufferedInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipInputStream;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.http.HttpServletRequest;

import net.sf.jsr107cache.Cache;
import net.sf.jsr107cache.CacheFactory;
import net.sf.jsr107cache.CacheManager;

import org.waterforpeople.mapping.analytics.dao.SurveyInstanceSummaryDao;
import org.waterforpeople.mapping.analytics.dao.SurveyQuestionSummaryDao;
import org.waterforpeople.mapping.analytics.domain.SurveyQuestionSummary;
import org.waterforpeople.mapping.app.web.dto.DataProcessorRequest;
import org.waterforpeople.mapping.dao.AccessPointDao;
import org.waterforpeople.mapping.dao.DeviceFilesDao;
import org.waterforpeople.mapping.dao.QuestionAnswerStoreDao;
import org.waterforpeople.mapping.dao.SurveyInstanceDAO;
import org.waterforpeople.mapping.dataexport.SurveyReplicationImporter;
import org.waterforpeople.mapping.domain.AccessPoint;
import org.waterforpeople.mapping.domain.GeoCoordinates;
import org.waterforpeople.mapping.domain.QuestionAnswerStore;
import org.waterforpeople.mapping.domain.SurveyInstance;

import com.gallatinsystems.common.Constants;
import com.gallatinsystems.device.domain.DeviceFiles;
import com.gallatinsystems.framework.rest.AbstractRestApiServlet;
import com.gallatinsystems.framework.rest.RestRequest;
import com.gallatinsystems.framework.rest.RestResponse;
import com.gallatinsystems.framework.servlet.PersistenceFilter;
import com.gallatinsystems.gis.location.GeoLocationService;
import com.gallatinsystems.gis.location.GeoLocationServiceGeonamesImpl;
import com.gallatinsystems.gis.location.GeoPlace;
import com.gallatinsystems.messaging.dao.MessageDao;
import com.gallatinsystems.messaging.domain.Message;
import com.gallatinsystems.operations.dao.ProcessingStatusDao;
import com.gallatinsystems.operations.domain.ProcessingStatus;
import com.gallatinsystems.survey.dao.QuestionDao;
import com.gallatinsystems.survey.dao.QuestionGroupDao;
import com.gallatinsystems.survey.dao.QuestionOptionDao;
import com.gallatinsystems.survey.dao.SurveyDAO;
import com.gallatinsystems.survey.dao.SurveyUtils;
import com.gallatinsystems.survey.domain.Question;
import com.gallatinsystems.survey.domain.QuestionGroup;
import com.gallatinsystems.survey.domain.QuestionOption;
import com.gallatinsystems.survey.domain.Survey;
import com.gallatinsystems.surveyal.dao.SurveyedLocaleDao;
import com.gallatinsystems.surveyal.domain.SurveyedLocale;
import com.google.appengine.api.backends.BackendServiceFactory;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.stdimpl.GCacheFactory;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;

/**
 * Restful servlet to do bulk data update operations
 *
 * @author Christopher Fagiani
 *
 */
public class DataProcessorRestServlet extends AbstractRestApiServlet {
	private static final Logger log = Logger
			.getLogger("DataProcessorRestServlet");
	private static final long serialVersionUID = -7902002525342262821L;
	private static final String REBUILD_Q_SUM_STATUS_KEY = "rebuildQuestionSummary";
	private static final Integer QAS_PAGE_SIZE = 300;
	private static final String QAS_TO_REMOVE = "QAStoRemove";

	@Override
	protected RestRequest convertRequest() throws Exception {
		HttpServletRequest req = getRequest();
		RestRequest restRequest = new DataProcessorRequest();
		restRequest.populateFromHttpRequest(req);
		return restRequest;
	}

	@Override
	protected RestResponse handleRequest(RestRequest req) throws Exception {
		DataProcessorRequest dpReq = (DataProcessorRequest) req;
		if (DataProcessorRequest.PROJECT_FLAG_UPDATE_ACTION
				.equalsIgnoreCase(dpReq.getAction())) {
			updateAccessPointProjectFlag(dpReq.getCountry(), dpReq.getCursor());
		} else if (DataProcessorRequest.REBUILD_QUESTION_SUMMARY_ACTION
				.equalsIgnoreCase(dpReq.getAction())) {
			rebuildQuestionSummary(dpReq.getSurveyId());
		} else if (DataProcessorRequest.COPY_SURVEY.equalsIgnoreCase(dpReq
				.getAction())) {
			copySurvey(dpReq.getSurveyId(), Long.valueOf(dpReq.getSource()));
		} else if (DataProcessorRequest.IMPORT_REMOTE_SURVEY_ACTION
				.equalsIgnoreCase(dpReq.getAction())) {
			SurveyReplicationImporter sri = new SurveyReplicationImporter();
			sri.executeImport(dpReq.getSource(), dpReq.getSurveyId(), dpReq.getApiKey());
		} else if (DataProcessorRequest.RESCORE_AP_ACTION
				.equalsIgnoreCase(dpReq.getAction())) {
			rescoreAp(dpReq.getCountry());
		} else if (DataProcessorRequest.FIX_NULL_SUBMITTER_ACTION
				.equalsIgnoreCase(dpReq.getAction())) {
			fixNullSubmitter();
		} else if (DataProcessorRequest.FIX_DUPLICATE_OTHER_TEXT_ACTION
				.equalsIgnoreCase(dpReq.getAction())) {
			fixDuplicateOtherText();
		} else if (DataProcessorRequest.TRIM_OPTIONS.equalsIgnoreCase(dpReq
				.getAction())) {
			trimOptions();
		} else if (DataProcessorRequest.FIX_OPTIONS2VALUES_ACTION
				.equalsIgnoreCase(dpReq.getAction())) {
			fixOptions2Values();
		} else if (DataProcessorRequest.SURVEY_INSTANCE_SUMMARIZER
				.equalsIgnoreCase(dpReq.getAction())) {
			surveyInstanceSummarizer(dpReq.getSurveyInstanceId(),
					dpReq.getQasId(), dpReq.getDelta());
		} else if (DataProcessorRequest.DELETE_DUPLICATE_QAS
				.equalsIgnoreCase(dpReq.getAction())) {
			deleteDuplicatedQAS(dpReq.getOffset());
		} else if (DataProcessorRequest.CHANGE_LOCALE_TYPE_ACTION
				.equalsIgnoreCase(dpReq.getAction())) {
			changeLocaleType(dpReq.getSurveyId());
		}
		return new RestResponse();
	}

	@Override
	protected void writeOkResponse(RestResponse resp) throws Exception {
		getResponse().setStatus(200);
	}

	/**
	 * lists all QuestionOptions and trims trailing/leading spaces. Then does
	 * the same for any dependencies
	 */
	private void trimOptions() {
		QuestionOptionDao optDao = new QuestionOptionDao();
		QuestionDao qDao = new QuestionDao();
		String cursor = null;
		do {
			List<QuestionOption> optList = optDao.list(cursor);

			if (optList != null && optList.size() > 0) {
				for (QuestionOption opt : optList) {
					if (opt.getText() != null) {
						opt.setText(opt.getText().trim());
					}
					List<Question> qList = qDao.listQuestionsByDependency(opt
							.getQuestionId());
					for (Question q : qList) {
						if (q.getText() != null) {
							q.setText(q.getText().trim());
						}
						if (q.getDependentQuestionAnswer() != null) {
							q.setDependentQuestionAnswer(q
									.getDependentQuestionAnswer().trim());
						}
					}
				}
				if (optList.size() == QuestionOptionDao.DEFAULT_RESULT_COUNT) {
					cursor = QuestionOptionDao.getCursor(optList);
				} else {
					cursor = null;
				}
			} else {
				cursor = null;
			}
		} while (cursor != null);
	}

	/**
	 * lists all "OTHER" type answers and checks if the last tokens are
	 * duplicates. Fixes if they are.
	 */
	private void fixDuplicateOtherText() {
		QuestionAnswerStoreDao qasDao = new QuestionAnswerStoreDao();
		int pageSize = 300;
		String cursor = null;
		do {
			List<QuestionAnswerStore> answers = qasDao.listByTypeAndDate(
					"OTHER", null, null, cursor, pageSize);
			if (answers != null) {
				for (QuestionAnswerStore ans : answers) {
					if (ans.getValue() != null && ans.getValue().contains("|")) {
						String[] tokens = ans.getValue().split("\\|");
						String lastVal = null;
						boolean droppedVal = false;
						StringBuilder buf = new StringBuilder();
						for (int i = 0; i < tokens.length; i++) {
							if (!tokens[i].equals(lastVal)) {
								lastVal = tokens[i];
								if (i > 0) {
									buf.append("|");
								}
								buf.append(lastVal);
							} else {
								droppedVal = true;
							}
						}
						if (droppedVal) {
							// only dirty the object if needed
							ans.setValue(buf.toString());
						}
					}
				}

				if (answers.size() == pageSize) {

					cursor = QuestionAnswerStoreDao.getCursor(answers);
				} else {
					cursor = null;
				}
			}
		} while (cursor != null);
	}

	/**
	 * changes the surveyedLocales attached to a survey to a different type
	 * 1 = Point
	 * 2 = Household
	 * 3 = Public Institutions
	 */
	private void changeLocaleType(Long surveyId) {
		SurveyInstanceDAO siDao = new SurveyInstanceDAO();
		SurveyedLocaleDao slDao = new SurveyedLocaleDao();
		SurveyDAO sDao = new SurveyDAO();
		String cursor = null;
		// get the desired type from the survey definition
		Survey s = sDao.getByKey(surveyId);
		if (s != null && s.getPointType() != null && s.getPointType().length() > 0){
			String localeType = s.getPointType();

			do {
				List<SurveyInstance> siList = siDao.listSurveyInstanceBySurvey(surveyId, QAS_PAGE_SIZE, cursor);
				List<SurveyedLocale> slList = new ArrayList<SurveyedLocale>();
				if (siList != null && siList.size() > 0) {
					for (SurveyInstance si : siList) {
						if (si.getSurveyedLocaleId() != null) {
							SurveyedLocale sl = slDao.getByKey(si.getSurveyedLocaleId());
							if (sl != null){
								// if the locale type is not set or if it is not equal to the survey setting, 
								// reset the local type
								if (sl.getLocaleType() == null || !sl.getLocaleType().equals(localeType)) {
									sl.setLocaleType(localeType);
									slList.add(sl);
								}
							}
						}
					}
					slDao.save(slList);
					if (siList.size() == QAS_PAGE_SIZE) {
						cursor = SurveyInstanceDAO.getCursor(siList);
					} else {
						cursor = null;
					}
				}
			} while (cursor != null);
		}
	}

	private void fixNullSubmitter() {
		SurveyInstanceDAO instDao = new SurveyInstanceDAO();
		List<SurveyInstance> instances = instDao.listInstanceBySubmitter(null);

		if (instances != null) {
			DeviceFilesDao dfDao = new DeviceFilesDao();
			for (SurveyInstance inst : instances) {
				DeviceFiles f = dfDao.findByInstance(inst.getKey().getId());
				if (f != null) {
					try {
						URL url = new URL(f.getURI());
						BufferedInputStream bis = new BufferedInputStream(
								url.openStream());
						ZipInputStream zis = new ZipInputStream(bis);
						ArrayList<String> lines = TaskServlet
								.extractDataFromZip(zis);
						zis.close();

						if (lines != null) {
							for (String line : lines) {
								String[] parts = line.split("\t");
								if (parts.length > 5) {
									if (parts[5] != null
											&& parts[5].trim().length() > 0) {
										inst.setSubmitterName(parts[5]);
										break;
									}
								}
							}
						}
					} catch (Exception e) {
						log("Could not download zip: " + f.getURI());
					}
				}
			}
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void deleteDuplicatedQAS(Long offset) {
		log.log(Level.INFO, "Searching for duplicated QAS entities [Offset: "
				+ offset + "]");

		Cache cache = null;
		Map props = new HashMap();
		props.put(GCacheFactory.EXPIRATION_DELTA, 12 * 60 * 60);
		props.put(MemcacheService.SetPolicy.SET_ALWAYS, true);
		try {
			CacheFactory cacheFactory = CacheManager.getInstance()
					.getCacheFactory();
			cache = cacheFactory.createCache(props);
		} catch (Exception e) {
			log.log(Level.SEVERE,
					"Couldn't initialize cache: " + e.getMessage(), e);
		}

		if (cache == null) {
			return;
		}

		final PersistenceManager pm = PersistenceFilter.getManager();
		final Query q = pm.newQuery(QuestionAnswerStore.class);
		q.setOrdering("createdDateTime asc");
		q.setRange(offset, offset + QAS_PAGE_SIZE);

		final List<QuestionAnswerStore> results = (List<QuestionAnswerStore>) q
				.execute();

		List<QuestionAnswerStore> toRemove;

		if (cache.containsKey(QAS_TO_REMOVE)) {
			toRemove = (List<QuestionAnswerStore>) cache.get(QAS_TO_REMOVE);
		} else {
			toRemove = new ArrayList<QuestionAnswerStore>();
		}

		for (QuestionAnswerStore item : results) {

			final Long questionID = Long.valueOf(item.getQuestionID());
			final Long surveyInstanceId = item.getSurveyInstanceId();

			final Map<Long, Long> k = new HashMap<Long, Long>();
			k.put(surveyInstanceId, questionID);

			if (cache.containsKey(k)) {
				toRemove.add(item);
			}

			cache.put(k, true);
		}

		if (results.size() == QAS_PAGE_SIZE) {

			cache.put(QAS_TO_REMOVE, toRemove);

			final TaskOptions options = TaskOptions.Builder
					.withUrl("/app_worker/dataprocessor")
					.param(DataProcessorRequest.ACTION_PARAM,
							DataProcessorRequest.DELETE_DUPLICATE_QAS)
					.param(DataProcessorRequest.OFFSET_PARAM,
							String.valueOf(offset + QAS_PAGE_SIZE))
					.header("Host",
							BackendServiceFactory.getBackendService()
									.getBackendAddress("dataprocessor"));
			Queue queue = QueueFactory.getDefaultQueue();
			queue.add(options);

		} else {
			log.log(Level.INFO, "Removing " + toRemove.size()
					+ " duplicated QAS entities");
			QuestionAnswerStoreDao dao = new QuestionAnswerStoreDao();
			pm.makePersistentAll(toRemove); // some objects are in "transient" state
			dao.delete(toRemove);
		}
	}

	/**
	 * this method re-runs scoring on all access points for a country
	 *
	 * @param country
	 */
	private void rescoreAp(String country) {
		AccessPointDao apDao = new AccessPointDao();
		String cursor = null;
		List<AccessPoint> apList = null;
		do {
			apList = apDao.listAccessPointByLocation(country, null, null, null,
					cursor, 200);
			if (apList != null) {
				cursor = AccessPointDao.getCursor(apList);

				for (AccessPoint ap : apList) {
					apDao.save(ap);
				}
			}
		} while (apList != null && apList.size() == 200);
	}

	private void copySurvey(Long surveyId, Long sourceId) {

		final QuestionGroupDao qgDao = new QuestionGroupDao();
		final Map<Long, Long> qMap = new HashMap<Long, Long>();

		final List<QuestionGroup> qgList = qgDao.listQuestionGroupBySurvey(sourceId);

		if (qgList == null) {
			log.log(Level.INFO, "Nothing to copy from {surveyId: " + sourceId
					+ "} to {surveyId: " + surveyId + "}");
			SurveyUtils.resetSurveyState(surveyId);
			return;
		}

		log.log(Level.INFO, "Copying " + qgList.size() + " `QuestionGroup`");
		int qgOrder = 1;
		for (final QuestionGroup sourceQG : qgList) {
			SurveyUtils.copyQuestionGroup(sourceQG, surveyId, qgOrder++, qMap);
		}

		SurveyUtils.resetSurveyState(surveyId);

		MessageDao mDao = new MessageDao();
		Message message = new Message();

		message.setObjectId(surveyId);
		message.setActionAbout("copySurvey");
		message.setShortMessage("Copy from Survey " + sourceId + " to Survey " + surveyId + " completed");
		mDao.save(message);

	}

	/**
	 * rebuilds the SurveyQuestionSummary object for ALL data in the system.
	 * This method should only be run on a Backend instance as it is unlikely to
	 * complete within the task duration limits on other instances.
	 */
	private void rebuildQuestionSummary(Long surveyId) {
		ProcessingStatusDao statusDao = new ProcessingStatusDao();
		List<Long> surveyIds = new ArrayList<Long>();
		if (surveyId == null) {
			SurveyDAO surveyDao = new SurveyDAO();
			List<Survey> surveys = surveyDao.list(Constants.ALL_RESULTS);
			if (surveys != null) {
				for (Survey s : surveys) {
					surveyIds.add(s.getKey().getId());
				}
			}
		} else {
			surveyIds.add(surveyId);
		}

		for (Long sid : surveyIds) {
			ProcessingStatus status = statusDao
					.getStatusByCode(REBUILD_Q_SUM_STATUS_KEY
							+ (sid != null ? ":" + sid : ""));

			Map<String, Map<String, Long>> summaryMap = summarizeQuestionAnswerStore(
					sid, null);
			if (summaryMap != null) {
				saveSummaries(summaryMap);
			}
			// now update the status so we can know it last ran
			if (status == null) {
				status = new ProcessingStatus();
				status.setCode(REBUILD_Q_SUM_STATUS_KEY
						+ (sid != null ? ":" + sid : ""));
			}
			status.setInError(false);
			status.setLastEventDate(new Date());
			statusDao.save(status);
		}
	}

	/**
	 * iterates over the new summary counts and updates the records in the
	 * datastore. Where appropriate, new records will be created and defunct
	 * records will be removed.
	 *
	 * @param summaryMap
	 */
	private void saveSummaries(Map<String, Map<String, Long>> summaryMap) {
		SurveyQuestionSummaryDao summaryDao = new SurveyQuestionSummaryDao();
		for (Entry<String, Map<String, Long>> summaryEntry : summaryMap
				.entrySet()) {
			List<SurveyQuestionSummary> summaryList = summaryDao
					.listByQuestion(summaryEntry.getKey());
			// iterate over all the counts and update the summaryList with the
			// count values. Create any missing elements and remove defunct
			// entries as we go
			List<SurveyQuestionSummary> toDeleteList = new ArrayList<SurveyQuestionSummary>(
					summaryList);
			List<SurveyQuestionSummary> toCreateList = new ArrayList<SurveyQuestionSummary>();
			for (Entry<String, Long> valueEntry : summaryEntry.getValue()
					.entrySet()) {
				String val = valueEntry.getKey();
				boolean found = false;
				for (SurveyQuestionSummary sum : summaryList) {
					if (sum.getResponse() != null
							&& sum.getResponse().equals(val)) {
						// since it's still valid, remove it from toDeleteList
						toDeleteList.remove(sum);
						// update the count. Since we still have the
						// persistenceContext open, this will automatically be
						// flushed to the datastore without an explicit call to
						// save
						sum.setCount(valueEntry.getValue());
						found = true;
					}
				}
				if (!found) {
					// need to create it
					SurveyQuestionSummary s = new SurveyQuestionSummary();
					s.setCount(valueEntry.getValue());
					s.setQuestionId(summaryEntry.getKey());
					s.setResponse(val);
					toCreateList.add(s);
				}
			}
			// delete the unseen entities
			if (toDeleteList.size() > 0) {
				summaryDao.delete(toDeleteList);
			}
			// save the new items
			if (toCreateList.size() > 0) {
				summaryDao.save(toCreateList);
			}
			// flush the datastore operation
			summaryDao.flushBatch();
		}
	}

	/**
	 * loads all the summarizable QuestionAnswerStore instances from the data
	 * store and accrues counts by value occurrence in a map keyed on the
	 * questionId
	 *
	 * @param sinceDate
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map<String, Map<String, Long>> summarizeQuestionAnswerStore(
			Long surveyId, Date sinceDate) {

		final QuestionAnswerStoreDao qasDao = new QuestionAnswerStoreDao();
		final QuestionDao questionDao = new QuestionDao();
		final List<Question> qList = questionDao.listQuestionByType(surveyId,
				Question.Type.OPTION);

		Cache cache = null;
		Map props = new HashMap();
		props.put(GCacheFactory.EXPIRATION_DELTA, 60 * 60 * 2); // 2h
		props.put(MemcacheService.SetPolicy.SET_ALWAYS, true);
		try {
			CacheFactory cacheFactory = CacheManager.getInstance()
					.getCacheFactory();
			cache = cacheFactory.createCache(props);
		} catch (Exception e) {
			log.log(Level.SEVERE,
					"Couldn't initialize cache: " + e.getMessage(), e);
		}

		String cursor = null;
		final Map<String, Map<String, Long>> summaryMap = new HashMap<String, Map<String, Long>>();

		for (Question q : qList) {
			List<QuestionAnswerStore> qasList  = qasDao.listByQuestion(q.getKey().getId(), cursor, QAS_PAGE_SIZE);

			if (qasList == null || qasList.size() == 0) {
				continue; // skip
			}

			do {
				cursor = QuestionAnswerStoreDao.getCursor(qasList);

				for(QuestionAnswerStore qas : qasList) {

					if (cache != null) {
						Map<Long, String> answer = new HashMap<Long, String>();
						answer.put(qas.getSurveyInstanceId(), qas.getQuestionID());

						if (cache.containsKey(answer)) {
							log.log(Level.INFO, "Found duplicated QAS {surveyInstanceId: " + qas.getSurveyInstanceId() +" , questionID: " + qas.getQuestionID() +"}");
							continue;
						}

						cache.put(answer, true);
					}

					String val = qas.getValue();

					Map<String, Long> countMap = summaryMap.get(qas
							.getQuestionID());

					if (countMap == null) {
						countMap = new HashMap<String, Long>();
						summaryMap.put(qas.getQuestionID(), countMap);
					}

					// split up multiple answers
					String[] answers;

					if (val != null && val.contains("|")) {
						answers = val.split("\\|");
					} else {
						answers = new String[] { val };
					}

					// perform count
					for (int i = 0; i < answers.length; i++) {
						Long count = countMap.get(answers[i]);
						if (count == null) {
							count = 1L;
						} else {
							count = count + 1;
						}
						countMap.put(answers[i], count);
					}
				}

				qasList  = qasDao.listByQuestion(q.getKey().getId(), cursor, QAS_PAGE_SIZE);

			} while (qasList != null && qasList.size() > 0);

			cursor = null;
		}

		return summaryMap;
	}

	/**
	 * iterates over all AccessPoints in a country and applies a static set of
	 * rules to determine the proper value of the WFPProjectFlag
	 *
	 * @param country
	 * @param cursor
	 */
	private void updateAccessPointProjectFlag(String country, String cursor) {
		AccessPointDao apDao = new AccessPointDao();
		Integer pageSize = 200;
		List<AccessPoint> apList = apDao.listAccessPointByLocation(country,
				null, null, null, cursor, pageSize);
		if (apList != null) {
			for (AccessPoint ap : apList) {

				if ("PE".equalsIgnoreCase(ap.getCountryCode())) {
					ap.setWaterForPeopleProjectFlag(false);
				} else if ("RW".equalsIgnoreCase(ap.getCountryCode())) {
					ap.setWaterForPeopleProjectFlag(false);
				} else if ("MW".equalsIgnoreCase(ap.getCountryCode())) {
					if (ap.getCommunityName().trim()
							.equalsIgnoreCase("Kachere/Makhetha/Nkolokoti")) {
						ap.setCommunityName("Kachere/Makhetha/Nkolokoti");
						if (ap.getWaterForPeopleProjectFlag() == null) {
							ap.setWaterForPeopleProjectFlag(true);
						}
					} else if (ap.getWaterForPeopleProjectFlag() == null) {
						ap.setWaterForPeopleProjectFlag(false);
					}
				} else if ("HN".equalsIgnoreCase(ap.getCountryCode())) {
					if (ap.getCommunityCode().startsWith("IL")) {
						ap.setWaterForPeopleProjectFlag(false);
					} else {
						ap.setWaterForPeopleProjectFlag(true);
					}

				} else if ("IN".equalsIgnoreCase(ap.getCountryCode())) {
					if (ap.getWaterForPeopleProjectFlag() == null) {
						ap.setWaterForPeopleProjectFlag(true);
					}
				} else if ("GT".equalsIgnoreCase(ap.getCountryCode())) {
					if (ap.getWaterForPeopleProjectFlag() == null) {
						ap.setWaterAvailableDayVisitFlag(true);
					}
				} else {
					// handles BO, DO, SV
					if (ap.getWaterForPeopleProjectFlag() == null) {
						ap.setWaterForPeopleProjectFlag(false);
					}
				}
			}

			if (apList.size() == pageSize) {
				// check for more
				sendProjectUpdateTask(country, AccessPointDao.getCursor(apList));
			}
		}
	}

	/**
	 * Sends a message to a task queue to start or continue the processing of
	 * the AP Project Flag
	 *
	 * @param country
	 * @param cursor
	 */
	public static void sendProjectUpdateTask(String country, String cursor) {
		TaskOptions options = TaskOptions.Builder
				.withUrl("/app_worker/dataprocessor")
				.param(DataProcessorRequest.ACTION_PARAM,
						DataProcessorRequest.PROJECT_FLAG_UPDATE_ACTION)
				.param(DataProcessorRequest.COUNTRY_PARAM, country)
				.param(DataProcessorRequest.CURSOR_PARAM,
						cursor != null ? cursor : "");
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(options);

	}

	/**
	 * fixes wrong Types in questionAnswerStore objects. When cleaned data is
	 * uploaded using an excel file, the type of the answer is set according to
	 * the type of the question, while the device sets the type according to a
	 * different convention. The action handles QAS_PAGE_SIZE items in one call, and
	 * invokes new tasks as necessary if there are more items.
	 *
	 * @param cursor
	 * @author M.T. Westra
	 */
	public static void fixOptions2Values() {
		SurveyInstanceDAO siDao = new SurveyInstanceDAO();
		QuestionAnswerStoreDao qasDao = new QuestionAnswerStoreDao();
		List<QuestionAnswerStore> qasList = siDao.listQAOptions(null,
				QAS_PAGE_SIZE, "OPTION", "FREE_TEXT", "NUMBER", "SCAN", "PHOTO");
		List<QuestionAnswerStore> qasChangedList = new ArrayList<QuestionAnswerStore>();
		log.log(Level.INFO, "Running fixOptions2Values");
		if (qasList != null) {
			for (QuestionAnswerStore qas : qasList) {
				if (Question.Type.OPTION.toString().equals(qas.getType())
						|| Question.Type.NUMBER.toString()
								.equals(qas.getType())
						|| Question.Type.FREE_TEXT.toString().equals(
								qas.getType())
						|| Question.Type.SCAN.toString().equals(qas.getType())) {
					qas.setType("VALUE");
					qasChangedList.add(qas);
				} else if (Question.Type.PHOTO.toString().equals(qas.getType())) {
					qas.setType("IMAGE");
					qasChangedList.add(qas);
				}
			}
			qasDao.save(qasChangedList);
			// if there are more, invoke another task
			if (qasList.size() == QAS_PAGE_SIZE) {
				log.log(Level.INFO, "invoking another fixOptions task");
				Queue queue = QueueFactory.getDefaultQueue();
				TaskOptions options = TaskOptions.Builder
						.withUrl("/app_worker/dataprocessor")
						.param(DataProcessorRequest.ACTION_PARAM,
								DataProcessorRequest.FIX_OPTIONS2VALUES_ACTION);
				queue.add(options);
			}
		}
	}

	public static void surveyInstanceSummarizer(Long surveyInstanceId,
			Long qasId, Integer delta) {
		SurveyInstanceDAO siDao = new SurveyInstanceDAO();
		QuestionAnswerStoreDao qasDao = new QuestionAnswerStoreDao();
		boolean success = false;
		if (surveyInstanceId != null) {
			SurveyInstance si = siDao.getByKey(surveyInstanceId);
			if (si != null && qasId != null) {
				QuestionAnswerStore qas = qasDao.getByKey(qasId);
				if (qas != null) {
					GeoCoordinates geoC = null;
					if (qas.getValue() != null
							&& qas.getValue().trim().length() > 0) {
						geoC = GeoCoordinates.extractGeoCoordinate(qas
								.getValue());
					}
					if (geoC != null) {
						GeoLocationService gisService = new GeoLocationServiceGeonamesImpl();
						GeoPlace gp = gisService.findDetailedGeoPlace(geoC
								.getLatitude().toString(), geoC.getLongitude()
								.toString());
						if (gp != null) {
							SurveyInstanceSummaryDao.incrementCount(
									gp.getSub1(), gp.getCountryCode(),
									qas.getCollectionDate(), delta.intValue());
							success = true;
						}
					}
				}
			}
		}
		if (!success) {
			log.log(Level.SEVERE,
					"Couldnt find geoplace for instance. Instance id: "
							+ surveyInstanceId);
		}
	}

}
