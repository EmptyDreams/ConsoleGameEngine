## 👋 Foreword

The engine is designed for JVM -side Console game development, and can also be used for other purposes. Currently, the function of displaying "Bad Apple" in the Console is implemented.

✨ **Demo**

[https://youtu.be/8MEbOSIptuA](https://user-images.githubusercontent.com/41804496/231933915-4928c5a4-e8c7-47f1-b411-90396c27a5e8.mp4  

Synchronously published in： [Bilibili](https://www.bilibili.com/video/BV1kM411T7Bh) | [Youtube](https://www.youtube.com/watch?v=8MEbOSIptuA))  

🛠️ engine currently supports the following features:

- Fill string anywhere
- Modify text properties anywhere
- Listen for keyboard and mouse keys
- No splash screen refresh (multi-buffer)

⚠️ Note: The engine is currently only available on the **Windows** platform and is not supported on other platforms!

Below we begin to explain the use of the engine.

## ⚙️ initialization and destruction

Before using the engine, you need to initialize the information of the Console. The initialization method has been encapsulated in `GMap. Builder `. The Console information will be automatically initialized when calling `GMap.Builder.build () `.

We can also use `ConsolePrinter.init `for manual initialization, but **please note that** if you have already done the initialization with `GMap. Builder `, do not repeat the initialization with `ConsolePrinter.init `.

If the game has ended and you need to exit the Console, you need to call `GMap. Builder.dispose () `to destroy the Console. As mentioned in the comment, this function will only destroy the Console after all `GMap `objects are recycled by the virtual machine or call the `close () `function., this design is to prevent the Console from continuing to operate the Console through the `GMap `after the Console is destroyed, which will cause the program to crash.

If you need to force destruction, you can manually call `ConsolePrinter.dispose () `, this function will not destroy the check, but be sure to pay attention to the destruction of the Console can not be operated through the engine Console.

PS: Console can be reinitialized after destruction.

Code examples:

🔔 each sample module will provide code samples in both **Kotlin** / **Java** languages

**Kotlin**

```Kotlin
fun main() {
    GMap.Builder.apply {
        width = 80
        height = 40
    }.build().use {
        // do something
        //block the main thread in some way until the program ends
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
            //block the main thread in some way until the program ends
        }
        GMap.Builder.dispose();
    }

}
```

The above code uses `Builder `to create a horizontal 80 character, vertical 40 character map, `BuilderJava `is specialized for Java language classes, used to optimize the Java call experience, can be mixed with the `Builder `class.

Next, we introduce the role of each field in the `Builder `(Default Value in parentheses):

- `Width `- number of landscape characters
- `Height `- number of vertical characters
- `fontWidth `- character width (10)
- `Cache `- number of caches (2)
- `ignoreClose `- Whether to ignore control shortcuts like `Ctrl + C `(false)
- `File `- Path to the DLL file ( `./utils.dll `)

Due to Console restrictions, the character width can only ensure that the character width is close to the set value, and cannot guarantee that it will always be equal to the set value. The ideal Latin character aspect ratio is `1:2 `, and the Chinese character aspect ratio is `1:1 `. Sometimes it deviates from this ratio. If you want to achieve the perfect ratio, you need to adjust the value of `fontWidth `.

## ⌨️ keyboard and mouse monitor

The engine supports monitoring keyboard and mouse key input (mouse coordinate monitoring is not supported for the time being), the related functions are packaged in `EventListener `, and the `pushEvent () `function can be used to publish a keyboard event.

You can register events through `the registry... `function:

- `registryKeyboardEvent `- register keyboard events
- `registryMouseEvent `- Register mouse events

Both functions have a corresponding `remove `function that can be used to remove specified events.

Here is an example code for listening events:

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

Please note: When calling `pushEvent `, the engine can only detect the currently pressed button, not the button pressed before calling `pushEvent `. If the time interval between two `pushEvent `calls is too large, it will be very easy to miss some inputs, so it should be appropriate Increase the call frequency.

## 💿 custom entities

Everything in the engine is designed as an "entity" with four general properties:

- `Visible `- Mark whether the entity is visible, `false `will skip rendering
- `Collisible `- marks whether the entity has a collision box, `false `will skip collision box detection
- `Died `- Marks whether the entity is dead, when `true `it will be automatically removed from the map
- `X/y/width/height `- coordinate and dimension information

Some events are provided in the `GEntity `:

- `Render `- triggered when rendering, used to render the entity, invisible entities do not trigger the event
- `Update `- update event for each logical loop
- `beKilled `- Triggered after being "killed" by another entity
- `onCollision `- triggered on collision with another entity
- `onRemove `- triggered when the entity is removed from the map

The entity also needs to implement some tool functions:

- `getCollision `- get all points with collision volume in the specified area of the current entity
- `Copy `- deep copy the current object

In the following code example, we implement a rectangular visually collidable entity (the code in the `render `function will explain later):

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

## 🖊️ use brushes

The engine provides the `SafeGraphics `class for drawing graphics, which encapsulates the basic drawing function. At the same time, as the name says, this class is a "safe" brush class, which ensures that the content drawn does not exceed the boundaries set for it.

`GEntity #render `function will receive a `SafeGraphics `object, `GMap `will automatically set parameters for it, when used in the entity, the drawing coordinates of the brush are relative to the entity itself, that is to say, the following code will be used in the upper left corner of the entity Draw a `# `instead of drawing in the upper left corner of the map:

```
graphics.fillRect(#, 0, 0, 1, 1)
```

`Many functions in SafeGraphics `provide default parameters, but since Java do not support this feature, if you write code using Java, you need to manually fill in all parameters.

Some of the concepts used in the drawing are as follows:

- ATTR: This is the value used to control properties such as end point font color, background color, etc. All supported types are listed in `ConsolePrinter `. It should be noted that when calling the `flush `function, The last set ATTR information will also not be cleared.
- For all functions except the clear series function, passing in `attr = -1 `means invalid attr, and attr information will be ignored when printing the content.
- Character width: In the Console, different character widths are different. The Latin character width is 1, while the Chinese character width is 2. When calculating the character width, the width that satisfies `char < 0x100 `is regarded as 1, otherwise it is 2.

More information can be found in the comments in the `ConsolePrinter `class, which are detailed.

In general, our `GMap `class will automatically generate brushes for users and does not require users to create their own, but if you want to manually create brush objects, you can refer to the following code:

**Kotlin**

```Kotlin
fun main() {
    GMap.Builder.apply {
        width = 80
        height = 40
        file = File("D:\\Workspace\\jni\\cmake-build-release\\libjni.dll")
    }.build().use {
        //Create a brush as big as a map
        val g1 = SafeGraphics(it)
        //Create a brush with a drawing starting point of (10,5) and a drawing area size of 20x10. The drawing area is limited by the map size
        val g2 = SafeGraphics(it, 10, 5, 20, 10)
        //Create a brush with a drawing starting point of (10,5) and a drawing area size of 20x10. The drawing area is not limited in any way
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
            //Create a brush as big as a map
            SafeGraphics g1 = SafeGraphics.createSafeGraphics(map);
            //Create a brush with a drawing starting point of (10,5) and a drawing area size of 20x10. The drawing area is limited by the map size
            SafeGraphics g2 = SafeGraphics.createSafeGraphics(map, 10, 5, 20, 10);
            //Create a brush with a drawing starting point of (10,5) and a drawing area size of 20x10. The drawing area is not limited in any way
            SafeGraphics g3 = SafeGraphics.createHalfSafeGraphics(10, 5, 20, 10, ConsolePrinter.getIndex());
            // do something
        }
        GMap.Builder.dispose();
    }

}
```

## ⏲️ timing control

The engine supports the user to perform timing control by himself, and also supports letting the engine take over all timing control. Calling `GMap #start `can make the engine take over all tasks.

Please note: If you let the engine take over timing control, the logic task and the event task will not be executed in the same thread, you can use `GMap #runTaskOnLogicThread `to add a task to the logic thread for execution, this function is thread safe.

The code is as follows:

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

The above is the basic usage of the engine. The code comments have described the usage and precautions of the code in as much detail as possible. If you have any questions, welcome to raise issues.
