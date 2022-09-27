### 为什么要使用单例？
#### 处理资源访问冲突
#### 表示全局唯一类
### 如何实现一个单例？
* 构造函数需要是 private 访问权限的，这样才能避免外部通过 new 创建实例；
* 考虑对象创建时的线程安全问题；
* 考虑是否支持延迟加载；
* 考虑 getInstance() 性能是否高（是否加锁）。

1. 饿汉式

在类加载的时候，instance 静态实例就已经创建并初始化好了，所以，instance 实例的创建过程是线程安全的。不过，这样的实现方式不支持延迟加载（在真正用到 IdGenerator 的时候，再创建实例）。

```java
public class IdGenerator { 
  private AtomicLong id = new AtomicLong(0);
  private static final IdGenerator instance = new IdGenerator();
  private IdGenerator() {}
  public static IdGenerator getInstance() {
    return instance;
  }
  public long getId() { 
    return id.incrementAndGet();
  }
}
```

2. 懒汉式

懒汉式相对于饿汉式的优势是支持延迟加载,不过懒汉式的缺点也很明显，我们给 getInstance() 这个方法加了一把大锁（synchronzed），导致这个函数的并发度很低。

```java
public class IdGenerator { 
  private AtomicLong id = new AtomicLong(0);
  private static IdGenerator instance;
  private IdGenerator() {}
  public static synchronized IdGenerator getInstance() {
    if (instance == null) {
      instance = new IdGenerator();
    }
    return instance;
  }
  public long getId() { 
    return id.incrementAndGet();
  }
}
```

3. 双重检测

饿汉式不支持延迟加载，懒汉式有性能问题，不支持高并发。那我们再来看一种既支持延迟加载、又支持高并发的单例实现方式，也就是双重检测实现方式。在这种实现方式中，只要 instance 被创建之后，即便再调用 getInstance() 函数也不会再进入到加锁逻辑中了。所以，这种实现方式解决了懒汉式并发度低的问题。

```java
public class IdGenerator { 
  private AtomicLong id = new AtomicLong(0);
  private static IdGenerator instance;
  private IdGenerator() {}
  public static IdGenerator getInstance() {
    if (instance == null) {
      synchronized(IdGenerator.class) { // 此处为类级别的锁
        if (instance == null) {
          instance = new IdGenerator();
        }
      }
    }
    return instance;
  }
  public long getId() { 
    return id.incrementAndGet();
  }
}
```
网上有人说，这种实现方式有些问题。因为指令重排序，可能会导致 IdGenerator 对象被 new 出来，并且赋值给 instance 之后，还没来得及初始化（执行构造函数中的代码逻辑），就被另一个线程使用了。要解决这个问题，我们需要给 instance 成员变量加上 volatile 关键字，禁止指令重排序才行。实际上，只有很低版本的 Java 才会有这个问题。我们现在用的高版本的 Java 已经在 JDK 内部实现中解决了这个问题（解决的方法很简单，只要把对象 new 操作和初始化操作设计为原子操作，就自然能禁止重排序）。jdk8已经确保new、初始化为原子性操作，不会出现JIT导致指令重排的情况

```java
class Foo {
    private volatile Helper helper;

    public Helper getHelper() {
        Helper localRef = helper; // --> 1
        if (localRef == null) {     
            synchronized (this) {
                localRef = helper;  // --> 2
                if (localRef == null) {
                    helper = localRef = new Helper();
                }
            }
        }
        return localRef;  // --> 3  
    }
    // other functions and members...
}
```
1. -->1 Using localRef, we are reducing the access of volatile variable to just one for positive usecase. If we do not use localRef, then we would have to access volatile variable twice - once for checking null and then at method return time. Accessing volatile memory is quite an expensive affair because it involves reaching out to main memory.
2. -->2 Refreshing local reference to latest value after acquiring a lock, since volatile variable may have changed by this time due.
3. -->3 volatile variable is accessed at method return time.

静态变量有了 volatile 关键字，需要从主存同步，修改又要同步到主存，消耗了io，而局部变量和主存无关，只要最后一次同步到主存上去就好了.
多个线程在多核CPU上跑，每个CPU都包含自己独有的缓存（解决读写效率的问题）。volatile的目的就是让CPU缓存中的数据"强行"刷新到内存中，从而保证各个线程都能够看到数据的变化(即保证"可见性")

REF: [Threadsafe Singleton Design Pattern Java](https://www.javacodemonk.com/threadsafe-singleton-design-pattern-java-806ad7e6)

1. 静态内部类

再来看一种比双重检测更加简单的实现方法，那就是利用 Java 的静态内部类。它有点类似饿汉式，但又能做到了延迟加载。

```java
public class IdGenerator { 
  private AtomicLong id = new AtomicLong(0);
  private IdGenerator() {}

  private static class SingletonHolder{
    private static final IdGenerator instance = new IdGenerator();
  }
  
  public static IdGenerator getInstance() {
    return SingletonHolder.instance;
  }
 
  public long getId() { 
    return id.incrementAndGet();
  }
}
```

SingletonHolder 是一个静态内部类，当外部类 IdGenerator 被加载的时候，并不会创建 SingletonHolder 实例对象。只有当调用 getInstance() 方法时，SingletonHolder 才会被加载，这个时候才会创建 instance。instance 的唯一性、创建过程的线程安全性，都由 JVM 来保证。所以，这种实现方法既保证了线程安全，又能做到延迟加载。

5. 枚举

这种实现方式通过 Java 枚举类型本身的特性，保证了实例创建的线程安全性和实例的唯一性。

```java
public enum IdGenerator {
  INSTANCE;
  private AtomicLong id = new AtomicLong(0);
 
  public long getId() { 
    return id.incrementAndGet();
  }
}
```
Java规范字规定，每个枚举类型及其定义的枚举变量在JVM中都是唯一的，因此在枚举类型的序列化和反序列化上，Java做了特殊的规定。在序列化的时候Java仅仅是将枚举对象的name属性输到结果中，反序列化的时候则是通过java.lang.Enum的valueOf()方法来根据名字查找枚举对象。也就是说，序列化的时候只将DATASOURCE这个名称输出，反序列化的时候再通过这个名称，查找对应的枚举类型，因此反序列化后的实例也会和之前被序列化的对象实例相同