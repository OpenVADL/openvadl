#ifndef [(${gen_arch_upper})]_CPU_BITS_H
#define [(${gen_arch_upper})]_CPU_BITS_H

#define get_field(reg, mask) (((reg) & \
(uint64_t)(mask)) / ((mask) & ~((mask) << 1)))
#define set_field(reg, mask, val) (((reg) & ~(uint64_t)(mask)) | \
(((uint64_t)(val) * ((mask) & ~((mask) << 1))) & \
(uint64_t)(mask)))

#define PRIV_M 0

/* Control and Status Registers */

#define CSR_MTVEC 0x305

/* mstatus CSR bits */
#define MSTATUS_MIE         0x00000008
#define MSTATUS_MPIE        0x00000080
#define MSTATUS_MPP         0x00001800

/* Exception causes */
typedef enum [(${gen_arch_upper})]Exception {
  [(${gen_arch_upper})]_EXCP_NONE = -1,
  [(${gen_arch_upper})]_EXCP_M_ECALL = 0xb,
} [(${gen_arch_upper})]Exception;


#define [(${gen_arch_upper})]_EXCP_INT_FLAG                0x80000000
#define [(${gen_arch_upper})]_EXCP_INT_MASK                0x7fffffff

#endif //[(${gen_arch_upper})]_CPU_BITS_H
