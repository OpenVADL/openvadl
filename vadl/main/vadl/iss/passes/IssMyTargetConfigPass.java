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

package vadl.iss.passes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.iss.MyTargetConfig;
import vadl.iss.SyscallProfiles;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Abi;
import vadl.viam.Specification;

public class IssMyTargetConfigPass extends AbstractIssPass {

  public IssMyTargetConfigPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("ISS MyTargetConfig Generation");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {

        Abi abi = viam.abi().orElseThrow(() -> 
            new RuntimeException("Error: 'application binary interface' block is missing")
        ); 
        
        int sysReg = findRegisterIndex(abi, "syscall_id");
        
        int retReg = findRegisterIndex(abi, "retval");

        List<Integer> args = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            try {
                args.add(findRegisterIndex(abi, "arg" + i));
            } catch (Exception e) {
                System.out.println("Warning: Register alias arg" + i + " not found, using index 0.");
                args.add(0);
            }
        }

        int pcReg = 32; 
        int insnSize = 4; 
        boolean bigEndian = false;

        String targetOs = "linux"; // standard fallback
        String errorConvention = "negative_return";
        Map<String, Integer> finalSyscalls = new HashMap<>();

       /* var osEnv = viam.getOsEnvironment(); 
        
        if (osEnv != null) {
            if (osEnv.getTargetOs() != null) {
                targetOs = osEnv.getTargetOs().toLowerCase();
            }

            SyscallProfiles.OsProfile activeProfile = "bsd".equals(targetOs) 
                ? SyscallProfiles.BSD_PROFILE 
                : SyscallProfiles.LINUX_PROFILE;

            finalSyscalls.putAll(activeProfile.syscalls);
            errorConvention = activeProfile.defaultErrorConvention;

            if (osEnv.getErrorConvention() != null) {
                errorConvention = osEnv.getErrorConvention(); 
            }
            if (osEnv.getCustomMappings() != null) {
                finalSyscalls.putAll(osEnv.getCustomMappings()); 
            }
        } else {
            finalSyscalls.putAll(SyscallProfiles.LINUX_PROFILE.syscalls);
        } */ 
       //TODO load syscalls based on os env 

        finalSyscalls.putAll(SyscallProfiles.LINUX_PROFILE.syscalls);

        String archName = viam.simpleName();

        MyTargetConfig config = new MyTargetConfig(
            archName,
            sysReg,
            retReg,
            args,
            pcReg,
            insnSize,
            bigEndian,
            targetOs,           
            errorConvention,    
            finalSyscalls
        );
        
        System.out.println("Generated MyTargetConfig for " + archName + 
                ": OS=" + targetOs + ", SyscallsLoaded=" + finalSyscalls.size());

        return config; 
    }

    // search for alias like "syscall_id" and return register index
    private int findRegisterIndex(Abi abi, String aliasName) {
        for (var entry : abi.aliases().entrySet()) {
            
            for (Abi.RegisterAlias alias : entry.getValue()) {
                if (aliasName.equals(alias.value())) {
                    return entry.getKey().right(); 
                }
            }
        }
        throw new RuntimeException("Missing alias: " + aliasName);
    }
}
