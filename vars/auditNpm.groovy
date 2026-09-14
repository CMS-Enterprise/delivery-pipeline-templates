def call(Map config = [:]) {
    def stagename = config.stage ?: 'Audit: NPM'
    def working_dir = config.working_dir ?: '.'
    def myunstash = config.unstash ?: 'workspace'
    def audit_level = config.audit_level ?: 'high'
    def exclude_cves = config.exclude_cves ?: []
    stage("${stagename}") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/node.yaml')) {
            node(POD_LABEL) {
                unstash "${myunstash}"
                container('node') {
                    sh "npm config set registry https://artifactory.cloud.cms.gov/artifactory/api/npm/npm/"
                    if (exclude_cves) {
                        def cve_list = exclude_cves.collect { it.trim() }.join(',')
                        sh """
                            cd ${working_dir}
                            set +e
                            npm audit --json --audit-level=${audit_level} > npm-audit-output.json 2>&1
                            audit_exit=\$?
                            set -e
                            if [ \$audit_exit -eq 0 ]; then
                                echo "No vulnerabilities found."
                                exit 0
                            fi
                            node -e '
                                var data = JSON.parse(require("fs").readFileSync("npm-audit-output.json","utf8"));
                                var excluded = new Set("${cve_list}".split(","));
                                var vulns = data.vulnerabilities || {};
                                var failed = [];
                                for (var name of Object.keys(vulns)) {
                                    var v = vulns[name];
                                    var advisories = (v.via || []).filter(function(x){ return typeof x === "object"; });
                                    if (advisories.length === 0) continue;
                                    var ids = advisories.map(function(a){ return a.url.split("/").pop(); });
                                    var unexcluded = ids.filter(function(id){ return !excluded.has(id); });
                                    if (unexcluded.length > 0) failed.push(name + " (" + v.severity + "): " + unexcluded.join(", "));
                                }
                                if (failed.length > 0) {
                                    console.log("npm audit found " + failed.length + " unexcluded vulnerability group(s):");
                                    failed.forEach(function(f){ console.log("  " + f); });
                                    process.exit(1);
                                }
                                console.log("All findings matched excluded advisories: " + Array.from(excluded).join(", "));
                            '
                        """
                    } else {
                        sh """
                            cd ${working_dir}
                            npm audit --audit-level=${audit_level}
                        """
                    }
                }
            }
        }
    }
}
