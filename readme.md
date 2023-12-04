## 四、SQLite数据库存储

Android系统其实是内置了数据库的，那就是---SQLite是一款轻量级的关系型数据库，它的运算速度非常快，占用资源很少，通常只需要几百KB的内存就足够了，因而特别适合在移动设备上使用。SQLite不仅支持标准的SQL语法，还遵循了数据库的ACID事务，所以只要你以前使用过其他的关系型数据库，就可以很快地上手SQLite。而SQLite又比一般的数据库要简单得多，它甚至不用设置用户名和密码就可以使用。

### 4.1 创建数据库

Android为了让我们能够更加方便地管理数据库，专门提供了一个`SQLiteOpenHelper`帮助类，借助这个类可以非常简单地对数据库进行创建和升级。

- 首先，`SQLiteOpenHelper`是一个抽象类，这意味着如果我们想要使用它，就需要创建一个自己的帮助类去继承它。`SQLiteOpenHelper`中有两个抽象方法：`onCreate()`和`onUpgrade()`。我们必须在自己的帮助类里重写这两个方法，然后分别在这两个方法中实现创建和升级数据库的逻辑。
- `SQLiteOpenHelper`中还有两个非常重要的实例方法：`getReadableDatabase()`和`getWritableDatabase()`。这两个方法都可以创建或打开一个现有的数据库（如果数据库已存在则直接打开，否则要创建一个新的数据库），并返回一个可对数据库进行读写操作的对象。不同的是，当数据库不可写入的时候（如磁盘空间已满），`getReadableDatabase()`方法返回的对象将以只读的方式打开数据库，而`getWritableDatabase()`方法则将出现异常。
- `SQLiteOpenHelper`中有两个构造方法可供重写，一般使用参数少一点的那个构造方法即可。这个构造方法中接收4个参数：**第一个参数是Context**，这个没什么好说的，必须有它才能对数据库进行操作；**第二个参数是数据库名**，创建数据库时使用的就是这里指定的名称；**第三个参数允许我们在查询数据的时候返回一个自定义的Cursor**，一般传入null即可；**第四个参数表示当前数据库的版本号**，可用于对数据库进行升级操作。构建出`SQLiteOpenHelper`的实例之后，再调用它的`getReadableDatabase()`或`getWritableDatabase()`方法就能够创建数据库了，数据库文件会存放在`/data/data/<package name>/databases/`目录下。此时，重写的`onCreate()`方法也会得到执行，所以通常会在这里处理一些创建表的逻辑。

我们希望创建一个名为`BookStore.db`数据库，然后在这个数据库中新建一张Book表，表中有id（主键）、作者、价格、页数和书名等列。创建数据库表当然还是需要用建表语句的。如下：

```sql
create table Book (
    id integer primary key autoincrement,
    author text,
    price real,
    pages integer,
    name text)
```

SQLite不像其他的数据库拥有众多繁杂的数据类型，它的数据类型很简单：**integer表示整型，real表示浮点型，text表示文本类型，blob表示二进制类型**。另外，在上述建表语句中，我们还使用了`primary key`将id列设为**主键**，并用`autoincrement`关键字表示id列是**自增长**的。

需要在代码中执行这条SQL语句，才能完成创建表的操作。新建MyDatabaseHelper类继承自SQLiteOpenHelper，代码如下所示：

```kotlin
package work.icu007.databasetest

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.widget.Toast


/*
 * Author: Charlie_Liao
 * Time: 2023/11/29-15:52
 * E-mail: rookie_l@icu007.work
 */

class MyDatabaseHelper(private val context: Context, name: String, version: Int) :
    SQLiteOpenHelper(context, name, null, version) {

    private val createBook = "create table Book (" +
            " id integer primary key autoincrement," +
            "author text," +
            "price real," +
            "pages integer," +
            "name text)"
    private val createCategory = "create table Category (" +
            "id integer primary key autoincrement," +
            "category_name text," +
            "category_code integer)"
    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(createBook)
        db?.execSQL(createCategory)
        Toast.makeText(context, "Create succeeded", Toast.LENGTH_SHORT).show()
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // TODO
    }
}
```

