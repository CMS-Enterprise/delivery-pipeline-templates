def call(Map config = [:]) {
    def jmeter_image = config.jmeter_image ?: 'artifactory.cloud.cms.gov/docker/justb4/jmeter@sha256:e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5'
    def test_plan = config.test_plan ?: 'tests/performance.jmx'
    def threads = config.threads ?: '10'
    def ramp_up = config.ramp_up ?: '30'
    def duration = config.duration ?: '60'

    stage("JMeter Performance Test") {
        podTemplate(containers: [
            containerTemplate(name: 'jmeter', image: jmeter_image, command: 'cat', ttyEnabled: true)
        ]) {
            node(POD_LABEL) {
                checkout scm
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
