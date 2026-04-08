read_liberty /opt/pdk/sky130_fd_sc_hd__tt_025C_1v80.lib
read_verilog /rtl/build/DECODE.v
link_design DECODE

# Timings
create_clock -name core_clk -period 5.0 [get_ports clock]
set_input_delay  1.0 -clock [get_clocks core_clk] [all_inputs]
set_output_delay 1.0 -clock [get_clocks core_clk] [all_outputs]

report_checks