可以把建表语句定义成了一个字符串变量，然后在onCreate()方法中又调用了SQLiteDatabase的execSQL()方法去执行这条建表语句，并弹出一个Toast提示创建成功，这样就可以保证在数据库创建完成的同时还能成功创建Book表。

接下来可以添加创建数据库的逻辑：

```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var aMBinding: ActivityMainBinding
    
    @SuppressLint("Range")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        aMBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(aMBinding.root)
        val dbHelper = MyDatabaseHelper(this, "BookStore.db", 1)
        // 创建database
        aMBinding.createDatabase.setOnClickListener {
            Log.d("LCR", "onCreate: clicked the button")
            dbHelper.writableDatabase
        }
    }
}
```

上述代码构建了一个`MyDatabaseHelper`对象，并且通过构造函数的参数将数据库名指定为`BookStore.db`，版本号指定为1，然后在“Create Database”按钮的点击事件里调用了`getWritableDatabase()`方法。这样当第一次点击“Create Database”按钮时，就会检测到当前程序中并没有`BookStore.db`这个数据库，于是会创建该数据库并调用`MyDatabaseHelper`中的`onCreate()`方法，这样Book表也就创建好了，然后会弹出一个Toast提示创建成功。再次点击“Create Database”按钮时，会发现此时已经存在`BookStore.db`数据库了，因此不会再创建一次。

### 4.2 升级数据库

`MyDatabaseHelper`中还有一个`onUpgrade()`方法是用于对数据库进行升级的，它在整个数据库的管理工作当中起着非常重要的作用。

现在项目中已经有了一张Book表用于存放书的详细数据，此时如果想添加一张表用于记录图书分类，就可以使用`onUpgrade()`方法：

```kotlin
package work.icu007.databasetest

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.widget.Toast


/*
 * Author: Charlie_Liao
 * Time: 2023/11/29-15:52
 * E-mail: rookie_l@icu007.work
 */

class MyDatabaseHelper(private val context: Context, name: String, version: Int) :
    SQLiteOpenHelper(context, name, null, version) {

    private val createBook = "create table Book (" +
            " id integer primary key autoincrement," +
            "author text," +
            "price real," +
            "pages integer," +
            "name text)"
    private val createCategory = "create table Category (" +
            "id integer primary key autoincrement," +
            "category_name text," +
            "category_code integer)"
    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(createBook)
        db?.execSQL(createCategory)
        Toast.makeText(context, "Create succeeded", Toast.LENGTH_SHORT).show()
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion <= 1) {
            db?.execSQL(createCategory)
        }
        onCreate(db)
    }
}
```

这里修改了`onUpgrade()`方法，判断`oldVersion`是否低于1，如果低于1的话就执行`sql`语句创建一张新表 Category 表

那为什么不能直接把执行 `sql`语句的代码放在 `onCreate()`当中呢？那是因为此时`BookStore.db`数据库已经存在了，之后不管我们怎样点击“Create Database”按钮，`MyDatabaseHelper`中的`onCreate()`方法都不会再次执行，因此新添加的表也就无法得到创建了。

那修改了 `onUpgrade()`方法该如何让他执行呢？其实很简单：`SQLiteOpenHelper`的构造方法里接收的第四个参数它表示当前数据库的版本号，之前我们传入的是1，现在只要传入一个比1大的数，就可以让`onUpgrade()`方法得到执行了。

### 4.3 添加数据

其实我们可以对数据进行的操作无非有4种，即`CRUD`。其中`C`代表添加（`create`），`R`代表查询（`retrieve`），`U`代表更新（`update`），`D`代表删除（`delete`）。每一种操作都对应了一种SQL命令，如果你比较熟悉SQL语言的话，一定会知道添加数据时使用`insert`，查询数据时使用`select`，更新数据时使用`update`，删除数据时使用`delete`。但是开发者的水平是参差不齐的，未必每一个人都能非常熟悉SQL语言，因此Android提供了一系列的辅助性方法，让你在Android中即使不用编写SQL语句，也能轻松完成所有的`CRUD`操作。

