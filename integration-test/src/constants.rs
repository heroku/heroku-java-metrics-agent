use crate::JavaGarbageCollector;

pub(crate) const GC_SPECIFIC_COUNTERS: [(&str, JavaGarbageCollector); 12] = [
    (
        "jvm_gc_collection_seconds_count.gc_PS_Scavenge",
        JavaGarbageCollector::Parallel,
    ),
    (
        "jvm_gc_collection_seconds_sum.gc_PS_Scavenge",
        JavaGarbageCollector::Parallel,
    ),
    (
        "jvm_gc_collection_seconds_count.gc_PS_MarkSweep",
        JavaGarbageCollector::Parallel,
    ),
    (
        "jvm_gc_collection_seconds_sum.gc_PS_MarkSweep",
        JavaGarbageCollector::Parallel,
    ),
    (
        "jvm_gc_collection_seconds_count.gc_G1_Old_Generation",
        JavaGarbageCollector::G1,
    ),
    (
        "jvm_gc_collection_seconds_sum.gc_G1_Old_Generation",
        JavaGarbageCollector::G1,
    ),
    (
        "jvm_gc_collection_seconds_count.gc_G1_Young_Generation",
        JavaGarbageCollector::G1,
    ),
    (
        "jvm_gc_collection_seconds_sum.gc_G1_Young_Generation",
        JavaGarbageCollector::G1,
    ),
    (
        "jvm_gc_collection_seconds_count.gc_ConcurrentMarkSweep",
        JavaGarbageCollector::ConcurrentMarkSweep,
    ),
    (
        "jvm_gc_collection_seconds_sum.gc_ConcurrentMarkSweep",
        JavaGarbageCollector::ConcurrentMarkSweep,
    ),
    (
        "jvm_gc_collection_seconds_count.gc_ParNew",
        JavaGarbageCollector::ConcurrentMarkSweep,
    ),
    (
        "jvm_gc_collection_seconds_sum.gc_ParNew",
        JavaGarbageCollector::ConcurrentMarkSweep,
    ),
];

// There is another count that wasn't emitted by the agent for a long time now:
// - jvm_gc_collection_seconds_sum.gc_all
// It is referenced in some places but seems to be unused. We don't test for it but leave
// this comment should anyone encounter this metric count name in the future.
pub(crate) const GENERIC_JVM_COUNTERS: [&str; 1] = ["jvm_gc_collection_seconds_count.gc_all"];

pub(crate) const GENERIC_JVM_GAUGES: [&str; 6] = [
    "jvm_memory_bytes_used.area_heap",
    "jvm_memory_bytes_used.area_nonheap",
    "jvm_buffer_pool_bytes_used.name_direct",
    "jvm_buffer_pool_bytes_used.name_mapped",
    "jvm_memory_bytes_committed.area_heap",
    "jvm_memory_bytes_committed.area_nonheap",
];
