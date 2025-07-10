use std::fmt::Debug;

use serde::Serialize;

pub mod diff;

#[derive(Debug, Serialize)]
pub struct Report {
    passed: bool,
    diffs: Vec<DiffEntry>,
}

impl Report {
    pub fn passed() -> Self {
        Report {
            passed: true,
            diffs: Vec::new(),
        }
    }

    pub fn failed(diffs: Vec<DiffEntry>) -> Self {
        Report {
            passed: false,
            diffs,
        }
    }
}

impl From<Vec<DiffEntry>> for Report {
    fn from(diffs: Vec<DiffEntry>) -> Self {
        if diffs.is_empty() {
            Self::passed()
        } else {
            Self::failed(diffs)
        }
    }
}

#[derive(Debug, Serialize)]
pub struct DiffEntry {
    key: String,
    values: Vec<String>,
    description: String,
}

pub enum DiffValue {
    Int(i64),
    Str(String),
}

impl DiffEntry {
    pub fn new(
        key: impl Into<String>,
        values: Vec<String>,
        description: impl Into<String>,
    ) -> Self {
        Self {
            key: key.into(),
            values,
            description: description.into(),
        }
    }
}