调用`SQLiteOpenHelper`的`getReadableDatabase()`或`getWritableDatabase()`方法是可以用于创建和升级数据库的，不仅如此，这两个方法还都会返回一个`SQLiteDatabase`对象，借助这个对象就可以对数据进行`CRUD`操作了。

`SQLiteDatabase`中提供了一个`insert()`方法，专门用于添加数据。它接收**3个参数**：**第一个参数是表名**，我们希望向哪张表里添加数据，这里就传入该表的名字；**第二个参数用于在未指定添加数据的情况下给某些可为空的列自动赋值NULL**，一般我们用不到这个功能，直接传入null即可；**第三个参数是一个`ContentValues`对象**，它提供了一系列的`put()`方法重载，用于向`ContentValues`中添加数据，只需要将表中的每个列名以及相应的待添加数据传入即可。

代码如下：

```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var aMBinding: ActivityMainBinding

    @SuppressLint("Range")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        aMBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(aMBinding.root)
        val dbHelper = MyDatabaseHelper(this, "BookStore.db", 1)
        ...
        // 往数据库里添加数据
        aMBinding.addDatabase.setOnClickListener {
            Log.d("LCR", "onCreate: insert")
            val db = dbHelper.writableDatabase
            val values1 = ContentValues().apply {
                // fist data
                put("name", "雪中悍刀行")
                put("author", "我吃西红柿")
                put("pages", 50000)
                put("price", 50.99)
            }
            db.insert("Book", null, values1)
            val values2 = ContentValues().apply {
                // second data
                put("name", "剑来")
                put("author", "我吃西红柿")
                put("pages", 60000)
                put("price", 52.99)
            }
            db.insert("Book", null, values2)
        }
    }
}
```

在添加数据按钮的点击事件里，我们先获取了`SQLiteDatabase`对象，然后使用`ContentValues`对要添加的数据进行组装。如果你比较细心的话，应该会发现这里只对Book表里其中4列的数据进行了组装，id那一列并没给它赋值。这是因为在前面创建表的时候，我们就将id列设置为自增长了，它的值会在入库的时候自动生成，所以不需要手动赋值了。接下来就可以调用`insert()`方法将数据添加到表当中

### 4.4 更新数据

`SQLiteDatabase`中提供了一个非常好用的`update()`方法，用于对数据进行更新。这个方法接收**4个参数**：**第一个参数和insert()方法一样，也是表名**，指定更新哪张表里的数据；**第二个参数是ContentValues对象**，要把更新数据在这里组装进去；**第三、第四个参数用于约束更新某一行或某几行中的数据，不指定的话默认会更新所有行**。

修改代码如下：

```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var aMBinding: ActivityMainBinding

    @SuppressLint("Range")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        aMBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(aMBinding.root)
        val dbHelper = MyDatabaseHelper(this, "BookStore.db", 1)
        ...
        // 更新数据
        aMBinding.updateDatabase.setOnClickListener {
            Log.d("LCR", "onCreate: update")
            val db = dbHelper.writableDatabase
            val values = ContentValues()
            values.put("price", 50.99)
            db.update("Book", values, "name = ?", arrayOf("剑来"))
        }
    }
}
```

这里在更新数据按钮的点击事件里面构建了一个`ContentValues`对象，并且只给它指定了一组数据，说明我们只是想把价格这一列的数据更新成50.99。然后调用了`SQLiteDatabase`的`update()`方法执行具体的更新操作，可以看到，这里使用了第三、第四个参数来指定具体更新哪几行。第三个参数对应的是SQL语句的where部分，**表示更新所有name等于?的行，而?是一个占位符，可以通过第四个参数提供的一个字符串数组为第三个参数中的每个占位符指定相应的内容**，`arrayOf()`方法是Kotlin提供的一种用于**便捷创建数组的内置方法**。因此上述代码想表达的意图就是将“剑来”这本书的价格改成50.99。

### 4.5 删除数据

`SQLiteDatabase`中提供了一个`delete()`方法，专门用于删除数据。这个方法接收**3个参数**：**第一个参数仍然是表名**，这个没什么好说的；**第二、第三个参数用于约束删除某一行或某几行的数据，不指定的话默认会删除所有行**。

代码如下：

