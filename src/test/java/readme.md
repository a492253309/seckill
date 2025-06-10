启动 RocketMQ：
bash

sh bin/mqnamesrv &
sh bin/mqbroker -n localhost:9876 &

初始化库存：
bash

curl -X POST "http://localhost/seckill/prewarm/sku_123/1000"

生成 Token：
bash

curl -X POST "http://localhost/seckill/token?userId=user1&sku=sku_123"

下单：
bash

curl -X POST "http://localhost/seckill/order?userId=user1&sku=sku_123&quantity=1&token=your_token"

1.Docker 部署（可选）：

version: '3'
services:
redis:
image: redis:latest
ports:
- "6379:6379"
mysql:
image: mysql:8.0
environment:
MYSQL_ROOT_PASSWORD: root
ports:
- "3306:3306"
rocketmq-namesrv:
image: apache/rocketmq:4.9.4
command: sh mqnamesrv
ports:
- "9876:9876"
rocketmq-broker:
image: apache/rocketmq:4.9.4
command: sh mqbroker -n namesrv:9876
depends_on:
- rocketmq-namesrv
xxl-job-admin:
image: xuxueli/xxl-job-admin:latest
ports:
- "8088:8088"
nginx:
image: nginx:latest
volumes:
- ./nginx.conf:/etc/nginx/nginx.conf
ports:
- "80:80"
seckill-app:
build: .
ports:
- "8080:8080"
depends_on:
- redis
- mysql
- rocketmq-broker
- xxl-job-admin


2.MySQL：
主从复制：订单查询走从库，写入走主库。

分表：使用 ShardingSphere 分片 orders 表：
yaml

shardingsphere:
rules:
sharding:
tables:
orders:
actual-data-nodes: ds0.orders_${0..3}
table-strategy:
inline:
sharding-column: order_id
algorithm-expression: orders_${order_id.hashCode() % 4}

3.Redis：
3.1配置集群：
yaml

spring:
redis:
cluster:
nodes: node1:6379,node2:6379

3.2启用 AOF 持久化：
bash

appendonly yes


4.XXL-Job：
  4.1动态调整 CRON（如高峰期缩短间隔）。

4.2配置告警：
yaml

xxl:
job:
mail:
host: smtp.example.com
username: alert@example.com
password: xxx


5.监控：
  RocketMQ Dashboard：监控队列积压。

Prometheus + Grafana：监控 Redis、MySQL、RocketMQ。

MySQL 慢查询：
sql

SET GLOBAL slow_query_log = 'ON';


