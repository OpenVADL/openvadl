// References:
// other plugins:
// https://gitlab.com/qemu-project/qemu/-/blob/master/contrib/plugins

#include <assert.h>
#include <fcntl.h>
#include <gio/gio.h>
#include <glib.h>
#include <qemu-plugin.h>
#include <semaphore.h>
#include <stdio.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/socket.h>
#include <time.h>
#include <unistd.h>

QEMU_PLUGIN_EXPORT int qemu_plugin_version = QEMU_PLUGIN_VERSION;

// adjust as needed
#define SHMSTRING_MAX_LEN 256
#define TBINFO_ENTRIES 1024
#define TBINSNINFO_ENTRIES 32

#define MAX_REGISTER_NAME_SIZE 64
#define MAX_REGISTER_DATA_SIZE 64
#define MAX_CPU_REGISTERS 256
#define MAX_CPU_COUNT 8

static qemu_plugin_id_t plugin_id;

typedef struct {
  size_t len;
  char value[SHMSTRING_MAX_LEN];
} SHMString;

typedef struct {
  struct qemu_plugin_register *handle;
  const char *name;
  const char *feature;
} Register;

typedef struct {
  GPtrArray *registers;
} CPU;

typedef struct {
  int size;
  uint8_t data[MAX_REGISTER_DATA_SIZE];
  SHMString name;
} SHMRegister;

typedef struct {
  unsigned int idx;
  size_t registers_size;
  SHMRegister registers[MAX_CPU_REGISTERS];
} SHMCPU;

typedef struct {
  uint64_t pc;
  size_t size;
  SHMString symbol;
  SHMString hwaddr;
  SHMString disas;
} TBInsnInfo;

typedef struct {
  uint64_t pc;
  size_t insns;
  size_t insns_info_size;
  TBInsnInfo insns_info[TBINSNINFO_ENTRIES];
} TBInfo;

typedef enum {
  INVALID_MODE = 0,
  TB_MODE = 1,
  EXEC_MODE = 2,
} ExecMode;

typedef struct {
  const gchar *client_id;
  ExecMode mode;
} Arguments;

typedef struct {
  size_t size;
  TBInfo infos[TBINFO_ENTRIES];
} BrokerSHM_TB;

typedef struct {
  // if bit at cpu_idx = 1 then data is set
  int init_mask;
  SHMCPU cpus[MAX_CPU_COUNT];
} BrokerSHM_Exec;

typedef union {
  BrokerSHM_TB shm_tb;
  BrokerSHM_Exec shm_exec;
} BrokerSHM;

static GArray *cpus;
static GRWLock cpus_lock;

static Arguments args;
static BrokerSHM *shm;
static sem_t *sem_client, *sem_server;

static CPU *get_cpu(int vcpu_index) {
  CPU *c;
  g_rw_lock_reader_lock(&cpus_lock);
  c = &g_array_index(cpus, CPU, vcpu_index);
  g_rw_lock_reader_unlock(&cpus_lock);

  return c;
}

static GPtrArray *registers_init(int vcpu_index) {
  GPtrArray *registers = g_ptr_array_new();
  g_autoptr(GArray) reg_list = qemu_plugin_get_registers();

  for (int r = 0; r < reg_list->len; r++) {
    qemu_plugin_reg_descriptor *rd =
        &g_array_index(reg_list, qemu_plugin_reg_descriptor, r);
    Register *reg = g_new0(Register, 1);
    reg->handle = rd->handle;
    reg->feature = rd->feature;
    reg->name = rd->name;
    g_ptr_array_add(registers, (gpointer)reg);
  }

  if (registers->len == 0) {
    g_ptr_array_free(registers, TRUE);
    return NULL;
  }

  return registers;
}