```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var aMBinding: ActivityMainBinding

    @SuppressLint("Range")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        aMBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(aMBinding.root)
        val dbHelper = MyDatabaseHelper(this, "BookStore.db", 1)
        ...
        aMBinding.deleteDatabase.setOnClickListener {
            // 删除数据
            Log.d("LCR", "onCreate: delete")
            val db = dbHelper.writableDatabase
            db.delete("Book", "pages > ?", arrayOf("50000"))
        }
    }
}
```

我们在删除按钮的点击事件里指明删除Book表中的数据，并且通过第二、第三个参数来指定仅删除那些页数超过50000页的书。当然这个需求很奇怪，这里仅仅是为了做个测试。你可以先查看一下当前Book表里的数据，其中 “剑来” 这本书的页数超过了50000页，也就是说当我们点击删除按钮时，这条记录应该会被删除。

### 4.6 查询数据

`SQL`的全称是`Structured Query Language`，翻译成中文就是结构化查询语言。它的大部分功能体现在“查”这个字上，而“增删改”只是其中的一小部分功能。

`SQLiteDatabase`中还提供了一个`query()`方法用于对数据进行查询。这个方法的参数非常复杂，最短的一个方法重载也需要传入**7个参数**。那我们就先来看一下这7个参数各自的含义吧。**第一个参数**不用说，当然还是**表名**，表示我们希望从哪张表中查询数据。**第二个参数用于指定去查询哪几列**，如果不指定则默认查询所有列。**第三、第四个参数用于约束查询某一行或某几行的数据**，不指定则默认查询所有行的数据。**第五个参数用于指定需要去group by的列**，不指定则表示不对查询结果进行group by操作。**第六个参数用于对group by之后的数据进行进一步的过滤**，不指定则表示不进行过滤。**第七个参数用于指定查询结果的排序方式**，不指定则表示使用默认的排序方式。更多详细的内容可以参考下表：

| query()方法参数 |        对应SQL部分        |             描述              |
| :-------------: | :-----------------------: | :---------------------------: |
|      table      |      from table_name      |        指定查询的表名         |
|     columns     |  select column1, column2  |        指定查询的列名         |
|    selection    |   where column = values   |      指定where的约束条件      |
|  selectionArgs  |             -             | 为where中的占位符提供具体的值 |
|     groupBy     |      group by column      |     指定需要group by的列      |
|     having      |   having column = value   | 对group by 后的结果进一步约束 |
|     orderBy     | order by column1, column2 |    指定查询结果的排序方式     |

举个栗子🌰：

```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var aMBinding: ActivityMainBinding

    @SuppressLint("Range")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        aMBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(aMBinding.root)
        val dbHelper = MyDatabaseHelper(this, "BookStore.db", 1)
        ...
        aMBinding.queryData.setOnClickListener {
            val db = dbHelper.writableDatabase
            val cursor = db.query("Book", null, null, null, null, null, null)
            if (cursor.moveToFirst()) {
                // // 移动游标到下一行数据，如果有的话。当游标没有更多的数据时 跳出循环
                do {
                    // 遍历
                    val name = cursor.getString(cursor.getColumnIndex("name"))
                    val author = cursor.getString(cursor.getColumnIndex("author"))
                    val pages = cursor.getString(cursor.getColumnIndex("pages"))
                    val price = cursor.getString(cursor.getColumnIndex("price"))
                    Log.d("LCR", "onCreate: book name is $name\n" +
                            "book author is $author\n" +
                            "book pages is $pages\n" +
                            "book price is $price.")
                } while (cursor.moveToNext())
            }
            cursor.close()
        }
    }
}
```

我们首先在查询按钮的点击事件里面调用了`SQLiteDatabase`的`query()`方法查询数据。这里的`query()`方法非常简单，只使用了第一个参数指明查询Book表，后面的参数全部为null。这就表示希望查询这张表中的所有数据，虽然这张表中目前只剩下一条数据了。查询完之后就得到了一个Cursor对象，接着我们调用它的`moveToFirst()`方法，将数据的指针移动到第一行的位置，然后进入一个循环当中，去遍历查询到的每一行数据。在这个循环中可以通过Cursor的`getColumnIndex()`方法获取某一列在表中对应的位置索引，然后将这个索引传入相应的取值方法中，就可以得到从数据库中读取到的数据了。接着我们使用Log将取出的数据打印出来，借此检查读取工作有没有成功完成。

