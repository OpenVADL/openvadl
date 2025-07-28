use crate::{bail_on_libc_err, eprintln_on_libc_err};
use anyhow::{Result, bail};
use libc::{
    CLOCK_REALTIME, ETIMEDOUT, PTHREAD_PROCESS_SHARED, clock_gettime, pthread_cond_broadcast,
    pthread_cond_destroy, pthread_cond_init, pthread_cond_t, pthread_cond_timedwait,
    pthread_cond_wait, pthread_condattr_init, pthread_condattr_setpshared, pthread_condattr_t,
    pthread_mutex_destroy, pthread_mutex_init, pthread_mutex_lock, pthread_mutex_t,
    pthread_mutex_unlock, pthread_mutexattr_init, pthread_mutexattr_setpshared,
    pthread_mutexattr_t, timespec,
};
use std::time::Duration;

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
    pub fn create() -> Result<Self> {
        let mut mutex_attr: pthread_mutexattr_t = unsafe { std::mem::zeroed() };
        let mutex_attr_ptr = &mut mutex_attr as *mut _;

        unsafe {
            bail_on_libc_err!(pthread_mutexattr_init(mutex_attr_ptr));
            bail_on_libc_err!(pthread_mutexattr_setpshared(
                mutex_attr_ptr,
                PTHREAD_PROCESS_SHARED
            ));
        }

        let mut mutex: pthread_mutex_t = unsafe { std::mem::zeroed() };
        let mutex_ptr = &mut mutex as *mut _;

        unsafe {
            bail_on_libc_err!(pthread_mutex_init(mutex_ptr, mutex_attr_ptr));
        }

        let mut cond_attr: pthread_condattr_t = unsafe { std::mem::zeroed() };
        let cond_attr_ptr = &mut cond_attr as *mut _;

        unsafe {
            bail_on_libc_err!(pthread_condattr_init(cond_attr_ptr));
            bail_on_libc_err!(pthread_condattr_setpshared(
                cond_attr_ptr,
                PTHREAD_PROCESS_SHARED
            ));
        }

        let mut cond_server: pthread_cond_t = unsafe { std::mem::zeroed() };
        let mut cond_client: pthread_cond_t = unsafe { std::mem::zeroed() };

        let cond_server_ptr = &mut cond_server as *mut _;
        let cond_client_ptr = &mut cond_client as *mut _;

        unsafe {
            bail_on_libc_err!(pthread_cond_init(cond_server_ptr, cond_attr_ptr));
            bail_on_libc_err!(pthread_cond_init(cond_client_ptr, cond_attr_ptr));
        }

        Ok(Self {
            mutex,
            cond_server,
            cond_client,
            // Initial value is `true` to indicate that the server has the initial mutex lock
            is_server: true,
        })
    }

    pub fn wait(&mut self) -> Result<()> {
        let mutex_ptr = &mut self.mutex as *mut _;
        let cond_ptr = &mut self.cond_server as *mut _;
        unsafe {
            bail_on_libc_err!(pthread_mutex_lock(mutex_ptr));
        }

        #[allow(clippy::while_immutable_condition)]
        while !self.is_server {
            unsafe {
                bail_on_libc_err!(pthread_cond_wait(cond_ptr, mutex_ptr));
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
        let cond_ptr = &mut self.cond_server as *mut _;

        unsafe {
            bail_on_libc_err!(pthread_mutex_lock(mutex_ptr));
            bail_on_libc_err!(clock_gettime(CLOCK_REALTIME, ts_ptr));
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
            rc = unsafe { pthread_cond_timedwait(cond_ptr, mutex_ptr, ts_ptr) };
        }

        match rc {
            0 => Ok(TimedWaitState::Success),
            ETIMEDOUT => {
                unsafe { bail_on_libc_err!(pthread_mutex_unlock(mutex_ptr)) };
                Ok(TimedWaitState::Timeout)
            }
            _ => {
                unsafe { bail_on_libc_err!(pthread_mutex_unlock(mutex_ptr)) };
                bail!("failed to timedwait")
            }
        }
    }

    pub fn post(&mut self) -> Result<()> {
        let mutex_ptr = &mut self.mutex as *mut _;
        let cond_ptr = &mut self.cond_client as *mut _;

        self.is_server = false;

        unsafe {
            bail_on_libc_err!(pthread_cond_broadcast(cond_ptr));
            bail_on_libc_err!(pthread_mutex_unlock(mutex_ptr), -1);
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
            eprintln_on_libc_err!(pthread_mutex_destroy(mutex_ptr));
            eprintln_on_libc_err!(pthread_cond_destroy(cond_server_ptr));
            eprintln_on_libc_err!(pthread_cond_destroy(cond_client_ptr));
        }
    }
}
