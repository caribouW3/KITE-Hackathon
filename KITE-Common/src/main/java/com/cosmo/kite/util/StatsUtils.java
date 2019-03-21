/*
 * Copyright (C) CoSMo Software Consulting Pte. Ltd. - All Rights Reserved
 */
package com.cosmo.kite.util;

import com.cosmo.kite.exception.KiteTestException;
import com.cosmo.kite.stats.*;
import com.cosmo.kite.report.custom_kite_allure.Status;
import org.apache.log4j.Logger;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import javax.json.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.cosmo.kite.entities.Timeouts.ONE_SECOND_INTERVAL;
import static com.cosmo.kite.util.ReportUtils.getStackTrace;
import static com.cosmo.kite.util.TestHelper.jsonToString;
import static com.cosmo.kite.util.TestUtils.waitAround;
import static com.cosmo.kite.util.WebDriverUtils.executeJsScript;

/**
 * The type Stats utils.
 */
public class StatsUtils {

  private static final Logger logger = Logger.getLogger(StatsUtils.class.getName());

  /**
   * Create a JsonObjectBuilder Object to eventually build a Json object
   * from data obtained via tests.
   *
   * @param clientStats array of data sent back from test
   *
   * @return JsonObjectBuilder.
   */
  private static JsonObject buildClientStatObject(Map<String, Object> clientStats) {
    return buildClientStatObject(clientStats, null);
  }


  /**
   * Create a JsonObjectBuilder Object to eventually build a Json object
   * from data obtained via tests.
   *
   * @param clientStats   array of data sent back from test
   * @param selectedStats list of selected stats
   * @return JsonObjectBuilder.
   */
  private static JsonObject buildClientStatObject(Map<String, Object> clientStats, JsonArray selectedStats) {
    try {
      JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
      Map<String, Object> clientStatMap = clientStats;

      List<Object> clientStatArray = (ArrayList) clientStatMap.get("stats");
      JsonArrayBuilder jsonclientStatArray = Json.createArrayBuilder();
      for (Object stats : clientStatArray) {
        JsonObjectBuilder jsonStatObjectBuilder = buildSingleStatObject(stats, selectedStats);
        jsonclientStatArray.add(jsonStatObjectBuilder);
      }
      if (selectedStats == null) {
        //only add SDP offer stuff if selectedStats is null
        JsonObjectBuilder sdpObjectBuilder = Json.createObjectBuilder();
        Map<Object, Object> sdpOffer = (Map<Object, Object>) clientStatMap.get("offer");
        Map<Object, Object> sdpAnswer = (Map<Object, Object>) clientStatMap.get("answer");
        sdpObjectBuilder.add("offer", new SDP(sdpOffer).getJsonObjectBuilder())
          .add("answer", new SDP(sdpAnswer).getJsonObjectBuilder());
          jsonObjectBuilder.add("sdp", sdpObjectBuilder);
      }
      jsonObjectBuilder.add("statsArray", jsonclientStatArray);
      return jsonObjectBuilder.build();
    } catch (ClassCastException e) {
      e.printStackTrace();
      return Json.createObjectBuilder().build();
    }
  }

  /**
   * Create a JsonObjectBuilder Object to eventually build a Json object
   * from data obtained via tests.
   *
   * @param statArray array of data sent back from test
   *
   * @return JsonObjectBuilder.
   */
  private static JsonObjectBuilder buildSingleStatObject(Object statArray) {
    return buildSingleStatObject(statArray, null);
  }

