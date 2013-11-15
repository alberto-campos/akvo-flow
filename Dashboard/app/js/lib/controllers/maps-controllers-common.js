/**
  Controllers related to the map tab
  Definition:
    "placemark" is an FLOW object that represents a single survey point.
    "marker" is a map object that is rendered as a pin. Each marker have
      a placemark counterpart.
**/

FLOW.placemarkController = Ember.ArrayController.create({
  content: null,
  map: null,
  overlays: Ember.A(),
  geocellCache: [],
  currentGcLevel: null,


  populate: function () {
    //this.set('content', FLOW.store.findAll(FLOW.Placemark));
  },

  populateMap: function () {
    var gcLevel, placemarks;
    if (this.content.get('isLoaded') === true) {
      console.log('clearing overlays');
      this.clearOverlays();
      gcLevel = this.get('currentGcLevel');
      
      // filter placemarks
      placemarks = FLOW.store.filter(FLOW.Placemark, function(item){
        return item.get('level') == gcLevel;
      });

      placemarks.forEach(function (placemark) {
        this.addMarker(placemark);
      }, this);
    }
  }.observes('this.content.isLoaded'),

  adaptMap: function(bestBB, zoomlevel){
    console.log('--------------------------- adapting the map --------------------');
    var bbString = "", gcLevel, listToRetrieve = [];
    // determine the level we want to show
    if (zoomlevel < 5){
      gcLevel = 2;
    } else if (zoomlevel < 7) {
      gcLevel = 3;
    } else if (zoomlevel < 9) {
      gcLevel = 4;
    } else if (zoomlevel < 11) {
      gcLevel = 5;
    } else {
      gcLevel = 0;
    }
    this.set('currentGcLevel',gcLevel);
    console.log('current gclevel:',gcLevel, ', zoomlevel:', zoomlevel);

    // on zoomlevel 2, the map repeats itself, leading to wrong results
    // therefore, we force to download the highest level on all the world.
    console.log('bestBB:',bestBB);
    if (zoomlevel == 2) {
    	bestBB = ["0","1","2","3","4","5","6","7","8","9","a","b","c","d","e","f"];
    }
    
     // see if we already have it in the cache
     // TODO this is not optimal at high zoom levels, as we will already have loaded the same points on a level before
    for (var i = 0; i < bestBB.length; i++){
      if (this.get('geocellCache').indexOf(bestBB[i]+"-"+gcLevel) < 0 ) {
        listToRetrieve.push(bestBB[i]);
        this.get('geocellCache').push(bestBB[i] + "-" + gcLevel);
      }
    }

    // pack best bounding box values in a string
    listToRetrieve.forEach(function(item){
      bbString += item + ",";
    });
    // delete last comma
    bbString = bbString.substring(0, bbString.length - 1);

    // go get it in the datastore
    // when the points come in, populateMap will trigger and place the points
    if (!Ember.empty(bbString)){
      console.log('--------- database query on ', bbString, ", zoomlevel:", gcLevel);
      this.set('content',FLOW.store.findQuery(FLOW.Placemark,
        {bbString: bbString, gcLevel: gcLevel}));
    } else {
    	// we might have stuff in cache, so draw anyway
    	console.log('nothing to fetch, all comes from cache');
    	this.populateMap();
    }
  },

  clearOverlays: function () {
    this.overlays.map(function (marker) {
      marker.setMap(null);
    });
    this.overlays.clear();
  },

  addMarker: function (placemark) {
    var marker, map, coordinate, div ,count, icon = 1, mapLoc;
    coordinate = new google.maps.LatLng(placemark.get('latitude'),placemark.get('longitude'));
    map = this.get('map');
    
    if (placemark.get('level') > 0){
      count = placemark.get('count');
      
      if (count < 10) {
        icon = 3;
      } else if (count < 100) {
        icon = 2;
      }
 
      div = document.createElement('DIV');
      div.innerHTML = '<div class="cluster-marker-' + icon + '">' + count + '</div>';
      marker = new RichMarker({
          position: coordinate,
          map: map,
          draggable: false,
          flat: true,
          content: div
          });
      
      placemark.set('marker', marker);
      this.get('overlays').pushObject(marker);
      return marker;
    } 

    // if we are here, we have a normal placemark.
    marker = new google.maps.Marker({
      position: coordinate,
      map: map,
      placemark: placemark,
      icon: '/images/maps/blueMarker.png'
    });

    placemark.addMarkerClickHandler = function (marker) {
      google.maps.event.addListener(marker, 'click', function () {
        marker.placemark.handleClick(marker);
      });
    },

    placemark.handleClick = function (marker) {
      var oldSelected;

      marker.placemark.toggleMarker(marker.placemark);
      oldSelected = FLOW.placemarkController.get('selected');
      if (Ember.none(oldSelected)) {
        // console.log("No previous selection");
        FLOW.placemarkController.set('selected', marker.placemark);
      } else {
        if (this.marker === oldSelected.marker) {
          // console.log("Clicked a selected marker");
          FLOW.placemarkController.set('selected', undefined);
        } else {
          // console.log("Clicked a new marker");
          oldSelected.toggleMarker(oldSelected);
          FLOW.placemarkController.set('selected', marker.placemark);
        }
      }
    };

    placemark.toggleMarker = function (placemark) {
      var coordinate, iconUrl, map, newMarker;

      map = FLOW.placemarkController.get('map');
      coordinate = new google.maps.LatLng(placemark.get('latitude'),
        placemark.get('longitude'));

      if (placemark.marker.icon === ('/images/maps/blueMarker.png')) {
        iconUrl = '/images/maps/redMarker.png';
      } else {
        iconUrl = '/images/maps/blueMarker.png';
      }

      newMarker = new google.maps.Marker({
        position: coordinate,
        map: map,
        placemark: placemark,
        icon: iconUrl
      });

      placemark.addMarkerClickHandler(newMarker);
      placemark.marker.setMap(null);
      newMarker.placemark = placemark;

      placemark.set('marker', newMarker);
      FLOW.placemarkController.overlays.pushObject(newMarker);

    };

    placemark.addMarkerClickHandler(marker);
    placemark.set('marker', marker);
    this.overlays.pushObject(marker);
    return marker;
  }

});

