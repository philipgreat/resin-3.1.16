CC=gcc
CFLAGS_SHLIB=-fpic
CFLAGS_OPENSSL= -DSSL_ENGINE
LD=/usr/bin/ld -m elf_x86_64
OS=
SO=so
CPU=x86_64
LDFLAGS=
LDFLAGS_SHLIB=-shared -fPIC
PROXY_LIBS=-lpthread -lc  -lssl -lcrypto
APACHE_INC=
resin_conf=
resin_libexec=
LIBEXEC=libexec64
resin_home=/home/philip/resin-compile/resin-3.1.15
JNI_INCLUDE=-I/home/philip/.sdkman/candidates/java/current/include -I/home/philip/.sdkman/candidates/java/current/include/linux
INCLUDES =  -I/usr/include $(JNI_INCLUDE) -I../common -I../resin
CFLAGS = -g -O2 -DPOLL -DEPOLL -D_POSIX_PTHREAD_SEMANTICS -DB64 -DHAS_SOCK_TIMEOUT -DHAS_JVMTI -DRESIN_HOME=\"$(resin_home)\" $(INCLUDES) -DCPU=\"$(CPU)\" -DOS=$(OS)  -m64 -fPIC -D_FILE_OFFSET_BITS=64 -fpic  -DSSL_ENGINE
OBJ=ssl.o ../resin/memory.o ../resin/std.o ../resin/java.o

all	: libresinssl.$(SO)

libresinssl.so	: $(OBJ)
	$(LD) $(LDFLAGS_SHLIB) -o $@ $(OBJ) $(PROXY_LIBS)

libresinssl.jnilib	: $(OBJ)
	$(LD) $(LDFLAGS_SHLIB) -framework JavaVM -o $@ $(OBJ) $(PROXY_LIBS)

$(OBJ)	: ../resin/resin.h Makefile

clean	:
	rm -f *.o *.$(SO)

install	: libresinssl.$(SO)
	- mkdir $(resin_home)/$(LIBEXEC)
	cp libresinssl.$(SO) $(resin_home)/$(LIBEXEC)