static SHMCPU get_cpu_state(unsigned int cpu_index) {
  g_rw_lock_reader_lock(&cpus_lock);
  CPU *c = get_cpu(cpu_index);
  g_rw_lock_reader_unlock(&cpus_lock);

  SHMCPU shm_cpu = {};
  shm_cpu.idx = cpu_index;
  shm_cpu.registers_size = c->registers->len;

  // NOTE: The register-count for each cpu is checked once at init. See:
  // vcpu_init
  for (int reg_idx = 0; reg_idx < c->registers->len; reg_idx++) {
    Register *reg = c->registers->pdata[reg_idx];
    SHMRegister shm_reg = {};
    GByteArray *buf = g_byte_array_new();

    shm_reg.size = qemu_plugin_read_register(reg->handle, buf);

    if (reg->name != NULL) {
      strncpy(shm_reg.name.value, reg->name, SHMSTRING_MAX_LEN);
      shm_reg.name.len = strlen(shm_reg.name.value);
    }

    if (buf->data != NULL) {
      memcpy(shm_reg.data, buf->data, shm_reg.size);
    }

    g_free(buf);

    shm_cpu.registers[reg_idx] = shm_reg;
  }

  return shm_cpu;
};

static void open_sems(void) {
  gchar *sem_client_name =
      g_strdup_printf("cosimulation-sem-client-%s", args.client_id);
  sem_client = sem_open(sem_client_name, O_RDWR);
  if (sem_client == SEM_FAILED) {
    g_error("failed to open sem_client for client: %s", args.client_id);
    return;
  }

  gchar *sem_server_name =
      g_strdup_printf("cosimulation-sem-server-%s", args.client_id);
  sem_server = sem_open(sem_server_name, O_RDWR);
  if (sem_server == SEM_FAILED) {
    g_error("failed to open sem_server for client: %s", args.client_id);
    return;
  }
}

static void plugin_exit(qemu_plugin_id_t id, void *p) {
  printf("plugin_exit\n");
}

// Connects to the broker by accessing the assigned shared memory
// The shared memory is located under /cosimulation/shm-{client_id}
static BrokerSHM *connect_to_broker(void) {
  gchar *shm_name = g_strdup_printf("/cosimulation-shm-%s", args.client_id);
  int shm_fd = shm_open(shm_name, O_RDWR, 0600);
  if (shm_fd == -1) {
    g_error("failed to open shared memory for client: %s", args.client_id);
    return NULL;
  }

  if (ftruncate(shm_fd, sizeof(BrokerSHM)) == -1) {
    g_error("failed to truncate shared memory for client: %s", args.client_id);
    return NULL;
  }

  BrokerSHM *shm = mmap(NULL, sizeof(BrokerSHM), PROT_READ | PROT_WRITE,
                        MAP_SHARED, shm_fd, 0);
  if (shm == MAP_FAILED) {
    g_error("failed to mmap jshared memory for client: %s", args.client_id);
    return NULL;
  }

  return shm;
}

static TBInfo get_tb_info(struct qemu_plugin_tb *tb) {
  uint64_t pc = qemu_plugin_tb_vaddr(tb);
  size_t insns = qemu_plugin_tb_n_insns(tb);

  TBInfo tbinfo;
  tbinfo.pc = pc;
  tbinfo.insns = insns;

  // TODO: check size of tbinfo.insns > insns
  for (int i = 0; i < insns; i++) {
    struct qemu_plugin_insn *insn = qemu_plugin_tb_get_insn(tb, i);

    TBInsnInfo insn_info = {};
    insn_info.pc = qemu_plugin_insn_vaddr(insn);
    insn_info.size = qemu_plugin_insn_size(insn);

    const char *insn_symbol = qemu_plugin_insn_symbol(insn);
    if (insn_symbol != NULL) {
      strncpy(insn_info.symbol.value, insn_symbol, SHMSTRING_MAX_LEN);
      insn_info.symbol.len = strlen(insn_info.symbol.value);
    }

    void *insn_hwaddr = qemu_plugin_insn_haddr(insn);
    if (insn_hwaddr != NULL) {
      char *hwaddrfmt = g_strdup_printf("%p", insn_hwaddr);
      strncpy(insn_info.hwaddr.value, hwaddrfmt, SHMSTRING_MAX_LEN);
      insn_info.hwaddr.len = strlen(insn_info.hwaddr.value);
    }

    char *insn_disas = qemu_plugin_insn_disas(insn);
    if (insn_disas != NULL) {
      strncpy(insn_info.disas.value, insn_disas, SHMSTRING_MAX_LEN);
      insn_info.disas.len = strlen(insn_info.disas.value);
    }

    tbinfo.insns_info[i] = insn_info;
  }

  tbinfo.insns_info_size = insns;
  return tbinfo;
}

