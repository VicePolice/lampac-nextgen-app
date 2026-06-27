package top.rootu.lampa.helpers

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import top.rootu.lampa.BuildConfig
import top.rootu.lampa.MainActivity
import top.rootu.lampa.browser.Browser
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL

object LampacHelper {

    const val CLIENT_VERSION = "2.1.16"
    const val CLIENT_VERSION_CODE = 16

    private const val INJECT_SCRIPT = """
(function() {
  'use strict';
  function patch() {
    window.lampa_settings = window.lampa_settings || {};
    window.lampa_settings.plugins_store = false;
    window.lampa_settings.services = false;
    window.lampa_settings.mirrors = false;
    window.lampa_settings.socket_use = false;
    window.lampa_settings.feed = false;
    window.lampa_settings.account_sync = false;
    window.lampa_settings.disable_features = window.lampa_settings.disable_features || {};
    window.lampa_settings.disable_features.install_proxy = true;
    if (typeof Lampa !== 'undefined') {
      if (Lampa.Extensions) {
        Lampa.Extensions.updatePluginDB = function() {};
        Lampa.Extensions.checkVersion = function() { return true; };
      }
      if (Lampa.DB && Lampa.DB.update) {
        Lampa.DB.update = function() {};
      }
    }
  }
  patch();
  var timer = setInterval(function() {
    patch();
    if (typeof Lampa !== 'undefined') {
      clearInterval(timer);
      patch();
    }
  }, 50);
})();
"""

    val isLampacBuild: Boolean
        get() = BuildConfig.FLAVOR == "lampac"

    fun normalizeLampacUrl(url: String): String {
        if (!isLampacBuild || url.isBlank()) return url
        val withoutLampaMain = url
            .replace(Regex("/lampa-main/?", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(?<!:)/{2,}"), "/")
            .trimEnd('/')
        return if (Regex(":\\d+$").containsMatchIn(withoutLampaMain)) {
            "$withoutLampaMain/"
        } else {
            withoutLampaMain
        }
    }

    fun shouldPatchPage(url: String?): Boolean {
        if (!isLampacBuild || url.isNullOrBlank()) return false
        val base = normalizeLampacUrl(MainActivity.LAMPA_URL).trimEnd('/')
        val page = normalizeLampacUrl(url).trimEnd('/')
        if (base.isEmpty()) return page.contains(":9118")
        return page.startsWith(base)
    }

    fun userAgentSuffix(): String {
        return if (isLampacBuild) " lampa_client personal.lampa" else " lampa_client"
    }

    fun injectSettings(browser: Browser?) {
        if (!isLampacBuild || browser == null) return
        browser.evaluateJavascript(INJECT_SCRIPT) {}
    }

    fun interceptRequest(request: WebResourceRequest?): WebResourceResponse? {
        if (!isLampacBuild || request == null) return null
        val pageUrl = request.url?.toString() ?: return null
        return interceptLampainit(pageUrl)
            ?: interceptExtensions(pageUrl)
            ?: if (request.isForMainFrame) interceptMainFrame(pageUrl) else null
    }

    private fun interceptLampainit(url: String): WebResourceResponse? {
        if (!url.contains("lampainit.js")) return null
        return patchTextResponse(url, "application/javascript") { body ->
            body
                .replace("plugins_store = true", "plugins_store = false")
                .replace("services = true", "services = false")
                .replace("mirrors = true", "mirrors = false")
                .replace("socket_use = true", "socket_use = false")
                .replace("feed = true", "feed = false")
                .replace("account_sync = true", "account_sync = false")
        }
    }

    private fun interceptExtensions(url: String): WebResourceResponse? {
        if (!url.contains("/extensions")) return null
        val emptyStore = """{"success":true,"secuses":true,"results":[]}"""
        return WebResourceResponse(
            "application/json",
            "utf-8",
            ByteArrayInputStream(emptyStore.toByteArray(Charsets.UTF_8))
        )
    }

    private fun interceptMainFrame(pageUrl: String): WebResourceResponse? {
        if (!shouldPatchPage(pageUrl)) return null
        return patchTextResponse(pageUrl, "text/html") { body ->
            val script = "<script>$INJECT_SCRIPT</script>"
            val headMatch = Regex("<head[^>]*>", RegexOption.IGNORE_CASE).find(body)
            val htmlMatch = Regex("<html[^>]*>", RegexOption.IGNORE_CASE).find(body)
            when {
                headMatch != null ->
                    body.replaceFirst(headMatch.value, headMatch.value + script)
                htmlMatch != null ->
                    body.replaceFirst(htmlMatch.value, htmlMatch.value + "<head>$script</head>")
                else -> script + body
            }
        }
    }

    private fun patchTextResponse(
        pageUrl: String,
        mime: String,
        patch: (String) -> String
    ): WebResourceResponse? {
        return try {
            val connection = (URL(pageUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 15000
            }
            connection.connect()
            val contentType = connection.contentType ?: return null
            if (!contentType.contains(mime.substringAfter("/"), ignoreCase = true)
                && !contentType.contains("text", ignoreCase = true)
                && !contentType.contains("javascript", ignoreCase = true)
                && !contentType.contains("json", ignoreCase = true)
            ) {
                connection.disconnect()
                return null
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            val patched = patch(body)
            WebResourceResponse(mime, "utf-8", ByteArrayInputStream(patched.toByteArray(Charsets.UTF_8)))
        } catch (_: Exception) {
            null
        }
    }
}
