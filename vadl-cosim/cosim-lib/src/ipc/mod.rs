use std::ffi::CStr;

use libc::strerror;

pub mod cstructs;
pub mod qemu;
pub mod sem;
pub mod shm;

const PERMISSONS: u32 = 0o600;

#[cfg(target_os = "macos")]
fn get_errno() -> i32 {
    unsafe { *libc::__error() }
}

#[cfg(target_os = "linux")]
fn get_errno() -> i32 {
    unsafe { *libc::__errno_location() }
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

#[macro_export]
macro_rules! bail_on_libc_err {
    ($expr:expr) => {{
        let res = $expr;
        if res != 0 {
            let s = stringify!($expr);
            bail!(get_last_error(&format!("{s} failed (with return: {res:?})")))
        }
        res
    }};
    ($expr:expr, $errcode:expr) => {{
        let res = $expr;
        if res == $errcode {
            let s = stringify!($expr);
            bail!(get_last_error(&format!("{s} failed (with return: {res:?})")))
        }
        res
    }};
}

#[macro_export]
macro_rules! eprintln_on_libc_err {
    ($expr:expr) => {
        let res = $expr;
        if res != 0 {
            let s = stringify!($expr);
            eprintln!("{}", get_last_error(&format!("{s} failed (with return: {res:?})")))
        }
    };
    ($expr:expr, $errcode:expr) => {
        let res = $expr;
        if res == $errcode {
            let s = stringify!($expr);
            eprintln!("{}", get_last_error(&format!("{s} failed (with return: {res:?})")))
        }
    };
}
