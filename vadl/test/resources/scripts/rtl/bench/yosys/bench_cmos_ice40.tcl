yosys -import
#set file sodor.v
#set top Core

set file build/Spike.sv
set top Spike

# cmos_cells.lib area stats
read_verilog -sv $file
hierarchy -top $top
synth
dfflibmap -liberty /scripts/bench/lib/cmos_cells.lib
abc -liberty /scripts/bench/lib/cmos_cells.lib -dff
clean
puts "CMOS_CELLS.LIB STATS"
puts "===================="
stat -liberty /scripts/bench/lib/cmos_cells.lib

# ice40 stat and timing
design -reset
read_verilog -sv $file
hierarchy
synth_ice40 -nobram
puts "ICE40 STAT AND TIMING"
puts "====================="
stat
sta