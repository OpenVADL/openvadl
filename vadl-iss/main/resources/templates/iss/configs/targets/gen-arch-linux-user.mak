TARGET_ARCH=rv64ume
TARGET_BIG_ENDIAN=[(${mem_big_endian} ? 'y' : 'n')]
TARGET_XML_FILES= gdb-xml/rv64ume-cpu.xml
# TODO: Find out what can be removed (probably most of it)
TARGET_BASE_ARCH=rv64ume
TARGET_ABI_DIR=rv64ume
TARGET_SYSTBL_ABI=common,64,rv64ume,rlimit,memfd_secret
TARGET_SYSTBL=syscall.tbl