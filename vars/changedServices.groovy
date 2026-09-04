// Selects which services a monorepo build needs to process, so a commit touching
// one service does not rebuild, rescan and redeploy all of them.
//
// Fails open: anything that cannot be attributed to specific services (no merge
// base, a shared path, a git error) selects every service, because a service
// skipped in error ships stale code. Only paths matching ignore_patterns are
// treated as provably build-irrelevant.
def call(Map config = [:]) {
    def services = config.services ?: error('services is required')
    def base_dir = config.base_dir ?: 'demos'
    def base_ref = config.base_ref ?: 'origin/main'
    def stagename = config.stage ?: 'Detect Changed Services'
    // Documentation and editor metadata cannot change a build artifact.
    def ignore_patterns = config.ignore_patterns ?: [
        /.*\.md$/,
        /.*\/\.gitignore$/,
        /^\.gitignore$/,
        /.*\/README$/,
        /^LICENSE.*$/,
    ]

    def selected = services

    stage("${stagename}") {
        def merge_base = config.build_all ? '' : sh(
            script: "git merge-base HEAD ${base_ref} 2>/dev/null || true",
            returnStdout: true
        ).trim()

        if (config.build_all) {
            echo 'build_all set — selecting every service'
        } else if (!merge_base) {
            echo "No merge base against ${base_ref} — selecting every service"
        } else {
            def changed = sh(
                script: "git diff --name-only ${merge_base}...HEAD",
                returnStdout: true
            ).trim()
            def all_files = changed ? changed.split('\n').findAll { it } : []
            def ignored = all_files.findAll { path ->
                ignore_patterns.any { path ==~ it }
            }
            def files = all_files - ignored
            def names = services.collect { it.name }
            // A relevant file outside base_dir/<service>/ may affect any service
            // (shared config, the Jenkinsfile, the shared library, pod specs).
            def shared = files.find { path ->
                !names.any { path.startsWith("${base_dir}/${it}/") }
            }

            if (ignored) {
                echo "Ignoring ${ignored.size()} documentation-only change(s)"
            }

            if (!all_files) {
                echo 'No changed files detected — selecting every service'
            } else if (!files) {
                // Nothing that can affect an artifact changed. Callers must skip
                // the build and deploy path on an empty result.
                selected = []
                echo 'Only documentation changed — no services need building'
            } else if (shared) {
                echo "Change outside a single service (${shared}) — selecting every service"
            } else {
                def touched = names.findAll { name ->
                    files.any { it.startsWith("${base_dir}/${name}/") }
                }
                selected = services.findAll { touched.contains(it.name) }
                echo "Changed services: ${touched.join(', ')}"
            }
        }
    }

    selected
}
