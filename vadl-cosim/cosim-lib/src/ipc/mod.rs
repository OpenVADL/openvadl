use std::ffi::CStr;

use libc::strerror;

pub mod sem;
pub mod shm;
pub mod cstructs;
pub mod qemu;

const PERMISSONS: u32 = 0o600;

#[cfg(target_os = "macos")]
fn get_errno() -> i32 {
    unsafe {
        *libc::__error()
    }
}


#[cfg(target_os = "linux")]
fn get_errno() -> i32 {
    unsafe {
        *libc::__errno_location()
    }
}

/// Retrieves and formats an error message from `errno`.
fn get_last_error(context: &str) -> String {
    unsafe {
        let err = get_errno();
        let err_str = strerror(err);
        format!(
            "{}: {} (errno: {})",
            context,
            CStr::from_ptr(err_str).to_string_lossy(),
            err
        )
    }
}
