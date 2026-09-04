def call(Map config = [:]) {
    def test_plan = config.test_plan ?: 'tests/performance.jmx'
    def threads = config.threads ?: '10'
    def ramp_up = config.ramp_up ?: '30'
    def duration = config.duration ?: '60'
    def myunstash = config.unstash ?: 'workspace'
    def output_name = config.output_name ?: 'jmeter'
    def stagename = config.stage ?: 'JMeter Performance Test'

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
                // Neither perfReport (Performance plugin) nor publishHTML (HTML
                // Publisher plugin) is installed on the controller; calling them
                // throws NoSuchMethodError and fails the stage after JMeter runs.
                archiveArtifacts allowEmptyArchive: true,
                    artifacts: "${output_name}-results.jtl,${output_name}-report/**"
            }
        }
    }
}
