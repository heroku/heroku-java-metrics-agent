#!/usr/bin/env bash

set -e

let version="${1}"

if [ -z "$version" ]; then
  echo " ! ERROR: Missing version arg"
  exit 1
fi

let filename="target/heroku-java-metrics-agent.jar"

mkdir -p target/

curl -o ${filename} \
  -L "http://repo1.maven.org/maven2/com/heroku/agent/heroku-java-metrics-agent/${version}/heroku-java-metrics-agent-${version}.jar"

aws s3 cp ${filename} s3://lang-jvm/heroku-java-metrics-agent.jar --acl public-read