// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
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
// NOTE: try to keep these values as small as possible to minimize memory usage
//       if set too small then crashes and/or invalid state can occur
#define SHMSTRING_MAX_LEN 256
#define TBINFO_ENTRIES 1024
#define TBINSNINFO_ENTRIES 32

#define MAX_REGISTER_NAME_SIZE 64
#define MAX_REGISTER_DATA_SIZE 64
#define MAX_CPU_REGISTERS 256
#define MAX_CPU_COUNT 8
#define MAX_INSN_DATA_SIZE 4

static qemu_plugin_id_t plugin_id;

#define PLUGIN_PRINT(format, ...)                                              \
  do {                                                                         \
    if (args.client_name_set) {                                                \
      gchar *_tmp_str = g_strdup_printf(                                       \
          "[LOG: plugin-id=%lu, client=%s(id=%s), %s:%d] " format, plugin_id,  \
          args.client_name, args.client_id,                                    \
          strrchr(__FILE__, '/') ? strrchr(__FILE__, '/') + 1 : __FILE__,      \
          __LINE__, ##__VA_ARGS__);                                            \
      qemu_plugin_outs(_tmp_str);                                              \
      g_free(_tmp_str);                                                        \
    } else {                                                                   \
      gchar *_tmp_str = g_strdup_printf(                                       \
          "[LOG: plugin-id=%lu, client=(id=%s), %s:%d] " format, plugin_id,    \
          args.client_id,                                                      \
          strrchr(__FILE__, '/') ? strrchr(__FILE__, '/') + 1 : __FILE__,      \
          __LINE__, ##__VA_ARGS__);                                            \
      qemu_plugin_outs(_tmp_str);                                              \
      g_free(_tmp_str);                                                        \
    }                                                                          \
                                                                               \
  } while (0)

#define PLUGIN_PRINTLN(format, ...) PLUGIN_PRINT(format "\n", ##__VA_ARGS__)

#define PLUGIN_ASSERT(cond, format, ...)                                       \
  do {                                                                         \
    if (!(cond)) {                                                             \
      PLUGIN_PRINTLN("Invalid plugin state: %s :: " format, #cond,             \
                     ##__VA_ARGS__);                                           \
      exit(EXIT_FAILURE);                                                      \
    }                                                                          \
  } while (0)

typedef struct {
  struct qemu_plugin_register *handle;
  const char *name;
  const char *feature;
} Register;

typedef struct {
  GPtrArray *registers;
} CPU;

typedef struct {
  size_t len;
  char value[SHMSTRING_MAX_LEN];
} SHMString;

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
  size_t size;
  uint8_t buffer[MAX_INSN_DATA_SIZE];
} InsnData;

typedef struct {
  uint64_t pc;
  size_t size;
  SHMString symbol;
  SHMString hwaddr;
  SHMString disas;
  InsnData data;
} TBInsnInfo;

typedef struct {
  uint64_t pc;
  size_t insns_info_size;
  TBInsnInfo insns_info[TBINSNINFO_ENTRIES];
} TBInfo;

// if bit at cpu_idx = 1 then data is set
typedef struct {
  int init_mask;
  SHMCPU cpus[MAX_CPU_COUNT];
  TBInfo tb_info;
} BrokerSHM_TB;

typedef struct {
  int init_mask;
  SHMCPU cpus[MAX_CPU_COUNT];
  TBInsnInfo insn_info;
} BrokerSHM_Exec;

typedef union {
  BrokerSHM_TB shm_tb;
  BrokerSHM_Exec shm_exec;
} BrokerSHM;

typedef enum {
  INVALID_MODE = 0,
  TB_MODE = 1,
  INSN_MODE = 2,
} ExecMode;

typedef struct {
  const gchar *client_id;
  ExecMode mode;
  const gchar *client_name;
  gboolean client_name_set;
} Arguments;

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

    g_byte_array_unref(buf);

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
  PLUGIN_PRINTLN("plugin_exit");
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
    g_error("failed to mmap shared memory for client: %s", args.client_id);
    return NULL;
  }

  return shm;
}

static TBInsnInfo get_tbinsn_info(struct qemu_plugin_insn *insn) {
  TBInsnInfo insn_info = {0};
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

  insn_info.data.size = qemu_plugin_insn_size(insn);
  PLUGIN_ASSERT(insn_info.data.size <= MAX_INSN_DATA_SIZE,
                "Some instruction-data had a larger size than configured in "
                "MAX_INSN_DATA_SIZE: %lu > %d",
                insn_info.data.size, MAX_INSN_DATA_SIZE);

  qemu_plugin_insn_data(insn, &insn_info.data.buffer,
                        sizeof(insn_info.data.buffer));

  return insn_info;
}

static TBInfo get_tb_info(struct qemu_plugin_tb *tb) {
  uint64_t pc = qemu_plugin_tb_vaddr(tb);
  size_t insns = qemu_plugin_tb_n_insns(tb);

  TBInfo tbinfo;
  tbinfo.pc = pc;

  PLUGIN_ASSERT(insns <= TBINSNINFO_ENTRIES,
                "Too many instructions in a single translation-block: %lu > %d",
                insns, TBINSNINFO_ENTRIES);
  for (int i = 0; i < insns; i++) {
    struct qemu_plugin_insn *insn = qemu_plugin_tb_get_insn(tb, i);
    tbinfo.insns_info[i] = get_tbinsn_info(insn);
  }

  tbinfo.insns_info_size = insns;
  return tbinfo;
}

