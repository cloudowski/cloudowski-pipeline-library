#!/usr/bin/env groovy

def call(creds, harbor, project, threshold='High') {
    withCredentials([usernameColonPassword(credentialsId: creds, variable: 'USERPASS')]) {
        script {
            def build = readJSON file: 'build.json'
            env.FULLTAG = build.builds[0].tag
            env.TAG = env.FULLTAG.replaceAll(/.*@(sha256:.+)/, /$1/)
            env.IMG = build.builds[0].imageName
        }

        println "Checking image $FULLTAG"

        sh """#!/bin/bash

set -o errexit
set -o pipefail
set -o xtrace

echo curl -XPOST -s -u $USERPASS https://${harbor}/api/v2.0/projects/${project}/repositories/\${IMG}/artifacts/\$TAG/scan
curl -XPOST -s -u $USERPASS https://${harbor}/api/v2.0/projects/${project}/repositories/\${IMG}/artifacts/\$TAG/scan
scan_status='Unknown'

i=0
while [ "\$scan_status" != "Success" ];do
    curl -s -u $USERPASS https://${harbor}/api/v2.0/projects/${project}/repositories/\${IMG}/artifacts/\$TAG?with_scan_overview=true > scan.json
    scan_status=\$(cat scan.json|jq -r  '.scan_overview."application/vnd.scanner.adapter.vuln.report.harbor+json; version=1.0".scan_status')
    [ \$i -gt 10 ] && { echo "Timeout reached waiting for scan results" >&2; exit 1; }
    i=\$((i+1))
    echo "Scan status: \$scan_status"
    sleep 3
done

cat scan.json|jq -r  '.scan_overview."application/vnd.scanner.adapter.vuln.report.harbor+json; version=1.0"'

RESULT_SEVERITY=\$(cat scan.json|jq -r  '.scan_overview."application/vnd.scanner.adapter.vuln.report.harbor+json; version=1.0".severity')
RESULT_SUMMARY=\$(cat scan.json|jq -r  '.scan_overview."application/vnd.scanner.adapter.vuln.report.harbor+json; version=1.0".summary')

cat << EOF
Scan results
============
  Severity=\$RESULT_SEVERITY
  Summary:
  \$(echo \$RESULT_SUMMARY|jq)
============
EOF

rm -f scan.json
if [[ RESULT_SEVERITY =~ ${threshold} ]];then
   echo 'ERROR: Image contains High severity vulnerabilities' >&2
   exit 1
fi

exit 0

        """
    }
}

return this
