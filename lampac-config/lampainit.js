(function() {
  'use strict';

  // Copy to lampac server: plugins/override/lampainit.js

  window.lampa_settings = window.lampa_settings || {};
  window.lampa_settings.plugins_store = false;
  window.lampa_settings.services = false;
  window.lampa_settings.mirrors = false;
  window.lampa_settings.socket_use = false;
  window.lampa_settings.feed = false;
  window.lampa_settings.account_sync = false;

  window.lampa_settings.disable_features = window.lampa_settings.disable_features || {};
  window.lampa_settings.disable_features.install_proxy = true;
})();