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
                
                val url = URL("https://horizonfitnesscorp.gt.tc/get_data.php")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                
                // Set a User-Agent to look like a browser (InfinityFree often blocks apps)
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")

                val responseCode = connection.responseCode
                Log.d("DatabaseResponse", "3. HTTP Response Code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("DatabaseResponse", "4. Downloaded Raw Text = $response")

                    val jsonArray = JSONArray(response)
                    Log.d("DatabaseResponse", "5. Successfully parsed ${jsonArray.length()} rows")
                    
                    for (i in 0 until jsonArray.length()) {
                        val row = jsonArray.getJSONObject(i)
                        Log.d("DatabaseResponse", "6. Row Data: $row")
                    }
                } else {
                    val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    Log.e("DatabaseResponse", "CRITICAL ERROR: Server returned $responseCode")
                    Log.e("DatabaseResponse", "Error Response: $errorResponse")
                }

                connection.disconnect()

            } catch (e: Exception) {
                Log.e("DatabaseResponse", "CRITICAL ERROR: ${e.javaClass.simpleName} - ${e.message}")
                e.printStackTrace()
            }
        }
    }
}