  /**
   * Create a JsonObjectBuilder Object to eventually build a Json object
   * from data obtained via tests.
   *
   * @param statArray     array of data sent back from test
   * @param selectedStats list of selected stats
   *
   * @return JsonObjectBuilder.
   */
  private static JsonObjectBuilder buildSingleStatObject(Object statArray, JsonArray selectedStats) {
    JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
    Map<String, List<RTCStatObject>> statObjectMap = new HashMap<>();
    if (statArray != null) {
      for (Object map : (ArrayList) statArray) {
        if (map != null) {
          Map<Object, Object> statMap = (Map<Object, Object>) map;
          String type = (String) statMap.get("type");
          if (selectedStats == null || selectedStats.size() == 0 || selectedStats.toString().contains(type)) {
            RTCStatObject statObject = null;
            switch (type) {
              case "codec": {
                statObject = new RTCCodecStats(statMap);
                break;
              }
              case "track": {
                statObject = new RTCMediaStreamTrackStats(statMap);
                break;
              }
              case "stream": {
                statObject = new RTCMediaStreamStats(statMap);
                break;
              }
              case "inbound-rtp": {
                statObject = new RTCRTPStreamStats(statMap, true);
                break;
              }
              case "outbound-rtp": {
                statObject = new RTCRTPStreamStats(statMap, false);
                break;
              }
              case "peer-connection": {
                statObject = new RTCPeerConnectionStats(statMap);
                break;
              }
              case "transport": {
                statObject = new RTCTransportStats(statMap);
                break;
              }
              case "candidate-pair": {
                statObject = new RTCIceCandidatePairStats(statMap);
                break;
              }
              case "remote-candidate": {
                statObject = new RTCIceCandidateStats(statMap);
                break;
              }
              case "local-candidate": {
                statObject = new RTCIceCandidateStats(statMap);
                break;
              }
            }
            if (statObject != null) {
              if (statObjectMap.get(type) == null) {
                statObjectMap.put(type, new ArrayList<RTCStatObject>());
              }
              statObjectMap.get(type).add(statObject);
            }
          }
        }
      }
    }
    if (!statObjectMap.isEmpty()) {
      for (String type : statObjectMap.keySet()) {
//        JsonArrayBuilder tmp = Json.createArrayBuilder();
        JsonObjectBuilder tmp = Json.createObjectBuilder();
        for (RTCStatObject stat : statObjectMap.get(type)) {
          tmp.add(stat.getId(), stat.getJsonObjectBuilder());
//          tmp.add(/*stat.getId(),*/ stat.getJsonObjectBuilder());
        }
        jsonObjectBuilder.add(type, tmp);
      }
    }
    return jsonObjectBuilder;
  }

  /**
   * Computes the average audioJitter
   *
   * @param statsArray object containing the list getStats result.
   * @param noStats    how many stats in jsonObject
   *
   * @return the average "audioJitter"
   * @throws KiteTestException the kite test exception
   */
  public static double computeAudioJitter(JsonArray statsArray, int noStats) throws KiteTestException {
    double jitter = 0;
    double ct = 0;

    if (noStats < 2) {
      throw new KiteTestException("Not enough stats to compute bitrate", Status.BROKEN);
    }
    try {
      for (int index = 0; index < noStats; index++) {
        JsonObject singleStatObject = statsArray.getJsonObject(index);
        if (singleStatObject.keySet().contains("inbound-rtp")) {
          singleStatObject = getRTCStats(singleStatObject, "inbound-rtp", "audio");
          if (singleStatObject != null) {
            String s = singleStatObject.getString("jitter");
            if (s != null && !"NA".equals(s) && isDouble(s)) {
              jitter += (1000 * Double.parseDouble(s));
              ct++;
            }
          }
        }
      }
      if (ct > 0) {
        return jitter / (1000 * ct);
      }
    } catch (Exception e) {
      throw new KiteTestException(e.getClass().getName() + " while computing audio jitter" + e.getLocalizedMessage(), Status.BROKEN);
    }
    return -1;
  }

