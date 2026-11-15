# Per-module timing for a flattened netlist.
# Yosys names flip-flops after their register (`rename -wire -suffix _reg`), and the
# flattened register names keep the former instance as prefix: "<INST>.<reg>_reg".
# Registers without prefix belong to the top module; "_<n>_" are registers Yosys created.

proc module_of {cell} {
  set name [get_full_name $cell]
  if {[regexp {^([^.]+)\.} $name -> m]} { return $m }
  if {[regexp {^_[0-9]+_$} $name]} { return unnamed }
  return top
}

# Register name of a flip-flop pin: "MODULE.regname[3]_reg/Q" -> "MODULE.regname[3]"
proc reg_of {pin} {
  regsub {_reg$} [file dirname [get_full_name $pin]] {} r
  return $r
}

# ff(<module>) -> list of flip-flops
array set ff {}
foreach c [all_registers -cells] { lappend ff([module_of $c]) $c }
foreach m [lsort [array names ff]] { puts [format "  %-12s %5d regs" $m [llength $ff($m)]] }

# Worst slack for every source -> destination pair
puts "\nworst slack from -> to (ns)"
set rows {}
foreach from_mod [array names ff] {
  foreach to_mod [array names ff] {
    set p [find_timing_paths -from $ff($from_mod) -to $ff($to_mod) -path_delay max]
    if {![llength $p]} continue
    set p [lindex $p 0]
    lappend rows [list [reg_of [get_property $p startpoint]] [reg_of [get_property $p endpoint]] \
      [get_property $p slack]]
  }
}
foreach r [lsort -real -index 2 $rows] { puts [format "  %-40s -> %-40s %8.3f" {*}$r] }
