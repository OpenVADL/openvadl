// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.iss;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SyscallProfiles {

    public static class OsProfile {
        public final String defaultErrorConvention;
        public final Map<String, Integer> syscalls;

        public OsProfile(String defaultErrorConvention, Map<String, Integer> syscalls) {
            this.defaultErrorConvention = defaultErrorConvention;
            this.syscalls = Collections.unmodifiableMap(syscalls);
        }
    }

    public static final OsProfile LINUX_PROFILE;
    public static final OsProfile BSD_PROFILE;

    static {
        Map<String, Integer> linuxMap = new HashMap<>();
        
        linuxMap.put("exit", 93);
        linuxMap.put("exit_group", 94);
        linuxMap.put("getpid", 172);
        
        // file I/O
        linuxMap.put("read", 63);
        linuxMap.put("write", 64);
        linuxMap.put("openat", 56);
        linuxMap.put("close", 57);
        linuxMap.put("lseek", 62);
        
        linuxMap.put("fstat", 80);
        linuxMap.put("getcwd", 17);
        linuxMap.put("ioctl", 29);
        linuxMap.put("fcntl", 25);
        linuxMap.put("dup", 23);
        
        linuxMap.put("brk", 214);
        linuxMap.put("mmap", 222);
        linuxMap.put("munmap", 215);
        linuxMap.put("mprotect", 226);
        
        linuxMap.put("gettimeofday", 169);
        linuxMap.put("clock_gettime", 113);
        linuxMap.put("rt_sigaction", 134);

        LINUX_PROFILE = new OsProfile("negative_return", linuxMap);

        Map<String, Integer> bsdMap = new HashMap<>();
        
        bsdMap.put("exit", 1);
        bsdMap.put("exit_group", 1); 
        bsdMap.put("getpid", 20);
        
        // file I/O
        bsdMap.put("read", 3);
        bsdMap.put("write", 4);
        bsdMap.put("openat", 499);
        bsdMap.put("close", 6);
        bsdMap.put("lseek", 478);
        
        bsdMap.put("fstat", 551);
        bsdMap.put("getcwd", 326);
        bsdMap.put("ioctl", 54);
        bsdMap.put("fcntl", 92);
        bsdMap.put("dup", 41);
        
        bsdMap.put("brk", 17);   
        bsdMap.put("mmap", 477);
        bsdMap.put("munmap", 73);
        bsdMap.put("mprotect", 74);
        
        bsdMap.put("gettimeofday", 116);
        bsdMap.put("clock_gettime", 232);
        bsdMap.put("rt_sigaction", 416); 

        BSD_PROFILE = new OsProfile("flag_set", bsdMap);
    }
}
