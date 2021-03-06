/*
 * Copyright 2013. Muhammad Ashraf
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.github.mansur.oozie.beans

import groovy.xml.MarkupBuilder;

/**
 * @author Muhammad Ashraf
 * @since 7/23/13
 */
class EmailNode extends ActionNode {
  private static final long serialVersionUID = 1L

  String to
  String cc
  String subject
  String body

  @Override
  public void buildXml(MarkupBuilder xml, CommonProperties common) {
    actionXml(xml, common) {
      xml.'email'(xmlns:"uri:oozie:email-action:0.1") {
        addNode(xml, 'to', to ?: common.emailTo)
        addNode(xml, 'cc', cc ?: common.emailCc)
        addNode(xml, 'subject', subject)
        addNode(xml, 'body', body)
      }
    }
  }
}
