// ***********************************************//
//                 Router                    
// ***********************************************//
  FLOW.Router = Ember.Router.extend({
  	enableLogging: true,
    location: 'hash',
    root: Ember.Route.extend({
      doNavHome: function(router, context) {  router.transitionTo('navHome'); },
 
      doNavSurveys: function(router, context) {  router.transitionTo('navSurveys'); },
 
      doNavDevices: function(router, context) { router.transitionTo('navDevices'); },
      
      doNavData: function(router, context) { router.transitionTo('navData.index'); },
           
      doNavReports: function(router, context) { router.transitionTo('navReports'); },
      
      doNavMaps: function(router, context) { router.transitionTo('navMaps'); },
      
      doNavUsers: function(router, context) { router.transitionTo('navUsers');},
      
      doNavAdmin: function(router, context) { router.transitionTo('navAdmin');},
      
      
      index: Ember.Route.extend({
      	route: '/',
       redirectsTo:'navHome'       
    	}),
      
      navHome: Ember.Route.extend({
        route: '/',
        connectOutlets: function(router, context) {
          router.get('applicationController').connectOutlet('navHome');
          router.set('navigationController.selected', 'navHome');
        }
      }),
      
      navSurveys: Ember.Route.extend({
        route: '/surveys',
        connectOutlets: function(router, context) {
          router.get('applicationController').connectOutlet('navSurveys');
          router.set('navigationController.selected', 'navSurveys');
        }
      }),
      
      navDevices: Ember.Route.extend({
        route: '/devices',
        connectOutlets: function(router, context) {
          router.get('applicationController').connectOutlet('navDevices');
          router.set('navigationController.selected', 'navDevices');
        }
      }),
      
      
      // the navData tab has subnavigation
      navData: Ember.Route.extend({
        route: '/data',
        connectOutlets: function(router, context) {
          router.get('applicationController').connectOutlet('navData');
          router.set('navigationController.selected', 'navData');
        },
       
        doInspectData: function(router, context) { router.transitionTo('navData.inspectData'); },
        
        doImportSurvey: function(router, context) { router.transitionTo('navData.importSurvey'); },
        
        doExcelImport: function(router, context) { router.transitionTo('navData.excelImport'); },
        
        doExcelExport: function(router, context) { router.transitionTo('navData.excelExport'); },
       
       
       index: Ember.Route.extend({
          route: '/',
          redirectsTo: 'inspectData'
        }),
        
        inspectData: Ember.Route.extend({ 
          route: '/inspectdata',
          connectOutlets: function(router, context) { 
   					router.get('navDataController').connectOutlet('inspectData');
   					router.get('inspectDataController').set('content', FLOW.store.findAll(FLOW.SurveyGroup));
        		var newrec= FLOW.store.createRecord(FLOW.SurveyGroup,  
    								{"description":"MTW description" , "displayName":"MTWTW"});
    				FLOW.store.commit();
   					router.set('datasubnavController.selected', 'inspectData');
       
        	}
        }),
        
        importSurvey: Ember.Route.extend({ 
          route: '/importsurvey',
          connectOutlets: function(router, context) { 
          	router.get('navDataController').connectOutlet('importSurvey');
            router.set('datasubnavController.selected', 'importSurvey');
        	}
        }),
        
         excelImport: Ember.Route.extend({ 
          route: '/excelimport',
          connectOutlets: function(router, context) { 
   					router.get('navDataController').connectOutlet('excelImport');
   				  router.set('datasubnavController.selected', 'excelImport');
         
        	}
        }),
        
         excelExport: Ember.Route.extend({ 
          route: '/excelexport',
          connectOutlets: function(router, context) { 
    				router.get('navDataController').connectOutlet('excelExport');
    				router.set('datasubnavController.selected', 'excelExport');
          
        	}
        }),
        
    }),
       
       navReports: Ember.Route.extend({
        route: '/reports',
        connectOutlets: function(router, context) {
          router.get('applicationController').connectOutlet('navReports');
          router.set('navigationController.selected', 'navReports');
        }
      }),
      
       navMaps: Ember.Route.extend({
        route: '/maps',
        connectOutlets: function(router, context) {
          router.get('applicationController').connectOutlet('navMaps');
          router.set('navigationController.selected', 'navMaps');
        }
      }),
      
       navUsers: Ember.Route.extend({
        route: '/users',
        connectOutlets: function(router, context) {
          router.get('applicationController').connectOutlet('navUsers');
          router.set('navigationController.selected', 'navUsers');
        }
      }),
      
       navAdmin: Ember.Route.extend({
        route: '/admin',
        connectOutlets: function(router, context) {
          router.get('applicationController').connectOutlet('navAdmin');
          router.set('navigationController.selected', 'navAdmin');
        }
      })

    
    })
  });


