#![warn(clippy::pedantic)]
#![warn(unused_crate_dependencies)]

mod constants;
mod gc;
mod mock_metrics_server;

use crate::constants::{GC_SPECIFIC_COUNTERS, GENERIC_JVM_COUNTERS, GENERIC_JVM_GAUGES};
use crate::gc::JavaGarbageCollector;
use crate::mock_metrics_server::CollectedRequest;
use clap::Parser;
use serde::Deserialize;
use std::collections::HashMap;
use std::convert::identity;
use std::net::{Ipv4Addr, SocketAddrV4};
use std::path::PathBuf;
use std::process::Command;
use std::time::{Duration, SystemTime};

#[derive(Parser, Debug)]
struct Args {
    agent_jar_path: PathBuf,
    gc: JavaGarbageCollector,
    port: u16,
}

fn main() {
    let args = Args::parse();

    let collect_duration = Duration::from_secs(31);
    let simple_host_app_path = PathBuf::from("java-src/simple-host-app");

    // Compile a simple Java app that serves as the host for the agent tests
    println!("Compiling Java host application...");

    let javac_exit_status = Command::new("javac")
        .current_dir(&simple_host_app_path)
        .args(&["-d", ".", "App.java"])
        .spawn()
        .expect("Couldn't spawn javac process to build test application!")
        .wait()
        .expect("Couldn't wait for javac process completion!");

    if javac_exit_status.success() {
        println!("Compilation successful!");
    } else {
        panic!(
            "Java compilation exited with exit-status: {}!",
            javac_exit_status
        );
    }

    // Spawn the compiled java application with the required GC settings and metrics agent injected
    println!(
        "Spawning Java process using agent at {} and GC '{:?}'",
        args.agent_jar_path.to_string_lossy(),
        args.gc
    );

    let mut java_child_process = Command::new("java")
        .args(&[
            format!("-javaagent:{}", args.agent_jar_path.to_string_lossy()),
            String::from(match args.gc {
                JavaGarbageCollector::ConcurrentMarkSweep => "-XX:+UseConcMarkSweepGC",
                JavaGarbageCollector::Parallel => "-XX:+UseParallelGC",
                JavaGarbageCollector::G1 => "-XX:+UseG1GC",
            }),
            String::from("-cp"),
            String::from(simple_host_app_path.to_string_lossy()),
            String::from("App"),
        ])
        .env(
            "HEROKU_METRICS_URL",
            &format!("http://localhost:{}", args.port),
        )
        .spawn()
        .expect("Couldn't spawn java process!");

    // Start a fake metrics server that will just collect the incoming requests for later validation
    println!(
        "Starting fake metrics server and collecting requests for {:?}",
        &collect_duration
    );

    let collected_requests = mock_metrics_server::collect_requests(
        SocketAddrV4::new(Ipv4Addr::LOCALHOST, args.port),
        collect_duration,
    );

    print!("Verifying request interval...");

    assert!(collected_requests
        .windows(2)
        .map(|window| {
            match window {
                [earlier_request, later_request] => later_request
                    .time
                    .duration_since(earlier_request.time)
                    .map(|duration| duration.as_millis() > 4500 && duration.as_millis() < 5500)
                    .unwrap_or_default(),
                _ => false,
            }
        })
        .all(identity));

    println!("OK");

    print!("Verifying requests...");
    for collected_request in collected_requests {
        verify_request(&collected_request, args.gc);
    }
    println!("OK");

    java_child_process
        .kill()
        .expect("Couldn't kill java process!");
}

fn verify_request(request: &CollectedRequest, expected_gc: JavaGarbageCollector) {
    assert_eq!(
        request.headers.get("Measurements-Count"),
        Some(&String::from("1"))
    );

    assert_eq!(
        request.headers.get("Content-Type"),
        Some(&String::from("application/json"))
    );

    chrono::DateTime::parse_from_rfc3339(
        request
            .headers
            .get("Measurements-Time")
            .expect("Missing 'Measurements-Time' header!"),
    )
    .expect("'Measurements-Time' header does not contain a valid RFC3339 datetime string!");

    let payload: Payload = serde_json::from_str(&request.body.clone().unwrap()).unwrap();

    for gauge in GENERIC_JVM_GAUGES {
        assert!(
            payload.gauges.contains_key(gauge),
            "Gauge '{}' is missing!",
            gauge
        );
    }

    for counter in GENERIC_JVM_COUNTERS {
        assert!(
            payload.counters.contains_key(counter),
            "Counter '{}' is missing!",
            counter
        );
    }

    for (counter_name, gc) in GC_SPECIFIC_COUNTERS {
        if gc == expected_gc {
            assert!(
                payload.counters.contains_key(counter_name),
                "Counter '{}' should be present when using '{:?}' GC!",
                counter_name,
                expected_gc
            );
        } else {
            assert!(
                !payload.counters.contains_key(counter_name),
                "Counter '{}' should not be present when using '{:?}' GC!",
                counter_name,
                expected_gc
            );
        }
    }
}

fn expected_metrics_report_count(duration: &Duration) -> u64 {
    let initial_delay = Duration::from_secs(5);
    let interval = Duration::from_secs(5);

    (*duration - initial_delay).as_secs() / interval.as_secs()
}

#[derive(Debug, Deserialize)]
struct Payload {
    counters: HashMap<String, f64>,
    gauges: HashMap<String, f64>,
}
