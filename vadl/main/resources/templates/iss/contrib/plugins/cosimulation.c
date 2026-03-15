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
#include <pthread.h>
#include <qemu-plugin.h>
#include <semaphore.h>
#include <stdatomic.h>
#include <stddef.h>
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
#define TBINSNINFO_ENTRIES 64

#define MAX_REGISTER_NAME_SIZE 64
#define MAX_REGISTER_DATA_SIZE 256
#define MAX_CPU_REGISTERS 512
#define MAX_CPU_COUNT 1
#define MAX_INSN_DATA_SIZE 64

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
} BrokerSHMTB;

typedef struct {
  uint64_t vaddr;
  // the size of the memory load / store in ^2: 0 = 1 byte, 1 = 2 bytes, ..., 4
  // = 16 bytes
  uint8_t size;
  // the amount written to the data-array depends on the size
  uint8_t data[16];
} MemAccessInfo;

typedef enum {
  INSN_EXEC = 0,
  INSN_MEM = 1 << 0,
} BrokerSHMInsnDataType;

typedef struct {
  int init_mask;
  BrokerSHMInsnDataType insn_data_type;
  SHMCPU cpus[MAX_CPU_COUNT];
  TBInsnInfo insn_info;
  MemAccessInfo mem_access_info;
} BrokerSHMInsn;

typedef union {
  BrokerSHMTB shm_tb;
  BrokerSHMInsn shm_insn;
} BrokerSHMData;

typedef struct {
  pthread_mutex_t mutex;
  pthread_cond_t cvar;
} Semaphore;

#define RING_BUFFER_SIZE 4
#define RING_BUFFER_MASK (RING_BUFFER_SIZE - 1)

typedef struct {
  BrokerSHMData data[RING_BUFFER_SIZE];
  size_t read_idx;
  size_t write_idx;
  atomic_size_t count;
  atomic_bool write_end;
  Semaphore notifier;
} BrokerSHMRingBuffer;

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

static Arguments args;

static BrokerSHMRingBuffer *shm_ring_buffer;

#define ringbuf_idx(idx) ((idx) & RING_BUFFER_MASK)

static void ringbuf_write(BrokerSHMData data) {
  size_t count = atomic_load(&shm_ring_buffer->count);

  // The buffer keeps the previous value in reserve in case a diff-context needs
  // to be built.
  if (count == RING_BUFFER_SIZE - 1) {
    pthread_mutex_lock(&shm_ring_buffer->notifier.mutex);
    while (atomic_load(&shm_ring_buffer->count) == RING_BUFFER_SIZE - 1) {
      pthread_cond_wait(&shm_ring_buffer->notifier.cvar,
                        &shm_ring_buffer->notifier.mutex);
    }
    pthread_mutex_unlock(&shm_ring_buffer->notifier.mutex);
  }

  shm_ring_buffer->data[ringbuf_idx(shm_ring_buffer->write_idx)] = data;
  shm_ring_buffer->write_idx++;

  atomic_fetch_add(&shm_ring_buffer->count, 1);

  pthread_cond_signal(&shm_ring_buffer->notifier.cvar);
}

