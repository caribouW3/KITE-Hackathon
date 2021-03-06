/*
 * Copyright (C) CoSMo Software Consulting Pte. Ltd. - All Rights Reserved
 */

package com.cosmo.kite.manager;

import com.jcraft.jsch.*;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.io.IOException;
import java.io.InputStream;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;

/**
 * Class handling SSH command running.
 */
public class SSHManager implements Callable<SSHManager> {
  
  /**
   * The Constant logger.
   */
  private static final Logger logger = Logger.getLogger(SSHManager.class.getName());
  
  static {
    JSch.setConfig("StrictHostKeyChecking", "no");
  }
  
  /**
   * The command line.
   */
  private String commandLine;
  private int count = 0;
  private int exitStatus = -1;
  /**
   * The host ip or name.
   */
  private String hostIpOrName;
  private int index = 0;
  /**
   * The key file address.
   */
  private String keyFilePath;
  /**
   * The password.
   */
  private String password;
  /**
   * The username.
   */
  private String username;
  
  
  /**
   * Instantiates a new Ssh manager.
   *
   * @param keyFilePath  the key file path
   * @param username     the username
   * @param hostIpOrName the host ip or name
   * @param commandLine  the command line
   */
  public SSHManager(String keyFilePath, String username, String hostIpOrName, String commandLine) {
    this.keyFilePath = keyFilePath;
    this.username = username;
    this.hostIpOrName = hostIpOrName;
    this.commandLine = commandLine;
  }
  
  /**
   * Instantiates a new Ssh manager.
   *
   * @param keyFilePath  the key file path
   * @param username     the username
   * @param password     the password
   * @param hostIpOrName the host ip or name
   * @param commandLine  the command line
   * @param index        the index
   * @param count        the count
   */
  public SSHManager(String keyFilePath, String username, String password, String hostIpOrName,
                    String commandLine, int index, int count) {
    this.keyFilePath = keyFilePath;
    this.username = username;
    this.password = password;
    this.hostIpOrName = hostIpOrName;
    this.commandLine = commandLine;
    this.index = index;
    this.count = count;
  }
  
  /**
   * Call.
   *
   * @return The same object in case of successful run
   * @throws Exception of KiteInsufficientValueException type if username and
   *                   keyFileAddress is not given and of SSHManagerException type if
   *                   there found an error while connecting to the host using SSH
   */
  @Override
  public SSHManager call() throws Exception {
    MDC.put("tag", this.hostIpOrName);
    Session session = null;
    Channel channel = null;
    InputStream inputStream = null;
    try {
      JSch jsch = new JSch();
      jsch.addIdentity(this.keyFilePath);
      if (this.hostIpOrName.contains(":")) {
        StringTokenizer st = new StringTokenizer(this.hostIpOrName, ":");
        session = jsch.getSession(this.username, st.nextToken(), Integer.parseInt(st.nextToken()));
      } else {
        session = jsch.getSession(this.username, this.hostIpOrName, 22);
      }
      if (this.password != null) {
        session.setPassword(this.password);
      }
      session.connect();
      
      // run stuff
      String command = this.commandLine;
      channel = session.openChannel("exec");
      ((ChannelExec) channel).setCommand(command);
      ((ChannelExec) channel).setErrStream(System.err);
      if (count > 0) {
        logger.debug(
          "Running ("
            + (this.index + 1)
            + "/"
            + this.count
            + ")' on \" + this.hostIpOrName + \" : "
            + this.commandLine);
      } else {
        logger.debug("Running the following command on " + this.hostIpOrName + " : " + this.commandLine);
      }
      exitStatus = -1;
      channel.connect();
      
      inputStream = channel.getInputStream();
      // setStartTimestamp reading the input from the executed commands on the shell
      int maxLines = 10;
      byte[] tmp = new byte[1024];
      while (maxLines-- > 0) {
        Thread.sleep(1000);
        while (inputStream.available() > 0) {
          int l = inputStream.read(tmp, 0, 1024);
          if (l < 0)
            break;
          logger.info(new String(tmp, 0, l));
        }
        if (channel.isClosed()) {
          this.exitStatus = channel.getExitStatus();
          logger.info("exit-status: " + this.exitStatus);
          break;
        }
      }
      channel.disconnect();
      session.disconnect();
    } catch (JSchException | IOException e) {
      logger.warn(e.getClass().getSimpleName() + " in SSHManager: " + e.getLocalizedMessage());
      // throw new SSHManagerException(this, e);
    } finally {
      if (inputStream != null)
        try {
          inputStream.close();
        } catch (IOException e) {
          logger.warn(e);
        }
      if (channel != null) {
        channel.disconnect();
      }
      if (session != null) {
        session.disconnect();
      }
    }
    
    return this;
  }
  
  /**
   * Command successful boolean.
   *
   * @return the boolean
   */
  public boolean commandSuccessful() {
    return this.exitStatus == 0;
  }
  
  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#finalize()
   */
  @Override
  protected void finalize() throws Throwable {
    MDC.remove("tag");
  }
  
  /**
   * Gets the host ip or name.
   *
   * @return the host ip or name
   */
  public String getHostIpOrName() {
    return hostIpOrName;
  }
  
  
}
