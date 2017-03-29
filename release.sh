#!/usr/bin/env bash

set -o pipefail
set -eu

VERSION="${1}"

mvn versions:set -DnewVersion=

rm pom.xml.versionsBackup

git add pom.xml

git commit -m "Set version $VERSION"

mvn clean package

aws s3 cp target/heroku-java-metrics-agent-$VERSION.jar s3://lang-jvm/heroku-java-metrics-agent-$VERSION.jar --acl public-read
aws s3 cp s3://lang-jvm/heroku-java-metrics-agent-$VERSION.jar s3://lang-jvm/heroku-metrics-agent.jar --acl public-read