static void vcpu_insn_exec(unsigned int cpu_index, void *udata) {
  sem_wait(sem_client);

  SHMCPU cpu = get_cpu_state(cpu_index);

  shm->shm_exec.cpus[cpu_index] = cpu;
  shm->shm_exec.init_mask |= (1 << cpu_index);

  TBInsnInfo *tbinsn_info = udata;
  shm->shm_exec.insn_info = *tbinsn_info;
  g_free(tbinsn_info);

  sem_post(sem_server);
}

static void vcpu_tb_exec(unsigned int cpu_index, void *udata) {
  sem_wait(sem_client);

  SHMCPU cpu = get_cpu_state(cpu_index);

  shm->shm_tb.cpus[cpu_index] = cpu;
  shm->shm_tb.init_mask |= (1 << cpu_index);

  TBInfo *tb_info = udata;
  shm->shm_tb.tb_info = *tb_info ;
  g_free(tb_info);

  sem_post(sem_server);
}

static void vcpu_tb_trans(qemu_plugin_id_t id, struct qemu_plugin_tb *tb) {
  if (args.mode == TB_MODE) {
    // TODO: move TB info collection to qemu_plugin_register_vcpu_tb_exec_cb,
    // this function should just manage the cb based on the modes

    TBInfo *tbinfo = g_new0(TBInfo, 1);
    *tbinfo = get_tb_info(tb);
    qemu_plugin_register_vcpu_tb_exec_cb(tb, vcpu_tb_exec,
                                         QEMU_PLUGIN_CB_R_REGS, tbinfo);
  } else if (args.mode == INSN_MODE) {
    size_t insns = qemu_plugin_tb_n_insns(tb);
    for (int i = 0; i < insns; i++) {
      struct qemu_plugin_insn *insn = qemu_plugin_tb_get_insn(tb, i);
      TBInsnInfo *tbinsn_info = g_new0(TBInsnInfo, 1);
      *tbinsn_info = get_tbinsn_info(insn);
      qemu_plugin_register_vcpu_insn_exec_cb(
          insn, vcpu_insn_exec, QEMU_PLUGIN_CB_R_REGS, tbinsn_info);
    }
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
  PLUGIN_ASSERT(
      c->registers->len <= MAX_CPU_REGISTERS,
      "Running on a CPU with more than %d registers: register-count: %d",
      MAX_CPU_REGISTERS, c->registers->len);
}

static void vcpu_exit(qemu_plugin_id_t id, unsigned int vcpu_index) {
  PLUGIN_PRINTLN("vcpu exiting: %d...", vcpu_index);
  fflush(stdout);
}

static ExecMode parse_mode(const char *mode_str) {
  if (g_strcmp0(mode_str, "tb") == 0) {
    return TB_MODE;
  } else if (g_strcmp0(mode_str, "insn") == 0) {
    return INSN_MODE;
  } else {
    return INVALID_MODE;
  }
}

QEMU_PLUGIN_EXPORT int qemu_plugin_install(qemu_plugin_id_t id,
                                           const qemu_info_t *info, int argc,
                                           char **argv) {
  cpus = g_array_sized_new(true, true, sizeof(CPU),
                           info->system_emulation ? info->system.max_vcpus : 1);

  args.client_name_set = false;

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
      PLUGIN_PRINTLN("running in mode: %d", args.mode);
    } else if (g_strcmp0(argname, "client-name") == 0) {
      PLUGIN_ASSERT(!args.client_name_set,
                    "illegally set client-name multiple times");
      args.client_name_set = true;
      args.client_name = strdup(argvalue);
    } else {
      PLUGIN_PRINTLN("option parsing failed: %s", p);
      return EXIT_FAILURE;
    }
  }

  // check required options
  if (args.client_id == NULL) {
    PLUGIN_PRINTLN(
        "option client-id=<gchar*> is required, no client-id was given");
    return EXIT_FAILURE;
  }

  if (args.mode == INVALID_MODE) {
    PLUGIN_PRINTLN("invalid or missing execution mode, option mode=<ExecMode> "
                   "is required");
    return EXIT_FAILURE;
  }

  PLUGIN_PRINTLN("::qemu_plugin_install");

  shm = connect_to_broker();
  if (shm == NULL) {
    return EXIT_FAILURE;
  }

  open_sems();
  if (sem_client == NULL || sem_server == NULL) {
    return EXIT_FAILURE;
  }

  if (args.mode == INSN_MODE) {
    shm->shm_exec.init_mask = 0;
  }

  plugin_id = id;

  qemu_plugin_register_vcpu_tb_trans_cb(id, vcpu_tb_trans);
  qemu_plugin_register_vcpu_init_cb(id, vcpu_init);
  qemu_plugin_register_vcpu_exit_cb(id, vcpu_exit);
  qemu_plugin_register_atexit_cb(id, plugin_exit, NULL);
  return EXIT_SUCCESS;
}
