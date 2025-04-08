#include <inttypes.h>
#include <assert.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <stdio.h>

#include <qemu-plugin.h>

typedef struct {
    struct qemu_plugin_register *handle;
    GByteArray *val;
    const char *name;
    int size;
} Register;

#define HTIF_DEV_SHIFT          56
#define HTIF_CMD_SHIFT          48
#define HTIF_DEV_SYSTEM         0
#define HTIF_SYSTEM_CMD_SYSCALL 0

QEMU_PLUGIN_EXPORT int qemu_plugin_version = QEMU_PLUGIN_VERSION;

// default to 0, indicating that it is not known
static uint64_t tohost_addr = 0;

static GPtrArray *registers;
static int accessCounter = 0;

static void qemu_plugin_outsf(const char *fmt, ...) {
    va_list args;
    va_start(args, fmt);

    g_autoptr(GString) str = g_string_new(NULL);
    g_string_append_vprintf(str, fmt, args);

    va_end(args);
    qemu_plugin_outs(str->str);
}

static void print_all_registers() {

    for (int i = 0; i < registers->len; i++) {
        Register *reg = registers->pdata[i];

        g_autoptr(GString) str = g_string_new("");
        g_string_append_printf(str, "[REG] %s: 0x", reg->name);
        for (int i = reg->size - 1; i >= 0; i--) {
            g_string_append_printf(str, "%02x",
                                   reg->val->data[i]);
        }
        g_string_append_printf(str, "\n");
        qemu_plugin_outs(str->str);
    }
}

static void vcpu_mem(unsigned int cpu_index, qemu_plugin_meminfo_t meminfo,
                     uint64_t vaddr, void *udata) {

    qemu_plugin_mem_value last = qemu_plugin_mem_get_value(meminfo);
    if (last.type != QEMU_PLUGIN_MEM_VALUE_U64) {
        return;
    }

    if (tohost_addr && tohost_addr != vaddr) {
        // if we know the tohost address and it differs to vaddr
        // we can skip
        return;
    }

    uint64_t val_written = last.data.u64;
    uint8_t device = val_written >> HTIF_DEV_SHIFT;
    uint8_t cmd = val_written >> HTIF_CMD_SHIFT;
    uint64_t payload = val_written & 0xFFFFFFFFFFFFULL;

    if (device != HTIF_DEV_SYSTEM || cmd != HTIF_SYSTEM_CMD_SYSCALL || !(payload & 0x1)) {
        return;
    }

    // save registers
    for (int i = 0; i < registers->len; i++) {
        Register* reg = registers->pdata[i];
        int size = qemu_plugin_read_register(reg->handle, reg->val);
        reg->size = size;
    }

    // get HTIF fields
    accessCounter++;
}

static void vcpu_tb_trans(qemu_plugin_id_t id, struct qemu_plugin_tb *tb)
{
    size_t n = qemu_plugin_tb_n_insns(tb);
    size_t i;

    for (i = 0; i < n; i++) {
        struct qemu_plugin_insn *insn = qemu_plugin_tb_get_insn(tb, i);
        qemu_plugin_register_vcpu_mem_cb(insn, vcpu_mem,
                                         QEMU_PLUGIN_CB_R_REGS,
                                         QEMU_PLUGIN_MEM_W, NULL);
    }
}

static void qemu_exit(qemu_plugin_id_t id, void *userdata) {
    print_all_registers();

    g_ptr_array_free(registers, TRUE);
}

static void vcpu_init(qemu_plugin_id_t id, unsigned int vcpu_index)
{
    qemu_plugin_outsf("[EState] use tohost addr: 0x%" PRIx64 "\n", tohost_addr);

    // initialize registers
    registers = g_ptr_array_new();
    g_autoptr(GArray) reg_list = qemu_plugin_get_registers();

    for (int r = 0; r < reg_list->len; r++) {
        qemu_plugin_reg_descriptor *rd = &g_array_index(
            reg_list, qemu_plugin_reg_descriptor, r);
            g_autofree gchar *rd_lower = g_utf8_strdown(rd->name, -1);
            Register *reg = g_new0(Register, 1);

            reg->handle = rd->handle;
            reg->name = g_intern_string(rd_lower);
            reg->val = g_byte_array_new();
            reg->size = 0;

            g_ptr_array_add(registers, reg);
    }
}

static uint64_t parse_vaddr(char *addr)
{
    return g_ascii_strtoull(addr, NULL, 16);
}

QEMU_PLUGIN_EXPORT int qemu_plugin_install(qemu_plugin_id_t id,
                                           const qemu_info_t *info,
                                           int argc, char **argv)
{
    for (int i = 0; i < argc; i++) {
        char *opt = argv[i];
        g_auto(GStrv) tokens = g_strsplit(opt, "=", 2);
        if (g_strcmp0(tokens[0], "tohost") == 0) {
            tohost_addr = parse_vaddr(tokens[1]);
        } else {
            fprintf(stderr, "option parsing failed: %s\n", opt);
            return -1;
        }
    }

    qemu_plugin_register_vcpu_init_cb(id, vcpu_init);
    qemu_plugin_register_vcpu_tb_trans_cb(id, vcpu_tb_trans);
    qemu_plugin_register_atexit_cb(id, qemu_exit, NULL);
    return 0;
}
