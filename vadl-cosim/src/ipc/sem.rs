use crate::ipc::{PERMISSONS, get_errno};
use anyhow::{Context, Result, bail};
use libc::{
    c_uint, clock_gettime, sem_close, sem_open, sem_post, sem_timedwait, sem_unlink, sem_wait, timespec, CLOCK_REALTIME, EINTR, ETIMEDOUT, O_CREAT, O_EXCL, O_RDWR, SEM_FAILED
};
use std::{ffi::CString, time::Duration};

use crate::ipc::get_last_error;

/// A semaphore implementation for inter-process synchronization.
pub struct Semaphore {
    id: *mut libc::sem_t,
    name: String,
}


pub enum TimedWaitState {
    Timeout,
    Success,
}

impl Semaphore {
    /// Creates a new named semaphore with the given initial value.
    ///
    /// # Arguments
    /// * `name` - The name of the semaphore.
    /// * `initial_value` - The initial count of the semaphore.
    ///
    /// # Returns
    /// * `Ok(Self)` if the semaphore is created successfully.
    /// * `Err(String)` if the creation fails.
    pub fn create(name: &str, initial_value: u32) -> Result<Self> {
        let name_cstr = CString::new(name).context("Invalid semaphore name")?;
        unsafe { sem_unlink(name_cstr.as_ptr()) }; // Remove existing semaphore
        let id = unsafe {
            sem_open(
                name_cstr.as_ptr(),
                O_CREAT | O_EXCL | O_RDWR,
                PERMISSONS,
                initial_value as c_uint,
            )
        };

        if id == SEM_FAILED {
            bail!(get_last_error(&format!(
                "Failed to create semaphore: {}",
                name
            )))
        }

        Ok(Self {
            id,
            name: name.to_string(),
        })
    }

    pub fn wait(&self) -> Result<()> {
        if unsafe { sem_wait(self.id) } == -1 {
            bail!(get_last_error(&format!(
                "Failed to lock semaphore: {}",
                self.name
            )));
        }
        Ok(())
    }

    pub fn timedwait(&self, duration: Duration) -> Result<TimedWaitState> {
        let mut ts = timespec {
            tv_sec: 0,
            tv_nsec: 0,
        };

        let ts_ptr = &mut ts as *mut timespec;

        if unsafe { clock_gettime(CLOCK_REALTIME, ts_ptr) } == -1 {
            bail!(get_last_error(&format!(
                "clock_gettime failed in semaphore timedwait: {}",
                self.name
            )));
        }

        ts.tv_nsec += duration.as_nanos() as i64;

        let mut s: i32;
        loop {
            s = unsafe { sem_timedwait(self.id, ts_ptr) };
            if s == -1 && get_errno() == EINTR {
                continue;
            }
            break;
        }

        if s == -1 {
            if get_errno() == ETIMEDOUT {
                Ok(TimedWaitState::Timeout)
            } else {
                bail!(get_last_error(&format!(
                    "Failed to timedwait semaphore: {}",
                    self.name
                )))
            }
        } else {
            Ok(TimedWaitState::Success)
        }
    }

    pub fn post(&self) -> Result<()> {
        if unsafe { sem_post(self.id) } == -1 {
            bail!(get_last_error(&format!(
                "Failed to unlock semaphore: {}",
                self.name
            )));
        }
        Ok(())
    }
}

impl Drop for Semaphore {
    /// Closes and optionally removes the semaphore when dropped.
    fn drop(&mut self) {
        unsafe {
            if sem_close(self.id) == -1 {
                let err = get_errno();
                eprintln!("Warning: sem_close failed {}: {}", self.name, err);
            }

            let name_cstr = CString::new(self.name.clone()).expect("Failed to create CString");
            if sem_unlink(name_cstr.as_ptr()) == -1 {
                let err = get_errno();
                eprintln!("Warning: sem_unlink failed {}: {}", self.name, err);
            }
        }
    }
}
