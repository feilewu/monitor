/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * @Author: pf_xu
 * @Date: 2024/4/22 21:54
 * @email：pfxuchn@gmail.com
 */
package com.github.feilewu.monitor.core.log

import org.slf4j.{Logger, LoggerFactory}

private [monitor] trait Logging {

  private var _log: Logger = _

  protected def logName = {
    // Ignore trailing $'s in the class names for Scala objects
    this.getClass.getName.stripSuffix("$")
  }

  // Method to get or create the logger for this object
  protected def log: Logger = {
    if (_log == null) {
      _log = LoggerFactory.getLogger(logName)
    }
    _log
  }


  protected def logInfo(msg: String): Unit = {
    _log.info(msg)
  }

  protected def logWarning(msg: String): Unit = {
    _log.warn(msg)
  }


  protected def logError(msg: String): Unit = {
    _log.error(msg)
  }

  protected def logError(msg: String, e: Throwable): Unit = {
    _log.error(msg, e)
  }

  protected def logDebug(msg: String, e: Throwable): Unit = {
    _log.debug(msg, e)
  }

}
