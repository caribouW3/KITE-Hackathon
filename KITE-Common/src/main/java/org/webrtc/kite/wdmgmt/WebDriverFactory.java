/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.webrtc.kite.wdmgmt;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.remote.MobileCapabilityType;
import org.apache.log4j.Logger;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariOptions;
import org.webrtc.kite.config.App;
import org.webrtc.kite.config.Browser;
import org.webrtc.kite.config.EndPoint;
import org.webrtc.kite.config.Mobile;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;

/**
 * Factory object for creating a web driver.
 */
public class WebDriverFactory {
  
  private static final Logger logger = Logger.getLogger(WebDriverFactory.class.getName());
  
  /**
   * Creates a web driver based on the given Browser object.
   *
   * @param endPoint EndPoint
   * @param testName the test name
   * @param id       an ID to identify the WebDriver
   *
   * @return WebDriver web driver
   * @throws MalformedURLException if no protocol is specified in the remoteAddress of the endPoint,                               or an unknown protocol is found, or spec is null.
   * @throws WebDriverException    the web driver exception
   */
  public static WebDriver createWebDriver(EndPoint endPoint, String testName, String id)
    throws MalformedURLException, WebDriverException {
    if (endPoint instanceof Browser) {
      return new RemoteWebDriver(new URL(endPoint.getRemoteAddress()),
        WebDriverFactory.createCapabilities(endPoint, testName, id));
    } else {
      if (endPoint.getPlatform().equalsIgnoreCase("android")) {
        return new AndroidDriver<>(new URL(endPoint.getRemoteAddress()),
          WebDriverFactory.createCapabilities(endPoint, testName, id));
      } else {
        return new IOSDriver<>(new URL(endPoint.getRemoteAddress()),
          WebDriverFactory.createCapabilities(endPoint, testName, id));
      }
    }
  }
  
  /**
   * Creates a web driver based on the given EndPoint object.
   *
   * @param endPoint EndPoint
   * @param testName the test name
   *
   * @return WebDriver web driver
   * @throws MalformedURLException if no protocol is specified in the remoteAddress of the endPoint,                               or an unknown protocol is found, or spec is null.
   */
  public static RemoteWebDriver createWebDriver(EndPoint endPoint, String testName)
    throws MalformedURLException {
    if (endPoint instanceof Browser) {
      return new RemoteWebDriver(new URL(endPoint.getRemoteAddress()),
        WebDriverFactory.createCapabilities(endPoint, testName, ""));
    } else {
      if (endPoint.getPlatform().equalsIgnoreCase("android")) {
        return new AndroidDriver<>(new URL(endPoint.getRemoteAddress()),
          WebDriverFactory.createCapabilities(endPoint, testName, ""));
      } else {
        return new IOSDriver<>(new URL(endPoint.getRemoteAddress()),
          WebDriverFactory.createCapabilities(endPoint, testName, ""));
      }
    }
  }
  
