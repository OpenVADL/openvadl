CREATE TABLE shm_register (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    cpu_id INTEGER, 
    size INTEGER NOT NULL,
    data BLOB NOT NULL,
    name TEXT NOT NULL,
    FOREIGN KEY (cpu_id) REFERENCES shm_cpu(id)
);

CREATE TABLE shm_cpu (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    parent_id INTEGER,
    idx INTEGER NOT NULL,
    registers_size INTEGER NOT NULL
);

CREATE TABLE tb_insn_info (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tb_info_id INTEGER,
    pc INTEGER NOT NULL,
    size INTEGER NOT NULL,
    symbol TEXT NOT NULL,
    hwaddr TEXT NOT NULL,
    disas TEXT NOT NULL,
    data_size INTEGER NOT NULL,
    data BLOB NOT NULL,
    FOREIGN KEY (tb_info_id) REFERENCES tb_info(id)
);

CREATE TABLE tb_info (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    pc INTEGER NOT NULL,
    insns_info_size INTEGER NOT NULL
);

CREATE TABLE broker_shm_tb (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    init_mask INTEGER NOT NULL,
    tb_info_id INTEGER NOT NULL,
    FOREIGN KEY (tb_info_id) REFERENCES tb_info(id)
);

CREATE TABLE broker_shm_exec (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    init_mask INTEGER NOT NULL,
    insn_info_id INTEGER NOT NULL,
    FOREIGN KEY (insn_info_id) REFERENCES tb_insn_info(id)
);

