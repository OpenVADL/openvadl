use std::{ffi::CString, marker::PhantomData, ptr, time::Duration};

use anyhow::{Context, Result, bail};
use libc::{
    MAP_FAILED, MAP_SHARED, O_CREAT, O_RDWR, PROT_READ, PROT_WRITE, close, ftruncate, mmap, munmap,
    shm_open, shm_unlink,
};
use tracing::debug;

use crate::ipc::{cstructs::BrokerSHM, get_errno, get_last_error, sem::TimedWaitState, PERMISSONS};

pub struct SharedMemory<T: Sized> {
    mmap_ptr: *mut u8,
    mmap_path: String,
    mmap_path_c: CString,
    size: usize,
    fd: i32,
    _phantom: PhantomData<T>,
}

impl SharedMemory<BrokerSHM> {
    pub fn release_client(&self) -> Result<()> {
        debug!("releasing client: {}", self.fd);
        self.get_mut().sync.post()
    }

    pub fn wait_client(&self) -> Result<()> {
        debug!("waiting client: {}", self.fd);
        self.get_mut().sync.wait()
    }

    pub fn timedwait_client(&self, duration: Duration) -> Result<TimedWaitState> {
        debug!("waiting client timed: {}, {:?}", self.fd, duration);
        self.get_mut().sync.timedwait(duration)
    }
}

impl<T: Sized> SharedMemory<T> {
    /// Creates a new shared memory segment with a semaphore.
    ///
    /// # Arguments
    /// * `mmap_path` - The file path for the shared memory.
    /// * `size` - The size of the shared memory.
    ///
    /// # Returns
    /// * `Ok(Self)` on success.
    /// * `Err(String)` on failure.
    pub fn create(mmap_path: &str) -> Result<Self> {
        let size = size_of::<T>();
        let mmap_path_c =
            CString::new(mmap_path).with_context(|| format!("Invalid mmap_path: {mmap_path}"))?;
        unsafe {
            let fd = shm_open(mmap_path_c.as_ptr(), O_CREAT | O_RDWR, PERMISSONS);
            if fd == -1 {
                bail!(get_last_error("Failed to open shared memory"));
            }

            if ftruncate(fd, size as i64) == -1 {
                bail!(get_last_error("Failed to truncate shared memory"));
            }

            let addr = mmap(
                ptr::null_mut(),
                size,
                PROT_READ | PROT_WRITE,
                MAP_SHARED,
                fd,
                0,
            );

            if addr == MAP_FAILED {
                bail!(get_last_error(&format!("Failed to map memory {mmap_path}")));
            }

            Ok(Self {
                mmap_ptr: addr as *mut u8,
                mmap_path: mmap_path.to_string(),
                mmap_path_c,
                size,
                fd,
                _phantom: PhantomData,
            })
        }
    }

    /// Reads and deserializes data from shared memory.
    /// Assumes that the shared memory contains valid data.
    pub fn get(&self) -> &T {
        let data_ptr = self.mmap_ptr as *const T;
        let shared: &T = unsafe { &*data_ptr };
        shared
    }

    #[allow(clippy::mut_from_ref)]
    pub fn get_mut(&self) -> &mut T {
        let data_ptr = self.mmap_ptr as *mut T;
        let shared: &mut T = unsafe { &mut *data_ptr };
        shared
    }
}

impl<T: Sized> Drop for SharedMemory<T> {
    fn drop(&mut self) {
        unsafe {
            if munmap(self.mmap_ptr as *mut _, self.size) == -1 {
                let err = get_errno();
                eprintln!("Warning: munmap failed on {}: {}", self.mmap_path, err);
            }

            if close(self.fd) == -1 {
                let err = get_errno();
                eprintln!(
                    "Warning: close failed on {} (fd={}): {}",
                    self.mmap_path, self.fd, err
                );
            }

            if shm_unlink(self.mmap_path_c.as_ptr()) == -1 {
                let err = get_errno();
                eprintln!(
                    "Warning: shm_unlink failed on {} (fd={}): {}",
                    self.mmap_path, self.fd, err
                );
            }
        }
    }
}
