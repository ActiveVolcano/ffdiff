English
- - - -
Under construction...

中文
- - - -

环境需求
========
* 装有 Java 8 或以上版本，java 程序路径在 PATH 环境变量中。
* 在 Linux 对命令文件设置执行权限：
```bash
chmod a+x ffdiff ffpatch
```

用法
====
* 根据新包、旧包生成差异包：
```bash
ffdiff -base {输入旧包文件名} -target {输入新包文件名} -diff {输出差异包文件名}
```
* 根据旧包、差异包生成新包：
```bash
ffpatch -base {输入旧包文件名} -diff {输入差异包文件名} -target {输出新包文件名}
```

参考文献
========
文件差异包生成工具 bsdiff 看了一下原始作者写的介绍
http://www.daemonology.net/bsdiff/
原来为了追求体积小，用了很吃内存的算法，怪不得试用的时候卡死了。
