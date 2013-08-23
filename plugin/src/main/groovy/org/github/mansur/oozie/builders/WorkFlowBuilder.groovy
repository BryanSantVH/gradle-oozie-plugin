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

package org.github.mansur.oozie.builders

import groovy.xml.MarkupBuilder
import org.github.mansur.oozie.beans.Workflow

/**
 * @author Muhammad Ashraf
 * @since 7/24/13
 */
class WorkFlowBuilder {
    HashMap<String, Object> registry = [
            "java": new JavaBuilder(),
            "fs": new FSBuilder(),
            "ssh": new SSHBuilder(),
            "mapreduce": new MapReduceBuilder(),
            "shell": new ShellBuilder(),
            "fork": new ForkBuilder(),
            "join": new JoinBuilder(),
            "decision": new DecesionBuilder(),
            "pig": new PigBuilder(),
            "hive": new HiveBuilder(),
            "email": new EmailBuilder(),
            "kill": new KillBuilder()
    ]

    def String buildWorkflow(Workflow wf) {
        def actions = wf.actions
        def graph = createDAG(actions)
        def writer = new StringWriter()
        def workflow = new MarkupBuilder(writer)
        workflow.'workflow-app'('xmlns': "$wf.namespace", name: "$wf.name") {
            if (wf.credentials != null && !wf.credentials.isEmpty()) {
              credentials {
                wf.credentials.each { k, v ->
                  credential(name: k, type: v.get("type")) {
                    v.get("configuration").each { propertyName, propertyValue ->
                      property {
                        name(propertyName)
                        value(propertyValue)
                      }
                    }
                  }
                }
              }
            }
            start(to: wf.start)
            graph.each {
                def action = findAction(it.toString(), actions)
                def type = action.get("type")
                def builder = findBuilder(type)
                builder.buildXML(workflow, action, wf.common)
            }
            end(name: wf.end)
        }
        writer.toString()
    }

    def String buildJobXML(HashMap<String, Object> props) {
        String result = null;
        if (!props.isEmpty()) {
            def writer = new StringWriter()
            def xml = new MarkupBuilder(writer)
            xml.configuration {
                props.each { k, v ->
                    property {
                        name(k)
                        value(v)
                    }
                }
            }
            result = writer.toString()
        }
        result
    }

    /**
     * Creates Direct Acyclic Graph of all the actions
     * @param actions
     * @return
     */
    private List<DirectedGraph.Node> createDAG(List<Object> actions) {
        def graph = new DirectedGraph();
        HashMap<String, DirectedGraph.Node> nodesMap = getNodeMap(actions)
        def nodes = nodesMap.values()
        nodes.each { n ->
            def nodeName = n.toString()
            def action = findAction(nodeName, actions)
            def type = action.get("type")
            if (type == "fork") {
                handleFork(action, nodesMap, n)
            } else if (type == "join") {
                handleJoin(action, nodesMap, n)
            } else if (type == "decision") {
                handleDecision(action, nodesMap, n)
            } else {
                def toNodeName = action.get("ok")
                def toNode = nodesMap.get(toNodeName)
                if (toNode != null) {
                    n.addEdge(toNode)
                }
                def fail = action.get("error")
                def failNode = nodesMap.get(fail)
                if (failNode != null) {
                    n.addEdge(failNode)
                }
            }
            graph.addNode(n)
        }
        graph.sort()
    }

    private void handleDecision(HashMap<String, Object> action, HashMap<String, DirectedGraph.Node> nodesMap, n) {
        action.get("switch")?.each { c ->
            def to = c.get("to")
            def toNode = nodesMap.get(to)
            if (toNode != null) {
                n.addEdge(toNode)
            }
        }
    }

    private void handleJoin(HashMap<String, Object> action, HashMap<String, DirectedGraph.Node> nodesMap, DirectedGraph.Node n) {
        def to = action.get("to")
        def toNode = nodesMap.get(to)
        if (toNode != null) {
            n.addEdge(toNode)
        }
    }

    private void handleFork(HashMap<String, Object> action, HashMap<String, DirectedGraph.Node> nodesMap, n) {
        def paths = action.get("paths")
        paths?.each { p ->
            def toNode = nodesMap.get(p)
            if (toNode != null) {
                n.addEdge(toNode)
            }
        }
    }

    private Map asMap(Object o) {
      return o instanceof Map ? o : o.toMap();
    }
    private Map<String, Object> findAction(String name, List<Object> actions) {
        asMap(actions.find { name == asMap(it).get("name") })
    }

    def Object findBuilder(String type) {
        def builder = registry.get(type)
        if (builder == null) {
            throw new IllegalArgumentException(String.format("Invalid action type %s, supported action types are %s", type, registry.keySet().toString()))
        }
        builder
    }

    private HashMap<String, DirectedGraph.Node> getNodeMap(List<Object> actions) {
        def nodesMap = new HashMap<String, DirectedGraph.Node>()
        actions.each {
            it = asMap(it)
            String name = it.get("name")
            def type = it.get("type")
            if (name == null || name.length() <= 0) {
                throw new IllegalStateException("Found action without a name!")
            }
            def node = new DirectedGraph.Node(name, type)
            nodesMap.put(name, node)
        }
        nodesMap
    }
}
