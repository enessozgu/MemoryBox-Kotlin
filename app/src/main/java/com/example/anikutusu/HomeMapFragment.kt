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
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.anikutusu.databinding.FragmentHomeMapBinding
import com.example.anikutusu.model.MemoryAddMode
import com.example.anikutusu.model.GeofenceBroadcastReceiver
import com.example.anikutusu.model.MemoryAddViewModel
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID

class HomeMapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentHomeMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var googleMap: GoogleMap
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var selectedImageUri: Uri? = null
    private var dialogImageView: ImageView? = null
    private var dialogEditText: EditText? = null
    private var addMemoryDialog: AlertDialog? = null
    private var recordedAudioFilePath: String? = null
    private var mediaRecorder: MediaRecorder? = null
    private var audioFilePath: String? = null
    private var isRecording = false
    private var locationPermissionGranted = false

    private lateinit var viewModel: MemoryAddViewModel

    private val GEOFENCE_RADIUS_IN_METERS = 100f

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                selectedImageUri = it
                dialogImageView?.setImageURI(it)
                dialogImageView?.visibility = View.VISIBLE
            }
        }

    private val locationAndAudioPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            locationPermissionGranted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
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

    val args: HomeMapFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        geofencingClient = LocationServices.getGeofencingClient(requireContext())

        viewModel = ViewModelProvider(this)[MemoryAddViewModel::class.java]

        viewModel.badges.observe(viewLifecycleOwner) { set ->
            binding.textViewBadgeStatus.text = "Rozet: ${set.size}"
        }

        binding.anlik.setOnClickListener { goToUserLocation() }

        val toolbar = binding.toolbar
        toolbar.title = "Anılarım"
        (requireActivity() as AppCompatActivity).apply {
            setSupportActionBar(toolbar)
            val toggle = ActionBarDrawerToggle(
                this, binding.drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close
            )
            binding.drawerLayout.addDrawerListener(toggle); toggle.syncState()
        }

        binding.homeNavigationView.setNavigationItemSelectedListener {
            showDrawerMenuItemAction(it.itemId); true
        }

        binding.btnModSec.setOnClickListener { viewModel.toggleMode() }
        viewModel.selectedMode.observe(viewLifecycleOwner) { mode ->
            binding.btnModSec.text =
                if (mode == MemoryAddMode.SERBEST_EKLE) "Mode: Free Add" else "Mode: Insert in Place"
        }

        locationAndAudioPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.RECORD_AUDIO
            )
        )

        // Map init
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)

        if (!Places.isInitialized()) {
            // ⚠️ API key’i güvenli yere taşı (manifest/local.properties)
            Places.initialize(requireContext(), "AIzaSyB85TYSviAbkfU8D7D7jZKgYHfopq4iHOY")
        }

        var autocomplete = childFragmentManager.findFragmentById(
            R.id.autocomplete_fragment_container
        ) as? AutocompleteSupportFragment
        if (autocomplete == null) {
            autocomplete = AutocompleteSupportFragment.newInstance()
            childFragmentManager.beginTransaction()
                .replace(R.id.autocomplete_fragment_container, autocomplete)
                .commitNow()
        }
        autocomplete.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG))
        autocomplete.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                place.latLng?.let { googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 15f)) }
            }
            override fun onError(status: com.google.android.gms.common.api.Status) {
                Toast.makeText(requireContext(), "Hata: ${status.statusMessage}", Toast.LENGTH_LONG).show()
                Log.e("AutocompleteError", "Place error: $status")
            }
        })
    }

    private fun showDrawerMenuItemAction(menuItemId: Int) {
        when (menuItemId) {
            R.id.nav_map -> {
                Toast.makeText(requireContext(), "You're already here 👀", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_badges, R.id.nav_rozet -> {
                // Rozet ekranına git (sign out kaldırıldı)
                findNavController().navigate(R.id.action_homeMapFragment_to_badgeFragment)
            }
            R.id.nav_memories -> {
                findNavController().navigate(R.id.action_homePageFragment_to_memoryListFragment)
            }
            R.id.nav_settings -> {
                googleMap.mapType = when (googleMap.mapType) {
                    GoogleMap.MAP_TYPE_NORMAL    -> GoogleMap.MAP_TYPE_SATELLITE
                    GoogleMap.MAP_TYPE_SATELLITE -> GoogleMap.MAP_TYPE_TERRAIN
                    GoogleMap.MAP_TYPE_TERRAIN   -> GoogleMap.MAP_TYPE_NORMAL
                    else -> GoogleMap.MAP_TYPE_NORMAL
                }
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        enableMyLocation()
        moveCameraToUserLocation()
        loadExistingMemories()

        val passedLat = args.latitude
        val passedLng = args.longitude
        if (passedLat != null && passedLng != null) {
            val target = LatLng(passedLat.toDouble(), passedLng.toDouble())
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 16f))
            googleMap.addMarker(MarkerOptions().position(target).title("Seçilen Konum"))
        }

        googleMap.setOnMapLongClickListener { latLng ->
            val currentMode = viewModel.selectedMode.value
            if (currentMode == MemoryAddMode.YERINDE_EKLE) {
                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null) {
                            val distance = FloatArray(1)
                            Location.distanceBetween(
                                location.latitude, location.longitude,
                                latLng.latitude, latLng.longitude, distance
                            )
                            if (distance[0] <= 50f) {
                                openAddMemoryDialog(latLng)
                                // Rozet tetiklemesi, kayıt tamamlanınca openAddMemoryDialog->saveMemory içinde yapılacak
                                Toast.makeText(requireContext(), "Konuma uygun, anı ekleyebilirsin.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(requireContext(),
                                    "Bu konumdan 50m uzaktasın, anı ekleyemezsin.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(requireContext(), "Konum alınamadı.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Konum izni yok.", Toast.LENGTH_SHORT).show()
                }
            } else {
                openAddMemoryDialog(latLng)
            }

            val geofenceId = UUID.randomUUID().toString()
            addGeofence(latLng, geofenceId)
        }
    }

    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = true
        }
    }

    private fun moveCameraToUserLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) return

        fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    googleMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 17f)
                    )
                }
            }
    }

    // Firestore'dan eski anıları marker olarak yükleme (mevcut akışın)
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
        } ?: run { dialogImageView?.visibility = View.GONE }

        buttonSelectPhoto.setOnClickListener { pickImageLauncher.launch("image/*") }

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
        mediaRecorder?.apply { stop(); release() }
        mediaRecorder = null
        isRecording = false
        recordedAudioFilePath = audioFilePath
    }

    // 🔥 Anı kaydetme (Firestore) + Realtime DB'ye iz + Rozet tetikleme
    private suspend fun saveMemory(
        latLng: LatLng,
        text: String,
        photoUri: Uri?,
        audioFilePath: String?
    ) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Log.e("FirestoreDebug", "Kullanıcı null, Firestore yazılamaz.")
            Toast.makeText(requireContext(), "Kullanıcı girişi yapılmamış!", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val fs = FirebaseFirestore.getInstance()
            val storageRef = com.google.firebase.Firebase.storage.reference

            val username = user.displayName ?: user.email?.substringBefore("@") ?: user.uid
            val userFolder = "${username}Data"

            // Fotoğraf
            var photoUrl: String? = null
            if (photoUri != null) {
                try {
                    val photoName = "${username}Photo${System.currentTimeMillis()}.jpg"
                    val photoRef = storageRef.child("users/$userFolder/images/$photoName")
                    photoRef.putFile(photoUri).await()
                    photoUrl = photoRef.downloadUrl.await().toString()
                } catch (e: Exception) {
                    Log.e("UPLOAD_ERROR", "Foto yükleme hatası: ${e.message}", e)
                }
            }

            // Ses
            var audioUrl: String? = null
            if (audioFilePath != null) {
                val audioFile = File(audioFilePath)
                if (audioFile.exists()) {
                    try {
                        val audioName = "${username}Audio${System.currentTimeMillis()}.3gp"
                        val audioRef = storageRef.child("users/$userFolder/audio/$audioName")
                        audioRef.putFile(Uri.fromFile(audioFile)).await()
                        audioUrl = audioRef.downloadUrl.await().toString()
                    } catch (e: Exception) {
                        Log.e("UPLOAD_ERROR", "Ses yükleme hatası: ${e.message}", e)
                    }
                }
            }

            // Firestore kaydı
            val memoryData = hashMapOf(
                "userId" to user.uid,
                "username" to username,
                "latitude" to latLng.latitude,
                "longitude" to latLng.longitude,
                "text" to text,
                "photoUrl" to photoUrl,
                "audioUrl" to audioUrl,
                "timestamp" to System.currentTimeMillis()
            )
            val docRef = fs.collection("memories").add(memoryData).await()

            // 🔹 Realtime DB'ye "iz" bırak: Users/{username}Data/Memories/push() = { ts }
            val rdb: DatabaseReference =
                FirebaseDatabase.getInstance("https://anikutusuapp-default-rtdb.firebaseio.com/").reference
            val userNode = rdb.child("Users").child("${username}Data")
            userNode.child("Memories").push().setValue(mapOf("ts" to System.currentTimeMillis()))

            Toast.makeText(requireContext(), "Anı kaydedildi!", Toast.LENGTH_SHORT).show()

            // Marker ekle
            val marker = googleMap.addMarker(
                MarkerOptions().position(latLng).title("Anı").snippet(text)
            )
            marker?.tag = docRef.id

            // ✅ Rozet tetikle (ilk anı + b2 ilerleme + opsiyonel konum rozetleri)
            viewModel.onMemoryPersisted(userLat = latLng.latitude, userLon = latLng.longitude)

        } catch (e: Exception) {
            Log.e("FirestoreError", "Anı kaydedilemedi", e)
            Toast.makeText(requireContext(), "Hata oluştu: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun addGeofence(latLng: LatLng, geofenceId: String) {
        if (!locationPermissionGranted) return
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
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
            requireContext(), 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        try {
            geofencingClient.addGeofences(geofenceRequest, pendingIntent)
                .addOnSuccessListener { /* ok */ }
                .addOnFailureListener { /* log */ }
        } catch (e: SecurityException) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Güvenlik hatası: Konum izni eksik!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun goToUserLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val userLatLng = LatLng(it.latitude, it.longitude)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 16f))
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
        requireActivity().findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.vp).isUserInputEnabled = false
    }

    override fun onPause() {
        requireActivity().findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.vp).isUserInputEnabled = true
        binding.mapView.onPause()
        super.onPause()
    }

    override fun onDestroyView() {
        binding.mapView.onDestroy()
        _binding = null
        super.onDestroyView()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }
}
