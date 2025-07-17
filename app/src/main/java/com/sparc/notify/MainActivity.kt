package com.sparc.notify

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.messaging.FirebaseMessaging
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONObject
import org.json.JSONArray

class MainActivity : AppCompatActivity() {
    private lateinit var adapter: PatientCodeAdapter
    private lateinit var prefs: android.content.SharedPreferences
    private val PREFS_KEY = "patient_codes"
    private val ALL_TYPES = listOf("FOOD", "DOCTOR_CALL", "RESTROOM", "EMERGENCY")
    private val TYPE_DISPLAY_NAMES = mapOf(
        "FOOD" to "Food",
        "DOCTOR_CALL" to "Doctor Call",
        "RESTROOM" to "Restroom",
        "EMERGENCY" to "Emergency"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

        prefs = getSharedPreferences("codes", Context.MODE_PRIVATE)
        val codeMap = loadCodeMap().mapValues { it.value.toMutableSet() }.toMutableMap()
        adapter = PatientCodeAdapter(codeMap, ::removeCode, ::showTypeDialog)

        val recyclerView = findViewById<RecyclerView>(R.id.patientCodeList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val fab = findViewById<FloatingActionButton>(R.id.addCodeFab)
        fab.setOnClickListener { showAddCodeDialog() }
    }

    private fun showAddCodeDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add_userid, null)
        val input = view.findViewById<EditText>(R.id.inputUserId)
        input.hint = getString(R.string.enter_patient_code)
        AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                val code = input.text.toString().trim()
                if (isValidUserId(code)) {
                    addCode(code)
                } else {
                    showInvalidUserIdDialog()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun isValidUserId(code: String): Boolean {
        // Only A-Z, a-z, 0-9, exactly 5 characters
        return code.matches(Regex("^[A-Za-z0-9]{5}$"))
    }

    private fun showInvalidUserIdDialog() {
        AlertDialog.Builder(this)
            .setTitle("Invalid UserID")
            .setMessage("• UserID must be exactly 5 characters long\n• It must contain only letters and digits (A-Z, a-z, 0-9).\n• No spaces or special characters allowed.")
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun addCode(code: String) {
        val codeMap = loadCodeMap().toMutableMap()
        if (!codeMap.containsKey(code)) {
            codeMap[code] = ALL_TYPES.toMutableSet()
            saveCodeMap(codeMap)
            adapter.addCode(code, ALL_TYPES.toMutableSet())
            FirebaseMessaging.getInstance().subscribeToTopic(code)
        }
    }

    private fun removeCode(code: String) {
        val codeMap = loadCodeMap().toMutableMap()
        if (codeMap.remove(code) != null) {
            saveCodeMap(codeMap)
            adapter.removeCode(code)
            FirebaseMessaging.getInstance().unsubscribeFromTopic(code)
        }
    }

    private fun loadCodeMap(): Map<String, Set<String>> {
        val value = prefs.all[PREFS_KEY]
        val json = when (value) {
            is String -> value
            else -> null // Not a string, treat as empty
        } ?: return emptyMap()
        val obj = JSONObject(json)
        val map = mutableMapOf<String, Set<String>>()
        for (key in obj.keys()) {
            val arr = obj.getJSONArray(key)
            val set = mutableSetOf<String>()
            for (i in 0 until arr.length()) {
                set.add(arr.getString(i))
            }
            map[key] = set
        }
        return map
    }

    private fun saveCodeMap(map: Map<String, Set<String>>) {
        val obj = JSONObject()
        for ((key, set) in map) {
            obj.put(key, JSONArray(set.toList()))
        }
        prefs.edit().putString(PREFS_KEY, obj.toString()).apply()
    }

    private fun showTypeDialog(code: String) {
        val codeMap = loadCodeMap().toMutableMap()
        val currentTypes = codeMap[code]?.toMutableSet() ?: ALL_TYPES.toMutableSet()
        val typeArray = ALL_TYPES.toTypedArray()
        val displayArray = ALL_TYPES.map { TYPE_DISPLAY_NAMES[it] ?: it }.toTypedArray()
        val checked = typeArray.map { currentTypes.contains(it) }.toBooleanArray()
        AlertDialog.Builder(this)
            .setTitle("Select notification types")
            .setMultiChoiceItems(displayArray, checked) { _, which, isChecked ->
                if (isChecked) currentTypes.add(typeArray[which]) else currentTypes.remove(typeArray[which])
            }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                codeMap[code] = currentTypes
                saveCodeMap(codeMap)
                adapter.updateTypes(code, currentTypes)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}

class PatientCodeAdapter(
    private val codes: MutableMap<String, MutableSet<String>>,
    private val onRemove: (String) -> Unit,
    private val onShowTypes: (String) -> Unit
) : RecyclerView.Adapter<PatientCodeAdapter.CodeViewHolder>() {
    class CodeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val codeText: TextView = view.findViewById(R.id.codeText)
        val removeText: TextView = view.findViewById(R.id.removeText)
        val typeSelectorButton: android.widget.ImageButton = view.findViewById(R.id.typeSelectorButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CodeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.patient_code_item, parent, false)
        return CodeViewHolder(view)
    }

    override fun onBindViewHolder(holder: CodeViewHolder, position: Int) {
        val (code, types) = codes.entries.elementAt(position)
        holder.codeText.text = code
        holder.removeText.setOnClickListener { onRemove(code) }
        holder.typeSelectorButton.setOnClickListener { onShowTypes(code) }
    }

    override fun getItemCount(): Int = codes.size

    fun addCode(code: String, types: MutableSet<String>) {
        codes[code] = types
        notifyItemInserted(codes.size - 1)
    }

    fun removeCode(code: String) {
        val removed = codes.remove(code)
        if (removed != null) {
            notifyItemRemoved(codes.size) // Adjust index for new size
        }
    }

    fun updateTypes(code: String, types: MutableSet<String>) {
        codes[code] = types
        notifyItemChanged(codes.entries.indexOfFirst { it.key == code })
    }
}