  /**
   * Computes the average bitrate.
   *
   * @param statsArray object containing the list getStats result.
   * @param noStats    how many stats in jsonObject
   * @param direction  "in" or "out" or "Sent" or "Received"
   * @param mediaType  "audio", "video" or "candidate-pair"
   *
   * @return totalNumberBytes sent or received
   * @throws KiteTestException the kite test exception
   */
  public static double computeBitrate(JsonArray statsArray, int noStats, String direction, String mediaType) throws KiteTestException {
    long bytesStart = 0;
    long bytesEnd = 0;
    long tsStart = 0;
    long tsEnd = 0;
    long avgBitrate = 0;
    if (noStats < 2) {
      throw new KiteTestException("Not enough stats to compute bitrate", Status.FAILED);
    }
    try {
      String jsonObjName = getJsonObjectName(direction, mediaType);
      String jsonKey = getJsonKey(direction);
      for (int index = 0; index < noStats; index++) {
        JsonObject singleStatObject = statsArray.getJsonObject(index);
        if (singleStatObject != null) {
          if (mediaType.equalsIgnoreCase("candidate-pair")) {
            singleStatObject = getSuccessfulCandidate(singleStatObject);
          } else {
            singleStatObject = getRTCStats(singleStatObject, jsonObjName, mediaType);
          }
          if (singleStatObject != null) {
            String s = singleStatObject.getString(jsonKey);
            if (s != null && !"NA".equals(s) && isLong(s)) {
              long b = Long.parseLong(s);
              bytesStart = (bytesStart == 0 || b < bytesStart) ? b : bytesStart;
              bytesEnd = (bytesEnd == 0 || b > bytesEnd) ? b : bytesEnd;
            }
            String ts = singleStatObject.getString("timestamp");
            if (ts != null && !"NA".equals(ts) && isLong(ts)) {
              long b = Long.parseLong(ts);
              if (index == 0) {
                tsStart = b;
              }
              if (index == noStats - 1) {
                tsEnd = b;
              }
            }
          }
        }
      }
      if (tsEnd != tsStart) {
        long timediff = (tsEnd - tsStart);
        avgBitrate = (8000 * (bytesEnd - bytesStart)) / timediff;
        avgBitrate = (avgBitrate < 0) ? avgBitrate * -1 : avgBitrate;
      }
    } catch (NullPointerException npe) {
      throw new KiteTestException("NullPointerException while computing bitrate " + npe.getMessage(), Status.BROKEN);
    }
    return avgBitrate;
  }

  /**
   * Computes the packet losses as a % packetLost/total packets
   *
   * @param statsArray object containing the list getStats result.
   * @param noStats    how many stats in jsonObject
   * @param mediaType  "audio" or "video"
   *
   * @return the packet losses (% packetLost/total packets)
   * @throws KiteTestException the kite test exception
   */
  public static double computePacketsLoss(JsonArray statsArray, int noStats, String mediaType) throws KiteTestException {
    if (noStats < 1) {
      throw new KiteTestException("Not enough stats to compute packetLoss", Status.BROKEN);
    }
    try {
      JsonObject myObject = statsArray.getJsonObject(noStats - 1);
      myObject = getRTCStats(myObject, "inbound-rtp", mediaType);
      if (myObject != null) {
        String s = myObject.getString("packetsReceived");
        String l = myObject.getString("packetsLost");
        if (s != null && !"NA".equals(s) && isLong(s)
          && l != null && !"NA".equals(l) && isLong(l)) {
          long packetsLost = Long.parseLong(l);
          long totalPackets = Long.parseLong(s) + packetsLost;
          if (totalPackets > 0) {
            double packetLoss = packetsLost / totalPackets;
            return packetLoss;
          }
        }
      }
    } catch (Exception e) {
      throw new KiteTestException(e.getClass().getName() + " while computing packet loss" + e.getLocalizedMessage(), Status.BROKEN);
    }
    return -1;
  }

  /**
   * Computes the average roundTripTime
   *
   * @param statsArray object containing the list getStats result.
   * @param noStats    how many stats in jsonObject
   * @param prefix     the prefix
   *
   * @return the average of valid (> 0) "totalRoundTripTime"
   * @throws KiteTestException the kite test exception
   */
  public static double computeRoundTripTime(JsonArray statsArray, int noStats, String prefix) throws KiteTestException {
    double rtt = 0;
    int ct = 0;
    try {
      for (int index = 0; index < noStats; index++) {
        JsonObject succeededCandidatePair = getSuccessfulCandidate(statsArray.getJsonObject(index));
        if (succeededCandidatePair != null) {
          String s = succeededCandidatePair.getString(prefix + "RoundTripTime");
          if (s != null && !"NA".equals(s) && !"0".equals(s) && isDouble(s)) {
            rtt += 1000 * Double.parseDouble(s);
            ct++;
          }
        }
      }
    } catch (NullPointerException npe) {
      throw new KiteTestException("Unable to find RoundTripTime in the stats " + npe.getLocalizedMessage(), Status.BROKEN);
    }
    if (ct > 0) {
      return rtt / ct;
    }
    return -1;
  }

