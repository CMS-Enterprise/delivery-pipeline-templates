def call(Map config = [:]) {
    def test_plan = config.test_plan ?: 'tests/performance.jmx'
    def threads = config.threads ?: '10'
    def ramp_up = config.ramp_up ?: '30'
    def duration = config.duration ?: '60'
    def myunstash = config.unstash ?: 'workspace'

    stage("JMeter Performance Test") {
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
                            -l jmeter-results.jtl \
                            -e -o jmeter-report/
                    """
                }
                perfReport sourceDataFiles: 'jmeter-results.jtl'
                publishHTML(target: [
                    reportDir: "jmeter-report",
                    reportFiles: "index.html",
                    reportName: "JMeter Report"
                ])
            }
        }
    }
}
