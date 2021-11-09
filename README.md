

# resin-3.1.16
A GPL license Resin-3.1.16 version works with JDK9+
## Goal

* Works with new JDK version JDK9,JDK11, JDK16, JDK 17 Tested
* Keep stupied and simple

## Intension
Caucho has Resin-4.0.X

* Resin-3.1.16 is a pretty stable version and less memory foot print;
* Resin-3.1.16 start time is less or around 1000ms;
* Smaller Size for distributing.

## Build Process

### Download and set apache ant

Download and set apache ant
```
https://ant.apache.org/
```

## Set ANT_HOME and PATH


Command as example

```
export ANT_HOME=/home/philip/resin-compile/apache-ant-1.9.16
export PATH="/home/philip/resin-compile/apache-ant-1.9.16/bin:$PATH"
```

### Start build

ant clean && ant build

### Verifing

As you go with caucho resin server before

```
[22:12:17.710] {main} 'select-manager' requires Resin Professional.  See http://www.caucho.com for information and licensing.
[22:12:17.710] {main} 
[22:12:17.710] {main} Mac OS X 12.0.1 x86_64
[22:12:17.710] {main} OpenJDK Runtime Environment 11.0.13+8, UTF-8, en
[22:12:17.710] {main} OpenJDK 64-Bit Server VM 11.0.13+8, 64, mixed mode, Eclipse Adoptium
[22:12:17.710] {main} user.name: Philip
[22:12:17.711] {main} resin.home = /opt/resin-3.1.12/
[22:12:17.711] {main} resin.root = /opt/resin-3.1.12/
[22:12:17.711] {main} resin.conf = /opt/resin-3.1.12/conf/resin.conf
[22:12:17.711] {main} 
[22:12:17.871] {main} WebApp[http://localhost:8080] active
[22:12:17.940] {main} WebApp[http://localhost:8080/resin-admin] active
[22:12:18.042] {main} WebApp[http://localhost:8080/sky] CodeGenContextFilter initialized
[22:12:18.043] {main} WebApp[http://localhost:8080/sky] active
[22:12:18.044] {main} Host[] active
[22:12:18.047] {main} hmux listening to localhost:6800
[22:12:18.049] {main} http listening to *:8080
[22:12:18.050] {main} Server[id=,cluster=app-tier] active
[22:12:18.051] {main} Resin started in 1190ms
```



## License

This code is derived from resin-3.1.16, following the license GPL3.0



