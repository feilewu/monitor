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
 * @Date: 2024/4/27 8:37
 * @email：pfxuchn@gmail.com
 */
package com.github.feilewu.monitor.core.conf.config

private[monitor] abstract class ConfigEntry[T](
   val key: String,
   val valueConverter: String => T,
   val stringConverter: T => String,
   val doc: String,
   val isPublic: Boolean) {

  def defaultValue: Option[T] = None

  def defaultValueString: String

}

private class ConfigEntryWithDefault[T](
   override val key: String,
   _defaultValue: T,
   override val valueConverter: String => T,
   override val stringConverter: T => String,
   override val doc: String,
   override val isPublic: Boolean)
  extends ConfigEntry[T](key, valueConverter, stringConverter, doc, isPublic) {

  override def defaultValueString: String = {
    stringConverter(_defaultValue)
  }
  override def defaultValue: Option[T] = Some(_defaultValue)
}

private class ConfigEntryWithDefaultString[T] (
    override val key: String,
    _defaultValue: String,
    override val valueConverter: String => T,
    override val stringConverter: T => String,
    override val doc: String,
    override val isPublic: Boolean)
  extends ConfigEntry(key, valueConverter, stringConverter, doc, isPublic) {

  override def defaultValue: Option[T] = Some(valueConverter(_defaultValue))

  override def defaultValueString: String = _defaultValue

}