static void vcpu_tb_exec(unsigned int cpu_index, void *udata) {
  if (args.mode == EXEC_MODE) {
    sem_wait(sem_client);

    // struct qemu_plugin_tb *tb = udata;
    // TBInfo tbinfo = get_tb_info(tb);
    SHMCPU cpu = get_cpu_state(cpu_index);

    shm->shm_exec.cpus[cpu_index] = cpu;
    shm->shm_exec.init_mask |= (1 << cpu_index);

    sem_post(sem_server);
  }
}

static void vcpu_tb_trans(qemu_plugin_id_t id, struct qemu_plugin_tb *tb) {
  if (args.mode == TB_MODE) {
    sem_wait(sem_client);

    shm->shm_tb.infos[shm->shm_tb.size] = get_tb_info(tb);
    shm->shm_tb.size = shm->shm_tb.size + 1;

    sem_post(sem_server);
  } else if (args.mode == EXEC_MODE) {
    qemu_plugin_register_vcpu_tb_exec_cb(tb, vcpu_tb_exec,
                                         QEMU_PLUGIN_CB_R_REGS, NULL);
  }
}

static void vcpu_init(qemu_plugin_id_t id, unsigned int vcpu_index) {
  g_rw_lock_writer_lock(&cpus_lock);
  if (vcpu_index >= cpus->len) {
    g_array_set_size(cpus, vcpu_index + 1);
  }
  g_rw_lock_writer_unlock(&cpus_lock);

  CPU *c = get_cpu(vcpu_index);
  c->registers = registers_init(vcpu_index);
  if (c->registers->len >= MAX_CPU_REGISTERS) {
    printf("Invalid plugin state: Running on a CPU with more than %d "
           "registers: register-count: %d",
           MAX_CPU_REGISTERS, c->registers->len);
    exit(EXIT_FAILURE);
  } 
}

static void vcpu_exit(qemu_plugin_id_t id, unsigned int vcpu_index) {
  printf("vcpu exiting: %d...\n", vcpu_index);
  fflush(stdout);
}

static ExecMode parse_mode(const char *mode_str) {
  if (g_strcmp0(mode_str, "tb")) {
    return EXEC_MODE;
  } else if (g_strcmp0(mode_str, "execb")) {
    return TB_MODE;
  } else {
    return INVALID_MODE;
  }
}

QEMU_PLUGIN_EXPORT int qemu_plugin_install(qemu_plugin_id_t id,
                                           const qemu_info_t *info, int argc,
                                           char **argv) {
  printf("::qemu_plugin_install\n");

  cpus = g_array_sized_new(true, true, sizeof(CPU),
                           info->system_emulation ? info->system.max_vcpus : 1);

  // parse options
  for (int i = 0; i < argc; i++) {
    char *p = argv[i];
    g_auto(GStrv) tokens = g_strsplit(p, "=", 2);
    const char *argname = tokens[0];
    const char *argvalue = tokens[1];
    if (g_strcmp0(argname, "client-id") == 0) {
      args.client_id = strdup(argvalue);
    } else if (g_strcmp0(argname, "mode") == 0) {
      args.mode = parse_mode(argvalue);
      printf("running in mode: %d\n", args.mode);
    } else {
      fprintf(stderr, "option parsing failed: %s\n", p);
      return EXIT_FAILURE;
    }
  }

  // check required options
  if (args.client_id == NULL) {
    fprintf(stderr,
            "option client-id=<gchar*> is required, no client-id was given\n");
    return EXIT_FAILURE;
  }

  if (args.mode == INVALID_MODE) {
    fprintf(stderr, "invalid or missing execution mode, option mode=<ExecMode> "
                    "is required");
    return EXIT_FAILURE;
  }

  shm = connect_to_broker();
  if (shm == NULL) {
    return EXIT_FAILURE;
  }

  open_sems();
  if (sem_client == NULL || sem_server == NULL) {
    return EXIT_FAILURE;
  }

  if (args.mode == EXEC_MODE) {
    shm->shm_exec.init_mask = 0;
  }

  plugin_id = id;

  qemu_plugin_register_vcpu_tb_trans_cb(id, vcpu_tb_trans);
  qemu_plugin_register_vcpu_init_cb(id, vcpu_init);
  qemu_plugin_register_vcpu_exit_cb(id, vcpu_exit);
  qemu_plugin_register_atexit_cb(id, plugin_exit, NULL);
  return EXIT_SUCCESS;
}
