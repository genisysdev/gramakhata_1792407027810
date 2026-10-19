package com.gramakhata.app.ui.settings

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.FileProvider
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import com.gramakhata.app.R
import com.gramakhata.app.databinding.FragmentShopProfileBinding
import com.gramakhata.app.GramaKhataApp
import com.gramakhata.app.data.db.entity.Customer
import com.gramakhata.app.data.db.entity.Transaction
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Shop Profile & Settings screen.
 * Allows setting shop name/owner, toggling app lock,
 * switching language (English/Kannada), and exporting data.
 */
class ShopProfileFragment : Fragment() {

    private var _binding: FragmentShopProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShopProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadProfile()
        setupAppLock()
        setupLanguageSwitch()

        binding.btnSaveProfile.setOnClickListener { saveProfile() }
        binding.btnExportCSV.setOnClickListener { exportCSV() }
        binding.btnExportJSON.setOnClickListener { exportJSON() }
    }

    private fun loadProfile() {
        val prefs = requireContext().getSharedPreferences("shop_prefs", 0)
        binding.etShopName.setText(prefs.getString("shop_name", ""))
        binding.etOwnerName.setText(prefs.getString("owner_name", ""))
    }

    private fun saveProfile() {
        val shopName = binding.etShopName.text?.toString()?.trim() ?: ""
        val ownerName = binding.etOwnerName.text?.toString()?.trim() ?: ""

        requireContext().getSharedPreferences("shop_prefs", 0)
            .edit()
            .putString("shop_name", shopName)
            .putString("owner_name", ownerName)
            .apply()

        Toast.makeText(context, getString(R.string.shop_saved), Toast.LENGTH_SHORT).show()
    }

    private fun setupAppLock() {
        val lockPrefs = requireContext().getSharedPreferences("app_lock_prefs", 0)
        val lockEnabled = lockPrefs.getBoolean("lock_enabled", false)
        val hasPin = lockPrefs.getString("pin", null) != null

        binding.switchAppLock.isChecked = lockEnabled
        binding.btnResetPin.visibility = if (lockEnabled && hasPin) View.VISIBLE else View.GONE

        binding.switchAppLock.setOnCheckedChangeListener { _, isChecked ->
            lockPrefs.edit().putBoolean("lock_enabled", isChecked).apply()
            binding.btnResetPin.visibility = if (isChecked && lockPrefs.getString("pin", null) != null) View.VISIBLE else View.GONE

            val message = if (isChecked) {
                "App Lock enabled. You'll be asked to set a PIN on next launch."
            } else {
                "App Lock disabled."
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }

        binding.btnResetPin.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Reset PIN")
                .setMessage("This will clear your current PIN. You'll set a new PIN on next app launch.")
                .setPositiveButton(getString(R.string.yes)) { _, _ ->
                    lockPrefs.edit().remove("pin").apply()
                    Toast.makeText(context, "PIN reset. Set new PIN on next launch.", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }
    }

    // --- Issue #3: Language Switch ---

    private fun setupLanguageSwitch() {
        val languages = arrayOf(
            "English",
            "ಕನ್ನಡ (Kannada)",
            "हिन्दी (Hindi)",
            "தமிழ் (Tamil)",
            "മലയാളം (Malayalam)",
            "తెలుగు (Telugu)",
            "मराठी (Marathi)"
        )
        val langCodes = arrayOf("en", "kn", "hi", "ta", "ml", "te", "mr")

        // Detect current language
        val currentLocale = AppCompatDelegate.getApplicationLocales()
        val currentLang = currentLocale.get(0)?.language ?: "en"
        val selectedIndex = langCodes.indexOf(currentLang).coerceAtLeast(0)
        binding.btnLanguage.text = languages[selectedIndex]

        binding.btnLanguage.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.language_title))
                .setItems(languages) { _, which ->
                    val localeList = LocaleListCompat.forLanguageTags(langCodes[which])
                    AppCompatDelegate.setApplicationLocales(localeList)
                    // Activity will recreate automatically
                }
                .show()
        }
    }

    // --- Issue #4: Export to Downloads folder + share ---

    private fun exportCSV() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = requireActivity().application as GramaKhataApp
                val customers = app.repository.getAllCustomers()
                val transactions = app.repository.getAllTransactions()

                val sb = StringBuilder()
                sb.appendLine("--- CUSTOMERS ---")
                sb.appendLine("ID,Name,Phone,CreatedAt")
                customers.forEach { c ->
                    val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(Date(c.createdAt))
                    sb.appendLine("${c.id},\"${c.name}\",\"${c.phone}\",$date")
                }
                sb.appendLine()
                sb.appendLine("--- TRANSACTIONS ---")
                sb.appendLine("ID,CustomerID,Amount,Type,Note,Timestamp")
                transactions.forEach { txn ->
                    val type = if (txn.isCredit) "CREDIT" else "PAYMENT"
                    val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(Date(txn.timestamp))
                    sb.appendLine("${txn.id},${txn.customerId},${txn.amount},$type,\"${txn.note}\",$date")
                }

                val fileName = "gramakhata_export_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"
                val fileUri = saveToDownloads(fileName, sb.toString(), "text/csv")

                withContext(Dispatchers.Main) {
                    if (fileUri != null) {
                        Toast.makeText(context, getString(R.string.export_success), Toast.LENGTH_SHORT).show()
                        shareExportedFile(fileUri, "text/csv", fileName)
                    } else {
                        Toast.makeText(context, getString(R.string.export_error), Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, getString(R.string.export_error), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun exportJSON() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = requireActivity().application as GramaKhataApp
                val customers = app.repository.getAllCustomers()
                val transactions = app.repository.getAllTransactions()

                val root = JSONObject()

                val customersArray = JSONArray()
                customers.forEach { c ->
                    val obj = JSONObject().apply {
                        put("id", c.id)
                        put("name", c.name)
                        put("phone", c.phone)
                        put("createdAt", c.createdAt)
                    }
                    customersArray.put(obj)
                }
                root.put("customers", customersArray)

                val transactionsArray = JSONArray()
                transactions.forEach { txn ->
                    val obj = JSONObject().apply {
                        put("id", txn.id)
                        put("customerId", txn.customerId)
                        put("amount", txn.amount)
                        put("type", if (txn.isCredit) "CREDIT" else "PAYMENT")
                        put("note", txn.note)
                        put("timestamp", txn.timestamp)
                    }
                    transactionsArray.put(obj)
                }
                root.put("transactions", transactionsArray)

                val fileName = "gramakhata_export_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json"
                val fileUri = saveToDownloads(fileName, root.toString(2), "application/json")

                withContext(Dispatchers.Main) {
                    if (fileUri != null) {
                        Toast.makeText(context, getString(R.string.export_success), Toast.LENGTH_SHORT).show()
                        shareExportedFile(fileUri, "application/json", fileName)
                    } else {
                        Toast.makeText(context, getString(R.string.export_error), Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, getString(R.string.export_error), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Save file to the public Downloads folder using MediaStore (works on all API levels).
     */
    private fun saveToDownloads(fileName: String, content: String, mimeType: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ — use MediaStore
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/GramaKhata")
            }
            val uri = requireContext().contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                requireContext().contentResolver.openOutputStream(it)?.use { os ->
                    os.write(content.toByteArray())
                }
            }
            uri
        } else {
            // Android 9 and below — write directly
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "GramaKhata")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, fileName)
            file.writeText(content)
            FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)
        }
    }

    /**
     * Open a share sheet so user can easily find/send the exported file.
     */
    private fun shareExportedFile(uri: Uri, mimeType: String, fileName: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Grama Khata Export — $fileName")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.export_share)))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}



