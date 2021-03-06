package com.cosmo.kite.report.custom_kite_allure;

import com.cosmo.kite.exception.KiteTestException;
import org.apache.log4j.Logger;

import javax.json.JsonObject;
import javax.json.JsonValue;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.cosmo.kite.util.ReportUtils.getStackTrace;
import static com.cosmo.kite.util.ReportUtils.timestamp;
import static com.cosmo.kite.util.TestHelper.jsonToString;
import static com.cosmo.kite.util.TestUtils.*;

public class Reporter {

  protected final Logger logger = Logger.getLogger(this.getClass().getName());

  public static Reporter getInstance() {
    return instance;
  }

  private final String DEFAULT_REPORT_FOLDER = "kite-allure-reports/";
  private String reportPath = DEFAULT_REPORT_FOLDER;
  private static Reporter instance = new Reporter();
  
  private List<Container> containers = new ArrayList<>();
  private List<AllureTestReport> tests = new ArrayList<>();;
  private List<CustomAttachment> attachments = new ArrayList<>();;
  
  public void setReportPath(String reportPath) {
    if (reportPath != null) {      
      this.reportPath = verifyPathFormat(reportPath) + DEFAULT_REPORT_FOLDER;
    }
  }

  public void jsonAttachment(AllureStepReport step, String name, JsonValue jsonObject) {
    jsonAttachment(step, name, (JsonObject)jsonObject);
  }

    public void jsonAttachment(AllureStepReport step, String name, JsonObject jsonObject) {
    String value = jsonToString(jsonObject);
    CustomAttachment attachment = new CustomAttachment(name, "text/json", "json");
    attachment.setText(value);
    addAttachment(step, attachment);
  }
  
  public void textAttachment(AllureStepReport step, String name, String value, String type) {
    CustomAttachment attachment = new CustomAttachment(name, "text/" + type, type);
    attachment.setText(value);
    addAttachment(step, attachment);
  }
  
  public void saveAttachmentToSubFolder(String name, String value, String type, String subFolder) {
    createDirs(this.reportPath + subFolder);
    printJsonTofile(value, verifyPathFormat(this.reportPath + subFolder) + name + "." + type);
  }
  
  public void screenshotAttachment(AllureStepReport step, byte[] screenshot) {
    CustomAttachment attachment = new CustomAttachment("Page-screenshot(" + timestamp() + ")", "image/png", "png");
    attachment.setScreenshot(screenshot);
    addAttachment(step, attachment);
  }
  
  public void screenshotAttachment(AllureStepReport step, String name, byte[] screenshot) {
    CustomAttachment attachment = new CustomAttachment(name, "image/png", "png");
    attachment.setScreenshot(screenshot);
    addAttachment(step, attachment);
  }
  
  private void addAttachment(AllureStepReport step, CustomAttachment attachment) {
    this.attachments.add(attachment);
    step.addAttachment(attachment);
  }
  
  private void prepareReportFolder(){
    File dir = new File(this.reportPath);
    if (!dir.isDirectory()) {
      dir.mkdirs();
    }
  }
  
  public void addContainer(Container container) {
    this.containers.add(container);
  }
  
  public void addTest(AllureTestReport test) {
    this.tests.add(test);
  }
  
  public void updateContainers() {
    prepareReportFolder();
    for (Container container : containers) {
      String fileName = this.reportPath + container.getUuid() + "-container.json";
      printJsonTofile(container.toString(), fileName);
    }
  }
  
  public void generateReportFiles(){
    updateContainers();
    
    for (AllureTestReport test : tests) {
      String fileName = this.reportPath + test.getUuid() + "-result.json";
      printJsonTofile(test.toString(), fileName);
    }
    
    for (CustomAttachment attachment : attachments) {
      attachment.saveToFile(reportPath);
    }
  }
  
  public void processException(AllureStepReport report, Exception e) {
    StatusDetails details = new StatusDetails();
    Status status;
    String message;
    String trace = getStackTrace(e) + (e.getCause() != null ? "\r\nCaused by: " + getStackTrace(e.getCause()) : "");
    if (e instanceof KiteTestException) {
      details.setKnown(true);
      if (((KiteTestException) e).isContinueOnFailure()) {
        details.setMuted();
        report.setIgnore();
      }
      status = ((KiteTestException) e).getStatus();
      message = e.getLocalizedMessage();
      if (report.canBeIgnore()) {
        logger.warn(
            "(Optional) Step " + status.value() + ":\r\n   message = " + message);
      } else {
        logger.error(
            "Step " + status.value() + ":\r\n   message = " + message);
      }
      logger.debug(trace);
    } else {
      message = "***UNHANDLED EXCEPTION*** \r\n This is a bug and must be fixed. The exception " +
        "must be caught and thrown as KiteTestException";
      details.setFlaky(true);
      status = Status.BROKEN;
      logger.error("Step " + status.value()+ ":\r\n   message = " + message + "\r\n   trace = " + trace);
    }
    details.setMessage(message);
    details.setTrace(trace);
    report.setStatus(status);
    report.setDetails(details);
  }
  
  
}
