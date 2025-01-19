# Paxos协议实现

这是一个基于Java实现的Paxos共识算法示例项目。该项目展示了Paxos协议的核心概念和基本工作流程，主要用于学习和理解分布式系统中的共识机制。

## 项目概述

本项目实现了一个基于Paxos协议的领导者选举系统，包含以下主要特点：

- 完整实现了Paxos协议的三个核心角色：Proposer、Acceptor和Learner
- 支持多节点并发运行
- 模拟网络延迟和节点故障场景
- 提供了详细的选举过程日志输出

## 系统架构

系统中包含两种类型的节点：

1. PAL节点（PAL_Role）：同时扮演Proposer、Acceptor和Learner三种角色
2. AL节点（AL_Role）：只扮演Acceptor和Learner角色

### 核心组件

- `PAL_Role`: 实现了完整的Paxos节点功能
- `AL_Role`: 实现了Acceptor和Learner功能的节点
- `ElectionModel`: 选举模型，负责管理整个选举过程
- `Server`: 基础服务器类，提供网络通信功能
- `Message`: 定义了节点间通信的消息格式

## Paxos协议实现细节

### 准备阶段（Prepare）

1. Proposer生成一个新的提案ID（递增）
2. 向所有Acceptor发送Prepare请求
3. Acceptor根据以下规则响应：
   - 如果收到的提案ID大于之前承诺的ID，则接受并返回PASS
   - 否则返回REJECT和当前最大的提案ID

### 接受阶段（Accept）

1. 如果Proposer收到多数派的PASS响应，则发送Accept请求
2. Acceptor根据以下规则处理Accept请求：
   - 如果提案ID小于之前承诺的ID，则拒绝
   - 否则接受该提案并广播给所有Learner

### 学习阶段（Learn）

1. Learner接收被接受的提案
2. 当收到多数派相同的提案时，认为该提案已经达成共识

## 特色功能

1. 故障模拟
   - 支持设置随机响应延迟
   - 支持模拟节点断开连接
   - 支持自定义节点响应延迟

2. 健康检查
   - 系统启动时进行节点健康检查
   - 动态发现可用的Acceptor和Learner节点

3. 容错机制
   - 实现了基本的超时重试机制
   - 支持节点动态加入和退出

## 配置说明

系统默认配置：
- 端口范围：8001-8009
- PAL节点：M1-M3（端口8001-8003）
- AL节点：M4-M9（端口8004-8009）
- 默认超时时间和重试间隔可在Config类中配置

## 使用示例

```java
// 创建选举模型
ElectionModel electionModel = new ElectionModel();

// 启动正常选举
electionModel.startElection();

// 启动带节点断开的选举
electionModel.startElectionAndDisconnect();

// 设置节点响应延迟
electionModel.setRandomReplyDelay(1000); // 1秒延迟

// 设置节点不可用
electionModel.setNoReceive("M2");
```

## 项目特点

1. 教学价值
   - 代码结构清晰，易于理解
   - 完整展示了Paxos协议的工作流程
   - 提供了丰富的调试信息输出

2. 实践性
   - 实现了真实的网络通信
   - 支持多种故障场景测试
   - 提供了完整的测试用例

3. 可扩展性
   - 模块化设计，易于扩展
   - 支持自定义配置和策略
   - 可作为更复杂分布式系统的基础

## 注意事项

1. 这是一个用于学习和演示的项目，主要关注Paxos协议的核心概念
2. 生产环境使用需要考虑更多的边界情况和优化
3. 建议先理解基本的Paxos协议理论再阅读代码

## 贡献指南

欢迎提交Issue和Pull Request来改进项目。在提交代码前，请确保：

1. 代码符合项目的编码规范
2. 添加了适当的注释和文档
3. 通过了所有测试用例
