CC=gcc
CFLAGS_SHLIB=-fpic
LD=/usr/bin/ld -m elf_x86_64
OS=
SO=so
CPU=x86_64
LDFLAGS=
LDFLAGS_SHLIB=-shared -fPIC
PROXY_LIBS=-lpthread -lc
APACHE_INC=
resin_conf=
LIBEXEC=libexec64
resin_libexec=
resin_home=/home/philip/resin-compile/resin-3.1.15
JNI_INCLUDE=-I/home/philip/.sdkman/candidates/java/current/include -I/home/philip/.sdkman/candidates/java/current/include/linux
INCLUDES =  -I/usr/include $(JNI_INCLUDE) -I../common -I.
CFLAGS = -g -O2 -DPOLL -DEPOLL -D_POSIX_PTHREAD_SEMANTICS -DB64 -DHAS_SOCK_TIMEOUT -DHAS_JVMTI -DRESIN_HOME=\"$(resin_home)\" $(INCLUDES) -DCPU=\"$(CPU)\" -DOS=$(OS)  -m64 -fPIC -D_FILE_OFFSET_BITS=64 -fpic
OBJ= std.o memory.o jni_vfs.o java.o boot.o profile.o heap.o lru.o symbol.o crc64.o

all	: libresin.$(SO)

libresin.so	: $(OBJ)
	$(LD) $(LDFLAGS_SHLIB) -o $@ $(OBJ) $(PROXY_LIBS)

libresin.jnilib	: $(OBJ)
	$(LD) $(LDFLAGS_SHLIB) -framework JavaVM -o $@ $(OBJ) $(PROXY_LIBS)

jni_vfs.o	: resin.h Makefile
memory.o	: resin.h Makefile
lru.o	: resin.h Makefile
java.o	: resin.h Makefile
std.o		: resin.h Makefile
boot.o		: resin.h Makefile
profile.o	: resin.h Makefile
symbol.o	: resin.h Makefile
heap.o	: resin.h Makefile

clean	:
	rm -f *.o *.$(SO) resin

install	: libresin.$(SO)
#	cp resin $(resin_home)/bin
	- mkdir $(resin_home)/$(LIBEXEC)
	cp libresin.$(SO) $(resin_home)/$(LIBEXEC)