  /**
   * Creates a Capabilities object based on the given EndPoint object.
   *
   * @param endPoint kite config object
   * @param testName         name for individual test case
   * @return Capabilities
   */
  private static Capabilities createCapabilities(EndPoint endPoint, String testName, String id) {
    
    MutableCapabilities capabilities = new MutableCapabilities();
    
    for (String capabilityName : endPoint.getExtraCapabilities().keySet()) {
      logger.info("extraCapabilites : " + capabilityName + ": " + endPoint.getExtraCapabilities().get(capabilityName));
      capabilities.setCapability(capabilityName, endPoint.getExtraCapabilities().get(capabilityName));
    }
    
    if (endPoint instanceof Browser) {
      Browser browser = (Browser) endPoint;
      if (browser.getVersion() != null) {
        capabilities.setCapability(CapabilityType.VERSION, browser.getVersion());
      }
      if (browser.getPlatform() != null) {
        capabilities.setCapability(CapabilityType.PLATFORM_NAME, browser.getPlatform());
      }
      // Remote test identifier
      if (testName != null) {
        capabilities.setCapability("name", testName);
      }
      if (id != null) {
        capabilities.setCapability("id", id);
      }

      if (browser.getGateway() != null) {
        capabilities.setCapability("gateway", browser.getGateway());
      }
      
      // Only consider next code block if this is a browser.
      switch (browser.getBrowserName()) {
        case "chrome":
          ChromeOptions chromeOptions = new ChromeOptions();
          if (browser.useFakeMedia()) {
            chromeOptions.addArguments("use-fake-ui-for-media-stream");
            chromeOptions.addArguments("use-fake-device-for-media-stream");
          }
          chromeOptions.addArguments("auto-select-desktop-capture-source=Entire screen");
//          chromeOptions.addArguments("disable-background-timer-throttling");
//          chromeOptions.addArguments("disable-background-throttling");
//          chromeOptions.addArguments("disableBackgroundThrottling");
          if (!"electron".equals(browser.getVersion())) {
            // CHROME ONLY
            String extension = System.getProperty("kite.chrome.extension");
            if (extension != null) {
              chromeOptions.addExtensions(new File(extension));
            } else {
              logger.warn("CHROME: Some of the test (screen sharing, ...) need specific extension to be able to work properly.");
            }
          } else {
            // ELECTRON CLIENT ONLY:
            // Cannot use it for every chrome because there might be 2 instances using same port (conflict)
            // chromeOptions.addArguments("remote-debugging-port=5000");
          }
          if (browser.getFakeMediaFile() != null) {
            chromeOptions.addArguments("allow-file-access-from-files");
            chromeOptions.addArguments("use-file-for-fake-video-capture=" + browser.getFakeMediaFile() + "");
            if (browser.getFakeMediaAudio() != null && browser.getFakeMediaAudio().length() > 0) {
              chromeOptions.addArguments("use-file-for-fake-audio-capture=" + browser.getFakeMediaAudio());
            }
          }
          if (browser.isHeadless()) {
            chromeOptions.addArguments("headless");
          }
          if (browser.getWindowSize() != null) {
            chromeOptions.addArguments("window-size=" + browser.getWindowSize());
          }
          for (String flag : browser.getFlags()) {
            chromeOptions.addArguments(flag);
            // Examples:
            /*
             * chromeOptions.addArguments("--disable-gpu");
             * chromeOptions.addArguments("no-sandbox");
             * chromeOptions.addArguments("disable-infobars");
             * chromeOptions.addArguments("test-type=browser");
             * chromeOptions.addArguments("disable-extensions");
             * chromeOptions.addArguments("--js-flags=--expose-gc");
             * chromeOptions.addArguments("--disable-default-apps");
             * chromeOptions.addArguments("--disable-popup-blocking");
             * chromeOptions.addArguments("--enable-precise-memory-info");
             */
          }
          capabilities.setCapability(ChromeOptions.CAPABILITY, chromeOptions);
//          capabilities.setCapability(CapabilityType.PAGE_LOAD_STRATEGY, "none");
          break;
        case "firefox":
          FirefoxProfile firefoxProfile = null;
          String profile = System.getProperty("kite.firefox.profile");
          if (profile != null) {
            switch (browser.getPlatform().toUpperCase()) {
              case "WINDOWS":
                profile += "firefox-h264-profiles/h264-windows";
                break;
              case "MAC":
                profile += "firefox-h264-profiles/h264-mac";
                break;
              case "LINUX":
                profile += "firefox-h264-profiles/h264-linux";
                break;
            }
            firefoxProfile = new FirefoxProfile(new File(profile));
          } else {
            logger.warn("FIREFOX: Some tests require specific profile for firefox to work properly.");
            firefoxProfile = new FirefoxProfile();
          }
          firefoxProfile.setPreference("media.navigator.streams.fake", browser.useFakeMedia());
          FirefoxOptions firefoxOptions = new FirefoxOptions();
          firefoxOptions.setProfile(firefoxProfile);
          if (browser.isHeadless()) {
            firefoxOptions.addArguments("-headless");
          }
          if (browser.getWindowSize() != null) {
            firefoxOptions.addArguments("-window-size " + browser.getWindowSize());
          }
          for (String flag : browser.getFlags()) {
            firefoxOptions.addArguments(flag);
          }
          capabilities.merge(firefoxOptions);
          break;
        case "MicrosoftEdge":
          EdgeOptions MicrosoftEdgeOptions = new EdgeOptions();
          capabilities.setCapability("edgeOptions", MicrosoftEdgeOptions);
          capabilities.setCapability("avoidProxy", true);
          break;
        case "edge":
          EdgeOptions edgeOptions = new EdgeOptions();
          capabilities.setCapability("edgeOptions", edgeOptions);
          capabilities.setCapability("avoidProxy", true);
          break;
        case "safari":
          SafariOptions options = new SafariOptions();
          options.setUseTechnologyPreview(browser.isTechnologyPreview());
          capabilities.setCapability(SafariOptions.CAPABILITY, options);
          break;
      }
      // Add log preference to webdriver
      // TODO put log preference into config file
      LoggingPreferences logPrefs = new LoggingPreferences();
      logPrefs.enable(LogType.BROWSER, Level.ALL);
      capabilities.setCapability(CapabilityType.LOGGING_PREFS, logPrefs);
      // Capabilities for mobile browser/app
      Mobile mobile = ((Browser)endPoint).getMobile();
      if (mobile != null) {
        // deviceName:
        // On iOS, this should be one of the valid devices returned by instruments with instruments -s devices.
        // On Android this capability is currently ignored, though it remains required.
        capabilities.setCapability("deviceName", mobile.getDeviceName());
        capabilities.setCapability("platformName", mobile.getPlatformName());
        capabilities.setCapability("platformVersion", mobile.getPlatformVersion());
        if (mobile.getPlatformName().equalsIgnoreCase("iOS")) {
          capabilities.setCapability("automationName", "XCUITest");
        } else {
          capabilities.setCapability("autoGrantPermissions", true);
          capabilities.setCapability(MobileCapabilityType.NEW_COMMAND_TIMEOUT, 300);
        }
        capabilities.setCapability("noReset", true);
      }
    } else {
      App app = (App) endPoint;
      // The absolute local path or remote http URL to an .ipa or .apk file, or a .zip containing one of these.
      // Appium will attempt to install this app binary on the appropriate device first.
      capabilities.setCapability("app", app.getAppPath());
      capabilities.setCapability("deviceName", app.getDeviceName());
      capabilities.setCapability("platformName", app.getPlatform());
      if (app.getPlatform().equalsIgnoreCase("iOS")) {
        capabilities.setCapability("automationName", "XCUITest");
      } else {
        capabilities.setCapability("autoGrantPermissions", true);
        capabilities.setCapability("fullReset", app.getReset());
      }
      if (app.getAppPackage() == null || app.getAppActivity() == null) {
        logger.warn("Using [" + app.getAppPath() + "]: Some mobile applications may require appPackage and appActivity " +
          "to setStartTimestamp properly ..");
        if (app.getAppPackage() != null) {
          capabilities.setCapability("appPackage", app.getAppPackage());
        }
        if (app.getAppActivity() == null) {
          capabilities.setCapability("appActivity", app.getAppActivity());
        }
      } else {
        capabilities.setCapability("appPackage", app.getAppPackage());
        capabilities.setCapability("appActivity", app.getAppActivity());
      }
      
    }
    return capabilities;
    
  }
  
}
