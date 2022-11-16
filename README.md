# Heroku Java Metrics Agent

![License](https://img.shields.io/github/license/heroku/heroku-java-metrics-agent)
![Maven Central](https://img.shields.io/maven-central/v/com.heroku.agent/heroku-java-metrics-agent)
[![CI](https://github.com/heroku/heroku-java-metrics-agent/actions/workflows/ci.yml/badge.svg)](https://github.com/heroku/heroku-java-metrics-agent/actions/workflows/ci.yml)

A lightweight (no dependencies, ~22KiB) JVM agent that is used to collect metrics for Heroku's [JVM runtime metrics](https://devcenter.heroku.com/articles/language-runtime-metrics-jvm) feature.

It is automatically added to JVM applications by the language buildpacks for [JVM](https://github.com/heroku/heroku-buildpack-jvm-common), [Java](https://github.com/heroku/heroku-buildpack-java), [Gradle](https://github.com/heroku/heroku-buildpack-gradle), [Scala](https://github.com/heroku/heroku-buildpack-scala) and [Clojure](https://github.com/heroku/heroku-buildpack-clojure).
Users of these buildpacks don't need to work with this agent directly.

## Manual Setup

Users that use custom buildpacks or Heroku's [container runtime](https://devcenter.heroku.com/articles/container-registry-and-runtime) can set up this agent manually to get JVM runtime metrics in
Heroku's dashboard. Add `-javaagent:/path/to/heroku-metrics-agent.jar` to your main `java` process (i.e. in your app's [Procfile](https://devcenter.heroku.com/articles/procfile) or your Dockerfile's [`CMD` instruction](https://docs.docker.com/engine/reference/builder/#cmd)). It will automatically configure itself when run on Heroku and does nothing when run elsewhere.

## Debugging

To enable more detailed logging, set the `HEROKU_METRICS_DEBUG` environment variable to `true`.
