use crate::ipc::{get_errno, PERMISSONS};
use libc::{
    c_uint, sem_close, sem_open, sem_post, sem_unlink, sem_wait, O_CREAT, O_EXCL, O_RDWR, SEM_FAILED
};
use std::ffi::CString;

use crate::ipc::get_last_error;

/// A semaphore implementation for inter-process synchronization.
pub struct Semaphore {
    id: *mut libc::sem_t,
    name: String,
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
    pub fn create(name: &str, initial_value: u32) -> Result<Self, String> {
        let name_cstr = CString::new(name).map_err(|_| "Invalid semaphore name".to_string())?;
        unsafe { sem_unlink(name_cstr.as_ptr()) }; // Remove existing semaphore
        let id = unsafe {
            sem_open(
                name_cstr.as_ptr(),
                O_CREAT | O_EXCL | O_RDWR ,
                PERMISSONS,
                initial_value as c_uint,
            )
        };

        if id == SEM_FAILED {
            return Err(get_last_error(&format!(
                "Failed to create semaphore {}",
                name
            )));
        }

        Ok(Self {
            id,
            name: name.to_string(),
        })
    }

    /// Performs a blocking wait (decrement) operation on the semaphore.
    ///
    /// # Returns
    /// * `Ok(())` if successful.
    /// * `Err(String)` if the operation fails.
    pub fn wait(&self) -> Result<(), String> {
        if unsafe { sem_wait(self.id) } == -1 {
            return Err(get_last_error(&format!(
                "Failed to lock semaphore {}",
                self.name
            )));
        }
        Ok(())
    }

    /// Performs a post (increment) operation on the semaphore.
    ///
    /// # Returns
    /// * `Ok(())` if successful.
    /// * `Err(String)` if the operation fails.
    pub fn post(&self) -> Result<(), String> {
        if unsafe { sem_post(self.id) } == -1 {
            return Err(get_last_error(&format!(
                "Failed to unlock semaphore {}",
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
