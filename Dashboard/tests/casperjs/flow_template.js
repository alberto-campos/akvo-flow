//
// Test - Import Cleaned Survey Data
// neha@akvo.org
//
// var require = patchRequire(require);

//casperjs convention - call utils for phantom extension

var utils = require('utils');
var ember_xpath = require('casper').selectXPath;
// var loginModule = require('./lib/loginGAE.js');
var system = require('system');

// var consoleLog = require("./lib/jsConsole.js");
// var testrailModule = require("./lib/testrailPostResults.js");

casper.options.verbose = true;
casper.options.javascriptEnabled = true;
casper.options.logLevel = "debug";
casper.options.loadImages = true;
phantom.cookiesEnabled = true;
//  casper.options.waitTimeout = 1000000;

// casper.userAgent('Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_4) AppleWebKit/537.36  (KHTML, like Gecko) ');
// print out all the messages in the headless browser context
casper.on('remote.message', function(msg) {
this.echo('remote message caught: ' + msg);
});

casper.on("page.error", function(msg, trace) {
this.echo("Page Error: " + msg, "ERROR");
});


casper.test.begin('FLOW Test Template', function suite(test) {
	var url = 'http://akvoflowsandbox.appspot.com/admin';

	casper.start(url, function() {
		 console.log("Initial Akvo FLOW Login Page");
		 // loginModule.login("akvoqa@gmail.com");
		 this.test.assertExists('form#gaia_loginform', 'GAE Login Form is Found');
		 this.fill('form#gaia_loginform', {
		 		Email:  'akvoqa@gmail.com',
		 		Passwd: 'R4inDr0p!'
		 }, true);
	});
});


casper.run();
