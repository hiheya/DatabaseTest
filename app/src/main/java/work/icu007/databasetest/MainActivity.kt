package work.icu007.databasetest

import android.annotation.SuppressLint
import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import work.icu007.databasetest.databinding.ActivityMainBinding
import java.lang.NullPointerException

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
        // 更新数据
        aMBinding.updateDatabase.setOnClickListener {
            Log.d("LCR", "onCreate: update")
            val db = dbHelper.writableDatabase
            val values = ContentValues()
            values.put("price", 50.99)
            db.update("Book", values, "name = ?", arrayOf("剑来"))
        }
        aMBinding.deleteDatabase.setOnClickListener {
            // 删除数据
            Log.d("LCR", "onCreate: delete")
            val db = dbHelper.writableDatabase
            db.delete("Book", "pages > ?", arrayOf("50000"))
        }
        aMBinding.queryData.setOnClickListener {
            val db = dbHelper.writableDatabase
            val cursor = db.query("Book", null, null, null, null, null, null)
            if (cursor.moveToFirst()) {
                // 移动游标到下一行数据，如果有的话。当游标没有更多的数据时 跳出循环
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
        aMBinding.replaceData.setOnClickListener {
            // 替换数据
            val db = dbHelper.writableDatabase
            // 开启事务
            db.beginTransaction()
            try {
                db.delete("Book", null, null)
                if (false){
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