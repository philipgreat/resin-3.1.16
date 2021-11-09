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
resin_libexec=
LIBEXEC=libexec64
resin_home=/home/philip/resin-compile/resin-3.1.15
JNI_INCLUDE=-I/home/philip/.sdkman/candidates/java/current/include -I/home/philip/.sdkman/candidates/java/current/include/linux
INCLUDES =  -I/usr/include $(JNI_INCLUDE) -I../common
CFLAGS = -g -O2 -DPOLL -DEPOLL -D_POSIX_PTHREAD_SEMANTICS -DB64 -DHAS_SOCK_TIMEOUT -DHAS_JVMTI  -m64 -fPIC -D_FILE_OFFSET_BITS=64 -DRESIN_HOME=\"$(resin_home)\" $(INCLUDES) -DCPU=\"$(CPU)\" -DOS=$(OS)
OBJ= jni_os.o jni_jvmti.o jni_jvmdi.o

all	: libresin_os.$(SO)

libresin_os.so	: $(OBJ)
	$(LD) $(LDFLAGS_SHLIB) -o $@ $(OBJ) $(PROXY_LIBS)

libresin_os.jnilib	: $(OBJ)
	$(LD) $(LDFLAGS_SHLIB) -framework JavaVM -o $@ $(OBJ) $(PROXY_LIBS)

jni_os.o	: Makefile
jni_jvmti.o	: Makefile
jni_jvmdi.o	: Makefile

clean	:
	rm -f *.o *.$(SO)

install	: libresin_os.$(SO)
#	cp resin $(resin_home)/bin
	- mkdir $(resin_home)/$(LIBEXEC)
	cp libresin_os.$(SO) $(resin_home)/$(LIBEXEC)



