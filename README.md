## 👋前言 

　　该引擎设计用途是 JVM 端控制台小游戏开发，也可用于其它用途，目前实现了在控制台中显示“Bad Apple”的功能。

[简体中文](README.md) | [English](README_en.md)

✨ **Demo**

https://user-images.githubusercontent.com/41804496/231933915-4928c5a4-e8c7-47f1-b411-90396c27a5e8.mp4


🛠️目前引擎支持以下功能：

- 任意位置填充字符串
- 任意位置修改文本属性
- 监听键盘和鼠标的按键
- 无闪屏刷新（多缓冲）

⚠️注意：引擎目前只能在 **Windows** 平台使用，不支持其它平台！

　　下面我们开始说明引擎的使用方法。

## ⚙️初始化和销毁 

　　使用引擎之前，需要先初始化控制台的信息， `GMap.Builder` 中已经封装好了初始化的方法，调用 `GMap.Builder.build()` 时会自动初始化控制台信息。

　　我们还可以使用 `ConsolePrinter.init` 进行手动初始化， 但是 **请注意** ：如果已经通过 `GMap.Builder` 进行了初始化操作，请勿通过 `ConsolePrinter.init` 重复初始化。

　　如果游戏已经结束需要退出控制台，则需调用 `GMap.Builder.dispose()` 销毁控制台，正如注释中所说的一样，该函数只有在所有的 `GMap` 对象被虚拟机回收或调用 `close()` 函数后才会销毁控制台，这个设计是为了防止控制台被销毁后继续通过 `GMap` 操作控制台，从而导致程序崩溃。

　　如果需要强制销毁，可以手动调用 `ConsolePrinter.dispose()` ，这个函数不会进行销毁检查，但是务必注意销毁控制台后不能再通过引擎对控制台进行操作。

　　PS：控制台销毁后可以重新初始化。

　　代码示例：

🔔每个模块都将提供**Kotlin** / **Java** 两种语言的代码示例

**Kotlin**

```Kotlin
fun main() {
    GMap.Builder.apply {
        width = 80
        height = 40
    }.build().use {
        // do something
        // 通过某种方法阻塞主线程直到程序结束
    }
    GMap.Builder.dispose()
}
```

**Java**

```Java
public class Main {

    public static void main(String[] args) {
        GMap.BuilderJava.INSTANCE
                .setWidth(80)
                .setHeight(40);
        try (GMap map = GMap.Builder.build()) {
            // do something
            // 通过某种方法阻塞主线程直到程序结束
        }
        GMap.Builder.dispose();
    }

}
```

　　上述代码使用 `Builder` 创建了一个横向 80 个字符，纵向 40 个字符的地图， `BuilderJava` 是为 Java 语言特化的类，用于优化 Java 的调用体验，可以与 `Builder` 类混用。

　　接下来我们介绍 `Builder` 中各个字段的作用（括号中为缺省值）：

- `width` - 横向字符数量
- `height` - 纵向字符数量
- `fontWidth` - 字符宽度（10）
- `cache` - 缓存数量（2）
- `ignoreClose` - 是否忽略 `Ctrl + C` 一类的控制快捷键（false）
- `file` - DLL 文件的路径（ `./utils.dll` ）

　　由于控制台限制，字符宽度只能保证字符宽度与设置的值接近，并不能保证恒等于设置的值。理想的拉丁字符宽高比为 `1:2` ，中文字符宽高比为 `1:1` ，在有些时候会偏离这个比例，如果希望达到完美比例，需要调整 `fontWidth` 的值。

## ⌨️键鼠监听 

　　引擎支持监听键盘和鼠标的按键输入（暂不支持鼠标坐标监听），相关函数均封装在 `EventListener` 中，调用 `pushEvent()` 函数即可发布一次键盘事件。

　　可以通过 `registry...` 函数注册事件：

- `registryKeyboardEvent` - 注册键盘事件
- `registryMouseEvent` - 注册鼠标事件

　　两个函数均有对应的 `remove` 函数，可以用于移除指定事件。

　　下面给出监听事件的示例代码：

**Kotlin**

```Kotlin
fun main() {
    EventListener.registryKeyboardEvent(object : IKeyboardListener {
        
        override fun onPressed(code: Int) {
            println("pressed: $code")
        }

        override fun onReleased(code: Int) {
            println("released: $code")
        }

        override fun onActive(code: Int) { }
        
    })
    while (true) {
        EventListener.pushEvent()
        Thread.sleep(10)
    }
}
```

**Java**

