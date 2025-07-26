use crate::ipc::get_errno;
use anyhow::{Result, bail};
use libc::{
    clock_gettime, pthread_cond_broadcast, pthread_cond_destroy, pthread_cond_init, pthread_cond_t, pthread_cond_timedwait, pthread_cond_wait, pthread_condattr_init, pthread_condattr_setpshared, pthread_condattr_t, pthread_mutex_destroy, pthread_mutex_init, pthread_mutex_lock, pthread_mutex_t, pthread_mutex_unlock, pthread_mutexattr_init, pthread_mutexattr_setpshared, pthread_mutexattr_t, timespec, CLOCK_REALTIME, ETIMEDOUT, PTHREAD_PROCESS_SHARED
};
use std::{mem::MaybeUninit, time::Duration};

use crate::ipc::get_last_error;

/// A semaphore implementation for inter-process synchronization.
#[repr(C)]
pub struct Semaphore {
    pub mutex: pthread_mutex_t,
    pub cond_server: pthread_cond_t,
    pub cond_client: pthread_cond_t,
    pub is_server: bool,
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
    pub fn create() -> Self {
        let mut mutex_attr: pthread_mutexattr_t = unsafe { MaybeUninit::zeroed().assume_init() };
        let mutex_attr_ptr = &mut mutex_attr as *mut _;
        unsafe { 
            pthread_mutexattr_init(mutex_attr_ptr);
            pthread_mutexattr_setpshared(mutex_attr_ptr, PTHREAD_PROCESS_SHARED) 
        };

        let mut mutex: pthread_mutex_t = unsafe { MaybeUninit::zeroed().assume_init() };
        let mutex_ptr = &mut mutex as *mut _;
        unsafe { pthread_mutex_init(mutex_ptr, mutex_attr_ptr) };

        let mut cond_attr: pthread_condattr_t = unsafe { MaybeUninit::zeroed().assume_init() };
        let cond_attr_ptr = &mut cond_attr as *mut _;
        unsafe { 
            pthread_condattr_init(cond_attr_ptr);
            pthread_condattr_setpshared(cond_attr_ptr, PTHREAD_PROCESS_SHARED);
        };

        let mut cond_server: pthread_cond_t = unsafe { MaybeUninit::zeroed().assume_init() };
        let mut cond_client: pthread_cond_t = unsafe { MaybeUninit::zeroed().assume_init() };

        let cond_server_ptr = &mut cond_server as *mut _;
        let cond_client_ptr = &mut cond_client as *mut _;

        unsafe { 
            pthread_cond_init(cond_server_ptr, cond_attr_ptr);
            pthread_cond_init(cond_client_ptr, cond_attr_ptr);
        };

        let is_server = true;
        Self {
            mutex,
            cond_server,
            cond_client,
            is_server,
        }
    }

    pub fn wait(&mut self) -> Result<()> {
        let mutex_ptr = &mut self.mutex as *mut _;
        let cond_ptr = &mut self.cond_server as *mut _;
        if unsafe { pthread_mutex_lock(mutex_ptr) } == -1 {
            bail!(get_last_error(&format!("Failed to lock semaphore",)));
        }

        #[allow(clippy::while_immutable_condition)]
        while !self.is_server {
            if unsafe { pthread_cond_wait(cond_ptr, mutex_ptr) } == -1 {
                bail!(get_last_error(&format!("Failed to lock semaphore",)));
            }
        }

        Ok(())
    }

    pub fn timedwait(&mut self, duration: Duration) -> Result<TimedWaitState> {
        let mut ts = timespec {
            tv_sec: 0,
            tv_nsec: 0,
        };

        let ts_ptr = &mut ts as *mut _;
        let mutex_ptr = &mut self.mutex as *mut _;
        let cond_ptr= &mut self.cond_server as *mut _;

        if unsafe { pthread_mutex_lock(mutex_ptr) } != 0 {
            bail!(get_last_error("fail"));
        }

        if unsafe { clock_gettime(CLOCK_REALTIME, ts_ptr) } == -1 {
            bail!(get_last_error(&format!(
                "clock_gettime failed in semaphore timedwait"
            )));
        }

        ts.tv_sec += duration.as_secs() as i64;
        ts.tv_nsec += duration.subsec_nanos() as i64;

        // ensure that tv_nsec is less than TV_NSEC_MAX (but larger than 0)
        // otherwise sem_timedwait returns EINVAL
        const TV_NSEC_MAX: i64 = 1_000_000_000;
        if ts.tv_nsec >= TV_NSEC_MAX {
            ts.tv_sec += 1;
            ts.tv_nsec -= TV_NSEC_MAX;
        }

        let mut rc: i32 = 0;
        while !self.is_server && rc == 0 {
            rc = unsafe {
                pthread_cond_timedwait(cond_ptr, mutex_ptr, ts_ptr)
            };
        }

        match rc {
            0 => Ok(TimedWaitState::Success),
            ETIMEDOUT =>  {
                if unsafe { pthread_mutex_unlock(mutex_ptr) } != 0 {
                    bail!(get_last_error("fail"));
                }
                Ok(TimedWaitState::Timeout) 
            },
            _ => {
                if unsafe { pthread_mutex_unlock(mutex_ptr) } != 0 {
                    bail!(get_last_error("fail"));
                }
                bail!("failed to timedwait")
            }
        }
    }

    pub fn post(&mut self) -> Result<()> {
        let mutex_ptr = &mut self.mutex as *mut _;
        let cond_ptr = &mut self.cond_client as *mut _;
        
        self.is_server = false;

        unsafe { pthread_cond_broadcast(cond_ptr) };

        if unsafe { pthread_mutex_unlock(mutex_ptr) } == -1 {
            bail!(get_last_error(&format!("Failed to unlock semaphore",)));
        }

        Ok(())
    }
}

impl Drop for Semaphore {
    /// Closes and optionally removes the semaphore when dropped.
    fn drop(&mut self) {
        let mutex_ptr = &mut self.mutex as *mut _;
        let cond_server_ptr = &mut self.cond_server as *mut _;
        let cond_client_ptr = &mut self.cond_client as *mut _;
        unsafe {
            if pthread_mutex_destroy(mutex_ptr) == -1 {
                let err = get_errno();
                eprintln!("Warning: sem_close failed: {}", err);
            }

            if pthread_cond_destroy(cond_server_ptr) == -1 {
                let err = get_errno();
                eprintln!("Waringin: pthread_cond_destroy on cond_server failed: {}", err);
            }

            if pthread_cond_destroy(cond_client_ptr) == -1 {
                let err = get_errno();
                eprintln!("Waringin: pthread_cond_destroy on cond_client failed: {}", err);
            }
        }
    }
}
