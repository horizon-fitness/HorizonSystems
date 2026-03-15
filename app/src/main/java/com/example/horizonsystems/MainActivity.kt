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
        Log.d("DatabaseResponse", "1. Starting network request...")
        
        // Run network request in a background thread
        GlobalScope.launch(Dispatchers.IO) {
            try {
                Log.d("DatabaseResponse", "2. Background thread launched, connecting to InfinityFree...")
                
                // Point to the InfinityFree PHP API URL
                val url = URL("https://horizonfitnesscorp.gt.tc/get_data.php")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000 // 15 seconds
                connection.readTimeout = 15000

                // Read the JSON Response
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d("DatabaseResponse", "3. Downloaded Raw Text = $response")

                // Parse JSON Array
                val jsonArray = JSONArray(response)
                Log.d("DatabaseResponse", "4. Successfully parsed JSON Array containing ${jsonArray.length()} rows")
                
                // Log all rows natively mapped to JSON objects
                for (i in 0 until jsonArray.length()) {
                    val row = jsonArray.getJSONObject(i)
                    Log.d("DatabaseResponse", "5. Row Data: $row")
                }

                connection.disconnect()

            } catch (e: Exception) {
                // Log the exception under the same tag so the user sees the error
                Log.e("DatabaseResponse", "CRITICAL ERROR: ${e.message}")
            }
        }
    }
}