  /**
   * format 1.536834943435905E12 (nano seconds) to 1536834943435 (ms)
   * and convert timestamp to milliseconds
   *
   * @param s raw String obtained from getStats.
   *
   * @return the formatted timestamp
   */
  public static String formatTimestamp(String s) {
    String str = s;
    if (str.contains("E")) {
      //format 1.536834943435905E12 to 1536834943435905
      str = "1" + str.substring(str.indexOf(".") + 1, str.indexOf("E"));
    }
    if (str.length() > 13) {
      // convert timestamps to millisecond (obtained in nano seconds)
      str = str.substring(0, 13);
    }
    return str;
  }

  /**
   * @param direction "in" or "out" or "Sent" or "Received"
   * @param mediaType "audio", "video" or "candidate-pair"
   *
   * @return "candidate-pair_" or "inbound-rtp" or "outbound-rtp"
   *
   //todo delete after test ok (other function should be correct)
  private static String getJsonObjectName(String direction, String mediaType) {
    if (!"candidate-pair".equals(mediaType)) {
      return direction + "bound-rtp";
    }
    return mediaType;
  }*/

  /**
   *
   * @param direction "in" or "out" or "Sent" or "Received"
   * @param mediaType "audio", "video" or "candidate-pair"
   * @return "candidate-pair_" or "inbound-audio_" or "inbound-video_" or "outbound-audio_" or "outbound-video_"
   */
  private static String getJsonObjectName(String direction, String mediaType) {
    if ("candidate-pair".equals(mediaType)) {
      return "candidate-pair_";
    }
    //else  "inbound-audio_"
    return direction + "bound-" + mediaType + "_";
  }


  /**
   *
   * @param direction "in" or "out" or "Sent" or "Received"
   * @return bytesSent or bytesReceived
   */
  private static String getJsonKey(String direction) {
    if ("Sent".equals(direction) || "out".equals(direction)) {
      return "bytesSent";
    }
    if ("Received".equals(direction) || "in".equals(direction)) {
      return "bytesReceived";
    }
    return null;
  }



  /**
   * Stashes stats into a global variable and collects them 1s after
   *
   * @param webDriver      used to execute command.
   * @param peerConnection the peer connection
   *
   * @return String. pc stat once
   * @throws InterruptedException
   */
  public static Object getPCStatOnce(WebDriver webDriver, String peerConnection) {
    String stashStatsScript = "const getStatsValues = () =>" +
      peerConnection + "  .getStats()" +
      "    .then(data => {" +
      "      return [...data.values()];" +
      "    });" +
      "const stashStats = async () => {" +
      "  window.KITEStats = await getStatsValues();" +
      "  return 0;" +
      "};" +
      "stashStats();";
    String getStashedStatsScript = "return window.KITEStats;";

    executeJsScript(webDriver, stashStatsScript);
    waitAround(ONE_SECOND_INTERVAL);
    return executeJsScript(webDriver, getStashedStatsScript);
  }

  /**
   * stat JsonObjectBuilder to add to callback result.
   *
   * @param webDriver              used to execute command.
   * @param peerConnection         the peer connection
   * @param durationInMilliSeconds during which the stats will be collected.
   * @param intervalInMilliSeconds between each time getStats gets called.
   *
   * @return JsonObjectBuilder of the stat object
   * @throws InterruptedException the interrupted exception
   * @throws KiteTestException    the kite test exception
   */
  public static JsonObject getPCStatOvertime(WebDriver webDriver, String peerConnection, int durationInMilliSeconds, int intervalInMilliSeconds)
    throws InterruptedException, KiteTestException {
    return getPCStatOvertime(webDriver, peerConnection, durationInMilliSeconds, intervalInMilliSeconds, null);
  }

  /**
   * stat JsonObjectBuilder to add to callback result.
   *
   * @param webDriver              used to execute command.
   * @param peerConnection         the peer connection
   * @param durationInMilliSeconds during which the stats will be collected.
   * @param intervalInMilliSeconds between each time getStats gets called.
   * @param selectedStats          list of selected stats.
   *
   * @return JsonObjectBuilder of the stat object
   * @throws KiteTestException the kite test exception
   */
  public static JsonObject getPCStatOvertime(WebDriver webDriver, String peerConnection, int durationInMilliSeconds, int intervalInMilliSeconds, JsonArray selectedStats)
    throws KiteTestException {
    Map<String, Object> statMap = new HashMap<String, Object>();
    for (int timer = 0; timer < durationInMilliSeconds; timer += intervalInMilliSeconds) {
      // No sleep needed since already sleep in getPCStatOnce
      Object stats = getPCStatOnce(webDriver, peerConnection);
      if (timer == 0) {
        statMap.put("stats", new ArrayList<>());

        Object offer = getSDPMessage(webDriver, peerConnection, "offer");
        Object answer = getSDPMessage(webDriver, peerConnection, "answer");
        statMap.put("offer", offer);
        statMap.put("answer", answer);
      }
      List<Object> tmp = (List) statMap.get("stats");
      tmp.add(stats);
    }
    return buildClientStatObject(statMap, selectedStats);
  }

