package org.github.mansur.oozie

import org.custommonkey.xmlunit.Diff
import org.custommonkey.xmlunit.XMLUnit
import org.github.mansur.oozie.beans.Workflow
import org.github.mansur.oozie.builders.WorkFlowBuilder
import spock.lang.Specification

/**
 * @author Muhammad Ashraf
 * @since 7/23/13
 */
class WorkflowSpecification extends Specification {

    def "WorkFlow dsl should be able to create a valid oozie xml Spec"() {
        when:
        def jobTracker = "http://jobtracker"
        def namenode = "http://namenode"

        def common = [
                jobTracker: "$jobTracker",
                namenode: "$namenode",
                jobXML: "dev_prop.xml"
        ]

        def credentials = [
                "hive_credentials": [
                  type: "hcat",
                  configuration: [
                    "hcat.metastore.uri": "thrift://localhost:9083/",
                    "hcat.metastore.principal": "hive/_HOST@DOMAIN"
                  ]
                ]
        ]

        def shell_to_prod = [
                name: "shell_to_prod",
                type: 'shell',
                ok: "fork_flow",
                error: "fail",
                delete: ["/tmp/workDir"],
                mkdir: ["/tmp/workDir"],
                exec: "ssh test@localhost",
                captureOutput: true,
                configuration: [
                        "mapred.map.output.compress": "false",
                        "mapred.job.queue.name": "queuename"
                ],
                args: [
                        "input",
                        "output",
                        "cache.txt"
                ],
                envVar: [
                        "java_home"
                ],
                file: [
                        "file1.txt",
                        "file2.txt"
                ],
                archive: [
                        "job.tar"
                ]
        ]

        def move_files = [
                name: "move_files",
                type: 'fs',
                ok: "join_flow",
                error: "fail",
                delete: ["hdfs://foo:9000/usr/tucu/temp-data"],
                mkdir: ['archives/${wf:id()}'],
                move: [
                        [source: '${jobInput}', target: 'archives/${wf:id()}/processed-input'],
                        [source: '${jobInput}', target: 'archives/${wf:id()}/raw-input']

                ],
                chmod: [
                        [path: '${jobOutput}', permissions: '-rwxrw-rw-', dir_files: 'true']
                ]
        ]

        def mahout_pfpgrowth = [
                name: "mahout_fp_growth",
                type: "java",
                delete: ["${jobTracker}/pattern"],
                mainClass: "some.random.class",
                jobXML: "job.xml",
                ok: "join_flow",
                error: "fail",
                configuration: [
                        "mapred.map.output.compress": "false",
                        "mapred.job.queue.name": "queuename"
                ],
                args: [
                        "--input",
                        "/cart",
                        "--output",
                        "--maxheapSize",
                        "50"
                ]
        ]

        def fork_flow = [
                name: "fork_flow",
                type: "fork",
                paths: [
                        "move_files",
                        "mahout_fp_growth"
                ]
        ]

        def join_flow = [
                name: "join_flow",
                type: "join",
                to: "pig_job"
        ]

        def pig_job = [
                name: "pig_job",
                type: "pig",
                delete: ["${jobTracker}/pattern"],
                mainClass: "some.random.class",
                jobXML: "job.xml",
                ok: "hive_job",
                error: "fail",
                configuration: [
                        "mapred.map.output.compress": "false",
                        "mapred.job.queue.name": "queuename"
                ],
                script: "first.pig",
                params: [
                        "--input",
                        "/cart",
                        "--output",
                        "--maxheapSize",
                        "50"
                ]
        ]

        def hive_job = [
                name: "hive_job",
                type: "hive",
                delete: ["${jobTracker}/pattern"],
                mainClass: "some.random.class",
                jobXML: "job.xml",
                ok: "authenticated_hive_job",
                error: "fail",
                configuration: [
                        "mapred.map.output.compress": "false",
                        "mapred.job.queue.name": "queuename"
                ],
                script: "first.hql",
                params: [
                        "--input",
                        "/cart",
                        "--output",
                        "--maxheapSize",
                        "50"
                ]
        ]

        def authenticated_hive_job = [
                name: "authenticated_hive_job",
                type: "hive",
                cred: "hive_credentials",
                delete: ["${jobTracker}/pattern"],
                mainClass: "some.random.class",
                jobXML: "job.xml",
                ok: "flow_decision",
                error: "fail",
                configuration: [
                        "mapred.map.output.compress": "false",
                        "mapred.job.queue.name": "queuename"
                ],
                script: "first.hql",
                params: [
                        "--input",
                        "/cart",
                        "--output",
                        "--maxheapSize",
                        "50"
                ]
        ]

        def first_map_reduce = [
                name: "first_map_reduce",
                type: "mapreduce",
                delete: ["${jobTracker}/pattern"],
                jobXML: "job.xml",
                ok: "end_node",
                error: "fail",
                configuration: [
                        "mapred.map.output.compress": "false",
                        "mapred.job.queue.name": "queuename"
                ]
        ]

        def flow_decision = [
                name: "flow_decision",
                type: "decision",
                switch: [
                        [to: "end_node", if: "some condition"],
                        [to: "first_map_reduce", if: "some other condition"]
                ],
                default: "end_node"
        ]

        def fail = [
                name: "fail",
                type: "kill",
                message: "workflow failed!"
        ]

        def actions = [shell_to_prod, move_files, mahout_pfpgrowth, fork_flow, join_flow, pig_job, hive_job, authenticated_hive_job, first_map_reduce, flow_decision, fail]
        def workflow = new Workflow()
        workflow.actions = actions
        workflow.common = common
        workflow.end = "end_node"
        workflow.name = 'oozie_flow'
        workflow.namespace = 'uri:oozie:workflow:0.1'
        workflow.credentials = credentials

        def builder = new WorkFlowBuilder()
        def result = builder.buildWorkflow(workflow)

        XMLUnit.setIgnoreWhitespace(true)
        def xmlDiff = new Diff(result, SAMPLE_XML.EXPECTED_FLOW)

        then:
        xmlDiff.similar()
    }

    def "Missing Credentials should be no problem"() {
        when:
        def jobTracker = "http://jobtracker"
        def namenode = "http://namenode"

        def common = [
                jobTracker: "$jobTracker",
                namenode: "$namenode",
                jobXML: "dev_prop.xml"
        ]

        def fail = [
                name: "fail",
                type: "kill",
                message: "workflow failed!"
        ]

        def actions = [fail]
        def workflow = new Workflow()
        workflow.actions = actions
        workflow.common = common
        workflow.end = "end_node"
        workflow.name = 'oozie_flow'
        workflow.namespace = 'uri:oozie:workflow:0.1'

        def builder = new WorkFlowBuilder()
        def result = builder.buildWorkflow(workflow)

        print result

        XMLUnit.setIgnoreWhitespace(true)
        def xmlDiff = new Diff(result, SAMPLE_XML.EXPECTED_EMPTY_FLOW)

        then:
        xmlDiff.similar()
    }

    def "generate job.xml"() {
        when:
        def builder = new WorkFlowBuilder()
        def jobXML = builder.buildJobXML([
                "mapred.map.output.compress": "false",
                "mapred.job.queue.name": "queuename"
        ])

       then:
       XMLUnit.setIgnoreWhitespace(true)
        def xmlDiff = new Diff(jobXML, SAMPLE_XML.EXPECTED_JOB_XML)
        xmlDiff.similar()


    }
}
