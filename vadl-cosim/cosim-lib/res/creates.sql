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
    idx INTEGER NOT NULL,
	broker_shm_id INTEGER NOT NULL,
	FOREIGN KEY (broker_shm_id) REFERENCES broker_shm(id)
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
    pc INTEGER NOT NULL
);

CREATE TABLE broker_shm_tb (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tb_info_id INTEGER NOT NULL,
    FOREIGN KEY (id) REFERENCES broker_shm(id),
    FOREIGN KEY (tb_info_id) REFERENCES tb_info(id)
);

CREATE TABLE broker_shm_insn (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    insn_info_id INTEGER NOT NULL,
    FOREIGN KEY (id) REFERENCES broker_shm(id),
    FOREIGN KEY (insn_info_id) REFERENCES tb_insn_info(id)
);

CREATE TABLE broker_shm (
	id INTEGER PRIMARY KEY AUTOINCREMENT,
    init_mask INTEGER NOT NULL,
	-- shm_type: 0 = tb, 1 = insn
	shm_type INTEGER CHECK ( shm_type IN (0, 1) )
);

CREATE TABLE client (
	id INTEGER PRIMARY KEY,
	name TEXT NULL
);

CREATE TABLE cosimulation_run (
	id INTEGER PRIMARY KEY AUTOINCREMENT,
    start DATETIME DEFAULT CURRENT_TIMESTAMP,
	end DATETIME DEFAULT NULL,
	passed BOOLEAN -- NULL means not finished yet
);

CREATE TABLE cosimulation_run_clients (
	run_id INTEGER,
	client_id INTEGER,

	PRIMARY KEY (run_id, client_id),
	FOREIGN KEY (run_id) REFERENCES cosimulation_run(id),
	FOREIGN KEY (client_id) REFERENCES client(id)
);

CREATE TABLE client_entry (
	id INTEGER PRIMARY KEY AUTOINCREMENT,
	client_id INTEGER NOT NULL,
	broker_shm_id INTEGER NOT NULL,
	run_count INTEGER NOT NULL,
	FOREIGN KEY (client_id) REFERENCES client(id),
	FOREIGN KEY (broker_shm_id) REFERENCES broker_shm(id)
);
