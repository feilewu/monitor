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
 * @Date: 2024/4/27 8:45
 * @email：pfxuchn@gmail.com
 */
package com.github.feilewu.monitor.core.conf.config

import java.util.concurrent.TimeUnit

import com.github.feilewu.monitor.core.util.JavaUtils

private[monitor] case class ConfigBuilder(key: String) {

  private [config] var _doc: String = null

  private[config] var _public: Boolean = true

  import ConfigHelpers._

  def internal(): ConfigBuilder = {
    _public = false
    this
  }

  def stringConf(): TypedConfigBuilder[String] = {
    new TypedConfigBuilder[String](this, v => v, v => v)
  }

  def intConf(): TypedConfigBuilder[Int] = {
    new TypedConfigBuilder[Int](this, v => v.toInt, v => v.toString)
  }

  def booleanConf(): TypedConfigBuilder[Boolean] = {
    new TypedConfigBuilder[Boolean](this, v => v.toBoolean, v => v.toString)
  }

  def timeConf(unit: TimeUnit): TypedConfigBuilder[Long] = {
    new TypedConfigBuilder(this, timeFromString(_, unit), timeToString(_, unit))
  }



}

private object ConfigHelpers {

  def timeFromString(str: String, unit: TimeUnit): Long = JavaUtils.timeStringAs(str, unit)

  def timeToString(v: Long, unit: TimeUnit): String = TimeUnit.MILLISECONDS.convert(v, unit) + "ms"

}

private[monitor] class TypedConfigBuilder[T](val parent: ConfigBuilder,
                                           val converter: String => T,
                                           val stringConverter: T => String) {


  def createWithDefault(default: T): ConfigEntry[T] = {
    // Treat "String" as a special case, so that both createWithDefault and createWithDefaultString
    // behave the same w.r.t. variable expansion of default values.
    default match {
      case str: String => createWithDefaultString(str)
      case _ =>
        val transformedDefault = converter(stringConverter(default))
        val entry = new ConfigEntryWithDefault[T](parent.key, transformedDefault, converter,
          stringConverter, parent._doc, parent._public)
        entry
    }
  }

  def createWithDefaultString(default: String): ConfigEntry[T] = {
    val entry = new ConfigEntryWithDefaultString[T](parent.key, default, converter, stringConverter,
      parent._doc, parent._public)
    entry
  }



}
