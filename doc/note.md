## RDB

> 触发机制

1. 满足配置文件中的save规则；
2. 执行flushall命令；
3. 退出Redis(关闭Redis服务)。

> 如何恢复RDB文件

1. 将RDB文件放置在Redis启动目录，Redis启动的时候就会自动检查dump.rdb恢复数据。

优点：

1. 适合大规模的数据恢复；
2. 对数据完整性要求不高

缺点：

1. 需要一定的时间间隔进行操作，如果Redis意外宕机，最后一次快照之后的数据会丢失；
2. fork进程，需要额外的内存开销。

## AOF

类似于操作日志，恢复数据时回放appendonly.aof。

如果aof文件有错误，Redis将无法启动，Redis提供了`redis-check-aof --fix`修复aof文件。

```yml
appendonly no #默认不开启aof模式，默认使用rdb模式
appendfilename "appendonly.aof" #持久化文件的名字
appendfsync always #每次修改都会sync everysec:每秒执行1次sync，可能会丢失这1秒的数据 no:操作系统自己同步数据
```

缺点：

1. 数据文件：aof远大于rdb，恢复速度也更慢；
2. aof运行效率也比rdb慢。

> 重写规则

```yml
no-appendfsync-on-rewrite no
auto-aof-rewrite-percentage 100
auto-aof-rewrite-min-size 64mb
```

如果aof文件大于64mb了，fork一个新的进程来重写文件。

## RDB-AOF混合

Redis4.0版本之后，aof-use-rdb-preamble: yes开启。

**新的aof文件前半段是rdb格式的全量数据后半段是aof格式的增量数据**

当开启混合持久化时，fork出的子进程先将共享的内存副本全量的以rdb方式写入aof文件，然后在将重写缓冲区的增量命令以aof方式写入到文件，写入完成后通知主进程更新统计信息，并将新的含有rdb格式和aof格式的aof文件替换旧的的aof文件。

当我们开启了混合持久化时，启动redis依然优先加载aof文件，aof文件加载可能有两种情况如下：

1. aof文件开头是rdb的格式, 先加载 rdb内容再加载剩余的 aof；
2. aof文件开头不是rdb的格式，直接以aof格式加载整个文件。