```Java
public class Main {

    public static void main(String[] args) {
        EventListener.registryKeyboardEvent(new IKeyboardListener() {
            
            @Override
            public void onPressed(int code) {
                System.out.println("pressed: " + code);
            }

            @Override
            public void onReleased(int code) {
                System.out.println("released: " + code);
            }

            @Override
            public void onActive(int code) { }
            
        });
        while (true) {
            EventListener.pushEvent();
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
        }
    }

}
```

请注意：调用 `pushEvent` 的时候，引擎只能检测当前按下的按键，无法检测调用 `pushEvent` 之前按下的按键，如果两次 `pushEvent` 调用的时间间隔过大，会非常容易漏掉一些输入，所以应当适当提高调用频率。

## 💿自定义实体 

　　引擎中所有内容都被设计为“实体”，实体有四个通用属性：

- `visible` - 标记实体是否可见，为 `false` 时将跳过渲染
- `collisible` - 标记实体是否具有碰撞箱，为 `false` 时将跳过碰撞箱检测
- `died` - 标记实体是否死亡，为 `true` 时将自动从地图中移除
- `x/y/width/height` - 坐标和尺寸信息

　　实体（ `GEntity` ）中提供了一些事件：

- `render` - 渲染时触发，用于渲染该实体，不可见实体不会触发该事件
- `update` - 每次逻辑循环的更新事件
- `beKilled` - 被其它实体“击杀”后触发
- `onCollision` - 与其它实体碰撞时触发
- `onRemove` - 实体被从地图中移除后触发

　　实体中还需要实现一些工具函数：

- `getCollision` - 获取当前实体指定区域内所有具有碰撞体积的点
- `copy` - 深拷贝当前对象

　　下面我们给出一个代码示例，在下面的代码中，我们实现了一个矩形可视可碰撞的实体（ `render` 函数中的代码后面会说明）：

**Kotlin**

```Kotlin
class BlockEntity(
    override var x: Int,
    override var y: Int,
    override val width: Int,
    override val height: Int
) : GEntity {
    
    override val collisible = true
    override var died = false
        private set

    override fun render(graphics: SafeGraphics) {
        graphics.fillRect('#', 0, 0, width, height)
    }

    override fun getCollision(x: Int, y: Int, width: Int, height: Int): Stream<Point2D> {
        val builder = Stream.builder<Point2D>()
        val right = x + width
        val bottom = y + height
        for (i in y until bottom) {
            for (k in x until right) {
                builder.add(Point2D(k, i))
            }
        }
        return builder.build()
    }

    override fun update(map: GMap, time: Long) {
        // do something
    }

    override fun beKilled(map: GMap, killer: GEntity) {
        died = true
    }
    
    override fun copy() = BlockEntity(x, y, width, height)

}
```

**Java**

```Java
public class BlockEntity implements GEntity {

    private boolean died = false;
    private int x;
    private int y;
    private final int width;
    private final int height;
    
    public BlockEntity(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public void render(@NotNull SafeGraphics graphics) {
        graphics.fillRect('#', 0, 0, width, height, -1);
    }

    @NotNull
    @Override
    public Stream<Point2D> getCollision(int x, int y, int width, int height) {
        Stream.Builder<Point2D> builder = Stream.builder();
        int right = x + width;
        int bottom = y + height;
        for (int i = y; y != bottom; ++y) {
            for (int k = x; k != right; ++k) {
                builder.add(new Point2D(k, i));
            }
        }
        return builder.build();
    }

    @Override
    public void update(@NotNull GMap map, long time) {
        // do something
    }

    @Override
    public void beKilled(@NotNull GMap map, @NotNull GEntity killer) {
        died = true;
    }

    @Override
    public boolean getCollisible() {
        return true;
    }

    @Override
    public boolean getDied() {
        return died;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @NotNull
    @Override
    public GEntity copy() {
        return new BlockEntity(x, y, width, height);
    }
    
}
```

## 🖊️使用画笔 

　　引擎提供了 `SafeGraphics` 类用来绘制图形，其中封装了基础的绘制函数。同时正如其名所说的一样，这个类是一个“安全”的画笔类，它可以确保绘制的内容不会超过为其设定的边界。

`GEntity#render` 函数会接收一个 `SafeGraphics` 的对象， `GMap` 会自动为其设定参数，在实体中使用时，该画笔的绘制坐标是相对于实体本身的，也就是说使用如下代码会在实体的左上角绘制一个 `#` ，而非是在地图左上角绘制：

```
graphics.fillRect(#, 0, 0, 1, 1)
```

`SafeGraphics` 中很多函数都提供了缺省参数，不过由于 Java 不支持这个特性，所以如果使用 Java 编写代码，则需要手动填入所有参数。

　　至于绘制中所用到的一些概念如下所示：

