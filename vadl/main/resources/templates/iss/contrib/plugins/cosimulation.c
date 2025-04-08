#include <gio/gio.h>
#include <glib.h>
#include <json-glib/json-glib.h>
#include <qemu-plugin.h>
#include <stdio.h>
#include <sys/socket.h>

QEMU_PLUGIN_EXPORT int qemu_plugin_version = QEMU_PLUGIN_VERSION;

static qemu_plugin_id_t plugin_id;
static unsigned long bb_count;
static unsigned long insn_count;

typedef struct {
  uint64_t pc;
  size_t insns;
  GSList *insns_info;
} TBInfo;

typedef struct {
  uint64_t pc;
  size_t size;
  const char *symbol;
  void *hwaddr;
  char *disas;
} TBInsnInfo;

typedef struct {
  gchar *hostname;
  guint16 port;
} Arguments;

static GSocketConnection *connection;
static Arguments args;

static JsonNode *serialize_insn_info(TBInsnInfo *insn_info) {
  JsonBuilder *builder = json_builder_new();

  json_builder_begin_object(builder);
  json_builder_set_member_name(builder, "pc");
  json_builder_add_int_value(builder, insn_info->pc);

  json_builder_set_member_name(builder, "size");
  json_builder_add_int_value(builder, insn_info->size);

  if (insn_info->symbol) {
    json_builder_set_member_name(builder, "symbol");
    json_builder_add_string_value(builder, insn_info->symbol);
  }

  if (insn_info->disas) {
    json_builder_set_member_name(builder, "disas");
    json_builder_add_string_value(builder, insn_info->disas);
  }

  char addr_str[17]; 
  sprintf(addr_str, "%016llx", (unsigned long long)insn_info->hwaddr);
  json_builder_set_member_name(builder, "hwaddr");
  json_builder_add_string_value(builder, addr_str);

  json_builder_end_object(builder);

  JsonNode *node = json_builder_get_root(builder);

  g_object_unref(builder);

  return node;
}

static char *tbinfo_to_json(TBInfo *tb_info) {
  JsonBuilder *builder = json_builder_new();

  json_builder_begin_object(builder);
  json_builder_set_member_name(builder, "pc");
  json_builder_add_int_value(builder, tb_info->pc);

  json_builder_set_member_name(builder, "insns");
  json_builder_add_int_value(builder, tb_info->insns);

  json_builder_set_member_name(builder, "insns_info");
  json_builder_begin_array(builder);

  GSList *iter;
  for (iter = tb_info->insns_info; iter != NULL; iter = iter->next) {
    TBInsnInfo *insn_info = iter->data;
    JsonNode *node = serialize_insn_info(insn_info);
    json_builder_add_value(builder, node);
  }

  json_builder_end_array(builder);
  json_builder_end_object(builder);

  JsonGenerator *gen = json_generator_new();
  JsonNode *root = json_builder_get_root(builder);
  json_generator_set_root(gen, root);
  gchar *json_str = json_generator_to_data(gen, NULL);

  json_node_free(root);
  g_object_unref(gen);
  g_object_unref(builder);

  return json_str;
}

static void plugin_cleanup(qemu_plugin_id_t id) {
  qemu_plugin_outs("::plugin_cleanup");
}

static void plugin_exit(qemu_plugin_id_t id, void *p) {
  qemu_plugin_outs("::plugin_exit");
  plugin_cleanup(id);
}

static guint16 parse_port(const char *string) {
  gchar *endptr = NULL;
  guint64 port = g_ascii_strtoull(string, &endptr, 10);
  if (endptr != NULL && endptr == string) {
    fprintf(stderr, "parse_port::failed to parse port: %s\n", string);
    return EXIT_FAILURE;
  }

  if (port <= 0 || port > G_MAXUINT16) {
    fprintf(stderr, "parse_port::invalid port number: %s\n", string);
    return EXIT_FAILURE;
  }

  return port;
}

static GSocketConnection *connect_to_broker(void) {
  GError *error = NULL;
  GSocketClient *client = g_socket_client_new();

  GSocketConnection *connection = g_socket_client_connect_to_host(
      client, args.hostname, args.port,
      NULL, // Cancellable gobject, might be useful later
      &error);

  if (error != NULL) {
    g_error("%s", error->message);
    // FIXME: retry-policy and/or crash
    return NULL;
  }

  g_print("Client connected to broker\n");

  if (error != NULL) {
    g_error("connect_to_broker::g_output_stream_write: %s", error->message);
    return NULL;
  }

  return connection;
}

static void vcpu_tb_trans(qemu_plugin_id_t id, struct qemu_plugin_tb *tb) {
  GError *error = NULL;
  GOutputStream *ostream =
      g_io_stream_get_output_stream(G_IO_STREAM(connection));

  uint64_t pc = qemu_plugin_tb_vaddr(tb);
  size_t insns = qemu_plugin_tb_n_insns(tb);

  GSList *insn_infos;
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
    insn_info.symbol = insn_symbol;
    insn_info.hwaddr = insn_hwaddr;
    insn_info.disas = insn_disas;

    insn_infos = g_slist_append(insn_infos, &insn_info);
  }

  TBInfo tbinfo;
  tbinfo.pc = pc;
  tbinfo.insns = insns;
  tbinfo.insns_info = insn_infos;

  char *json_msg = tbinfo_to_json(&tbinfo);
  if (json_msg == NULL) {
    g_error("failed to convert tbinfo to json");
    return;
  }

  g_output_stream_write(ostream, json_msg, strlen(json_msg), NULL, &error);

  if (error != NULL) {
    g_error("connect_to_broker::g_output_stream_write: %s", error->message);
  }
}

QEMU_PLUGIN_EXPORT int qemu_plugin_install(qemu_plugin_id_t id,
                                           const qemu_info_t *info, int argc,
                                           char **argv) {
  qemu_plugin_outs("::qemu_plugin_install");

  // set default option-values
  args.hostname = (gchar *)"localhost";

  // parse options
  for (int i = 0; i < argc; i++) {
    char *p = argv[i];
    g_auto(GStrv) tokens = g_strsplit(p, "=", 2);
    const char *argname = tokens[0];
    const char *argvalue = tokens[1];
    if (g_strcmp0(argname, "port") == 0) {
      args.port = parse_port(argvalue);
    } else {
      fprintf(stderr, "option parsing failed: %s\n", p);
      return EXIT_FAILURE;
    }
  }

  // check required options
  if (args.port == 0) {
    fprintf(stderr, "option port=<uint16_t> is required, no port was given\n");
    return EXIT_FAILURE;
  }

  connection = connect_to_broker();
  if (connection == NULL) {
    return EXIT_FAILURE;
  }

  plugin_id = id;

  qemu_plugin_register_vcpu_tb_trans_cb(id, vcpu_tb_trans);
  // qemu_plugin_register_atexit_cb(id, qemu_plugin_udata_cb_t cb, void *userdata);
  return EXIT_SUCCESS;
}