  /**
   * Gets the successful
   *
   * @param jsonObject of the stats
   *
   * @return
   */
  private static JsonObject getRTCStats(JsonObject jsonObject, String stats, String mediaType) {
    JsonObject myObj = jsonObject.getJsonObject(stats);
    if (myObj != null) {
      for (String key : myObj.keySet()) {
        JsonObject o = myObj.getJsonObject(key);
        if (mediaType.equals(o.getString("mediaType"))) {
          return o;
        }
      }
    }
    return null;
  }

  /**
   * Execute and return the requested SDP message
   *
   * @param webDriver      used to execute command.
   * @param peerConnection the peer connection
   * @param type           offer or answer.
   *
   * @return SDP object.
   * @throws KiteTestException the kite test exception
   */
  public static Object getSDPMessage(WebDriver webDriver, String peerConnection, String type) throws KiteTestException {
    return ((JavascriptExecutor) webDriver).executeScript(getSDPMessageScript(peerConnection, type));
  }

  /**
   * Returns the test's getSDPMessageScript to retrieve the sdp message for either the offer or answer.
   * If it doesn't exist then the method returns 'unknown'.
   *
   * @return the getSDPMessageScript as string.
   */
  private static String getSDPMessageScript(String peerConnection, String type) throws KiteTestException {
    switch (type) {
      case "offer":
        return "var SDP;"
          + "try {SDP = " + peerConnection + ".remoteDescription;} catch (exception) {} "
          + "if (SDP) {return SDP;} else {return 'unknown';}";
      case "answer":
        return "var SDP;"
          + "try {SDP = " + peerConnection + ".localDescription;} catch (exception) {} "
          + "if (SDP) {return SDP;} else {return 'unknown';}";
      default:
        throw new KiteTestException("Not a valid type for sdp message.", Status.BROKEN);
    }
  }

  /**
   * Gets the successful candidate pair (state = succeed)
   *
   * @param jsonObject of the successful candidate pair
   *
   * @return
   */
  private static JsonObject getSuccessfulCandidate(JsonObject jsonObject) {
    JsonObject candObj = jsonObject.getJsonObject("candidate-pair");
    if (candObj == null) {
      return null;
    }
    for (String key : candObj.keySet()) {
      JsonObject o = candObj.getJsonObject(key);
      if ("succeeded".equals(o.getString("state"))) {
        return o;
      }
    }
    for (String key : candObj.keySet()) {
      //sometimes there are no "succeeded" pair, but the "in-progress" with
      //a valid currentRoundTripTime value looks just fine.
      JsonObject o = candObj.getJsonObject(key);
      if ("in-progress".equals(o.getString("state")) &&
        !"NA".equals(o.getString("currentRoundTripTime"))) {
        return o;
      }
    }
    return null;
  }








  private static final String[] candidatePairStats = {"bytesSent", "bytesReceived", "currentRoundTripTime", "totalRoundTripTime", "timestamp"};
  private static final String[] inboundStats = {"bytesReceived", "packetsReceived", "packetsLost", "jitter", "timestamp"};
  private static final String[] outboundStats = {"bytesSent", "timestamp"};

  /**
   * Build a simple JsonObject of selected stats meant to test NW Instrumentation. * Stats
   * includes bitrate, packetLoss, Jitter and RTT
   *
   * @param senderStats the sender's PC stats
   * @param receiverStats the list of receiver PCs stats
   * @return
   */
  public static JsonObject extractStats(JsonObject senderStats, List<JsonObject> receiverStats) {
    JsonObjectBuilder mainBuilder = Json.createObjectBuilder();
    mainBuilder.add("localPC", extractStats(senderStats, "out"));
    int i = 0;
    for (JsonObject recvStats : receiverStats) {
      mainBuilder.add("remotePC[" + i++ + "]", extractStats(recvStats, "in"));
    }
    return mainBuilder.build();
  }

