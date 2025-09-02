// TimeCapsuleFragment.kt
package com.example.anikutusu

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
import androidx.navigation.fragment.findNavController
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TimeCapsule : Fragment(R.layout.fragment_time_capsule) {


    // UI
    private lateinit var etTitle: EditText
    private lateinit var ivPhotoPreview: ImageView
    private lateinit var btnPickPhoto: Button

    private lateinit var etMemory: EditText

    private lateinit var tvAudioStatus: TextView
    private lateinit var btnRecordAudio: Button
    private lateinit var btnPlayAudio: Button
    private lateinit var btnDeleteAudio: Button
    private lateinit var btnPickAudio: Button

    private lateinit var switchTimeCapsule: Switch
    private lateinit var layoutUnlock: LinearLayout
    private lateinit var layoutUnlockButtons: LinearLayout
    private lateinit var etUnlockDate: EditText
    private lateinit var etUnlockTime: EditText
    private lateinit var btnPickUnlockDate: Button
    private lateinit var btnPickUnlockTime: Button
    private lateinit var btnPickOnMap: Button

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
            recordedFile = null // seçili dosya varsa kayıt dosyasını geçersiz say
            tvAudioStatus.text = "Ses: dosya seçildi"
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

        btnPickOnMap = view.findViewById(R.id.btnPickOnMap)   // <--- EKLENDİ


        btnPickOnMap.setOnClickListener {
            findNavController().navigate(R.id.action_global_homeMapFragment)
        }



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

        btnAddCollaborator = view.findViewById(R.id.btnAddCollaborator)
        collaboratorsContainer = view.findViewById(R.id.collaboratorsContainer)

        btnSave = view.findViewById(R.id.btnSave)

        // Listeners
        btnPickPhoto.setOnClickListener { pickImage.launch("image/*") }

        btnPickAudio.setOnClickListener { pickAudio.launch("audio/*") }

        btnRecordAudio.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                // izin iste
                requestMicPermissionThenRecord()
            }
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
        }

        switchTimeCapsule.setOnCheckedChangeListener { _, checked ->
            layoutUnlock.isVisible = checked
            layoutUnlockButtons.isVisible = checked
        }

        btnPickUnlockDate.setOnClickListener { showDatePicker { etUnlockDate.setText(it) } }
        btnPickUnlockTime.setOnClickListener { showTimePicker { etUnlockTime.setText(it) } }
        // alanlara tıklayınca da picker açılsın
        etUnlockDate.setOnClickListener { btnPickUnlockDate.performClick() }
        etUnlockTime.setOnClickListener { btnPickUnlockTime.performClick() }

        switchAddCollaborator.setOnCheckedChangeListener { _, checked ->
            layoutCollaborator.isVisible = checked
            collaboratorsContainer.isVisible = checked && collaboratorsContainer.childCount > 0
        }

        btnAddCollaborator.setOnClickListener {
            val name = etCollaborator.text?.toString()?.trim().orEmpty()
            if (name.isEmpty()) return@setOnClickListener
            addSingleCollaboratorTag(name)
            etCollaborator.text?.clear()
        }

        btnSave.setOnClickListener { onSave() }

        // İlk durum
        syncAudioButtons()
    }

    // ====== Audio ======
    private fun requestMicPermissionThenRecord() {
        // Sadece RECORD_AUDIO yeter (cache'e yazıyoruz)
        requestRecordAudioPermission.launch(android.Manifest.permission.RECORD_AUDIO)
    }

    private fun startRecording() {
        stopPlayback() // çalma açıksa kapat

        // hedef dosya
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
                syncAudioButtons()
                btnRecordAudio.text = "Durdur"
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
        isPlaying = true              // <- serbest; çünkü var
        btnPlayAudio.text = "Durdur"
        tvAudioStatus.text = "Oynatılıyor..."
        mediaPlayer?.setOnCompletionListener { stopPlayback() }
        syncAudioButtons()
    }

    private fun stopPlayback() {
        mediaPlayer?.runCatching { if (isPlaying) stop() }  // bu isPlaying, MediaPlayer’ınki; sorun değil
        mediaPlayer?.runCatching { release() }
        mediaPlayer = null

        isPlaying = false            // <- serbest; çünkü var
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

    // ====== Collaborator (tek kişi sınırı) ======
    private fun addSingleCollaboratorTag(name: String) {
        collaboratorsContainer.removeAllViews()
        val tv = TextView(requireContext()).apply {
            text = "$name  ✕"
            setPadding(24, 12, 24, 12)
            // Basit görsel vurgu (opsiyonel)
            setBackgroundColor(0xFFE0E0E0.toInt())
            setOnClickListener {
                collaboratorsContainer.removeAllViews()
                collaboratorsContainer.isVisible = false
            }
        }
        collaboratorsContainer.addView(tv)
        collaboratorsContainer.isVisible = switchAddCollaborator.isChecked
    }

    // ====== Save ======
    private fun onSave() {
        val title = etTitle.text?.toString()?.trim().orEmpty()
        val memory = etMemory.text?.toString()?.trim().orEmpty()
        val unlockDate = if (switchTimeCapsule.isChecked) etUnlockDate.text?.toString()?.trim().orEmpty() else ""
        val unlockTime = if (switchTimeCapsule.isChecked) etUnlockTime.text?.toString()?.trim().orEmpty() else ""
        val collaborator = if (switchAddCollaborator.isChecked && collaboratorsContainer.childCount > 0) {
            (collaboratorsContainer.getChildAt(0) as? TextView)?.text?.toString()?.replace("  ✕", "")?.trim().orEmpty()
        } else ""

        val audioUri: Uri? = pickedAudioUri ?: recordedFile?.let { Uri.fromFile(it) }

        // burada DB/Upload entegrasyonunu yaparsın; şimdilik sadece basic doğrulama
        if (title.isEmpty()) { toast("Başlık zorunlu."); return }
        if (memory.isEmpty()) { toast("Anı metni boş olamaz."); return }
        if (switchTimeCapsule.isChecked && (unlockDate.isEmpty() || unlockTime.isEmpty())) {
            toast("Açılma tarihi ve saatini seç."); return
        }

        // örnek log/toast
        val info = buildString {
            appendLine("Başlık: $title")
            appendLine("Metin: ${memory.take(40)}${if (memory.length > 40) "..." else ""}")
            appendLine("Foto: ${photoUri ?: "yok"}")
            appendLine("Ses: ${audioUri ?: "yok"}")
            appendLine("Zaman kapsülü: ${switchTimeCapsule.isChecked} ($unlockDate $unlockTime)")
            appendLine("Ortak: ${collaborator.ifEmpty { "yok" }}")
        }
        toast("Kaydedildi (demo).")
        android.util.Log.d("TimeCapsule", info)
    }

    // ====== Utils ======
    private fun toast(msg: String) = Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

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
