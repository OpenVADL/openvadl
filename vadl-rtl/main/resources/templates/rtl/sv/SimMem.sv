// Must match Data and Address in SimMem.cpp
`define SIMMEM_DATA_TYPE byte unsigned
`define SIMMEM_ADDR_TYPE longint unsigned

`define SIMMEM_DATA_WIDTH $bits(`SIMMEM_DATA_TYPE)
`define SIMMEM_ADDR_WIDTH $bits(`SIMMEM_ADDR_TYPE)

import "DPI-C" function chandle simmem_init(
  input string name,
  input string file
);

import "DPI-C" function `SIMMEM_DATA_TYPE simmem_read(
  input chandle ptr,
  input `SIMMEM_ADDR_TYPE address
);

import "DPI-C" function void simmem_write(
  input chandle ptr,
  input `SIMMEM_ADDR_TYPE address,
  input `SIMMEM_DATA_TYPE data
);

import "DPI-C" function `SIMMEM_ADDR_TYPE simmem_entry(
  input chandle ptr
);

import "DPI-C" function `SIMMEM_ADDR_TYPE simmem_symbol(
  input chandle ptr,
  input string symbol
);

function chandle simmem_initial(
  input string name
);
  string file;
  chandle ptr;
  if ($value$plusargs($sformatf("%s=%%s", name), file)) begin
    ptr = simmem_init(name, file);
    if (ptr == null) begin
      $fwrite(32'h8000_0002, "SimMem init failed.\n");
      $fatal;
    end
  end else begin
    $fwrite(32'h8000_0002, "Init file for SimMem not provided. Use +%s=<file>.\n", name);
    $fatal;
  end
  return ptr;
endfunction

function int randDelay(int max);
  if (max > 0) begin
    return $urandom % max;
  end else begin
    return 0;
  end
endfunction

module SimMemRead #(
  parameter string NAME,
  parameter int DATA_WIDTH,
  parameter int ADDR_WIDTH,
  parameter int WORD_WIDTH,
  parameter int RANDOM_DELAY = 0
) (
  input clock,
  input reset,

  input enable,
  input [ADDR_WIDTH-1:0] address,
  input [$clog2(DATA_WIDTH/WORD_WIDTH+1)-1:0] words,
  output reg [DATA_WIDTH-1:0] data,
  output reg valid
);

  chandle ptr;
  int delay = randDelay(RANDOM_DELAY);

  initial begin
    ptr = simmem_initial(NAME);
  end

  always @*
  begin
    data = 0;
    valid = 0;
    if (enable && delay == 0) begin
      for (int i = 0; i < (words * WORD_WIDTH)/`SIMMEM_DATA_WIDTH; i++) begin
        data[i*`SIMMEM_DATA_WIDTH +: `SIMMEM_DATA_WIDTH] =
            `SIMMEM_DATA_WIDTH'(simmem_read(ptr, `SIMMEM_ADDR_WIDTH'(address + i)));
      end
      valid = 1;
    end
  end

  always @(posedge clock) begin
    if (!reset) begin
      if (enable) begin
        if (delay == 0) begin
          delay <= randDelay(RANDOM_DELAY);
        end else begin
          delay <= delay - 1;
        end
      end
    end
  end

endmodule

module SimMemWrite #(
  parameter string NAME,
  parameter int DATA_WIDTH,
  parameter int ADDR_WIDTH,
  parameter int WORD_WIDTH,
  parameter int RANDOM_DELAY = 0
) (
  input clock,
  input reset,

  input enable,
  input [ADDR_WIDTH-1:0] address,
  input [DATA_WIDTH-1:0] data,
  input [$clog2(DATA_WIDTH/WORD_WIDTH+1)-1:0] words,
  output reg valid
);

  chandle ptr;
  int delay = randDelay(RANDOM_DELAY);

  initial begin
    ptr = simmem_initial(NAME);
  end

  always @*
  begin
    valid = 0;
    if (enable && delay == 0) begin
      for (int i = 0; i < (words * WORD_WIDTH)/`SIMMEM_DATA_WIDTH; i++) begin
        simmem_write(ptr, `SIMMEM_ADDR_WIDTH'(address + i),
            `SIMMEM_DATA_WIDTH'(data[i*`SIMMEM_DATA_WIDTH +: `SIMMEM_DATA_WIDTH]));
      end
      valid = 1;
    end
  end

  always @(posedge clock) begin
    if (!reset) begin
      if (enable) begin
        if (delay == 0) begin
          delay <= randDelay(RANDOM_DELAY);
        end else begin
          delay <= delay - 1;
        end
      end
    end
  end

endmodule

module SimMemSymbols #(
  parameter string NAME,
  parameter int DATA_WIDTH,
  parameter int ADDR_WIDTH,
  parameter int WORD_WIDTH
) (
  input clock,
  input reset,

  output reg [ADDR_WIDTH-1:0] entry,
  output reg [ADDR_WIDTH-1:0] fromhost,
  output reg [ADDR_WIDTH-1:0] tohost
);

  chandle ptr;

  initial begin
    ptr = simmem_initial(NAME);
    entry = ADDR_WIDTH'(simmem_entry(ptr));
    fromhost = ADDR_WIDTH'(simmem_symbol(ptr, "fromhost"));
    tohost = ADDR_WIDTH'(simmem_symbol(ptr, "tohost"));
  end

endmodule