FLOW.countryController = Ember.ArrayController.create({
  content: [],
  country: null,
  countryCode: null,

  init: function () {
    this._super();
    if (!Ember.none(FLOW.Env) && !Ember.none(FLOW.Env.countries)) {
      this.set('content', this.getContent(FLOW.Env.countries));
    }
  },

  handleCountrySelection: function () {
    this.resetMap();
    this.positionMap();
    this.set('countryCode', this.country.get('iso'));
    FLOW.placemarkController.populate(this.country);
  }.observes('this.country'),

  resetMap: function () {
    var selectedMarker;

    selectedMarker = FLOW.placemarkController.get('selected');
    if (!Ember.none(selectedMarker)) {
      selectedMarker.toggleMarker(selectedMarker);
      FLOW.placemarkController.set('selected', null);
    }
  },

  positionMap: function () {
    var country, map;

    country = this.get('country');
    map = FLOW.placemarkController.get('map');

    if (!Ember.none(country)) {
      map.panTo(new google.maps.LatLng(
        country.get('lat'), country.get('lon')));
      map.setZoom(country.get('zoom'));
    }
  },


  /**
    Helper function to parse backend countries to countryList
  */
  getContent: function (countries) {
    var countryList = [];

    countries.sort(function (a, b) {
      if (a.name < b.name) return -1;
      if (a.name > b.name) return 1;
      return 0;
    });

    for (var i = 0; i < countries.length; i++) {
      if (!Ember.none(countries[i].centroidLat) && !Ember.none(countries[i].centroidLon)) {
        var zoom = 7; // default zoom level
        if (!Ember.none(countries[i].zoomLevel)) {
          zoom = countries[i].zoomLevel;
        }

        countryList.push(
          Ember.Object.create({
            label: countries[i].name,
            iso: countries[i].isoAlpha2Code,
            lat: countries[i].centroidLat,
            lon: countries[i].centroidLon,
            zoom: zoom
          })
        );
      }
    }
    return countryList;
  }

});


FLOW.placemarkDetailController = Ember.ArrayController.create({
  content: Ember.A(),
  sortProperties: ['questionText'],
  sortAscending: true,

  populate: function (placemark) {
    if (placemark && placemark.id) {
      this.set('content', FLOW.store.find(FLOW.PlacemarkDetail, {
        placemarkId: placemark.id
      }));
    } else {
      this.set('content', Ember.A());
    }
  },

  handlePlacemarkSelection: function () {
    var selected;

    selected = FLOW.placemarkController.get('selected');
    this.populate(selected);
  }.observes('FLOW.placemarkController.selected'),

  photoUrl: function () {
    var photoDetails, photoUrls = [],
      rawPhotoUrl;

    if (!this.get('content').get('isLoaded')) {
      return null;
    }

    // filter out details with images
    photoDetails = this.get('content').filter(function (detail) {
      return detail.get('questionType') === 'PHOTO';
    });

    if (Ember.empty(photoDetails)) {
      return null;
    }

    photoDetails.forEach(function (photo) {
      rawPhotoUrl = photo.get('stringValue');
      // Since photos have a leading path from devices that we need to trim
      photoUrls.push(FLOW.Env.photo_url_root + rawPhotoUrl.split('/').pop());
    });

    return Ember.ArrayController.create({
      content: photoUrls
    });
  }.property('content.isLoaded')

});
