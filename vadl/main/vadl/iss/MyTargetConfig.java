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

import java.util.List;
import java.util.Map;

public class MyTargetConfig {

    private final String archName;       
    private final int syscallRegister; 
    private final int returnRegister;   
    private final List<Integer> argRegisters;
    private final int pcRegister;        
    private final int instructionSize;  
    private final boolean isBigEndian;   

    private final String targetOs; 
    private final String errorConvention;
    private final Map<String, Integer> syscallMappings;

    public MyTargetConfig(String archName, int syscallRegister, int returnRegister, 
                           List<Integer> argRegisters, int pcRegister, 
                           int instructionSize, boolean isBigEndian,
                           String targetOs, String errorConvention, 
                           Map<String, Integer> syscallMappings) {
        this.archName = archName;
        this.syscallRegister = syscallRegister;
        this.returnRegister = returnRegister;
        this.argRegisters = argRegisters;
        this.pcRegister = pcRegister;
        this.instructionSize = instructionSize;
        this.isBigEndian = isBigEndian;

        this.targetOs = targetOs;
        this.errorConvention = errorConvention;
        this.syscallMappings = syscallMappings;
    }
    
    public String getArchName() { return archName; }
    public int getSyscallRegister() { return syscallRegister; }
    public int getReturnRegister() { return returnRegister; }
    public List<Integer> getArgRegisters() { return argRegisters; }
    public int getPcRegister() { return pcRegister; }
    public int getInstructionSize() { return instructionSize; }
    public boolean isBigEndian() { return isBigEndian; }

    public String getTargetOs() { return targetOs; }
    public String getErrorConvention() { return errorConvention; }
    public Map<String, Integer> getSyscallMappings() { return syscallMappings; }
    
    public int getArgReg(int index) {
        if (index < argRegisters.size()) {
            return argRegisters.get(index);
        }
        return 0; 
    }
}
