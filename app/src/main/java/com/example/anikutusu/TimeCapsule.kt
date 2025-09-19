package com.example.anikutusu

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TimeCapsule : Fragment(R.layout.fragment_time_capsule) {

    // UI
    private lateinit var etTitle: EditText
    private lateinit var ivPhotoPreview: ImageView
    private lateinit var btnPickPhoto: Button

    private lateinit var tvAudioStatus: TextView
    private lateinit var btnRecordAudio: Button
    private lateinit var btnPlayAudio: Button
    private lateinit var btnDeleteAudio: Button
    private lateinit var btnPickAudio: Button

    private lateinit var btnPickOnMap: Button
    private lateinit var tvLocationStatus: TextView
    private lateinit var tvLat: TextView
    private lateinit var tvLng: TextView
    private lateinit var layoutLatLngHidden: LinearLayout

    private lateinit var switchTimeCapsule: Switch
    private lateinit var layoutUnlock: LinearLayout
    private lateinit var layoutUnlockButtons: LinearLayout
    private lateinit var etUnlockDate: EditText
    private lateinit var etUnlockTime: EditText
    private lateinit var btnPickUnlockDate: Button
    private lateinit var btnPickUnlockTime: Button

    private lateinit var switchAddCollaborator: Switch
    private lateinit var layoutCollaborator: LinearLayout
    private lateinit var etCollaborator: EditText
    private lateinit var btnAddCollaborator: Button
    private lateinit var collaboratorsContainer: LinearLayout

    private lateinit var btnSave: Button

    // State
    private var photoUri: Uri? = null
    private var pickedAudioUri: Uri? = null
    private var recordedFile: File? = null

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isRecording = false
    private var isPlaying = false

    // (opsiyonel) haritadan dönüşte set edebilirsin
    private var selectedLat: Double? = null
    private var selectedLng: Double? = null

    // Launchers
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            photoUri = it
            ivPhotoPreview.setImageURI(it)
        }
    }

    private val pickAudio = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            pickedAudioUri = it
            recordedFile = null
            tvAudioStatus.text = "Ses: dosya seçildi"
            syncAudioButtons()
        }
    }

    private val requestRecordAudioPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startRecording() else toast("Mikrofon izni gerekli.")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // findViewById
        etTitle = view.findViewById(R.id.etTitle)

        ivPhotoPreview = view.findViewById(R.id.ivPhotoPreview)
        btnPickPhoto = view.findViewById(R.id.btnPickPhoto)
        btnPickOnMap = view.findViewById(R.id.btnPickOnMap)

        tvLocationStatus = view.findViewById(R.id.tvLocationStatus)
        tvLat = view.findViewById(R.id.tvLat)
        tvLng = view.findViewById(R.id.tvLng)
        layoutLatLngHidden = view.findViewById(R.id.layoutLatLngHidden)

        tvAudioStatus = view.findViewById(R.id.tvAudioStatus)
        btnRecordAudio = view.findViewById(R.id.btnRecordAudio)
        btnPlayAudio = view.findViewById(R.id.btnPlayAudio)
        btnDeleteAudio = view.findViewById(R.id.btnDeleteAudio)
        btnPickAudio = view.findViewById(R.id.btnPickAudio)

        switchTimeCapsule = view.findViewById(R.id.switchTimeCapsule)
        layoutUnlock = view.findViewById(R.id.layoutUnlock)
        layoutUnlockButtons = view.findViewById(R.id.layoutUnlockButtons)
        etUnlockDate = view.findViewById(R.id.etUnlockDate)
        etUnlockTime = view.findViewById(R.id.etUnlockTime)
        btnPickUnlockDate = view.findViewById(R.id.btnPickUnlockDate)
        btnPickUnlockTime = view.findViewById(R.id.btnPickUnlockTime)

        switchAddCollaborator = view.findViewById(R.id.switchAddCollaborator)
        layoutCollaborator = view.findViewById(R.id.layoutCollaborator)
        etCollaborator = view.findViewById(R.id.etCollaborator)
        btnAddCollaborator = view.findViewById(R.id.btnAddCollaborator)
        collaboratorsContainer = view.findViewById(R.id.collaboratorsContainer)

        btnSave = view.findViewById(R.id.btnSave)



        findNavController().currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<Bundle>("picked_location")
            ?.observe(viewLifecycleOwner) { b ->
                selectedLat = b.getDouble("picked_lat")
                selectedLng = b.getDouble("picked_lng")
                val addr = b.getString("picked_address").orEmpty()

                layoutLatLngHidden.isVisible = true
                tvLat.text = "lat: ${"%.6f".format(selectedLat)}"
                tvLng.text = "lng: ${"%.6f".format(selectedLng)}"
                tvLocationStatus.text = if (addr.isNotBlank())
                    "Konum seçildi: $addr" else "Konum seçildi"
            }


        // --- Map
        btnPickOnMap.setOnClickListener {
            // Haritaya git; projendeki action id'yi doğrula
            findNavController().navigate(R.id.action_global_homeMapFragment)
        }

        // --- Photo & Audio
        btnPickPhoto.setOnClickListener { pickImage.launch("image/*") }
        btnPickAudio.setOnClickListener { pickAudio.launch("audio/*") }

        btnRecordAudio.setOnClickListener {
            if (isRecording) stopRecording() else requestRecordAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
        }

        btnPlayAudio.setOnClickListener {
            if (isPlaying) stopPlayback() else startPlayback()
        }

        btnDeleteAudio.setOnClickListener {
            stopPlayback()
            pickedAudioUri = null
            recordedFile?.runCatching { delete() }
            recordedFile = null
            tvAudioStatus.text = "Ses: seçilmedi / kaydedilmedi"
            syncAudioButtons()
        }

        // --- Time Capsule toggles
        switchTimeCapsule.setOnCheckedChangeListener { _, checked ->
            layoutUnlock.isVisible = checked
            layoutUnlockButtons.isVisible = checked
        }

        btnPickUnlockDate.setOnClickListener { showDatePicker { etUnlockDate.setText(it) } }
        btnPickUnlockTime.setOnClickListener { showTimePicker { etUnlockTime.setText(it) } }
        etUnlockDate.setOnClickListener { btnPickUnlockDate.performClick() }
        etUnlockTime.setOnClickListener { btnPickUnlockTime.performClick() }

        // --- Collaborator (BottomSheet ile)
        switchAddCollaborator.setOnCheckedChangeListener { _, checked ->
            layoutCollaborator.isVisible = checked
            collaboratorsContainer.isVisible = checked && collaboratorsContainer.childCount > 0
        }

        btnAddCollaborator.setOnClickListener {
            CollaboratorPickerBottomSheet().show(parentFragmentManager, "CollaboratorPicker")
        }

        parentFragmentManager.setFragmentResultListener(
            CollaboratorPickerBottomSheet.REQUEST_KEY_COLLABORATOR,
            viewLifecycleOwner
        ) { _, bundle ->
            val name = bundle.getString(CollaboratorPickerBottomSheet.BUNDLE_USER_NAME).orEmpty()
            val mail = bundle.getString(CollaboratorPickerBottomSheet.BUNDLE_USER_MAIL).orEmpty()

            etCollaborator.setText("@$name")
            collaboratorsContainer.removeAllViews()
            val tv = TextView(requireContext()).apply {
                text = "$name  <$mail>"
                setPadding(24, 12, 24, 12)
                setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
                setOnClickListener {
                    collaboratorsContainer.removeAllViews()
                    collaboratorsContainer.isVisible = false
                    etCollaborator.text = null
                }
            }
            collaboratorsContainer.addView(tv)
            collaboratorsContainer.isVisible = switchAddCollaborator.isChecked
        }

        // --- Save
        btnSave.setOnClickListener { onSave() }

        // Initial
        syncAudioButtons()
    }

    // ====== Audio ======
    private fun startRecording() {
        stopPlayback()

        recordedFile = File.createTempFile("time_capsule_", ".m4a", requireContext().cacheDir)
        pickedAudioUri = null

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(requireContext())
        } else {
            MediaRecorder()
        }

        mediaRecorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(96_000)
            setAudioSamplingRate(44_100)
            setOutputFile(recordedFile!!.absolutePath)
            try {
                prepare()
                start()
                isRecording = true
                tvAudioStatus.text = "Kayıt alınıyor..."
                btnRecordAudio.text = "Durdur"
                syncAudioButtons()
            } catch (e: Exception) {
                toast("Kayıt başlatılamadı: ${e.message}")
                cleanupRecorder()
            }
        }
    }

    private fun stopRecording() {
        runCatching { mediaRecorder?.stop() }
        cleanupRecorder()
        isRecording = false
        tvAudioStatus.text = if (recordedFile != null) "Kayıt hazır" else "Ses: seçilmedi / kaydedilmedi"
        btnRecordAudio.text = "Kaydet"
        syncAudioButtons()
    }

    private fun cleanupRecorder() {
        runCatching { mediaRecorder?.reset() }
        runCatching { mediaRecorder?.release() }
        mediaRecorder = null
    }

    private fun startPlayback() {
        val playUri = pickedAudioUri ?: recordedFile?.let { Uri.fromFile(it) } ?: run {
            toast("Çalınacak ses yok."); return
        }
        stopPlayback()

        mediaPlayer = MediaPlayer().apply {
            setDataSource(requireContext(), playUri)
            prepare()
            start()
        }
        isPlaying = true
        btnPlayAudio.text = "Durdur"
        tvAudioStatus.text = "Oynatılıyor..."
        mediaPlayer?.setOnCompletionListener { stopPlayback() }
        syncAudioButtons()
    }

    private fun stopPlayback() {
        mediaPlayer?.runCatching { if (isPlaying) stop() }
        mediaPlayer?.runCatching { release() }
        mediaPlayer = null

        isPlaying = false
        btnPlayAudio.text = "Çal"
        tvAudioStatus.text =
            if (pickedAudioUri != null || recordedFile != null) "Ses hazır"
            else "Ses: seçilmedi / kaydedilmedi"
        syncAudioButtons()
    }

    private fun syncAudioButtons() {
        val hasAudio = pickedAudioUri != null || recordedFile != null
        btnRecordAudio.isEnabled = !isPlaying
        btnPlayAudio.isEnabled   = !isRecording && hasAudio
        btnDeleteAudio.isEnabled = !isRecording && hasAudio
    }

    // ====== Date & Time ======
    private fun showDatePicker(onPicked: (String) -> Unit) {
        val c = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, y, m, d -> onPicked(String.format(Locale.getDefault(), "%02d.%02d.%04d", d, m + 1, y)) },
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker(onPicked: (String) -> Unit) {
        val c = Calendar.getInstance()
        TimePickerDialog(
            requireContext(),
            { _, h, min -> onPicked(String.format(Locale.getDefault(), "%02d:%02d", h, min)) },
            c.get(Calendar.HOUR_OF_DAY),
            c.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun parseUnlockMillis(dateStr: String, timeStr: String): Long? {
        return try {
            val full = "$dateStr $timeStr"
            val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            sdf.parse(full)?.time
        } catch (_: Exception) { null }
    }

    // ====== Save (Storage → Firestore → RTDB) ======
    private fun onSave() {
        val title = etTitle.text?.toString()?.trim().orEmpty()
        val unlockDate = if (switchTimeCapsule.isChecked) etUnlockDate.text?.toString()?.trim().orEmpty() else ""
        val unlockTime = if (switchTimeCapsule.isChecked) etUnlockTime.text?.toString()?.trim().orEmpty() else ""
        val collaborator = if (switchAddCollaborator.isChecked && collaboratorsContainer.childCount > 0) {
            (collaboratorsContainer.getChildAt(0) as? TextView)?.text?.toString()
                ?.replace("  ✕", "")?.trim().orEmpty()
        } else ""

        val audioUri: Uri? = pickedAudioUri ?: recordedFile?.let { Uri.fromFile(it) }

        if (title.isEmpty()) { toast("Başlık zorunlu."); return }
        if (switchTimeCapsule.isChecked && (unlockDate.isEmpty() || unlockTime.isEmpty())) {
            toast("Açılma tarihi ve saatini seç."); return
        }

        val unlockAtMillis = if (switchTimeCapsule.isChecked) parseUnlockMillis(unlockDate, unlockTime) else null

        btnSave.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            val ok = saveMemoryToFirebase(
                title = title,
                photoUri = photoUri,
                audioUri = audioUri,
                unlockAtMillis = unlockAtMillis,
                collaborator = collaborator,
                latitude = selectedLat,
                longitude = selectedLng
            )
            btnSave.isEnabled = true
            if (ok) {
                toast("Anı kaydedildi!")
                // Temizle
                etTitle.text?.clear()
                ivPhotoPreview.setImageDrawable(null)
                photoUri = null
                pickedAudioUri = null
                recordedFile?.runCatching { delete() }
                recordedFile = null
                tvAudioStatus.text = "Ses: seçilmedi / kaydedilmedi"
            } else {
                toast("Kaydetme sırasında hata oluştu.")
            }
        }
    }

    private suspend fun getNextPostIndex(uid: String): Long {
        val fs = FirebaseFirestore.getInstance()
        val metaRef = fs.collection("users_meta").document(uid)

        return fs.runTransaction { tx ->
            val snap = tx.get(metaRef)
            val current = snap.getLong("postCount") ?: 0L
            val next = current + 1L
            if (snap.exists()) {
                tx.update(metaRef, mapOf("postCount" to next))
            } else {
                tx.set(metaRef, mapOf("postCount" to next))
            }
            next
        }.await() // <<< kritik kısım
    }




    private suspend fun saveMemoryToFirebase(
        title: String,
        photoUri: Uri?,
        audioUri: Uri?,
        unlockAtMillis: Long?,
        collaborator: String,
        latitude: Double?,
        longitude: Double?
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val user = FirebaseAuth.getInstance().currentUser ?: return@withContext false
            val uid = user.uid
            val username = user.displayName ?: user.email?.substringBefore("@") ?: uid
            val nextIndex = getNextPostIndex(uid)
            val prettyIndex = nextIndex.toString()
            val userFolder = "${username}Data"

            val storageRef = Firebase.storage.reference

            // Foto yükle
            var photoUrl: String? = null
            if (photoUri != null) {
                val photoName = "${username}photo${prettyIndex}.jpg"
                val photoRef = storageRef.child("users/$userFolder/images/$photoName")
                photoRef.putFile(photoUri).await()
                photoUrl = photoRef.downloadUrl.await().toString()
            }

            // Ses yükle
            var audioUrl: String? = null
            if (audioUri != null) {
                val audioName = "${username}audio${prettyIndex}.m4a"
                val audioRef = storageRef.child("users/$userFolder/audio/$audioName")
                audioRef.putFile(audioUri).await()
                audioUrl = audioRef.downloadUrl.await().toString()
            }

            // 🔽🔽🔽 BURAYA EKLE 🔽🔽🔽
            // --- GPS JSON ---
            var gpsUrl: String? = null
            if (latitude != null && longitude != null) {
                val gpsJson = """
                {
                  "lat": $latitude,
                  "lng": $longitude,
                  "title": ${JSONObject.quote(title)},
                  "username": ${JSONObject.quote(username)},
                  "timestamp": ${System.currentTimeMillis()}
                }
            """.trimIndent()

                val gpsName = "${username}gps${nextIndex}.json"
                val gpsRef  = storageRef.child("users/$userFolder/gps/$gpsName")
                val bytes = gpsJson.toByteArray(Charsets.UTF_8)
                gpsRef.putBytes(bytes).await()
                gpsUrl = gpsRef.downloadUrl.await().toString()
            }
            // 🔼🔼🔼 BURAYA EKLE 🔼🔼🔼

            // Firestore dokümanı oluştur
            val fs = FirebaseFirestore.getInstance()
            val data = mutableMapOf<String, Any?>(
                "userId" to uid,
                "username" to username,
                "title" to title,
                "photoUrl" to photoUrl,
                "audioUrl" to audioUrl,
                "gpsUrl" to gpsUrl,   // 🔽 Firestore’a linki de ekliyoruz
                "timestamp" to System.currentTimeMillis(),
                "collaborator" to collaborator.ifBlank { null },
                "unlockAt" to unlockAtMillis,
                "postIndex" to nextIndex
            )
            if (latitude != null && longitude != null) {
                data["latitude"] = latitude
                data["longitude"] = longitude
            }

            fs.collection("memories").add(data).await()
            true
        } catch (e: Exception) {
            false
        }
    }


    // ====== Utils ======
    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

    override fun onStop() {
        super.onStop()
        if (isRecording) stopRecording()
        if (isPlaying) stopPlayback()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaRecorder?.release(); mediaRecorder = null
        mediaPlayer?.release(); mediaPlayer = null
    }
}
