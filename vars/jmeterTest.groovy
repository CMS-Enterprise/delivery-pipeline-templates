def call(Map config = [:]) {
    def test_plan = config.test_plan ?: 'tests/performance.jmx'
    def threads = config.threads ?: '10'
    def ramp_up = config.ramp_up ?: '30'
    def duration = config.duration ?: '60'
    def myunstash = config.unstash ?: 'workspace'
    def output_name = config.output_name ?: 'jmeter'
    def stagename = config.stage ?: 'JMeter Performance Test'
    def report_name = config.report_name ?: 'JMeter Report'

    stage("${stagename}") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/jmeter.yaml')) {
            node(POD_LABEL) {
                unstash "${myunstash}"
                container('jmeter') {
                    sh """
                        jmeter -n \
                            -t ${test_plan} \
                            -Jthreads=${threads} \
                            -Jrampup=${ramp_up} \
                            -Jduration=${duration} \
                            -l ${output_name}-results.jtl \
                            -e -o ${output_name}-report/
                    """
                }
                perfReport sourceDataFiles: "${output_name}-results.jtl"
                publishHTML(target: [
                    reportDir: "${output_name}-report",
                    reportFiles: "index.html",
                    reportName: report_name
                ])
            }
        }
    }
}
