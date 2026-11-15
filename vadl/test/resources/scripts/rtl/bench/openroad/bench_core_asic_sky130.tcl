# tech + cells
read_lef /opt/pdk/sky130_fd_sc_hd__nom.tlef
read_lef /opt/pdk/sky130_fd_sc_hd.lef
read_liberty /opt/pdk/sky130_fd_sc_hd__tt_025C_1v80.lib

# synthesized gate netlist
read_verilog build/TOP_MODULE_netlist.v
link_design TOP_MODULE

# timing constraints
create_clock -name core_clk -period CLOCK_PERIOD [get_ports clock]
set_input_delay  1.0 -clock [get_clocks core_clk] [delete_from_list [all_inputs] [get_ports clock]]
set_output_delay 1.0 -clock [get_clocks core_clk] [all_outputs]

# rough wire model before routing
set_wire_rc -layer met2

# floorplan
initialize_floorplan \
  -utilization 45 \
  -aspect_ratio 1.0 \
  -core_space 10 \
  -site unithd

make_tracks
place_pins -hor_layers met3 -ver_layers met2

# placement
# 'timing_driven' optimizes for critical path delay, rather than total wire length (HPWL)
global_placement -timing_driven
repair_design
detailed_placement
repair_timing -setup
detailed_placement

# parasitic estimation from placement
estimate_parasitics -placement

# Report per-module critical paths
source "/scripts/bench/openroad/module_timing.tcl"

# STA Timing report
# this is picked up by bench_core_iter.sh and must be the last step
report_checks -path_delay max -fields {slew cap fanout input_pins nets} -digits 3

exit
