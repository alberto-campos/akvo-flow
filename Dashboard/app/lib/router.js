require('akvo-flow/core');


FLOW.Router.map(function () {

    this.resource('survey_groups', {
        path: '/'
    }, function () {

        this.resource('survey_group', {
            path: '/survey_group/:survey_group_id'
        });
    });

    this.resource('devices', {
        path: '/devices'
    }, function () {

    });

    this.resource('data', {
        path: '/data'
    }, function () {

    });

    this.resource('reports', {
        path: '/reports'
    }, function () {

    });

    this.resource('maps', {
        path: '/users'
    }, function () {

    });

    this.resource('messages', {
        path: '/messages'
    }, function () {

    });

});

FLOW.SurveyGroupsRoute = Ember.Route.extend({
    model: function () {
        return FLOW.SurveyGroup.find();
    }
});

FLOW.SurveyGroupRoute = Ember.Route.extend({
    setupController: function (controller, model) {
        controller.set('model', model); // default action
        controller.set('surveys', FLOW.Survey.find({
            surveyGroupId: model.get('id')
        }));
    }
});

FLOW.DevicesRoute = Ember.Route.extend({
    model: function () {
        return FLOW.Device.find();
    }
});

FLOW.MessagesRoute = Ember.Route.extend({
    model: function () {
        return FLOW.Message.find();
    }
});
