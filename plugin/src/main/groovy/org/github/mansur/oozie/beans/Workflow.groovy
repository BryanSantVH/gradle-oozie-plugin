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

import java.util.Map;
import java.util.List;

/**
 * @author Muhammad Ashraf
 * @since 7/23/13
 */
class Workflow {
    String name
    String start
    String end
    String namespace
    HashMap<String, Object> common
    HashMap<String, Object> jobXML
    List<Map<String, Object>> actions
    Map<String, Map<String, String>> credentials
}