  /**
   * Build a simple JsonObject of selected stats meant to test NW Instrumentation.
   * Stats includes bitrate, packetLoss, Jitter and RTT
   *
   * @param obj
   * @return
   */
  public static JsonObjectBuilder extractStats(JsonObject obj, String direction) {
    JsonObjectBuilder mainBuilder = Json.createObjectBuilder();
    JsonArray jsonArray = obj.getJsonArray("statsArray");
    int noStats = 0;
    if (jsonArray != null) {
      noStats = jsonArray.size();
      for (int i = 0; i < noStats; i++) {
        mainBuilder.add("candidate-pair_" + i, getStatsJsonBuilder(jsonArray.getJsonObject(i), candidatePairStats, "candidate-pair", ""));
        if ("both".equalsIgnoreCase(direction) || "in".equalsIgnoreCase(direction)) {
          mainBuilder.add(
              "inbound-audio_" + i,
              getStatsJsonBuilder(
                  jsonArray.getJsonObject(i), inboundStats, "inbound-rtp", "audio"));
          mainBuilder.add(
              "inbound-video_" + i,
              getStatsJsonBuilder(
                  jsonArray.getJsonObject(i), inboundStats, "inbound-rtp", "video"));
        }
        if ("both".equalsIgnoreCase(direction) || "out".equalsIgnoreCase(direction)) {
          mainBuilder.add(
              "outbound-audio_" + i,
              getStatsJsonBuilder(
                  jsonArray.getJsonObject(i), outboundStats, "outbound-rtp", "audio"));
          mainBuilder.add(
              "outbound-video_" + i,
              getStatsJsonBuilder(
                  jsonArray.getJsonObject(i), outboundStats, "outbound-rtp", "video"));
        }
      }
    } else {
      logger.error(
        "statsArray is null \r\n ---------------\r\n"
          + obj.toString()
          + "\r\n ---------------\r\n");
    }
    JsonObject result = mainBuilder.build();
    JsonObjectBuilder csvBuilder = Json.createObjectBuilder();
    csvBuilder.add("currentRoundTripTime (ms)", computeRoundTripTime(result, noStats, "current"));
    csvBuilder.add("totalRoundTripTime (ms)", computeRoundTripTime(result, noStats, "total"));
    csvBuilder.add("totalBytesReceived (Bytes)", totalBytes(result, noStats, "Received"));
    csvBuilder.add("totalBytesSent (Bytes)", totalBytes(result, noStats, "Sent"));
    csvBuilder.add("avgSentBitrate (bps)", computeBitrate(result, noStats, "Sent", "candidate-pair"));
    csvBuilder.add("avgReceivedBitrate (bps)", computeBitrate(result, noStats, "Received", "candidate-pair"));
    if ("both".equalsIgnoreCase(direction) || "in".equalsIgnoreCase(direction)) {
      csvBuilder.add("inboundAudioBitrate (bps)", computeBitrate(result, noStats, "in", "audio"));
      csvBuilder.add("inboundVideoBitrate (bps)", computeBitrate(result, noStats, "in", "video"));
    }
    if ("both".equalsIgnoreCase(direction) || "out".equalsIgnoreCase(direction)) {
      csvBuilder.add("outboundAudioBitrate (bps)", computeBitrate(result, noStats, "out", "audio"));
      csvBuilder.add("outboundVideoBitrate (bps)", computeBitrate(result, noStats, "out", "video"));
    }
    if ("both".equalsIgnoreCase(direction) || "in".equalsIgnoreCase(direction)) {
      csvBuilder.add("audioJitter (ms)", computeAudioJitter(result, noStats));
      csvBuilder.add("audioPacketsLoss (%)", computePacketsLoss(result, noStats, "audio"));
      csvBuilder.add("videoPacketsLoss (%)", computePacketsLoss(result, noStats, "video"));
    }
    //uncomment the following line to add the detailed stats to the CSV
//    csvBuilder.add("stats", result);
    return csvBuilder;
  }




