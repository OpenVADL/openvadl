#include <vector>
#include <map>
#include <iostream>
#include <mutex>

#include <string.h>
#include <unistd.h>
#include <sys/stat.h>
#include <sys/mman.h>
#include <assert.h>
#include <fcntl.h>
#include <elf.h>


using Data = uint8_t;
using Address = uint64_t;

class SimMem {

  struct Segment {
    public:
      Address start;
      Address end; // exclusive (last address + 1)
      Data *data;

      Segment(Address start, Address end, Data *data)
          : start(start), end(end), data(data) {
      }

      bool match(Address address) {
        return (address >= start && address < end);
      }

      Data read(Address address) {
        return data[index(address)];
      }

      void write(Address address, Data data) {
        this->data[index(address)] = data;
      }

    private:
      Address index(Address address) {
        return (address - start);
      }
  };

  public:
    bool loadElf(const char *file);

    void map(Address address, size_t bufLen, Data *buf, size_t len);

    Data read(Address address);
    void write(Address address, Data data);

    Address getEntry();
    Address getSymbol(std::string symbol);

  private:
    Address entry;
    std::map<std::string, Address> symbols;
    std::vector<Segment> segments;

};

// Constructor and ELF Loader

#define IS_ELF(hdr) \
  ((hdr).e_ident[0] == 0x7f && (hdr).e_ident[1] == 'E' && \
   (hdr).e_ident[2] == 'L'  && (hdr).e_ident[3] == 'F')

#define IS_ELF32(hdr) (IS_ELF(hdr) && (hdr).e_ident[4] == 1)
#define IS_ELF64(hdr) (IS_ELF(hdr) && (hdr).e_ident[4] == 2)

#define LOAD_ELF(ehdr_t, phdr_t, shdr_t, sym_t) do { \
  ehdr_t* eh = (ehdr_t*)buf; \
  phdr_t* ph = (phdr_t*)(buf + eh->e_phoff); \
  this->entry = eh->e_entry; \
  assert(size >= eh->e_phoff + eh->e_phnum * sizeof(*ph)); \
  for (unsigned i = 0; i < eh->e_phnum; i++) { \
    if(ph[i].p_type == PT_LOAD && ph[i].p_memsz) { \
      if (ph[i].p_filesz) \
        assert(size >= ph[i].p_offset + ph[i].p_filesz); \
      this->map(ph[i].p_paddr, \
        ph[i].p_filesz * (sizeof(uint8_t)+sizeof(Data)-1)/sizeof(Data), \
        (Data*)(buf + ph[i].p_offset), \
        ph[i].p_memsz * (sizeof(uint8_t)+sizeof(Data)-1)/sizeof(Data)); \
    } \
  } \
  shdr_t* sh = (shdr_t*)(buf + eh->e_shoff); \
  assert(size >= eh->e_shoff + eh->e_shnum * sizeof(*sh)); \
  assert(eh->e_shstrndx < eh->e_shnum); \
  assert(size >= sh[eh->e_shstrndx].sh_offset + sh[eh->e_shstrndx].sh_size); \
  char *shstrtab = buf + sh[eh->e_shstrndx].sh_offset; \
  signed strtabidx = eh->e_shnum, symtabidx = eh->e_shnum; \
  for (unsigned i = 0; i < eh->e_shnum; i++) { \
    if (strcmp(shstrtab + sh[i].sh_name, ".strtab") == 0) strtabidx = i; \
    if (strcmp(shstrtab + sh[i].sh_name, ".symtab") == 0) symtabidx = i; \
  } \
  if (strtabidx != eh->e_shnum && symtabidx != eh->e_shnum) { \
    char* strtab = buf + sh[strtabidx].sh_offset; \
    sym_t* sym = (sym_t*)(buf + sh[symtabidx].sh_offset); \
    for (unsigned i = 0; i < sh[symtabidx].sh_size / sizeof(sym_t); i++) { \
      this->symbols[strtab + sym[i].st_name] = sym[i].st_value; \
    } \
  } \
} while(0)

bool SimMem::loadElf(const char *file) {
  int fd = open(file, O_RDONLY);
  struct stat s;
  if (fd == -1 || fstat(fd, &s) < 0) {
    std::cerr << "Could not open SimMem file: " << file << std::endl;
    return false;
  }
  size_t size = s.st_size;
  char* buf = (char*)mmap(NULL, size, PROT_READ, MAP_PRIVATE, fd, 0);
  if (buf == MAP_FAILED) {
    std::cerr << "Could not mmap SimMem file: " << file << std::endl;
    close(fd);
    return false;
  }
  close(fd);

  const Elf64_Ehdr* eh64 = (const Elf64_Ehdr*)buf;
  if (IS_ELF32(*eh64)) {
    LOAD_ELF(Elf32_Ehdr, Elf32_Phdr, Elf32_Shdr, Elf32_Sym);
  } else if (IS_ELF64(*eh64)) {
    LOAD_ELF(Elf64_Ehdr, Elf64_Phdr, Elf64_Shdr, Elf64_Sym);
  } else {
    std::cerr << "Could not load SimMem file: " << file << std::endl;
    munmap(buf, size);
    return false;
  }

  munmap(buf, size);
  return true;
}

// Create segments

void SimMem::map(Address address, size_t bufLen, Data *buf, size_t len) {
  Data *mem = new Data[len];
  memcpy(mem, buf, bufLen);
  memset(mem + bufLen, 0, len - bufLen);
  this->segments.push_back(Segment(address, address + len, mem));
}

// Read, Write

Data SimMem::read(Address address) {
  for (auto seg : this->segments) {
    if (seg.match(address)) {
      return seg.read(address);
    }
  }
  return 0;
}

void SimMem::write(Address address, Data data) {
  for (auto seg : this->segments) {
    if (seg.match(address)) {
      return seg.write(address, data);
    }
  }
}

// Get Symbols

Address SimMem::getEntry() {
  return entry;
}

Address SimMem::getSymbol(std::string symbol) {
  return symbols[symbol];
}

// DPI calls

std::map<std::string, SimMem*> mems;
std::mutex mems_mutex;

extern "C" void *simmem_init(char *name, char *file) {
  mems_mutex.lock();
  SimMem *mem;
  auto it = mems.find(name);
  if (it != mems.end()) {
    mem = it->second;
  } else {
    mem = new SimMem();
    if (file[0] != '\0' && !mem->loadElf(file)) {
      std::cerr << "Could not load SimMem file: " << file << std::endl;
      return nullptr;
    }
    mems[name] = mem;
  }
  mems_mutex.unlock();
  return mem;
}

extern "C" Data simmem_read(void *mem, Address address) {
  return ((SimMem*)mem)->read(static_cast<Address>(address));
}

extern "C" void simmem_write(void *mem, Address address, Data data) {
  ((SimMem*)mem)->write(static_cast<Address>(address), static_cast<Data>(data));
}

extern "C" Address simmem_entry(void *mem) {
  return ((SimMem*)mem)->getEntry();
}

extern "C" Address simmem_symbol(void *mem, char *symbol) {
  return ((SimMem*)mem)->getSymbol(symbol);
}