- ATTR： 这是用于控制终端字体颜色、背景颜色等属性的值，所有支持的类型已在 `ConsolePrinter` 中列出。需要注意的是，在调用 `flush` 函数时， 同样不会清除上一次设置的 ATTR 信息。
-  除 clear 系列函数外的所有函数，传入 `attr = -1` 表示无效 attr，打印内容时将忽略 attr 信息。
- 字符宽度： 在控制台中，不同字符宽度不同，拉丁文字符宽度为 1，而中文字符宽度为 2，计算字符宽度时，满足 `char < 0x100` 的宽度视为 1，否则为 2。

　　更多的内容可以查阅 `ConsolePrinter` 类中的注释，其中有详细说明。

　　一般情况下，我们 `GMap` 类会自动为用户生成画笔，并不需要用户自己创建，但是如果想要手动创建画笔对象的话可以参考以下代码：

**Kotlin**

```Kotlin
fun main() {
    GMap.Builder.apply {
        width = 80
        height = 40
        file = File("D:\\Workspace\\jni\\cmake-build-release\\libjni.dll")
    }.build().use {
        // 创建一个和地图等大的画笔
        val g1 = SafeGraphics(it)
        // 创建一个绘制起点为 (10, 5)，绘制区域大小为 20x10 的画笔，绘制区域受地图尺寸限制
        val g2 = SafeGraphics(it, 10, 5, 20, 10)
        // 创建一个绘制起点为 (10, 5)，绘制区域大小为 20x10 的画笔，绘制区域不受任何限制
        val g3 = HalfSafeGraphics(10, 5, 20, 10, ConsolePrinter.index)
        // do something
    }
    GMap.Builder.dispose()
}
```

**Java**

```Java
public class Main {

    public static void main(String[] args) {
        GMap.BuilderJava.INSTANCE
                .setWidth(80)
                .setHeight(40);
        try (GMap map = GMap.Builder.build()) {
            // 创建一个和地图等大的画笔
            SafeGraphics g1 = SafeGraphics.createSafeGraphics(map);
            // 创建一个绘制起点为 (10, 5)，绘制区域大小为 20x10 的画笔，绘制区域受地图尺寸限制
            SafeGraphics g2 = SafeGraphics.createSafeGraphics(map, 10, 5, 20, 10);
            // 创建一个绘制起点为 (10, 5)，绘制区域大小为 20x10 的画笔，绘制区域不受任何限制
            SafeGraphics g3 = SafeGraphics.createHalfSafeGraphics(10, 5, 20, 10, ConsolePrinter.getIndex());
            // do something
        }
        GMap.Builder.dispose();
    }

}
```

## ⏲️时序控制 

　　引擎支持用户自己进行时序控制，也支持让引擎接管所有时序控制，调用 `GMap#start` 即可使引擎接管所有任务。

　　请注意：如果让引擎接管时序控制，逻辑任务和事件任务将不在一个线程中执行，可以使用 `GMap#runTaskOnLogicThread` 将一个任务添加到逻辑线程中执行，该函数是线程安全的。

　　代码如下所示：

**Kotlin**

```Kotlin
fun main() {
  GMap.Builder.apply {
    width = 80
    height = 40
  }.build().use { map ->
    EventListener.registryKeyboardEvent(object : IKeyboardListener {

      override fun onPressed(code: Int) {
        map.runTaskOnLogicThread {
          println("pressed: $code")
          true
        }
      }

      override fun onReleased(code: Int) {
        map.runTaskOnLogicThread {
          println("released: $code")
          true
        }
      }

      override fun onActive(code: Int) { }

    })
    map.start(10, 50) { true }
  }
  GMap.Builder.dispose()
}
```

**Java**

```Java
public class Main {

    public static void main(String[] args) {
        GMap.BuilderJava.INSTANCE
                .setWidth(80)
                .setHeight(40);
        try (GMap map = GMap.Builder.build()) {
            EventListener.registryKeyboardEvent(new IKeyboardListener() {

                @Override
                public void onPressed(int code) {
                    map.runTaskOnLogicThread(() -> {
                        System.out.println("pressed: $code");
                        return false;
                    });
                }

                @Override
                public void onReleased(int code) {
                    map.runTaskOnLogicThread(() -> {
                        System.out.println("released: $code");
                        return false;
                    });
                }

                @Override
                public void onActive(int code) { }

            });
            map.start(10, 50, () -> true);
        }
        GMap.Builder.dispose();
    }

}
```

　　以上就是引擎的基本用法，代码注释中已经尽量详细的描述代码的用法和注意事项，如果存有疑问欢迎提 issue。