### 4.7 使用SQL操作数据库

Android同样提供了一系列的方法，使得可以直接通过SQL来操作数据库。

下面演示一下如何直接使用SQL来完成前面学过的CRUD操作。

- 添加数据

```kotlin
db.execSQL("insert into Book (name, author, pages, price) values(?, ?, ?, ?)", arrayOf("雪中悍刀行", "我吃西红柿", "50000", "50.99"))
db.execSQL("insert into Book (name, author, pages, price) values(?, ?, ?, ?)", arrayOf("剑来", "我吃西红柿", "60000", "52.99"))
```

- 更新数据

```kotlin
db.execSQL("update Book set price = ? where name = ?", arrayOf("50.99", "剑来"))
```

- 删除数据

```kotlin
db.execSQL("delete from Book where pages > ?", arrayOf("50000"))
```

- 查询数据

```kotlin
val cursor = db.rawQuery("select * from Book", null)
```

除了查询数据的时候调用的是`SQLiteDatabase`的`rawQuery()`方法，其他操作都是调用的`execSQL()`方法。以上演示的几种方式的执行结果会和前面学习的CRUD操作的结果完全相同，选择使用哪一种方式就看个人的喜好了。

## 五、SQLite数据库的最佳实践

### 5.1 使用事务

SQLite数据库是支持事务的，事务的特性可以保证让一系列的操作要么全部完成，要么一个都不会完成。那么在什么情况下才需要使用事务呢？想象以下场景，比如你正在进行一次转账操作，银行会先将转账的金额从你的账户中扣除，然后再向收款方的账户中添加等量的金额。看上去好像没什么问题吧？可是，如果当你账户中的金额刚刚被扣除，这时由于一些异常原因导致对方收款失败，这一部分钱就凭空消失了！当然银行肯定已经充分考虑到了这种情况，它会保证扣款和收款的操作要么一起成功，要么都不会成功，而使用的技术当然就是事务了。

那如何在Android中使用事务呢？其实很简单，只需要用到两个方法：`beginTransaction()` 和`endTransaction()`方法，一个用于开启事务，一个用于结束事务。`setTransactionSuccessful()` 方法用于表示事务已经执行成功。

```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var aMBinding: ActivityMainBinding

    @SuppressLint("Range")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        aMBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(aMBinding.root)
        val dbHelper = MyDatabaseHelper(this, "BookStore.db", 1)
        ...
        aMBinding.replaceData.setOnClickListener {
            // 替换数据
            val db = dbHelper.writableDatabase
            // 开启事务
            db.beginTransaction()
            try {
                db.delete("Book", null, null)
                if (true){
                    //
                    throw NullPointerException()
                }
                val values = ContentValues().apply {
                    put("name", "吞噬星空")
                    put("author", "我吃西红柿")
                    put("pages", 68000)
                    put("price", 60.99)
                }
                db.insert("Book", null, values)
                db.setTransactionSuccessful() // 表示事务已经执行成功
            } catch (e: Exception){
                e.printStackTrace()
            } finally {
                //结束事务
                db.endTransaction()
            }
        }
    }
}
```

上述代码就是Android中事务的标准用法，首先调用`SQLiteDatabase`的beginTransaction()方法开启一个事务，然后在一个异常捕获的代码块中执行具体的数据库操作，当所有的操作都完成之后，调用`setTransactionSuccessful()`表示事务已经执行成功了，最后在finally代码块中调用`endTransaction()`结束事务。我们在删除旧数据的操作完成后手动抛出了一个`NullPointerException`，这样添加新数据的代码就执行不到了。不过由于事务的存在，中途出现异常会导致事务的失败，此时旧数据应该是删除不掉的。

### 5.2 升级数据库的最佳写法

每一个数据库版本都会对应一个版本号，当指定的数据库版本号大于当前数据库版本号的时候，就会进入`onUpgrade()`方法中执行更新操作。这里需要为每一个版本号赋予其所对应的数据库变动，然后在`onUpgrade()`方法中对当前数据库的版本号进行判断，再执行相应的改变就可以了。

