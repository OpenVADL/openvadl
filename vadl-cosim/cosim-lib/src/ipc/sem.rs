use crate::{bail_on_dead_mutex_owner, bail_on_libc_err, eprintln_on_libc_err};
use color_eyre::{Result, eyre::bail};
use libc::{
    CLOCK_REALTIME, EOWNERDEAD, ETIMEDOUT, PTHREAD_MUTEX_ERRORCHECK, PTHREAD_MUTEX_ROBUST,
    PTHREAD_PROCESS_SHARED, clock_gettime, pthread_cond_destroy, pthread_cond_init,
    pthread_cond_signal, pthread_cond_t, pthread_cond_timedwait, pthread_condattr_init,
    pthread_condattr_setpshared, pthread_condattr_t, pthread_mutex_consistent,
    pthread_mutex_destroy, pthread_mutex_init, pthread_mutex_lock, pthread_mutex_t,
    pthread_mutex_unlock, pthread_mutexattr_init, pthread_mutexattr_setpshared,
    pthread_mutexattr_setrobust, pthread_mutexattr_settype, pthread_mutexattr_t, timespec,
};
use std::time::Duration;

use crate::ipc::get_last_error;

/// A semaphore implementation for inter-process synchronization.
#[repr(C)]
pub struct Semaphore {
    pub mutex: pthread_mutex_t,
    pub cvar: pthread_cond_t,
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

            bail_on_libc_err!(pthread_mutexattr_setrobust(
                mutex_attr_ptr,
                PTHREAD_MUTEX_ROBUST
            ));

            bail_on_libc_err!(pthread_mutexattr_settype(
                mutex_attr_ptr,
                PTHREAD_MUTEX_ERRORCHECK
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

        let mut cvar: pthread_cond_t = unsafe { std::mem::zeroed() };

        let cvar_ptr = &mut cvar as *mut _;

        unsafe {
            bail_on_libc_err!(pthread_cond_init(cvar_ptr, cond_attr_ptr));
        }

        Ok(Self { mutex, cvar })
    }

    pub fn lock(&mut self) -> Result<()> {
        let mutex_ptr = &mut self.mutex as *mut _;
        let res = unsafe { pthread_mutex_lock(mutex_ptr) };
        unsafe { bail_on_dead_mutex_owner!(res, mutex_ptr) };
        bail_on_libc_err!(res);

        Ok(())
    }

    pub fn unlock(&mut self) -> Result<()> {
        let mutex_ptr = &mut self.mutex as *mut _;
        unsafe { bail_on_libc_err!(pthread_mutex_unlock(mutex_ptr)) };

        Ok(())
    }

    pub fn timedwait<Cond>(&mut self, duration: Duration, cond: Cond) -> Result<TimedWaitState>
    where
        Cond: Fn() -> bool,
    {
        let mut ts = timespec {
            tv_sec: 0,
            tv_nsec: 0,
        };

        let ts_ptr = &mut ts as *mut _;
        let mutex_ptr = &mut self.mutex as *mut _;
        let cond_ptr = &mut self.cvar as *mut _;

        unsafe {
            let lock_res = pthread_mutex_lock(mutex_ptr);
            bail_on_dead_mutex_owner!(lock_res, mutex_ptr);
            bail_on_libc_err!(lock_res);
            bail_on_libc_err!(clock_gettime(CLOCK_REALTIME, ts_ptr));
        }

        unsafe {
            (*ts_ptr).tv_sec += duration.as_secs() as i64;
            (*ts_ptr).tv_nsec += duration.subsec_nanos() as i64;
        }

        // ensure that tv_nsec is less than TV_NSEC_MAX (but larger than 0)
        // otherwise sem_timedwait returns EINVAL
        const TV_NSEC_MAX: i64 = 1_000_000_000;
        if ts.tv_nsec >= TV_NSEC_MAX {
            unsafe {
                (*ts_ptr).tv_sec += 1;
                (*ts_ptr).tv_nsec -= TV_NSEC_MAX;
            }
        }

        let mut rc: i32 = 0;
        while !cond() && rc == 0 {
            rc = unsafe { pthread_cond_timedwait(cond_ptr, mutex_ptr, ts_ptr) };
        }

        match rc {
            0 => Ok(TimedWaitState::Success),
            ETIMEDOUT => Ok(TimedWaitState::Timeout),
            _ => {
                bail!("failed to timedwait")
            }
        }
    }

    pub fn signal(&mut self) -> Result<()> {
        let cond_ptr = &mut self.cvar as *mut _;

        unsafe {
            bail_on_libc_err!(pthread_cond_signal(cond_ptr));
        }

        Ok(())
    }
}

impl Drop for Semaphore {
    /// Closes and optionally removes the semaphore when dropped.
    fn drop(&mut self) {
        let mutex_ptr = &mut self.mutex as *mut _;
        let cvar_ptr = &mut self.cvar as *mut _;
        unsafe {
            eprintln_on_libc_err!(pthread_mutex_destroy(mutex_ptr));
            eprintln_on_libc_err!(pthread_cond_destroy(cvar_ptr));
        }
    }
}