  /**
   * Computes the average bitrate.
   *
   * @param jsonObject object containing the list getStats result.
   * @param noStats how many stats in jsonObject
   * @param direction "in" or "out" or "Sent" or "Received"
   * @param mediaType "audio", "video" or "candidate-pair"
   * @return totalNumberBytes sent or received
   */
  private static String computeBitrate(JsonObject jsonObject, int noStats, String direction, String mediaType) {
    long bytesStart = 0;
    long bytesEnd = 0;
    long tsStart = 0;
    long tsEnd = 0;
    long avgBitrate = 0;
    try {
      if (noStats < 2) {
        return "Error: less than 2 stats";
      }
      String jsonObjName = getJsonObjectName(direction, mediaType);
      String jsonKey = getJsonKey(direction);
      boolean debug = false;
      if (debug) {
        logger.info(" jsonObject:     " + jsonObject );
        logger.info("-----------------------------");
        logger.info(" jsonKey:     " + jsonKey );
      }
      for (int i = 0; i < noStats; i++) {
        if (debug) {
          logger.info("jsonObjName: " + jsonObjName + i);
        }
        String s = jsonObject.getJsonObject(jsonObjName + i).getString(jsonKey);
        if (s != null && !"NA".equals(s) && isLong(s)) {
          long b = Long.parseLong(s);
          bytesStart = (bytesStart == 0 || b < bytesStart) ? b : bytesStart;
          bytesEnd = (bytesEnd == 0 || b > bytesEnd) ? b : bytesEnd;
        }
        String ts = jsonObject.getJsonObject(jsonObjName + i).getString("timestamp");
        if (ts != null && !"NA".equals(ts) && isLong(ts)) {
          long b = Long.parseLong(ts);
          if (i == 0) {
            tsStart = b;
          }
          if (i == noStats - 1) {
            tsEnd = b;
          }
        }
        if (debug) {
          logger.info("jsonKey:     " + jsonKey);
          logger.info("bytesEnd:   " + bytesEnd);
          logger.info("bytesStart: " + bytesStart);
          logger.info("tsEnd:   " + tsEnd);
          logger.info("tsStart: " + tsStart);
        }
      }

      if (tsEnd != tsStart) {
        long timediff = (tsEnd - tsStart);
        avgBitrate = (8000 * (bytesEnd - bytesStart)) / timediff;
        avgBitrate = (avgBitrate < 0) ? avgBitrate * -1 : avgBitrate;
        if (debug) {
          logger.info(
            "computeBitrate()(8000 * ( " + bytesEnd + " - " + bytesStart + " )) /" + timediff);
        }
        return "" + (avgBitrate);
      } else {
        logger.error("computeBitrate() tsEnd == tsStart : " + tsEnd + " , " + tsStart);
      }
    } catch (NullPointerException npe) {
      logger.error("NullPointerException in computeBitrate");
      logger.error("" + getStackTrace(npe));
    }
    return "";
  }

  /**
   *  Computes the average roundTripTime
   *
   * @param jsonObject object containing the list getStats result.
   * @param noStats how many stats in jsonObject
   * @return the average of valid (> 0) "totalRoundTripTime"
   */
  private static String computeRoundTripTime(JsonObject jsonObject, int noStats, String prefix) {
    double rtt = 0;
    int ct = 0;
    try {
      for (int i = 0; i < noStats; i++) {
        String s = jsonObject.getJsonObject("candidate-pair_" + i).getString(prefix + "RoundTripTime");
        if (s != null && !"NA".equals(s) && !"0".equals(s) && isDouble(s)) {
          rtt += 1000 * Double.parseDouble(s);
          ct++;
        }
      }
    } catch (NullPointerException npe) {
      logger.error("Unable to find " + prefix + "RoundTripTime in the stats. ");
      logger.error("" + getStackTrace(npe));
    }
    if (ct > 0) {
      return "" + ((int)rtt/ct);
    }
    return "";
  }

