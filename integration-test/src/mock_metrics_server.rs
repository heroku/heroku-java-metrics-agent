use rouille::Response;
use std::collections::HashMap;
use std::io::Read;
use std::net::SocketAddrV4;
use std::sync::{Arc, Mutex};
use std::time::{Duration, Instant, SystemTime};

pub fn collect_requests(address: SocketAddrV4, duration: Duration) -> Vec<CollectedRequest> {
    let collected_requests = Arc::new(Mutex::new(vec![]));
    let return_requests = collected_requests.clone();

    let server = rouille::Server::new(address, move |request| {
        let mut collected_requests = collected_requests
            .lock()
            .expect("Could not acquire mutex lock to requests vector!");

        collected_requests.push(CollectedRequest {
            time: SystemTime::now(),
            headers: request
                .headers()
                .map(|(a, b)| (String::from(a), String::from(b)))
                .collect(),
            body: request.data().and_then(|mut data| {
                let mut ret = String::new();
                data.read_to_string(&mut ret).map(|_| ret).ok()
            }),
        });

        Response::empty_204()
    })
    .expect("Could not create HTTP server!");

    let start = Instant::now();
    loop {
        server.poll_timeout(Duration::from_millis(100));

        if start.elapsed() > duration {
            break;
        }
    }

    server.join();

    // Without dropping the server, it would still have a reference to collected_requests which will
    // prevent unwrapping the `Arc` down below.
    drop(server);

    Arc::try_unwrap(return_requests)
        .expect("Couldn't unwrap Arc smart pointer to collected request")
        .into_inner()
        .expect("Couldn't dissolve mutex for collected requests")
}

#[derive(Debug)]
pub struct CollectedRequest {
    pub time: SystemTime,
    pub headers: HashMap<String, String>,
    pub body: Option<String>,
}
