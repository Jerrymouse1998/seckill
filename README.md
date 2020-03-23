学习项目旨在调通一个秒杀业务后台的最基本开发构建。<!-- more -->

本篇采用SSM+MySQL+Redis。后序会另起新篇从<u>框架方面进行重构</u>和<u>业务性能方面进行优化</u>。

本文项目源码上传[github](https://github.com/Jerrymouse1998/seckill)： https://github.com/Jerrymouse1998/seckill  

## 开发环境

操作系统：win10

JDK：1.8

IDE：IDEA2019.3.1

项目构建工具：Maven3.6.3

数据库：MySQL5.1.49 + Redis3.2.100

## 项目构建

Maven构建一个webapp模板的项目，要把web.xml修改为servlet3.1版本。(可以去tomcat/webapps/下的实例的web.xml中获取)

添加文件夹，构成完整的目录框架：

![8t50X9.png](https://s1.ax1x.com/2020/03/17/8t50X9.png)

## 补全项目依赖

日志：slf4j接口+logback实现。

数据库：MySQL驱动、c3p0连接池。

dao框架：Mybatis、Mybatis整合Spring。

ServletWeb相关：jstl、taglibs、jackson、Servlet-API。

Spring：core、beans(ioc)、context、spring-jdbc、tx(事务)、web、MVC、test。

### 秒杀业务简单分析

从数据库及操作角度来看：

主要是商家、库存、用户三个实体之间的相互操作。![8NVZMF.png](https://s1.ax1x.com/2020/03/17/8NVZMF.png)

秒杀主要的点在于用户秒杀操作(废话)，即用户向库存的这个操作。我们需要将这个向库存的的操作绑定为一个事务。

![8NVkGV.png](https://s1.ax1x.com/2020/03/17/8NVkGV.png)

==所谓的秒杀就是多个用户之间的"竞争"，反映到背后的技术就是<font color=red>**事务+行级锁**</font>。==

事务的流程：

```
Start 事务
update库存数量
insert购买明细
commit事务
```

竞争出现在update库存数量。

关于行级锁：![8NZNlT.png](https://s1.ax1x.com/2020/03/17/8NZNlT.png)

秒杀的关键就是==如何高效的处理这种竞争！==

<u>关于高并发等优化现在不考虑，先把基本的流程进行完善之后，在进行深一步的挖掘优化。</u>

## 当前需要完成的功能

![8Newgf.png](https://s1.ax1x.com/2020/03/17/8Newgf.png)

接口暴露：防止用户提前拿到秒杀的接口，从而使用插件脚本进行秒杀。保证秒杀公平。

执行秒杀：用户在秒杀开启后可以正常的进行秒杀操作。无论秒杀成败都应该正确处理并响应。

相关查询：商品、订单等查看。

## 数据库创建

库存表和秒杀成功信息表。

库存表：

```mysql
create table seckill(
    seckill_id bigint not null AUTO_INCREMENT COMMENT '商品库存id',
    name varchar(120) not null comment '商品名称',
    number int NOT NULL comment '库存数量',
    create_time  timestamp not null default CURRENT_TIMESTAMP comment '创建时间',
    start_time timestamp  not null  comment '秒杀开始时间',
    end_time timestamp not null comment '秒杀结束时间',
    PRIMARY KEY (seckill_id),/*主键*/
    /*其他索引，加速查询*/
    key idx_start_time(start_time),
    key idx_end_time(end_time),
    key idx_create_time(create_time)
) ENGINE= InnoDB AUTO_INCREMENT=1000 DEFAULT CHARSET=utf8 COMMENT='秒杀库存表'; /*采用InnoDB引擎支持事务*/
```

秒杀成功信息表：

```mysql
create table success_killed(
    seckill_id bigint not null comment '秒杀商品id',
    user_phone bigint not null comment '用户手机号',
    state tinyint not null default -1 comment '状态标示：-1无效，0：成功 1：已付款',
    create_time timestamp not null comment '创建时间',
    /*联合主键,可以顺便防止一个用户多次秒杀*/
    PRIMARY KEY(seckill_id,user_phone),
    key idx_create_time(create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='秒杀成功明细表';
```

## 实体类编写

<u>要注意如果存在有参构造方法，就必须存在无参构造方法。</u>

<u>如果没有手动添加构造方法，就会使用隐式的默认无参构造。</u>

## Dao层编码

==编写Mybatis-config.xml文件==：自增获取、别名、驼峰命名等等。

==接口设计和SQL编写==，选择mapper.xml+接口自动实现的方式，好处是需要注意的问题：

1. 秒杀成功信息mapper中插入商品id和手机号的时候可能会出现主键冲突报错的情况(重复了)，但是不希望发生重复的时候报错，所以我们采用ignore关键字，"insert ignore"之后出现主键冲突的时候不会报错而是返回0。方便业务代码处理。

2. 秒杀减库存方法中要比较当前时间和秒杀开启时间和秒杀结束时间，需要用到<=、>=。可以进行替换

   | <     | <=     | >     | >=     | &      | '       | "       |
   | ----- | ------ | ----- | ------ | ------ | ------- | ------- |
   | \&lt; | \&lt;= | \&gt; | \&gt;= | \&amp; | \&apos; | \&quot; |

   或者修改成： 大于等于 \<![CDATA[ >= ]]> ；小于等于 \<![CDATA[ <= ]]>  的方式。

3. 秒杀成功信息实体中有秒杀库存类型的属性，怎么在使用秒杀商品id查询出某条秒杀成功信息数据的同时将属性赋值给秒杀成功信息实体以及其属性中的秒杀库存类型的对象。sql语句中使用inner join做连接查询之后，select列名：

   ![8UUOaR.png](https://s1.ax1x.com/2020/03/17/8UUOaR.png)

   sk是秒杀成功信息表，s是秒杀库存表。在mybatis-config.xml开启别名和驼峰命名法之后，会将形如user_phone转换成userPhone去和实体对象的属性字段去匹配赋值，seckill.seckill_id会和成功秒杀信息实体对象的seckill对象属性的seckillId字段去匹配赋值。

==Spring整合Mybatis==

1. 数据库相关配置；
2. 连接池连接属性和私有属性(连接池数量关闭连接后不自动commit、获取连接超时时间、连接失败重试次数等等)；
3. 配置sqlSessionFactory(连接池注入、mybatis-config.xml、扫entity包开别名、扫描mapper需要的xml文件)；
4. 扫描Dao接口包(动态实现Dao接口，注入到spring容器中。需要给出会话工厂和包位置)。会话工厂要用value引入bean的id，不能使用ref，因为使用了jdbc.properties文件，这个加载比较慢，如果用ref的话jdbc配置文件还没有加载到dataSource中，会报错 。

**<u>到这里其实可以对Dao层接口进行一次单元测试，确定接口都是可以调通的。</u>**

## Service层编码

Dao层完成了接口设计和SQL编写没有进行任何逻辑代码编写，好处是代码和SQL分离方便review。Dao拼接等逻辑在Service层完成。

==创建包==：service存放业务接口和接口实现、exception存放需要定义的的异常类、dto存放数据传输层需要的封装类(<u>和pojo概念上相似但是不同，pojo主要是封装实体类和表对应之类的，dto的封装类主要是方便接口传参之类的</u>)、enums存放执行秒杀状态码的枚举类。

==创建接口==：SeckillService接口包括：查询所有秒杀记录、根据Id查询秒杀记录、暴露秒杀地址接口(秒杀开启输出秒杀接口地址，否则输出系统时间和秒杀开启时间)。

==创建dto类==：Exposer用来封装暴露秒杀地址返回参数，作为暴露秒杀地址接口的返回类型。SeckillExecution封装成功秒杀后的信息，作为秒杀执行接口的返回类型。

==创建enum类==：数据字典，主要是秒杀执行状态码。

==创建异常类==：秒杀业务父异常、重复秒杀异常、秒杀关闭异常。异常都是运行时异常，Spring声明式事务只接受运行时异常并进行回滚，如果是编译时异常不会接受并进行回滚。

==service.impl包中实现接口==：md5采用直接id+盐值拼接后转换。可能抛出异常的方法进行try catch，异常"从小到大"catch，将所有编译时异常统一转换成运秒杀业务父异常(运行时异常)，便于Spring声明式事务接受异常对秒杀事务进行回滚。

==使用Spring声明式事务==：

```
开启事务;
修改型sql1;
修改型sql2;
修改型sql3;
修改型sql4;
...
提交事务;
```

将上述事务模式交由Spring框架进行管理，叫做Spring声明式事务。

声明式事务独有的一个概念"事务方法嵌套"，主要体现在"传播行为"上：当我们有多个方法调用的时候是创建一个新事务还是加入已有的事务。Spring默认传播行为是：propagation_required，意思是当新事务进来时候直接加入原有的事务中，没有原有事务就创建新事务。

上面多次交代：Spring声明式事务默认只接受运行时异常并进行回滚，如果是编译时异常不会接受并进行回滚。

==spring-service.xml==：扫描service包下的所有使用的注解、配置事务管理器

**<u>到这里可以对service层接口进行一次单元测试，确定接口都是可以调通的。</u>**

## Web端设计

==前端交互设计==：

前端页面的之间的逻辑：

![8sC0Yj.png](https://s1.ax1x.com/2020/03/19/8sC0Yj.png)

当前业务最重要的详情页逻辑设计：

![8sCU0S.png](https://s1.ax1x.com/2020/03/19/8sCU0S.png)

==URL设计==：/模块/资源/{标识}/集合1/...

URL中不出现"动词"，只定位资源。对资源进行的动作则有请求类型GET、POST、PUT、DELETE等等来表示。

GET /seckill/list 秒杀列表

GET /seckill/{id}/detail 详情页

GET /seckill/time/now 系统时间

POST /seckill/{id}/exposer 暴露秒杀

POST /seckill/{id}/{md5}/execution 执行秒杀

==配置MVC==：配置spring-web.xml、web.xml。

==实现Controller==：基于上述URL设计RESTful风格的API。

==jsp编写==：BootStrap+jQuery、js逻辑。暂时用cookie存用户手机号代替登录，因为这不是本次业务的重点。

**<u>至此完成了业务的基本编码，同时dao、service使用spring整合junit4进行了单元测试、web(Controller)使用PostMan工具对接口进行的了测试，确保业务流程已经调通。</u>**

## 性能优化分析

==页面等静态资源CDN优化==：将detail页静态化之后和其他的静态资源(css、js等等)部署到CDN节点上。这样用户刷新页面操作，就不会直接访问我们的系统服务获取静态资源了，除了这些静态资源之外其余的暴露地址、执行秒杀、获取系统时间等请求依然是发往系统服务器的。

![8hme78.png](https://s1.ax1x.com/2020/03/21/8hme78.png)

但是静态化页面之后，页面就不能拿到系统时间了，所以前面web层单独写了一个获取系统时间的接口用来给页面去做请求。

==暴露秒杀地址Redis缓存优化==：这个是没法做CDN优化的，因为这个请求结果是实时变化的。但是可以使用Redis在服务器端进行缓存。

![8hJSW6.png](https://s1.ax1x.com/2020/03/21/8hJSW6.png)

请求先到Redis查询是否有对应得秒杀地址，如果没有再去查数据库获取秒杀商品的相关时间。一致性很好维护，对Redis中缓存的内容做超时清除处理(时间到了清掉对应缓存，之后查询将到达MySQL)，当MySQL商品数据发生修改时主动更新rRedis。

==秒杀操作优化==：无法使用CDN优化、也不能使用Redis去进行缓存，因为库存信息变化太快一致性难以维护。

这里采用Redis实现一个原子计数器，记录商品库存。请求成功之后对应商品的库存原子计数器减少，并记录一个行为"谁成功执行"，并将这个行为放入MQ。最后后端服务去MQ中消费之前产生的消息，并记录到MySQL当中。

![8hUL5j.png](https://s1.ax1x.com/2020/03/21/8hUL5j.png)

每个秒杀执行事务内update减库存之后通知执行insert插入信息之间存在着延迟和可能发生的GC，insert之后通知执行提交或回滚事务之间也存在着网络延迟和可能发生的GC，如下图形式：

![8hgBRS.png](https://s1.ax1x.com/2020/03/21/8hgBRS.png)

秒杀执行的瓶颈点就在于：<u>**Java和数据库通信所产生的延迟、过程中可能发生的GC会暂停**</u>。

MySQL中如果多个秒杀执行事务对同一个id进行，第一个事务会拿到行级锁，其余的事务要等待这个行级锁释放。只有当第一个事务提交或者回滚事务之后这个行级锁才会释放，阻塞的事务才能获得这个行级锁。这种情况下，已经将对同一行数据进行操作的事务变成了串行化的处理方式。

**<u>综上所述，优化的方向在于减少每个事务持有行级锁的时间。这个时间在不考虑执行SQL语句所花费的时间的情况下，就主要取决去<font color=red>Java客户端和数据库通信的延迟和Java本身GC所造成的事务代码暂停的时间</font>。</u>**

**<u>针对上述分析，优化思路是：<font color=red>把客户端事务逻辑放到MySQL服务器上，从而避免通信延迟和GC的影响</font>。</u>**

优化实现方式：使用**<font color=red><u>存储过程</u></font>**将整个事务都在MySQL端完成，而不再是Spring客户端控制事务。

## 优化实现

CDN主要是部署的问题，暂时先直说编码开发的问题。

==使用Rdies缓存优化暴露秒杀地址接口==：先引入jedis客户端依赖，在dao包下创建cache.RedisDao类用来完成对缓存的操作。类中有Seckill getSeckill(long id)方法和String putSeckill(Seckill seckill)方法，分别用来获取缓存中的Seckill对象和向缓存中放入Seckill对象。

Redis内部并没有实现对象的序列化操作，get缓存中的对象其实是：get一个byte[]数组，我们需要将这个数组反序列化成为Java对象；set到缓存：先将对象序列化为byte[]，再放入缓存。

最简单的解决方法就是让Seckill对象实现Serializable接口，使用JDK提供的序列化方法。但是无论是从序列化速度还是序列化结果的字节数来看这个方法都并不高效。

所以选择了自定义序列化，采用Google生产的序列化方案protostuff。先引入protostuff需要的依赖，然后使用RuntimeSchema对象去进行对象的序列化操作。

编码完成后，将RedisDao交给Spring容器去管理，到spring-dao.xml中进行bean的注入配置(只是为了测试使用，否则应该有Redis自己的配置文件)。

**<u>然后对RedisDao进行测试。</u>**

RedisDao测试没问题之后，对SeckillServiceImpl的exposerSeckillUrl方法进行调整。先去缓存中查，如果缓存中没有再去数据库查，如果数据库有就放入缓存中，如果数据库也没有则直接返回暴露失败没有这条id对应的秒杀信息。

<font color=red>**Redis缓存的一致性是通过超时完成的。一般不允许修改秒杀商品的信息，如果需要修改就删除数据库中的旧秒杀信息重建新的秒杀信息。**</font>

**<u>优化完成之后进行单元测试，观察redis和数据库中的变化。</u>**

==优化秒杀执行接口==：秒杀事务执行逻辑是update拿到行锁，insert后commit/rollback之后free行级锁。

<u>对这个事务逻辑可以进行一些优化</u>：事务开始先insert返回结果，成功插入后在执行update拿行锁，最后commit释放锁。这样做的好处：减少了每个事务持有锁的时间，同时也先行阻挡了一些重复秒杀的请求。

上述事务逻辑优化只需要调整一下service方法中语句的执行顺序即可。

但是到目前为止，我们的事务依然是交由Spring来管理的。虽然经过顺序调整后，持有锁的时间得到了优化，但是依然没有达到之前分析的结果：<u>减少延迟+GC的影响。</u>

所以需要去实现之前性能分析时提到的解决方案：<u>**存储过程**</u>。

首先去seckill数据库中创建需要的存储过程(创建语句在sql包下)，逻辑和Java中优化之后的事务逻辑一样。

创建好存储过程，再到service中创建一个executeSeckillProcedure方法用来和之前的执行秒杀方法进行区分。在新创建的方法中调用我们创建好的存储过程即可，这样就将事务控制全权交由数据库来直接管理，Java代码只需要关心最终事务结束后的返回值即可。

新service方法只需要去调用SeckillDao中新创建的调用存储过程的接口。

**<u>旧的执行秒杀接口修改之后和新的执行秒杀接口编写完成之后，都要进行单元测试。</u>**

完成以上操作，将Controller对应方法所使用的service执行秒杀接口修改为新的执行秒杀接口，用Postman等接口测试工具进行测试，然后运行tomcat容器进行前后端交互的debug测试。

## 完结撒花

最后简单的进行一下压力测试，测试本次秒杀业务所能承载的QPS。