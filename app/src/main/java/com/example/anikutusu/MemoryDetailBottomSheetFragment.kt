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
    import java.util.Locale


    class MemoryDetailBottomSheetFragment : BottomSheetDialogFragment() {

        private lateinit var binding: FragmentMemoryDetailBottomSheetBinding
        private lateinit var placesClient: PlacesClient

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            Places.initialize(requireContext(), "AIzaSyBiMZF5oOBDgpTJutx1EnwUnGg1lv_aL-8") // Replace with real key
            placesClient = Places.createClient(requireContext())
        }

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
        ): View {
            binding = FragmentMemoryDetailBottomSheetBinding.inflate(inflater, container, false)
            return binding.root


        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            val placeId = arguments?.getString("userId") ?: return
            fetchPlaceDetails(placeId)

            val latitude = arguments?.getDouble("latitude") ?: return
            val longitude = arguments?.getDouble("longitude") ?: return
            binding.placeName.text = arguments?.getString("text") ?: "Anı"

            getAddressFromLatLng(latitude, longitude)




        }

        private fun fetchPlaceDetails(placeId: String) {
            val request = FetchPlaceRequest.builder(
                placeId, listOf(Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
            ).build()

            placesClient.fetchPlace(request)
                .addOnSuccessListener { response ->
                    val place = response.place
                    binding.placeName.text = place.name
                    binding.placeAddress.text = place.address
                    // Static Map or actual GoogleMapFragment eklersin burada
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Yer bilgisi alınamadı", Toast.LENGTH_SHORT).show()
                }
        }



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









    }
