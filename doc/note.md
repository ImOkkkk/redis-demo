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

## 发布订阅

subscribe channel 订阅频道

unsubscribe channel 取消订阅频道

publish channel message 将消息发送到指定的频道

> 原理

redis-server里维护一个字典, key是频道, value则是一个链表, 链表中保存了所有订阅了这个channel的客户端。

通过PUBLISH命令向订阅者发送消息, redis-server会使用给定的频道作为键, 在它所维护的channel字典中查找记录了订阅这个频道的所有客户端的链表, 遍历这个链表, 将消息发布给所有订阅者。

## 主从复制

主从复制, 是指将一台Redis服务器的数据, 复制到其他的Redis服务器。前者称为主节点(master/leader) , 后者称为从节点(slave/follower) ; **数据的复制是单向的 ; 只能由主节点到从节点**。Master以写为主, Slave以读为主。

作用:

1. **数据冗余**:主从复制实现了数据的热备份, 是持久化之外的一种数据冗余方式;
2. **故障恢复**:当主节点出现问题时, 可以有从节点提供服务, 实现快速的故障恢复;
3. **负载均衡**:在主从复制的基础上, 配合读写分离, 可以有主节点提供写服务, 由从节点提供读服务, 分担服务器负载;
4. **高可用**

![](img/image-20220215224640520.png)

```shell
> info replication #查看当前库信息
# Replication
role:master #角色 master
connected_slaves:0 #从机数量
master_replid:038c1bdb823d591398b8d97c7ed35569ba9656be
master_replid2:0000000000000000000000000000000000000000
master_repl_offset:0
second_repl_offset:-1
repl_backlog_active:0
repl_backlog_size:1048576
repl_backlog_first_byte_offset:0
repl_backlog_histlen:0
```

从机执行命令选择主机

slaveof 127.0.0.1 6379

使用命令配置是暂时的，修改配置文件：

replicaof 配置主机地址即可

![](img/230314.png)

> 细节

主机可以写，从机不能写只能读，主机中的所有信息和数据都会自动被从机保存。

没有哨兵的情况下，即使主机宕机，从机连接的主机依然是之前的主机。主机恢复，集群依然可用。如果从机宕机，从机重启后(并且变为从机)，立马就会从主机中取到值。

**复制原理**

slave启动成功连接到master后会发送一个sync同步命令, master接受命令, 启动后台的存盘进程, 同时收集所有接收到的用于修改数据集命令, 在后台进程执行完毕之后, master将传送整个数据文件到slave, 并完成一次完全同步。

- 全量复制 : slave服务在接收到数据库文件数据后, 将其存盘并加载到内存中;
- 增量复制 : master继续将新的所有收集到的修改命令依次传给slave, 完成同步。

重新连接master，一定执行一次全量复制, master继续将新的所有收集到的数据传给slave, 即增量复制。