static CPU *get_cpu(int vcpu_index) {
  CPU *c;
  c = &g_array_index(cpus, CPU, vcpu_index);
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
  CPU *c = get_cpu(cpu_index);

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
    PLUGIN_ASSERT(shm_reg.size != -1,
                  "failed to read size of register at idx: %d", reg_idx);

    if (reg->name != NULL) {
      strncpy(shm_reg.name.value, reg->name, SHMSTRING_MAX_LEN - 1);
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

static void plugin_exit(qemu_plugin_id_t id, void *p) {
  PLUGIN_PRINTLN("plugin_exit");
  atomic_store(&shm_ring_buffer->write_end, true);
  pthread_cond_broadcast(&shm_ring_buffer->notifier.cvar);
}

// Connects to the broker by accessing the assigned shared memory
// The shared memory is located under /cosimulation/shm-{client_id}
static BrokerSHMRingBuffer *connect_to_broker_data(void) {
  gchar *shm_name = g_strdup_printf("/cosimulation-shm-%s", args.client_id);
  int shm_fd = shm_open(shm_name, O_RDWR, 0600);
  if (shm_fd == -1) {
    char *err = strerror(errno);
    g_error("failed to open shared memory for client: %s -> %s", args.client_id,
            err);
    return NULL;
  }

  if (ftruncate(shm_fd, sizeof(BrokerSHMRingBuffer)) == -1) {
    char *err = strerror(errno);
    g_error("failed to truncate shared memory for client: %s -> %s",
            args.client_id, err);
    return NULL;
  }

  BrokerSHMRingBuffer *shm_ring_buffer =
      mmap(NULL, sizeof(BrokerSHMRingBuffer), PROT_READ | PROT_WRITE,
           MAP_SHARED, shm_fd, 0);

  if (shm_ring_buffer == MAP_FAILED) {
    char *err = strerror(errno);
    g_error("failed to mmap shared memory for client: %s -> %s", args.client_id,
            err);
    return NULL;
  }

  return shm_ring_buffer;
}

static TBInsnInfo get_tbinsn_info(struct qemu_plugin_insn *insn) {
  TBInsnInfo insn_info = {0};
  insn_info.pc = qemu_plugin_insn_vaddr(insn);
  insn_info.size = qemu_plugin_insn_size(insn);

  const char *insn_symbol = qemu_plugin_insn_symbol(insn);
  if (insn_symbol != NULL) {
    strncpy(insn_info.symbol.value, insn_symbol, SHMSTRING_MAX_LEN - 1);
    insn_info.symbol.len = strlen(insn_info.symbol.value);
  }

  void *insn_hwaddr = qemu_plugin_insn_haddr(insn);
  if (insn_hwaddr != NULL) {
    char *hwaddrfmt = g_strdup_printf("%p", insn_hwaddr);
    strncpy(insn_info.hwaddr.value, hwaddrfmt, SHMSTRING_MAX_LEN - 1);
    insn_info.hwaddr.len = strlen(insn_info.hwaddr.value);
  }

  char *insn_disas = qemu_plugin_insn_disas(insn);
  if (insn_disas != NULL) {
    strncpy(insn_info.disas.value, insn_disas, SHMSTRING_MAX_LEN - 1);
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

  TBInfo tbinfo = {0};
  tbinfo.pc = pc;

  PLUGIN_ASSERT(insns <= TBINSNINFO_ENTRIES,
                "Too many instructions in a single translation-block: %lu > %d",
                insns, TBINSNINFO_ENTRIES);
  for (int i = 0; i < insns; i++) {
    struct qemu_plugin_insn *insn = qemu_plugin_tb_get_insn(tb, i);
    PLUGIN_ASSERT(insn != NULL, "insn must not be null: %d", i);
    tbinfo.insns_info[i] = get_tbinsn_info(insn);
  }

  tbinfo.insns_info_size = insns;

  return tbinfo;
}

static BrokerSHMData combined_mem_data = {0};

inline static bool is_combined_mem_data_set(void) {
  return combined_mem_data.shm_insn.insn_data_type == INSN_MEM;
}

inline static void clear_combined_mem_data(void) {
  combined_mem_data = (const BrokerSHMData){0};
}

inline static void write_combined_mem_data(void) {
  ringbuf_write(combined_mem_data);
  clear_combined_mem_data();
}

static void vcpu_insn_exec(unsigned int cpu_index, void *udata) {
  if (is_combined_mem_data_set()) {
    write_combined_mem_data();
  }

  TBInsnInfo *tbinsn_info = udata;

  SHMCPU cpu = get_cpu_state(cpu_index);

  BrokerSHMData shm = {0};
  shm.shm_insn.insn_data_type = INSN_EXEC;
  shm.shm_insn.cpus[cpu_index] = cpu;

  // TODO: needs a global init_mask to keep track of current state
  // NOTE: rather: refactor to just assign the cpu_index
  shm.shm_insn.init_mask |= (1 << cpu_index);
  shm.shm_insn.insn_info = *tbinsn_info;

  ringbuf_write(shm);

  // TODO: we cannot free here because the same callback might be used multiple
  // times when a tb gets reused g_free(tbinsn_info);
}

inline static bool is_consecutive_memory_region(uint64_t addr1,
                                                uint8_t addr1_size,
                                                uint64_t addr2) {
  return addr1 + (1 << addr1_size) == addr2;
}

static void vcpu_mem_cb(unsigned int cpu_index, qemu_plugin_meminfo_t info,
                        uint64_t vaddr, void *udata) {

  if (!is_combined_mem_data_set()) {
    TBInsnInfo *tbinsn_info = udata;
    BrokerSHMData shm = {0};

    shm.shm_insn.insn_data_type = INSN_MEM;
    shm.shm_insn.mem_access_info.vaddr = vaddr;

    qemu_plugin_mem_value data = qemu_plugin_mem_get_value(info);

    shm.shm_insn.mem_access_info.size = data.type;
    memcpy(&shm.shm_insn.mem_access_info.data, &data.data, 1 << data.type);
    shm.shm_insn.insn_info = *tbinsn_info;

    combined_mem_data = shm;
  } else if (is_consecutive_memory_region(
                 combined_mem_data.shm_insn.mem_access_info.vaddr,
                 combined_mem_data.shm_insn.mem_access_info.size, vaddr)) {
    qemu_plugin_mem_value data = qemu_plugin_mem_get_value(info);
    
    // NOTE: only consecutive memory access of the same size is supported
    //       because the size has to be a power of two
    //       This also means that e.g. 4 1byte accesses cannot currently be grouped by this analysis
    if(data.type != combined_mem_data.shm_insn.mem_access_info.size){
      write_combined_mem_data();
      vcpu_mem_cb(cpu_index, info, vaddr, udata);
      return;
    }

    uint8_t data_offset =
        (1 << combined_mem_data.shm_insn.mem_access_info.size);
    memcpy(combined_mem_data.shm_insn.mem_access_info.data + data_offset,
           &data.data, 1 << data.type);
    combined_mem_data.shm_insn.mem_access_info.size++;
  } else {
    write_combined_mem_data();
    vcpu_mem_cb(cpu_index, info, vaddr, udata);
  }
}

static TBInfo tb_info_collect = {0};
static int64_t insns_sum_collect = 0;

// if the start-pc + the offset of the executed instructions does not equal
// the new pc, then a jump has occurred
inline static bool is_jump(TBInfo *tb_info) {
  return tb_info_collect.pc + insns_sum_collect != tb_info->pc;
}

static void vcpu_tb_exec(unsigned int cpu_index, void *udata) {
  TBInfo *tb_info = udata;

  for (int i = 0; i < tb_info->insns_info_size; i++) {
    insns_sum_collect += tb_info->insns_info[i].size;
  }

  memcpy(&tb_info_collect.insns_info + tb_info_collect.insns_info_size,
         tb_info->insns_info, sizeof(TBInsnInfo) * tb_info->insns_info_size);
  tb_info_collect.insns_info_size += tb_info->insns_info_size;

  // TB-Data is only returned (= written to the buffer) if a jump occurred,
  // otherwise the data is simply collected on the qemu-client
  if (is_jump(tb_info)) {
    SHMCPU cpu = get_cpu_state(cpu_index);
    BrokerSHMData shm = {0};

    shm.shm_tb.cpus[cpu_index] = cpu;
    // TODO: needs a global init_mask to keep track of current state
    // NOTE: rather: refactor to just assign the cpu_index
    shm.shm_tb.init_mask |= (1 << cpu_index);
    shm.shm_tb.tb_info = tb_info_collect;

    tb_info_collect.pc = 0;
    tb_info_collect.insns_info_size = 0;

    ringbuf_write(shm);
  }

  // TODO: we cannot free here because the same callback might be used multiple
  // times when a tb gets reused g_free(tb_info);
}

static void vcpu_tb_trans(qemu_plugin_id_t id, struct qemu_plugin_tb *tb) {
  if (args.mode == TB_MODE) {
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
      qemu_plugin_register_vcpu_mem_cb(insn, vcpu_mem_cb,
                                       QEMU_PLUGIN_CB_NO_REGS,
                                       QEMU_PLUGIN_MEM_RW, tbinsn_info);
    }
  }
}

static void vcpu_init(qemu_plugin_id_t id, unsigned int vcpu_index) {
  PLUGIN_ASSERT(vcpu_index < MAX_CPU_COUNT,
                "A CPU with vcpu_index larger than MAX_CPU_COUNT was "
                "initialized: %d (idx) >= %d (max-len)",
                vcpu_index, MAX_CPU_COUNT);
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
  cpus = g_array_sized_new(true, true, sizeof(CPU), MAX_CPU_COUNT);

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

  shm_ring_buffer = connect_to_broker_data();
  if (shm_ring_buffer == NULL) {
    return EXIT_FAILURE;
  }

  tb_info_collect.pc = 0;
  tb_info_collect.insns_info_size = 0;

  plugin_id = id;

  qemu_plugin_register_vcpu_tb_trans_cb(id, vcpu_tb_trans);
  qemu_plugin_register_vcpu_init_cb(id, vcpu_init);
  qemu_plugin_register_vcpu_exit_cb(id, vcpu_exit);
  qemu_plugin_register_atexit_cb(id, plugin_exit, NULL);
  return EXIT_SUCCESS;
}
