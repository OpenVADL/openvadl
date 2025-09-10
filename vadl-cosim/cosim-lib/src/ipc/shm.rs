use std::{ffi::CString, marker::PhantomData, ptr, time::Duration};

use anyhow::{Context, Result, bail};
use libc::{
    MAP_FAILED, MAP_SHARED, O_CREAT, O_RDWR, PROT_READ, PROT_WRITE, close, ftruncate, mmap, munmap,
    shm_open, shm_unlink,
};

use crate::{
    bail_on_libc_err, eprintln_on_libc_err, ipc::{
        cstructs::{BrokerSHMData, BrokerSHMRingBuffer, BrokerSem}, get_last_error, sem::{Semaphore, TimedWaitState}, PERMISSONS
    }
};

pub struct SharedMemory<T: Sized> {
    mmap_ptr: *mut u8,
    mmap_path_c: CString,
    size: usize,
    fd: i32,
    _phantom: PhantomData<T>,
}

impl SharedMemory<BrokerSem> {
    pub fn release_client(&mut self) -> Result<()> {
        self.get_mut().sync.post()
    }

    pub fn wait_client(&mut self) -> Result<()> {
        self.get_mut().sync.wait()
    }

    pub fn timedwait_client(&mut self, duration: Duration) -> Result<TimedWaitState> {
        self.get_mut().sync.timedwait(duration)
    }

    pub fn get_sync(&self) -> &Semaphore {
        &self.get().sync
    }
}

impl <const SIZE: usize> SharedMemory<BrokerSHMRingBuffer<SIZE>> {
    pub fn read_buffer(&mut self) -> anyhow::Result<&BrokerSHMData> {
        self.get_mut().start_read()
    }

    pub fn read_buffer_prev(&self) -> &BrokerSHMData {
        self.get().read_previous()
    }

    pub fn end_read_buffer(&mut self) {
        self.get_mut().end_read();
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

        let fd = unsafe {
            bail_on_libc_err!(
                shm_open(mmap_path_c.as_ptr(), O_CREAT | O_RDWR, PERMISSONS),
                -1
            )
        };

        unsafe { bail_on_libc_err!(ftruncate(fd, size as i64)) };

        let addr = unsafe {
            bail_on_libc_err!(
                mmap(
                    ptr::null_mut(),
                    size,
                    PROT_READ | PROT_WRITE,
                    MAP_SHARED,
                    fd,
                    0,
                ),
                MAP_FAILED
            )
        };

        Ok(Self {
            mmap_ptr: addr as *mut u8,
            mmap_path_c,
            size,
            fd,
            _phantom: PhantomData,
        })
    }

    /// Reads and deserializes data from shared memory.
    /// Assumes that the shared memory contains valid data.
    pub fn get(&self) -> &T {
        let data_ptr = self.mmap_ptr as *const T;
        let shared: &T = unsafe { &*data_ptr };
        shared
    }

    #[allow(clippy::mut_from_ref)]
    pub fn get_mut(&mut self) -> &mut T {
        let data_ptr = self.mmap_ptr as *mut T;
        let shared: &mut T = unsafe { &mut *data_ptr };
        shared
    }
}

impl<T: Sized> Drop for SharedMemory<T> {
    fn drop(&mut self) {
        unsafe {
            eprintln_on_libc_err!(munmap(self.mmap_ptr as *mut _, self.size));
            eprintln_on_libc_err!(close(self.fd));
            eprintln_on_libc_err!(shm_unlink(self.mmap_path_c.as_ptr()));
        }
    }
}
