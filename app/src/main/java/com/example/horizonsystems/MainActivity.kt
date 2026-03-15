package com.example.horizonsystems

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        fetchDataFromInfinityFree()
    }

    private fun fetchDataFromInfinityFree() {
        // Run network request in a background thread
        GlobalScope.launch(Dispatchers.IO) {
            try {
                // Point to the InfinityFree PHP API URL
                val url = URL("https://horizonfitnesscorp.gt.tc/get_data.php")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                // Read the JSON Response
                val response = connection.inputStream.bufferedReader().use { it.readText() }

                // Parse JSON Array
                val jsonArray = JSONArray(response)
                
                // Log all rows natively mapped to JSON objects
                for (i in 0 until jsonArray.length()) {
                    val row = jsonArray.getJSONObject(i)
                    Log.d("DatabaseResponse", "Row Data: $row")
                }

                connection.disconnect()

            } catch (e: Exception) {
                Log.e("DatabaseError", "Error: ${e.message}")
            }
        }
    }
}