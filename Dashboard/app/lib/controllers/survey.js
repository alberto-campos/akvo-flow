FLOW.SurveyController = Ember.ObjectController.extend({
    needs: 'survey_groups',
    survey_groups: Ember.computed.alias('controllers.survey_groups'),

    editingGroup: null,

    disableSave: function () {
        return !this.get('code') || this.get('code').trim() === '' || !this.get('isDirty') || this.get('isSaving');
    }.property('code', 'isDirty', 'isSaving'),

    disablePublish: function () {
        return this.get('isSaving');
    }.property('isSaving'),

    disablePreview: function () {
        return this.get('isSaving');
    }.property('isSaving'),

    actions: {
        save: function () {
            var survey = this.get('model');

            this.set('name', this.get('code'));

            survey.save();
        },

        collapseGroups: function (selected) {
            var editing = this.get('editingGroup');
            if (editing) {
                editing.send('collapse');
            }
        }
    }
});
