/*
 * Copyright (C) CoSMo Software Consulting Pte. Ltd. - All Rights Reserved
 */
package com.cosmo.kite.stats;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import java.util.Map;

/**
 * RTCCertificateStats, with attributes fingerprint,
 * fingerprintAlgorithm, base64Certificate, issuerCertificateId
 */
public class RTCCertificateStats extends RTCStatObject {
  
  private String fingerprint, fingerprintAlgorithm, base64Certificate, issuerCertificateId;
  
  /**
   * Instantiates a new Rtc certificate stats.
   *
   * @param statObject the stat object
   */
  public RTCCertificateStats(Map<Object, Object> statObject) {
    this.setId(getStatByName(statObject, "id"));
    this.fingerprint = getStatByName(statObject, "fingerprint");
    this.fingerprintAlgorithm = getStatByName(statObject, "fingerprintAlgorithm");
    this.base64Certificate = getStatByName(statObject, "base64Certificate");
    this.issuerCertificateId = getStatByName(statObject, "issuerCertificateId");
    
  }
  
  @Override
  public JsonObjectBuilder getJsonObjectBuilder() {
    JsonObjectBuilder jsonObjectBuilder =
      Json.createObjectBuilder()
        .add("fingerprint", this.fingerprint)
        .add("fingerprintAlgorithm", this.fingerprintAlgorithm)
        .add("base64Certificate", this.base64Certificate)
        .add("issuerCertificateId", this.issuerCertificateId);
    return jsonObjectBuilder;
  }
}
