# distribute-lock

分布式锁的两种实现，基于setNx和getSet命令的实现V1和基于lua脚本的实现V2

两者均是属于自旋锁，后续可能会实验性的增加AQS的版本，让其具有锁优化的功能，并测试使用效果。



# 使用手册

环境描述：jdk、git、maven

使用场景：Spring工程、依赖spring-boot-starter-data-redis、需要存在StringRedisTemplate Bean(不特殊处理的情况下会由spring-boot-starter-data-redis自动生成)



1. clone https://github.com/XCXCXCXCX/distribute-lock.git 到本地



2. 命令行cd到该工程路径下，执行mvn clean install命令，将jar包安装到本地maven库



3. 在需要使用分布式锁的spring工程中引入依赖

```xml
<dependency>
    <groupId>com.xcxcxcxcx</groupId>
    <artifactId>distribute-lock</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```



4. 代码使用示例

```java
private final DistributedLockFactory factory;

private final Lock lock;

/**
 * Spring的依赖注入功能，自动注入DistributedLockFactory
 * 通过factory.create()或factory.createV2()来构造锁
 * @param factory
 */
public SpringBootWithRedis(DistributedLockFactory factory) {
    this.factory = factory;
    this.lock = factory.createV2("com:xcxcxcxcx:test:SpringBootWithRedis", Duration.ofMinutes(1));
}
```



然后就可以愉快的使用了



# Addition

如果在代码阅读过程中存在疑惑或是有更好的想法，欢迎讨论。

如果在测试或使用过程中出现bug，欢迎提issue。

如果有兴趣修复bug，欢迎提pr。

