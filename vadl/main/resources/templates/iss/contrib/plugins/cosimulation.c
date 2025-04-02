#include <gio/gio.h>
#include <glib.h>
#include <qemu-plugin.h>
#include <stdio.h>
#include <sys/socket.h>

QEMU_PLUGIN_EXPORT int qemu_plugin_version = QEMU_PLUGIN_VERSION;

static qemu_plugin_id_t plugin_id;
static unsigned long bb_count;
static unsigned long insn_count;

typedef struct {
  gchar *hostname;
  guint16 port;
} Arguments;

typedef struct {
  uint64_t pc;
  uint64_t insns;
} BlockInfo;

typedef struct {
  BlockInfo *block;
  unsigned long insn_count;
  unsigned long block_count;
} ExecInfo;

typedef struct {
  uint64_t pc;
  uint64_t insn_count;
} ExecState;

typedef struct {
  GSList *log_pos;
  int distance;
} DivergeState;

static int socket_fd;
static Arguments args;

static void plugin_cleanup(qemu_plugin_id_t id) {
  qemu_plugin_outs("::plugin_cleanup");
}

static void plugin_exit(qemu_plugin_id_t id, void *p) {
  qemu_plugin_outs("::plugin_exit");
  plugin_cleanup(id);
}

static guint16 parse_port(const char *string) {
  g_autofree gchar *endptr = NULL;
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

static void connect_to_broker(void) {
  g_autofree GError *error = NULL;
  g_autofree GSocketClient *client = g_socket_client_new();

  g_autofree GSocketConnection *connection = g_socket_client_connect_to_host(
      client, args.hostname, args.port,
      NULL, // Cancellable gobject, might be useful later
      &error);

  if (error != NULL) {
    g_error("%s", error->message);
    // FIXME: retry-policy and/or crash
    return;
  } 

  g_print("Client connected to broker\n");

  GOutputStream *ostream =
      g_io_stream_get_output_stream(G_IO_STREAM(connection));
  g_autofree const gchar *msg = "Hello server!";
  g_output_stream_write(ostream, msg, strlen(msg), NULL, &error);

  if (error != NULL) {
    g_error("%s", error->message);
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

  connect_to_broker();

  plugin_id = id;
  return EXIT_SUCCESS;
}
