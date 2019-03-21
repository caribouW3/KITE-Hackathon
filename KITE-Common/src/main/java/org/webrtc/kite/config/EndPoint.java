package org.webrtc.kite.config;

import com.cosmo.kite.usrmgmt.AccountType;
import com.cosmo.kite.usrmgmt.TypeRole;
import org.apache.log4j.Logger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import java.util.HashMap;
import java.util.Map;

/**
 * The type End point.
 */
public abstract class EndPoint extends KiteConfigObject {

  protected final Logger logger = Logger.getLogger(this.getClass().getName());
  /**
   * The Remote address.
   */
  protected String remoteAddress;
  /**
   * The Platform name
   */
  protected String platformName;
  /**
   * The Extra capabilities.
   */
  protected Map<String, String> extraCapabilities = new HashMap<>();
  /**
   * The Focus.
   */
  protected boolean focus;
  /**
   * The Is browser.
   */
  protected boolean isBrowser = false;
  /**
   * The Type role.
   */
  protected TypeRole typeRole;
  /**
   * Max instances
   */
  protected int maxInstances;

  protected String gateway;
  /**
   * Instantiates a new End point.
   */
  public EndPoint(){
  }
  
  /**
   * Instantiates a new End point.
   *
   * @param endPoint the end point
   */
  protected EndPoint(EndPoint endPoint) {
    this.focus = endPoint.isFocus();
    this.remoteAddress = endPoint.getRemoteAddress();
    this.platformName = endPoint.getPlatform();
    this.gateway = endPoint.getGateway();
    TypeRole typeRole = new TypeRole();
    typeRole.setRole(endPoint.getTypeRole().getRole());
    typeRole.setType(endPoint.getTypeRole().getType());
    this.typeRole = typeRole;
    for (String capabilityName : endPoint.getExtraCapabilities().keySet()) {
      this.addCapabilities(capabilityName, endPoint.getExtraCapabilities().get(capabilityName));
    }
  }
  
  /**
   * Constructs a new KiteConfigObject with the given remote address and JsonObject.
   *
   * @param remoteAddress a string representation of the Selenium hub url
   * @param jsonObject    JsonObject
   */
  protected EndPoint(String remoteAddress, JsonObject jsonObject) {
    this.remoteAddress = remoteAddress;
    this.focus = jsonObject.getBoolean("focus", true);
    this.setTypeRole(jsonObject);
    JsonValue jsonValue = jsonObject.getOrDefault("extraCapabilities", null);
    if (jsonValue != null) {
      JsonObject extraCapabilitiesArray = (JsonObject) jsonValue;
      for (String capabilityName : extraCapabilitiesArray.keySet()) {
        this.addCapabilities(capabilityName, extraCapabilitiesArray.getString(capabilityName));
      }
    }
  }
  
  
  /**
   * Gets remote address.
   *
   * @return the remote address
   */
  public String getRemoteAddress() {
    return remoteAddress;
  }
  
  /**
   * Sets remote address.
   *
   * @param remoteAddress the remote address
   */
  public void setRemoteAddress(String remoteAddress) {
    this.remoteAddress = remoteAddress;
  }
  
  /**
   * add new capability name/value pair to browser
   *
   * @param capabilityName  capability name
   * @param capabilityValue capability value
   */
  public void addCapabilities(String capabilityName, String capabilityValue) {
    this.extraCapabilities.put(capabilityName, capabilityValue);
  }
  
  /**
   * Gets extra capabilities.
   *
   * @return extra capability map to be used to create webdriver
   */
  public Map<String, String> getExtraCapabilities() {
    return extraCapabilities;
  }
  
  /**
   * Return if the object is to be focused in the list.
   *
   * @return true or false
   */
  public boolean isFocus() {
    return focus;
  }
  
  /**
   * Sets focus to object
   *
   * @param focus true or false
   */
  public void setFocus(boolean focus) {
    this.focus = focus;
  }
  
  /**
   * Returns if object is a Browser object
   *
   * @return true or false
   */
  public boolean isBrowser() {
    return this instanceof Browser;
  }
  
  /**
   * Gets type role.
   *
   * @return the type role
   */
  public TypeRole getTypeRole() {
    return typeRole;
  }
  
  /**
   * Sets type role.
   *
   * @param typeRole the type role
   */
  public void setTypeRole(TypeRole typeRole) {
    this.typeRole = typeRole;
  }
  
  /**
   * Sets type role.
   *
   * @param jsonObject the json object
   */
  protected void setTypeRole(JsonObject jsonObject) {
    TypeRole tr = new TypeRole();
    tr.setType(AccountType.valueOf(jsonObject.getString("accountType", AccountType.RC_ACCOUNT_A.name())));
    this.typeRole = tr;
  }

  /**
   * Returns a JsonObject representation.
   *
   * @return JsonObject json object
   */
  public JsonObject getJsonObject() {
    return this.getJsonObjectBuilder().build();
  }
  
  @Override
  public String toString() {
    return this.getJsonObject().toString();
  }
  
  @Override
  public JsonObjectBuilder getJsonObjectBuilder() {
    JsonObjectBuilder builder =  Json.createObjectBuilder()
      .add("remoteAddress", remoteAddress)
      .add("platformName", platformName)
      .add("focus", focus)
      .add("maxInstances", maxInstances)
      ;
    
    if (!extraCapabilities.isEmpty()) {
      JsonObjectBuilder extraCapabilitiesBuilder = Json.createObjectBuilder();
      for (String cap : extraCapabilities.keySet()) {
        extraCapabilitiesBuilder.add(cap, extraCapabilities.get(cap));
      }
      builder.add("extraCapabilities", extraCapabilitiesBuilder);
    }
    return builder;
  }
  
  /**
   * Gets platformName.
   *
   * @return the platformName
   */
  public String getPlatform() {
    return platformName;
  }

  /**
   * Gets gateway.
   *
   * @return the gateway
   */
  public String getGateway() {
    return gateway;
  }

  /**
   * Sets platformName.
   *
   * @param platform the platformName
   */
  public void setPlatform(String platform) {
    this.platformName = platform;
  }
  
  /**
   * Gets max instances.
   *
   * @return the max instances
   */
  public int getMaxInstances() {
    return maxInstances;
  }
  
  /**
   * Sets max instances.
   *
   * @param maxInstances the max instances
   */
  public void setMaxInstances(int maxInstances) {
    this.maxInstances = maxInstances;
  }
}
