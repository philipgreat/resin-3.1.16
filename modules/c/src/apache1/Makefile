.SUFFIXES: .o .lo .so

CC=gcc
CP=/bin/cp
CFLAGS=-g -O2 -DPOLL -DEPOLL -D_POSIX_PTHREAD_SEMANTICS -DB64 -DHAS_SOCK_TIMEOUT -DHAS_JVMTI 
CFLAGS_SHLIB=-fpic
EGREP=/bin/grep -E
LIBTOOL=EGREP="/bin/grep -E" /home/philip/resin-compile/resin-3.1.15/libtool --silent
LD=/usr/bin/ld -m elf_x86_64
LD_SHLIB=gcc
LIBS_SHLIB=
LDFLAGS=
LDFLAGS_SHLIB=-shared -fPIC
APACHE_INC=
apache_conf=
apache_dir=
apache_libexec=
resin_home=/home/philip/resin-compile/resin-3.1.15
INCLUDES = $(APACHE_INC) -DRESIN_HOME=\"$(resin_home)\" -I../common
C=../common
COMMON_OBJ=$C/stream.lo $C/config.lo $C/memory.lo
OBJ_PICS=mod_caucho.lo $(COMMON_OBJ)

all	: mod_caucho.la

#mod_caucho.so	: mod_caucho.la
#	mv mod_caucho.la mod_caucho.so

mod_caucho.la	: $(OBJ_PICS)
	$(LIBTOOL) --mode=link $(LD_SHLIB) -module -o $@ -avoid-version -rpath $(apache_libexec) $(OBJ_PICS) $(LIBS_SHLIB)

.c.lo	:
	$(LIBTOOL) --mode=compile $(CC) -o $@ -c $(INCLUDES) $(CFLAGS) $<

.c.o	:
	$(CC) -c $(INCLUDES) $(CFLAGS) $<

install	: mod_caucho.la
	$(LIBTOOL) --mode=install $(CP) mod_caucho.la $(apache_libexec)
	- rm -f $(apache_libexec)/mod_caucho.la
	- rm -f $(apache_libexec)/mod_caucho.a
	sh install.sh -conf $(apache_conf) -libexec $(apache_libexec) \
	   -resin_home $(resin_home)

clean	:
	- rm *.o *.lo *.la *.so .libs

$(OBJ_PICS)	: Makefile ../common/cse.h
mod_caucho.lo	: Makefile ../common/cse.h