比如我们第一版程序，我们只有一张 `Book`表：

```kotlin
package work.icu007.databasetest

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.widget.Toast


/*
 * Author: Charlie_Liao
 * Time: 2023/11/29-15:52
 * E-mail: rookie_l@icu007.work
 */

class MyDatabaseHelper(private val context: Context, name: String, version: Int) :
    SQLiteOpenHelper(context, name, null, version) {

    private val createBook = "create table Book (" +
            " id integer primary key autoincrement," +
            "author text," +
            "price real," +
            "pages integer," +
            "name text)"
    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(createBook)
        Toast.makeText(context, "Create succeeded", Toast.LENGTH_SHORT).show()
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
    }
}
```

过了几周，我们又有了个新需求，需要向数据库当中添加一张 `Category`表，我们可以这样写：

```kotlin
package work.icu007.databasetest

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.widget.Toast


/*
 * Author: Charlie_Liao
 * Time: 2023/11/29-15:52
 * E-mail: rookie_l@icu007.work
 */

class MyDatabaseHelper(private val context: Context, name: String, version: Int) :
    SQLiteOpenHelper(context, name, null, version) {

    private val createBook = "create table Book (" +
            " id integer primary key autoincrement," +
            "author text," +
            "price real," +
            "pages integer," +
            "name text)"
    private val createCategory = "create table Category (" +
            "id integer primary key autoincrement," +
            "category_name text," +
            "category_code integer)"
    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(createBook)
        db?.execSQL(createCategory)
        Toast.makeText(context, "Create succeeded", Toast.LENGTH_SHORT).show()
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion <= 1) {
            db?.execSQL(createCategory)
        }
    }
}
```

可以看到，在`onCreate()`方法里我们新增了一条建表语句，然后又在`onUpgrade()`方法中添加了一个if判断，如果用户数据库的旧版本号小于等于1，就只会创建一张`Category`表。这样当用户直接安装第2版的程序时，就会进入`onCreate()`方法，将两张表一起创建。而当用户使用第2版的程序覆盖安装第1版的程序时，就会进入升级数据库的操作中，此时由于Book表已经存在了，因此只需要创建一张`Category`表即可。

但是没过多久，新的需求又来了，这次要给`Book`表和`Category`表之间建立关联，需要在`Book`表中添加一个`category_id`字段。这个时候可以这样写：

```kotlin
package work.icu007.databasetest

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.widget.Toast


/*
 * Author: Charlie_Liao
 * Time: 2023/11/29-15:52
 * E-mail: rookie_l@icu007.work
 */

class MyDatabaseHelper(private val context: Context, name: String, version: Int) :
    SQLiteOpenHelper(context, name, null, version) {
     private val createBook = "create table Book (" +
            " id integer primary key autoincrement," +
            "author text," +
            "price real," +
            "pages integer," +
            "name text," +
            "category_id integer)"
    // 与之前一致
    ...
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion <= 1) {
            db?.execSQL(createCategory)
        }
        if (oldVersion <= 2) {
            db.execSQL("alter table Book add column category_id integer")
        }
    }
}
```

首先我们在Book表的建表语句中添加了一个category_id列，这样当用户直接安装第3版的程序时，这个新增的列就已经自动添加成功了。然而，如果用户之前已经安装了某一版本的程序，现在需要覆盖安装，就会进入升级数据库的操作中。在onUpgrade()方法里，我们添加了一个新的条件，如果当前数据库的版本号是2，就会执行alter命令，为Book表新增一个category_id列。

这里请注意一个非常重要的细节：每当升级一个数据库版本的时候，`onUpgrade()`方法里都一定要写一个相应的if判断语句。为什么要这么做呢？这是为了保证App在跨版本升级的时候，每一次的数据库修改都能被全部执行。比如用户当前是从第2版升级到第3版，那么只有第二条判断语句会执行，而如果用户是直接从第1版升级到第3版，那么两条判断语句都会执行。使用这种方式来维护数据库的升级，不管版本怎样更新，都可以保证数据库的表结构是最新的，而且表中的数据完全不会丢失。