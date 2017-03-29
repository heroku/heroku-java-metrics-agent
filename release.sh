#!/usr/bin/env bash

set -o pipefail
set -eu

mvn clean package

aws s3 cp target/heroku-metrics-agent.jar s3://lang-jvm/heroku-metrics-agent.jar --acl public-read