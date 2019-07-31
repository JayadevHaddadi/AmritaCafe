package edu.amrita.jayadev.amritacafe

import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


class MainActivity : AppCompatActivity() {

    private var array: Array<String> = arrayOf<String>("item 1")
    internal lateinit var gridView: GridView
    val TAG = "debug"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gridView = findViewById(R.id.gridView) as GridView


        val adapter2 = RecipeAdapter(
            this, array
        )

        // Create adapter to set value for grid view
//        val adapter = ArrayAdapter(
//            this,
//            android.R.layout.simple_list_item_1, numbers
//        )
        val adapter = AlphabetAdapter(applicationContext, numbers)

        gridView.adapter = adapter

        gridView.onItemClickListener = AdapterView.OnItemClickListener { parent, v, position, id ->
            Toast.makeText(
                applicationContext,
                "hello", Toast.LENGTH_SHORT
            ).show()

            array += "geh"
            adapter2.notifyDataSetChanged()
        }

        var dir = File(
            Environment.getRootDirectory().toString() + File.separator + "AmritaCafe"
        )

        Log.d(TAG, "Path: : " + dir.toString())
        Log.d(TAG, "Exists? : " + dir.exists())
        if (!dir.exists()) {
            Log.d(TAG, "Crated: " + dir.mkdirs())
        }


//        val listView = findViewById<ListView>(R.id.order_ListView)
        order_ListView.adapter = adapter2

        order_ListView.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView: AdapterView<*>, view1: View, i: Int, l: Long ->

                // value of item that is clicked
                val itemValue = order_ListView.getItemAtPosition(i) as String

                // Toast the values
                Toast.makeText(
                    applicationContext,
                    "Position :$i\nItem Value : $itemValue", Toast.LENGTH_LONG
                ).show()

                array += "geh"
                adapter2.notifyDataSetChanged()
            }

    }

    class RecipeAdapter(
        private val context: Context,
        private var dataSource: Array<String>
    ) : BaseAdapter() {

        private val inflater: LayoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        //1
        override fun getCount(): Int {
            return dataSource.size
        }

        //2
        override fun getItem(position: Int): Any {
            return dataSource[position]
        }

        //3
        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        //4
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            // Get view for row item
            val rowView = inflater.inflate(R.layout.order_list, parent, false)

            val label = rowView.findViewById<TextView>(R.id.label);
            label.text = "hello222"

            return rowView
        }

    }


    companion object {
        internal val numbers = arrayOf(
            "Burgers",
            "VG",
            "CB",
            "LUXB",
            "E",
            "F",
            "G",
            "H",
            "I",
            "J",
            "K",
            "L",
            "M",
            "A",
            "N",
            "O",
            "P",
            "Q",
            "R",
            "S",
            "T",
            "U",
            "V",
            "W",
            "X",
            "Y",
            "Z",
            "B",
            "A",
            "C",
            "D",
            "E",
            "F",
            "G",
            "H",
            "I",
            "J",
            "K",
            "L",
            "M",
            "N",
            "A",
            "O",
            "P",
            "Q",
            "R",
            "S",
            "T",
            "U",
            "V",
            "W",
            "X",
            "Y",
            "Z",
            "B",
            "C",
            "D",
            "E",
            "F",
            "G",
            "H",
            "I",
            "J",
            "K",
            "L",
            "M",
            "N",
            "O",
            "P",
            "Q",
            "R",
            "S",
            "T",
            "U",
            "V",
            "W",
            "X",
            "Y",
            "Z"
        )
    }
}