  /**
   *  Computes the average audioJitter
   *
   * @param jsonObject object containing the list getStats result.
   * @param noStats how many stats in jsonObject
   * @return the average "audioJitter"
   */
  private static String computeAudioJitter(JsonObject jsonObject, int noStats) {
    double jitter = 0;
    int ct = 0;
    if (noStats < 2) return ""; //min two stats
    try {
      for (int i = 0; i < noStats; i++) {
        JsonObject myObject = jsonObject.getJsonObject("inbound-audio_" + i);
        if (myObject != null) {
          String s = myObject.getString("jitter");
          if (s != null && !"NA".equals(s) && isDouble(s)) {
            jitter += (1000 * Double.parseDouble(s));
            ct++;
          }
        }
      }
      if (ct > 0) {
        return "" + (jitter/ct);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return "";
  }

  /**
   * Computes the packet losses as a % packetLost/total packets
   *
   * @param jsonObject object containing the list getStats result.
   * @param noStats how many stats in jsonObject
   * @param mediaType "audio" or "video"
   * @return the packet losses (% packetLost/total packets)
   */
  private static String computePacketsLoss(JsonObject jsonObject, int noStats, String mediaType) {
    if (noStats < 1) return ""; // min one stats
    try {
      JsonObject myObject = jsonObject.getJsonObject("inbound-" + mediaType + "_" + (noStats - 1));
      if (myObject != null) {
        String s = myObject.getString("packetsReceived");
        String l = myObject.getString("packetsLost");
        if (s != null && !"NA".equals(s) && isLong(s)
          && l != null && !"NA".equals(l) && isLong(l)) {
          long packetsLost = Long.parseLong(l);
          long totalPackets = Long.parseLong(s) + packetsLost;
          if (totalPackets > 0) {
            double packetLoss = (packetsLost * 1000) / totalPackets;
            return "" + (new DecimalFormat("#0.000").format(packetLoss/1000));
          }
        } else {
          logger.error(
            "computePacketsLoss  \r\n ---------------\r\n"
              + myObject.toString()
              + "\r\n ---------------\r\n");
        }
      } else {
        logger.error(
          "computePacketsLoss  my object is null " + ("inbound-" + mediaType + "_" + (noStats - 1)));

      }
    } catch (Exception e) {
      logger.error("" + getStackTrace(e));
    }
    return "";
  }

  /**
   * Computes the total bytes sent or received by the candidate
   *
   * @param jsonObject object containing the list getStats result.
   * @param noStats how many stats in jsonObject
   * @param direction Sent or Received
   * @return totalNumberBytes sent or received
   */
  private static String totalBytes(JsonObject jsonObject, int noStats, String direction) {
    long bytes = 0;
    try {
      for (int i = 0; i < noStats; i++) {
        String s = jsonObject.getJsonObject("candidate-pair_" + i).getString("bytes" + direction);
        if (s != null && !"NA".equals(s) && isLong(s)) {
          long b = Long.parseLong(s);
          bytes = Math.max(b, bytes);
        }
      }
    } catch (NullPointerException npe) {
      logger.error("Unable to find \"bytes" + direction + "\" in the stats. ");
      logger.error("" + getStackTrace(npe));
    }
    return "" + bytes;
  }


  private static JsonObjectBuilder getStatsJsonBuilder(JsonObject jsonObject, String[] stringArray, String stats, String mediaType) {
    JsonObjectBuilder subBuilder = Json.createObjectBuilder();
    if ("candidate-pair".equals(stats)) {
      JsonObject successfulCandidate = getSuccessfulCandidate(jsonObject);
      if (successfulCandidate != null) {
        for (int j = 0; j < stringArray.length; j++) {
          if (successfulCandidate.containsKey(stringArray[j])) {
            subBuilder.add(stringArray[j], successfulCandidate.getString(stringArray[j]));
          }
        }
      }
    } else {
      JsonObject myObj = getRTCStats(jsonObject, stats, mediaType);
      if (myObj != null) {
        for (int j = 0; j < stringArray.length; j++) {
          if (myObj.containsKey(stringArray[j])) {
            subBuilder.add(stringArray[j], myObj.getString(stringArray[j]));
          }
        }
      }
    }
    return subBuilder;
  }


  /**
   *  Checks if a String is a double
   *
   * @param s the String to check
   * @return true if the String is a double
   */
  private static boolean isDouble(String s) {
    try {
      Double.parseDouble(s);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   *  Checks if a String is a long
   *
   * @param s the String to check
   * @return true if the String is a long
   */
  private static boolean isLong(String s) {
    try {
      Long.parseLong(s);
      return true;
    } catch (Exception e) {
      return false;
    }
  }










}

