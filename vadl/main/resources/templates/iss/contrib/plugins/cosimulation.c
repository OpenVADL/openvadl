#include "sys/shm.h"
#include <fcntl.h>
#include <gio/gio.h>
#include <glib.h>
#include <json-glib/json-glib.h>
#include <qemu-plugin.h>
#include <semaphore.h>
#include <stdio.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/socket.h>
#include <time.h>
#include <unistd.h>

QEMU_PLUGIN_EXPORT int qemu_plugin_version = QEMU_PLUGIN_VERSION;

#define SHMSTRING_MAX_LEN 256
#define TBINFO_ENTRIES 1024
#define TBINSNINFO_ENTRIES 32

static qemu_plugin_id_t plugin_id;
static unsigned long bb_count;
static unsigned long insn_count;

typedef struct {
  size_t len;
  char value[SHMSTRING_MAX_LEN];
} SHMString;

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

typedef struct {
  const gchar *client_id;
} Arguments;

typedef struct {
  size_t size;
  TBInfo infos[TBINFO_ENTRIES];
} BrokerSHM;

static Arguments args;
static BrokerSHM *shm;
static sem_t *sem_client, *sem_server;

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

static void vcpu_tb_exec(unsigned int cpu_index, void *udata) {
  // printf("vcpu exec\n");
  // GArray *regs = qemu_plugin_get_registers();
  // printf("in vcpu_tb_exec with: %d registers\n", regs->len);
  // for (size_t i = 0; i < regs->len; i++) {
  //   printf("starting to print reg: %lu\n", i);
  //   const qemu_plugin_reg_descriptor *desc = 
  //       g_array_index(regs, qemu_plugin_reg_descriptor *, i);
  //
  //   if()
  //   
  //   printf("starting to print reg\n");
  //   printf("ptr: %p\n", desc);
  //   printf("Register %s: ", desc->name);
  //
  //   // Read register value
  //   GByteArray *buf = g_byte_array_new();
  //   qemu_plugin_read_register(desc->handle, buf);
  //   printf("%lu\n", *(uint64_t *)buf->data);
  //   printf("done to print reg: %lu\n", i);
  // }
  //
  // g_array_free(regs, TRUE);
}

static void vcpu_tb_trans(qemu_plugin_id_t id, struct qemu_plugin_tb *tb) {
  sem_wait(sem_client);

  uint64_t pc = qemu_plugin_tb_vaddr(tb);
  size_t insns = qemu_plugin_tb_n_insns(tb);

  TBInfo tbinfo;
  tbinfo.pc = pc;
  tbinfo.insns = insns;

  for (int i = 0; i < insns; i++) {
    // qemu_plugin_insn_data();
    struct qemu_plugin_insn *insn = qemu_plugin_tb_get_insn(tb, i);
    uint64_t insn_pc = qemu_plugin_insn_vaddr(insn);
    size_t insn_size = qemu_plugin_insn_size(insn);
    const char *insn_symbol = qemu_plugin_insn_symbol(insn);
    void *insn_hwaddr = qemu_plugin_insn_haddr(insn);
    char *insn_disas = qemu_plugin_insn_disas(insn);

    TBInsnInfo insn_info;
    insn_info.pc = insn_pc;
    insn_info.size = insn_size;

    SHMString symbol = {};
    SHMString hwaddr = {};
    SHMString disas = {};

    if (insn_symbol != NULL) {
      strncpy(symbol.value, insn_symbol, SHMSTRING_MAX_LEN);
      symbol.len = strlen(symbol.value);
    }

    if (insn_hwaddr != NULL) {
      char *hwaddrfmt = g_strdup_printf("%p", insn_hwaddr);
      strncpy(hwaddr.value, hwaddrfmt, SHMSTRING_MAX_LEN);
      hwaddr.len = strlen(hwaddr.value);
    }

    if (insn_disas != NULL) {
      strncpy(disas.value, insn_disas, SHMSTRING_MAX_LEN);
      disas.len = strlen(disas.value);
    }

    insn_info.symbol = symbol;
    insn_info.hwaddr = hwaddr;
    insn_info.disas = disas;

    tbinfo.insns_info[i] = insn_info;
  }

  tbinfo.insns_info_size = insns;

  shm->infos[shm->size] = tbinfo;
  shm->size = shm->size + 1;

  qemu_plugin_register_vcpu_tb_exec_cb(tb, vcpu_tb_exec, QEMU_PLUGIN_CB_R_REGS,
                                       NULL);

  sem_post(sem_server);
}

static void vcpu_init(qemu_plugin_id_t id, unsigned int vcpu_index) {
    // CPU *c;
    //
    // g_rw_lock_writer_lock(&expand_array_lock);
    // if (vcpu_index >= cpus->len) {
    //     g_array_set_size(cpus, vcpu_index + 1);
    // }
    // g_rw_lock_writer_unlock(&expand_array_lock);
    //
    // c = get_cpu(vcpu_index);
    // c->last_exec = g_string_new(NULL);
    // c->registers = registers_init(vcpu_index);

}

static void vcpu_exit(qemu_plugin_id_t id, unsigned int vcpu_index) {
  printf("vcpu exiting: %d...\n", vcpu_index);
  fflush(stdout);
}

QEMU_PLUGIN_EXPORT int qemu_plugin_install(qemu_plugin_id_t id,
                                           const qemu_info_t *info, int argc,
                                           char **argv) {
  printf("::qemu_plugin_install\n");

  // parse options
  for (int i = 0; i < argc; i++) {
    char *p = argv[i];
    g_auto(GStrv) tokens = g_strsplit(p, "=", 2);
    const char *argname = tokens[0];
    const char *argvalue = tokens[1];
    if (g_strcmp0(argname, "client-id") == 0) {
      args.client_id = strdup(argvalue);
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

  shm = connect_to_broker();
  if (shm == NULL) {
    return EXIT_FAILURE;
  }

  open_sems();
  if (sem_client == NULL || sem_server == NULL) {
    return EXIT_FAILURE;
  }

  plugin_id = id;

  qemu_plugin_register_vcpu_tb_trans_cb(id, vcpu_tb_trans);
  qemu_plugin_register_vcpu_init_cb(id, vcpu_init);
  qemu_plugin_register_vcpu_exit_cb(id, vcpu_exit);
  qemu_plugin_register_atexit_cb(id, plugin_exit, NULL);
  return EXIT_SUCCESS;
}
