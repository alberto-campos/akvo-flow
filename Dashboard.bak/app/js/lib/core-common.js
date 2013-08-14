require('akvo-flow/templ');
Ember.LOG_BINDINGS = false;
// Create the application
window.FLOW = Ember.Application.create({
  VERSION: '0.0.1'
});

/* Generic FLOW view that also handles lanague rerenders*/
FLOW.View = Ember.View.extend({
  onLanguageChange: function () {
    this.rerender();
  }.observes('FLOW.dashboardLanguageControl.dashboardLanguage')
});