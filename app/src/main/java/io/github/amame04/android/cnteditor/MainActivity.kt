package io.github.amame04.android.cnteditor

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.*
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.core.os.HandlerCompat
import androidx.room.Room
import io.github.amame04.android.cnteditor.RoomController.*
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), TextWatcher {
    private var counter : TextView? = null
    private var linesList : ListView? = null
    private var editText : EditText? = null
    private var adapter : LinesAdapter? = null
    private var lines : MutableList<Line>? = null
    private var index = -1;
    private var linesDao : LinesDao? = null

    private var context : Context? = null

    @UiThread
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        context = this

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "AppDatabase"
        ).build()
        linesDao = db.LinesDao()
        counter = findViewById(R.id.counter)
        linesList = findViewById(R.id.lineList)
        editText = findViewById(R.id.edit_text)
        editText?.addTextChangedListener(this)

        val handler = HandlerCompat.createAsync(mainLooper)
        val initDatabase = InitDatabase(handler)
        val executor = Executors.newSingleThreadExecutor()
        executor.submit(initDatabase)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.add(R.string.delete_all)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
             0 -> {
                val handler = HandlerCompat.createAsync(mainLooper)
                val deleteAll = DeleteAll(handler)
                val executor = Executors.newSingleThreadExecutor()
                executor.submit(deleteAll)
                true
            }
            else ->{
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
    }

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        counter?.text = p0?.length.toString()
    }

    @UiThread
    override fun afterTextChanged(p0: Editable?) {
        p0?.let {
            if (it.endsWith('\n') && it.isNotBlank()) {
                val str = it.toString().removeSuffix("\n")
                it.clear()

                val handler = HandlerCompat.createAsync(mainLooper)
                val sendQuery = SendQuery(handler, str, index)
                val executor = Executors.newSingleThreadExecutor()
                executor.submit(sendQuery)

                index = -1
            }
        }
    }

    private inner class InitDatabase(private val handler: Handler): Runnable {
        @WorkerThread
        override fun run() {
            lines = linesDao?.getAll()

            context?.let {
                val postExecutor = ShowResult(it)
                handler.post(postExecutor)
            }
        }
    }

    private inner class ShowResult(private val context: Context): Runnable {
        @UiThread
        override fun run() {
            linesList?.setOnItemClickListener { _, _, p, _ ->
                index = p
                lines?.let {
                    val text = it[index].line
                    editText?.setText(text)
                    editText?.setSelection(text.length)
                }
            }

            lines?.let { adapter = LinesAdapter(context, it) }
            linesList?.adapter = adapter
        }
    }

    private inner class SendQuery(private val handler: Handler, private val str: String, private val index: Int): Runnable {
        @WorkerThread
        override fun run() {
            var key = lines?.size ?: 0
            while (lines?.find { line -> key == line.index } != null) key += 1
            var data : Line? = null

            if (index == -1) {
                data = Line(key, str.length, str)
                linesDao?.insert(data)
                lines?.add(data)
            }
            else {
                data = Line(index, str.length, str)
                linesDao?.update(data)
                lines?.indexOfFirst { line -> line.index == index }?.let {
                    lines!![it] = data
                }
            }

            val postExecutor = UpdateView(index)
            handler.post(postExecutor)
        }
    }

    private inner class DeleteAll(private val handler: Handler): Runnable {
        @WorkerThread
        override fun run() {
            linesDao?.let {
                it.getAll().forEach { line -> it.deleteLine(line) }
                lines?.clear()
            }
            val postExecutor = UpdateView(-1)
            handler.post(postExecutor)
        }
    }

    private inner class UpdateView(private val index: Int): Runnable {
        @UiThread
        override fun run() {
            adapter?.notifyDataSetChanged()
            linesList?.smoothScrollToPosition(if (index == -1) lines!!.size else index)
        }
    }

    inner class LinesAdapter(context: Context, objects: MutableList<Line>): ArrayAdapter<Line>(context, 0, objects) {
        private val mLayoutInflater : LayoutInflater
        init {
            mLayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: mLayoutInflater.inflate(android.R.layout.simple_list_item_2, null)

            val lines = getItem(position)
            val textView1 = view.findViewById<TextView>(android.R.id.text1)
            val textView2 = view.findViewById<TextView>(android.R.id.text2)

            textView1.text = lines?.line
            textView2.text = lines?.length.toString()

            return view
        }
    }

}