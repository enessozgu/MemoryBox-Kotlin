package com.example.anikutusu

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.anikutusu.databinding.FragmentMemoryDetailBottomSheetBinding
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.location.Geocoder
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Bottom sheet dialog fragment to display details of a selected memory
class MemoryDetailBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentMemoryDetailBottomSheetBinding
    private lateinit var placesClient: PlacesClient

    // Initialize Places API client
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Replace with your actual Google Maps API key
        Places.initialize(requireContext(), "AIzaSyBiMZF5oOBDgpTJutx1EnwUnGg1lv_aL-8")
        placesClient = Places.createClient(requireContext())
    }

    // Inflate the layout using ViewBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentMemoryDetailBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Called when the view is ready
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val placeId = arguments?.getString("userId") ?: return
        fetchPlaceDetails(placeId)

        val latitude = arguments?.getDouble("latitude") ?: return
        val longitude = arguments?.getDouble("longitude") ?: return

        binding.placeName.text = arguments?.getString("text") ?: "Anı"

        getAddressFromLatLng(latitude, longitude)
        fetchLocalTime(latitude, longitude)
        fetchWeatherData(latitude, longitude)
    }

    // Fetch place details like name and address using Places API
    private fun fetchPlaceDetails(placeId: String) {
        val request = FetchPlaceRequest.builder(
            placeId, listOf(Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
        ).build()

        placesClient.fetchPlace(request)
            .addOnSuccessListener { response ->
                val place = response.place
                binding.placeName.text = place.name
                binding.placeAddress.text = place.address
                // Optionally add map or static map here
            }
            .addOnFailureListener {
                // Handle fetch failure if needed
            }
    }

    // Use Android Geocoder to get a human-readable address from coordinates
    private fun getAddressFromLatLng(latitude: Double, longitude: Double) {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0].getAddressLine(0)
                binding.placeAddress.text = address
            } else {
                binding.placeAddress.text = "Adres bulunamadı"
            }
        } catch (e: Exception) {
            binding.placeAddress.text = "Adres alınırken hata"
            e.printStackTrace()
        }
    }

    // Fetch local time using Google Timezone API based on coordinates
    private fun fetchLocalTime(latitude: Double, longitude: Double) {
        val timestamp = System.currentTimeMillis() / 1000 // Get current time in seconds
        val apiKey = "AIzaSyB85TYSviAbkfU8D7D7jZKgYHfopq4iHOY" // Replace with your API key

        val url =
            "https://maps.googleapis.com/maps/api/timezone/json?location=$latitude,$longitude&timestamp=$timestamp&key=$apiKey"

        Thread {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)

                val rawOffset = json.getDouble("rawOffset") // UTC offset
                val dstOffset = json.getDouble("dstOffset") // DST offset
                val localTimeMillis = (timestamp + rawOffset + dstOffset).toLong() * 1000

                val localDate = Date(localTimeMillis)
                val sdf = SimpleDateFormat("dd MMM yyyy - HH:mm", Locale("tr", "TR"))
                val formattedTime = sdf.format(localDate)

                activity?.runOnUiThread {
                    binding.localTime.text = "Yerel Saat: $formattedTime"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                activity?.runOnUiThread {
                    binding.localTime.text = "Saat bilgisi alınamadı"
                }
            }
        }.start()
    }

    // Fetch current weather data using OpenWeatherMap API
    private fun fetchWeatherData(latitude: Double, longitude: Double) {
        val apiKey = "ae5422543ef7bf046342c729e9a72be2" // Replace with your OpenWeather API key
        val url =
            "https://api.openweathermap.org/data/2.5/weather?lat=$latitude&lon=$longitude&appid=$apiKey&units=metric&lang=tr"

        Thread {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)

                val weatherArray = json.getJSONArray("weather")
                val weatherObject = weatherArray.getJSONObject(0)
                val description = weatherObject.getString("description")

                val main = json.getJSONObject("main")
                val temp = main.getDouble("temp")

                val weatherInfo = "$description, ${temp.toInt()}°C"

                activity?.runOnUiThread {
                    binding.weatherText.text = "Hava Durumu: $weatherInfo"
                }

            } catch (e: Exception) {
                e.printStackTrace()
                activity?.runOnUiThread {
                    binding.weatherText.text = "Hava bilgisi alınamadı"
                }
            }
        }.start()
    }
}
