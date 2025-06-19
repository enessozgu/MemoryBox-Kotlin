package com.example.anikutusu

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.anikutusu.databinding.FragmentHomeMapBinding
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID
import com.example.anikutusu.MemoryAddMode
import com.google.android.libraries.places.api.Places
import com.ornek.anikutusu.ui.viewmodel.MemoryAddViewModel

class HomeMapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentHomeMapBinding? = null
    private val binding get() = _binding!!
    private lateinit var mapView: MapView
    private lateinit var autocompleteFragment: AutocompleteSupportFragment
    private lateinit var googleMap: GoogleMap
    private lateinit var geofencingClient: GeofencingClient
    private val GEOFENCE_RADIUS_IN_METERS = 100f
    private var selectedImageUri: Uri? = null
    private var dialogImageView: ImageView? = null
    private var dialogEditText: EditText? = null
    private var addMemoryDialog: AlertDialog? = null
    private var recordedAudioFilePath: String? = null
    private var mediaRecorder: MediaRecorder? = null
    private var audioFilePath: String? = null
    private var isRecording = false
    private var locationPermissionGranted = false

    // --- 1. ViewModel Tanımı ---
    private lateinit var viewModel: MemoryAddViewModel

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            dialogImageView?.setImageURI(it)
            dialogImageView?.visibility = View.VISIBLE
        }
    }

    private val locationAndAudioPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        locationPermissionGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        val audioPermissionGranted = permissions[Manifest.permission.RECORD_AUDIO] == true

        if (!locationPermissionGranted) {
            Toast.makeText(requireContext(), "Konum izni gerekli!", Toast.LENGTH_SHORT).show()
        } else if (::googleMap.isInitialized) {
            enableMyLocation()
        }

        if (!audioPermissionGranted) {
            Toast.makeText(requireContext(), "Mikrofon izni gerekli!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- 2. ViewModel’i Başlat ---
        viewModel = ViewModelProvider(this)[MemoryAddViewModel::class.java]

        // --- Rozetleri Gözleme: badge sayısını güncelle ---
        viewModel.badges.observe(viewLifecycleOwner) { badgesSet ->
            val badgeCount = badgesSet.size
            binding.textViewBadgeStatus.text = "Rozet: $badgeCount"
        }



        val toolbar = binding.toolbar
        toolbar.title = "Anılarım"

        val activity = requireActivity() as AppCompatActivity
        activity.setSupportActionBar(toolbar)

        val drawerLayout = binding.drawerLayout

        val toggle = ActionBarDrawerToggle(
            activity,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()





        binding.homeNavigationView.setNavigationItemSelectedListener {
            showDrawerMenuItemAction(it.itemId)
            true
        }


        // --- Mod Değiştirme Butonu ---
        binding.btnModSec.setOnClickListener {
            viewModel.toggleMode()
        }

        viewModel.selectedMode.observe(viewLifecycleOwner) { mode ->
            binding.btnModSec.text = when (mode) {
                MemoryAddMode.SERBEST_EKLE -> "Mod: Serbest Ekle"
                MemoryAddMode.YERINDE_EKLE -> "Mod: Yerinde Ekle"
            }
        }

        // --- Google Geofencing Client ---
        geofencingClient = LocationServices.getGeofencingClient(requireContext())

        Log.d("AuthStatus", "currentUser: ${FirebaseAuth.getInstance().currentUser?.uid}")

        // --- İzinleri İste ---
        locationAndAudioPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.RECORD_AUDIO
            )
        )

        // --- Haritayı Başlat ---
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)
        mapView = binding.mapView

        if (!Places.isInitialized()) {
            val MAPS_API_KEY = "AIzaSyBiMZF5oOBDgpTJutx1EnwUnGg1lv_aL-8"
            Places.initialize(requireContext(), MAPS_API_KEY)
        }

        mapView.getMapAsync { map ->
            googleMap = map
            // Harita ayarları burada (örn. marker renkleri, zoom tercihleri vs.)
        }

        // --- Places Autocomplete ---
        var autocomplete = childFragmentManager.findFragmentById(R.id.autocomplete_fragment_container) as? AutocompleteSupportFragment
        if (autocomplete == null) {
            autocomplete = AutocompleteSupportFragment.newInstance()
            childFragmentManager.beginTransaction()
                .replace(R.id.autocomplete_fragment_container, autocomplete)
                .commit()
        }
        autocomplete.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG))
        autocomplete.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.latLng!!, 15f))
            }
            override fun onError(status: com.google.android.gms.common.api.Status) {
                Toast.makeText(requireContext(), "HATA VAR LOO", Toast.LENGTH_SHORT).show()
            }
        })
    }




    private fun showDrawerMenuItemAction(menuItemId: Int) {
        when (menuItemId) {

            R.id.nav_map -> {

                Toast.makeText(requireContext(), "Zaten buradasın 👀", Toast.LENGTH_SHORT).show()
            }


            R.id.nav_badges -> {
                FirebaseAuth.getInstance().signOut()
                Toast.makeText(requireContext(), "Çıkış yapıldı", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_homeMapFragment_to_registerFragment)
            }

            R.id.nav_memories -> {
                findNavController().navigate(R.id.action_homeMapFragment_to_memoryListFragment)

            }

            R.id.nav_settings -> {

                    googleMap.mapType = when (googleMap.mapType) {
                        GoogleMap.MAP_TYPE_NORMAL -> GoogleMap.MAP_TYPE_SATELLITE
                        GoogleMap.MAP_TYPE_SATELLITE -> GoogleMap.MAP_TYPE_TERRAIN
                        GoogleMap.MAP_TYPE_TERRAIN -> GoogleMap.MAP_TYPE_NORMAL
                        else -> GoogleMap.MAP_TYPE_NORMAL
                    }

            }
        }
    }






    // ------------------
    //  Geofence Ekleme
    // ------------------
    private fun addGeofence(latLng: LatLng, geofenceId: String) {
        if (!locationPermissionGranted) return
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Konum izni gerekli!", Toast.LENGTH_SHORT).show()
            return
        }

        val geofence = Geofence.Builder()
            .setRequestId(geofenceId)
            .setCircularRegion(latLng.latitude, latLng.longitude, GEOFENCE_RADIUS_IN_METERS)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val geofenceRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        try {
            geofencingClient.addGeofences(geofenceRequest, pendingIntent)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Geofence eklendi!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Geofence eklenemedi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: SecurityException) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Güvenlik hatası: Konum izni eksik!", Toast.LENGTH_SHORT).show()
        }
    }

    // ------------------
    //  Anı Detay Dialogu
    // ------------------
    private fun showMemoryDetailDialog(documentId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("memories").document(documentId).get()
            .addOnSuccessListener { document ->
                val text = document.getString("text") ?: ""
                val photoUrl = document.getString("photoUrl")
                val audioUrl = document.getString("audioUrl")

                val dialogView = layoutInflater.inflate(R.layout.dialog_memory_detail, null)
                val textView = dialogView.findViewById<EditText>(R.id.textViewMemory)
                val imageView = dialogView.findViewById<ImageView>(R.id.imageViewPhoto)
                val playButton = dialogView.findViewById<Button>(R.id.buttonPlayAudio)

                textView.setText(text)

                if (photoUrl != null) {
                    Glide.with(this).load(photoUrl).into(imageView)
                } else {
                    imageView.visibility = View.GONE
                }

                playButton.setOnClickListener {
                    if (audioUrl != null) {
                        val mediaPlayer = MediaPlayer().apply {
                            setDataSource(audioUrl)
                            prepare()
                            start()
                        }
                    } else {
                        Toast.makeText(requireContext(), "Ses bulunamadı", Toast.LENGTH_SHORT).show()
                    }
                }

                AlertDialog.Builder(requireContext())
                    .setTitle("Anı Detayı")
                    .setView(dialogView)
                    .setPositiveButton("Kapat", null)
                    .show()
            }
    }




    // ------------------
    //  Harita Hazır (OnMapReady)
    // ------------------
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        enableMyLocation()
        moveCameraToUserLocation()

        loadExistingMemories()

        googleMap.setOnMapLongClickListener { latLng ->
            val currentMode = viewModel.selectedMode.value
            if (currentMode == MemoryAddMode.YERINDE_EKLE) {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null) {
                            val distance = FloatArray(1)
                            Location.distanceBetween(
                                location.latitude, location.longitude,
                                latLng.latitude, latLng.longitude,
                                distance
                            )
                            if (distance[0] <= 50f) {
                                openAddMemoryDialog(latLng)
                                viewModel.onMemoryAddedInPlace(latLng.latitude, latLng.longitude)
                                Toast.makeText(requireContext(), "Rozet kazandınız! 🎉", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(requireContext(), "Bu konumdan 50 metreden uzaksın, anı ekleyemezsin.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(requireContext(), "Konum alınamadı.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    // İzin yoksa izin istemeyi unutma
                    Toast.makeText(requireContext(), "Konum izni verilmedi.", Toast.LENGTH_SHORT).show()
                }
            } else {
                openAddMemoryDialog(latLng)
                viewModel.onMemoryAdded() // Serbest modda konum göndermeye gerek yok
            }

            // Geofence ekle
            val geofenceId = UUID.randomUUID().toString()
            addGeofence(latLng, geofenceId)
        }


    }

    // ------------------
    //  Yardımcı Fonksiyonlar
    // ------------------
    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
        }
    }

    private fun moveCameraToUserLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

        fusedLocationClient.getCurrentLocation(PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        LatLng(location.latitude, location.longitude), 17f))
                }
            }
    }

    private fun loadExistingMemories() {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "unknownUser"

        db.collection("memories")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val lat = document.getDouble("latitude") ?: continue
                    val lng = document.getDouble("longitude") ?: continue
                    val text = document.getString("text") ?: ""

                    val marker = googleMap.addMarker(
                        MarkerOptions().position(LatLng(lat, lng)).title("Anı").snippet(text)
                    )
                    marker?.tag = document.id
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Anılar yüklenemedi: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun openAddMemoryDialog(latLng: LatLng) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_memory, null)
        dialogEditText = dialogView.findViewById(R.id.editTextMemory)
        dialogImageView = dialogView.findViewById(R.id.imageViewSelectedPhoto)
        val buttonSelectPhoto = dialogView.findViewById<Button>(R.id.buttonSelectPhoto)
        val buttonRecordAudio = dialogView.findViewById<Button>(R.id.buttonRecordAudio)
        val buttonStopAudio = dialogView.findViewById<Button>(R.id.buttonStopAudio)

        selectedImageUri?.let {
            dialogImageView?.setImageURI(it)
            dialogImageView?.visibility = View.VISIBLE
        } ?: run {
            dialogImageView?.visibility = View.GONE
        }

        buttonSelectPhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        buttonRecordAudio.setOnClickListener {
            if (!isRecording) {
                startRecording()
                buttonRecordAudio.visibility = View.GONE
                buttonStopAudio.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "Ses kaydı başladı", Toast.LENGTH_SHORT).show()
            }
        }

        buttonStopAudio.setOnClickListener {
            if (isRecording) {
                stopRecording()
                buttonRecordAudio.visibility = View.VISIBLE
                buttonStopAudio.visibility = View.GONE
                Toast.makeText(requireContext(), "Ses kaydı durdu", Toast.LENGTH_SHORT).show()
            }
        }

        addMemoryDialog = AlertDialog.Builder(requireContext())
            .setTitle("Yeni Anı Ekle")
            .setView(dialogView)
            .setPositiveButton("Kaydet") { dialogInterface, _ ->
                val memoryText = dialogEditText?.text.toString()
                if (memoryText.isNotBlank()) {
                    CoroutineScope(Dispatchers.Main).launch {
                        saveMemory(latLng, memoryText, selectedImageUri, recordedAudioFilePath)
                        selectedImageUri = null
                        recordedAudioFilePath = null
                        dialogImageView = null
                        dialogEditText = null

                        // Burada rozet verme fonksiyonunu çağırıyoruz:
                        viewModel.onMemoryAddedInPlace(latLng.latitude, latLng.longitude)
                    }
                } else {
                    Toast.makeText(requireContext(), "Anı boş olamaz!", Toast.LENGTH_SHORT).show()
                }
                dialogInterface.dismiss()
            }
            .setNegativeButton("İptal") { dialogInterface, _ ->
                if (isRecording) stopRecording()
                selectedImageUri = null
                recordedAudioFilePath = null
                dialogImageView = null
                dialogEditText = null
                dialogInterface.dismiss()
            }
            .create()

        addMemoryDialog?.show()
    }




        private fun startRecording() {
        val fileName = "${requireContext().externalCacheDir?.absolutePath}/${UUID.randomUUID()}.3gp"
        audioFilePath = fileName

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(audioFilePath)
            prepare()
            start()
        }
        isRecording = true
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        isRecording = false
        recordedAudioFilePath = audioFilePath
    }

    private suspend fun saveMemory(latLng: LatLng, text: String, photoUri: Uri?, audioFilePath: String?) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Log.e("FirestoreDebug", "Kullanıcı null, Firestore yazılamaz.")
            Toast.makeText(requireContext(), "Kullanıcı girişi yapılmamış!", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val db = FirebaseFirestore.getInstance()
            val userId = user.uid
            val storageRef = Firebase.storage.reference

            var photoUrl: String? = null
            photoUri?.let {
                try {
                    Log.d("UPLOAD", "Fotoğraf yükleniyor: $it")
                    val photoRef = storageRef.child("images/${UUID.randomUUID()}.jpg")
                    photoRef.putFile(it).await()
                    photoUrl = photoRef.downloadUrl.await().toString()
                    Log.d("UPLOAD", "Fotoğraf yüklendi: $photoUrl")
                } catch (e: Exception) {
                    Log.e("UPLOAD_ERROR", "Fotoğraf yükleme hatası: ${e.message}", e)
                }
            }

            var audioUrl: String? = null
            audioFilePath?.let {
                val audioFile = File(it)
                Log.d("UPLOAD", "Ses dosyası yolu: $audioFilePath")
                if (audioFile.exists()) {
                    try {
                        Log.d("UPLOAD", "Ses dosyası bulundu, yükleniyor.")
                        val audioRef = storageRef.child("audio/${UUID.randomUUID()}.3gp")
                        audioRef.putFile(Uri.fromFile(audioFile)).await()
                        audioUrl = audioRef.downloadUrl.await().toString()
                        Log.d("UPLOAD", "Ses dosyası yüklendi: $audioUrl")
                    } catch (e: Exception) {
                        Log.e("UPLOAD_ERROR", "Ses dosyası yükleme hatası: ${e.message}", e)
                    }
                } else {
                    Log.e("UPLOAD_ERROR", "Ses dosyası bulunamadı!")
                }
            }

            val memoryData = hashMapOf(
                "userId" to userId,
                "latitude" to latLng.latitude,
                "longitude" to latLng.longitude,
                "text" to text,
                "photoUrl" to photoUrl,
                "audioUrl" to audioUrl,
                "timestamp" to System.currentTimeMillis()
            )


            val documentRef = db.collection("memories").add(memoryData).await()
            Toast.makeText(requireContext(), "Anı kaydedildi!", Toast.LENGTH_SHORT).show()

            val marker = googleMap.addMarker(
                MarkerOptions().position(latLng).title("Anı").snippet(text)
            )
            marker?.tag = documentRef.id

        } catch (e: Exception) {
            Log.e("FirestoreError", "Anı kaydedilemedi", e)
            Toast.makeText(requireContext(), "Hata oluştu: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        binding.mapView.onPause()
        super.onPause()
    }

    override fun onDestroyView() {
        binding.mapView.onDestroy()
        super.onDestroyView()
        _binding = null
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }
}
