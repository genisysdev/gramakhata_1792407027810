package com.gramakhata.app.ui.customer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.CircleCropTransformation
import com.gramakhata.app.R
import com.gramakhata.app.databinding.FragmentAddCustomerBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment for adding or editing a customer.
 * Supports camera capture, gallery selection, and voice input for name.
 */
class AddCustomerFragment : Fragment() {

    private var _binding: FragmentAddCustomerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CustomerViewModel by viewModels()

    private var customerId: Long = -1L
    private var currentPhotoPath: String? = null
    private var tempPhotoUri: Uri? = null

    // Camera result
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoUri != null) {
            currentPhotoPath = tempPhotoUri!!.path
            binding.ivCustomerPhoto.load(tempPhotoUri) {
                transformations(CircleCropTransformation())
            }
            binding.ivCustomerPhoto.setPadding(0, 0, 0, 0)
        }
    }

    // Gallery result
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val file = createImageFile()
            requireContext().contentResolver.openInputStream(it)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            currentPhotoPath = file.absolutePath
            binding.ivCustomerPhoto.load(file) {
                transformations(CircleCropTransformation())
            }
            binding.ivCustomerPhoto.setPadding(0, 0, 0, 0)
        }
    }

    // Voice input for customer name
    private val voiceNameLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            matches?.firstOrNull()?.let { spoken ->
                binding.etName.setText(spoken.trim())
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddCustomerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        customerId = arguments?.getLong("customerId", -1L) ?: -1L

        if (customerId > 0) {
            loadExistingCustomer()
        }

        binding.fabPhoto.setOnClickListener { showPhotoOptions() }
        binding.ivCustomerPhoto.setOnClickListener { showPhotoOptions() }

        binding.btnSave.setOnClickListener { saveCustomer() }

        // Voice input for name
        binding.btnVoiceName.setOnClickListener {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.voice_speak_name))
                }
                voiceNameLauncher.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(context, getString(R.string.voice_not_supported), Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.saveResult.observe(viewLifecycleOwner) { id ->
            if (id > 0) {
                Toast.makeText(context, "Customer saved!", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }
    }

    private fun loadExistingCustomer() {
        viewModel.getCustomerById(customerId).observe(viewLifecycleOwner) { customer ->
            customer?.let {
                binding.etName.setText(it.name)
                binding.etPhone.setText(it.phone)
                currentPhotoPath = it.photoPath

                if (!it.photoPath.isNullOrBlank() && File(it.photoPath).exists()) {
                    binding.ivCustomerPhoto.load(File(it.photoPath)) {
                        transformations(CircleCropTransformation())
                    }
                    binding.ivCustomerPhoto.setPadding(0, 0, 0, 0)
                }
            }
        }
    }

    private fun showPhotoOptions() {
        val options = arrayOf(
            getString(R.string.photo_camera),
            getString(R.string.photo_gallery)
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.photo_option_title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> takePhoto()
                    1 -> pickFromGallery()
                }
            }
            .show()
    }

    private fun takePhoto() {
        val file = createImageFile()
        tempPhotoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )
        currentPhotoPath = file.absolutePath
        takePictureLauncher.launch(tempPhotoUri)
    }

    private fun pickFromGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("CUSTOMER_${timeStamp}_", ".jpg", storageDir)
    }

    private fun saveCustomer() {
        val name = binding.etName.text?.toString()?.trim() ?: ""
        val phone = binding.etPhone.text?.toString()?.trim() ?: ""

        if (name.isBlank()) {
            binding.etName.error = "Name is required"
            return
        }

        viewModel.saveCustomer(name, phone, currentPhotoPath, customerId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

