use clap::builder::PossibleValue;

#[derive(Clone, Copy, Eq, PartialEq, Debug)]
pub(crate) enum JavaGarbageCollector {
    ConcurrentMarkSweep,
    Parallel,
    G1,
}

impl clap::ValueEnum for JavaGarbageCollector {
    fn value_variants<'a>() -> &'a [Self] {
        &[Self::Parallel, Self::ConcurrentMarkSweep, Self::G1]
    }

    fn to_possible_value(&self) -> Option<PossibleValue> {
        match self {
            JavaGarbageCollector::ConcurrentMarkSweep => Some(PossibleValue::new("cms")),
            JavaGarbageCollector::Parallel => Some(PossibleValue::new("parallel")),
            JavaGarbageCollector::G1 => Some(PossibleValue::new("g1")),
        }
    }
}
