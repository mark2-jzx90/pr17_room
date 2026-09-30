package com.bignerdranch.android.pract_17_Zuev

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import android.text.format.DateFormat

private const val REQUEST_CONTACT = 1
private const val DATE_FORMAT_REPORT = "EEE, MMM dd"

class FragmentCrime : Fragment() {
    private lateinit var crime: Crime
    private lateinit var titleField: EditText
    private lateinit var dateButton: Button
    private lateinit var solvedCheckBox: CheckBox
    private lateinit var suspectButton: Button
    private lateinit var reportButton: Button
    private lateinit var callButton: Button
    private lateinit var photoImageView: ImageView
    private lateinit var fabCamera: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crime = Crime()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_crime, container, false)
        titleField = view.findViewById(R.id.crime_title)
        dateButton = view.findViewById(R.id.crime_date)
        solvedCheckBox = view.findViewById(R.id.crime_solved)
        suspectButton = view.findViewById(R.id.crime_suspect)
        reportButton = view.findViewById(R.id.crime_report)
        callButton = view.findViewById(R.id.crime_call)
        photoImageView = view.findViewById(R.id.crime_photo)
        fabCamera = view.findViewById(R.id.fab_camera) as FloatingActionButton

        val dateFormat = DateFormat.getLongDateFormat(context)
        dateButton.text = dateFormat.format(crime.date)
        dateButton.isEnabled = false

        fabCamera.setOnClickListener {
            Toast.makeText(context, "Выбрать фото", Toast.LENGTH_SHORT).show()
        }

        suspectButton.setOnClickListener {
            val pickContactIntent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
            startActivityForResult(pickContactIntent, REQUEST_CONTACT)
        }

        reportButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getCrimeReport()) // Генерируем красивый отчет
                putExtra(Intent.EXTRA_SUBJECT, "Отчет о преступлении")
            }
            startActivity(Intent.createChooser(intent, "Отправить отчет с помощью..."))
        }

        callButton.setOnClickListener {
            if (crime.suspect.isBlank()) {
                Toast.makeText(context, "Сначала выберите подозреваемого", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val phoneNumber = getContactPhoneNumber(crime.suspect)
            if (phoneNumber != null) {
                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                }
                startActivity(dialIntent)
            } else {
                Toast.makeText(context, "У контакта нет номера телефона", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    override fun onStart() {
        super.onStart()

        val pickContactIntent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
        val packageManager: PackageManager = requireActivity().packageManager
        val resolvedActivity: ResolveInfo? = packageManager.resolveActivity(
            pickContactIntent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        if (resolvedActivity == null) {
            suspectButton.isEnabled = false
        }

        titleField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                crime.title = s.toString()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        solvedCheckBox.setOnCheckedChangeListener { _, isChecked ->
            crime.isSolved = isChecked
            Snackbar.make(
                requireView(),
                "Преступление ${if (isChecked) "раскрыто" else "нераскрыто"}",
                Snackbar.LENGTH_SHORT
            ).setAction("Отменить") {
                crime.isSolved = !isChecked
                solvedCheckBox.isChecked = !isChecked
            }.show()
        }

        updateUI()
    }

    private fun updateUI() {
        if (crime.suspect.isNotEmpty()) {
            suspectButton.text = crime.suspect
            callButton.isEnabled = true
        } else {
            suspectButton.text = "Выбрать подозреваемого"
            callButton.isEnabled = false
        }
    }

    private fun getCrimeReport(): String {
        val solvedString = if (crime.isSolved) "раскрыто" else "не раскрыто"
        val dateString = DateFormat.format(DATE_FORMAT_REPORT, crime.date).toString()
        val suspectString = if (crime.suspect.isBlank()) "подозреваемого нет" else "подозреваемый: ${crime.suspect}"

        return "Преступление: ${crime.title}! Было обнаружено $dateString. Дело $solvedString, и $suspectString."
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            resultCode != Activity.RESULT_OK -> return

            requestCode == REQUEST_CONTACT && data != null -> {
                val contactUri: Uri? = data.data
                val queryFields = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)

                val cursor = requireActivity().contentResolver
                    .query(contactUri!!, queryFields, null, null, null)

                cursor?.use {
                    if (it.count == 0) return
                    it.moveToFirst()
                    val suspect = it.getString(0)
                    crime.suspect = suspect
                    updateUI()
                }
            }
        }
    }

    private fun getContactPhoneNumber(contactName: String): String? {
        var phoneNumber: String? = null
        val contentResolver = requireActivity().contentResolver

        val cursor: Cursor? = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts._ID),
            "${ContactsContract.Contacts.DISPLAY_NAME} = ?",
            arrayOf(contactName),
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val contactId = it.getString(0)
                val phoneCursor: Cursor? = contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(contactId),
                    null
                )

                phoneCursor?.use { pCursor ->
                    if (pCursor.moveToFirst()) {
                        phoneNumber = pCursor.getString(0)
                    }
                }
            }
        }
        return phoneNumber
    }
}