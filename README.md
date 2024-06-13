# 蝎子莱莱爱打怪 的 IM开源项目

与im结缘是在2022年，因为此类系统有足够大的挑战性，所以我对此如痴如醉，之前做过架构以及细节方面的设计，但是一直没有落地。不落地的设计不是好设计。所以有了这个项目。
目前项目处于前期阶段，后期一点点完善并将补上架构图和我能想到的所有设计细节！

想要设计一个好的im系统，是很有难度的，本项目将尽可能达到以下几点：
- 高并发
- 高可用
- 高性能
- 稳定可靠
- 灵活好扩展
- 可观测

想要实现上边的几点，将不可避免的需要集成很多中间件或框架，如下：

## 本项目涉及到的 ***【技术】***
1. SpringBoot
1. Netty【长连接服务器】
1. Nacos【配置中心】
1. Dubbo【rpc调用，利用长连接做消息转发】
1. Redis【缓存，分布式锁】
1. RocketMQ【解耦、削峰】
1. Mybatis Plus【ORM】
1. Mysql【数据存储】

- 待集成
1. GateWay 【网关】
1. Oauth2 + Spring Security 【权限】
1. Sentinel 【限流】
1. docker+k8s【项目部署】
1. prometheus + grafana【服务监控】
1. SkyWalking 【链路追踪】
1. ElasticSearch【聊天记录等搜索】
1. Jmeter+python脚本【压测】
1. 等等

## 本项目涉及到的 ***【功能点】***
1. 登录
1. 单聊
1. 群聊
1. 撤回
1. 已读
1. 离线消息
1. 最近会话
1. 等... 


其他细节待补充，完善中 ~ 。