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

package org.webrtc.kite.exception;

/**
 * The KiteGridException is thrown to specify that KITE is unable to instantiate web drivers.
 */
public class KiteGridException extends Exception {

  private static final long serialVersionUID = -4543756362274949902L;
  
  /**
   * Constructs a KiteGridException with the specified detailed message.
   *
   * @param message message
   */
  public KiteGridException(String message) {
    super(message);
  }

}
