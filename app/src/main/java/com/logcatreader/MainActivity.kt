package com.logcatreader

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
//import android.graphics.fonts.Font
import android.net.Uri
import android.os.Bundle
import android.text.Editable
//import android.text.Html
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.SearchView
import android.widget.ViewAnimator
//import android.widget.TextView
//import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//    }
private var logTextView: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val paneleditor=findViewById<ScrollView>(R.id.paneleditor)
        val progressBar=findViewById<ProgressBar>(R.id.progressBar)
        paneleditor.visibility= View.GONE
        progressBar.visibility= View.GONE
        // Create this as a variable in your Fragment class
        val mLauncher = registerForActivityResult(
            StartActivityForResult()
        ) { result ->
            // Handle the result here
            if (result.resultCode == RESULT_OK) {
                // Extract data from the result intent (if needed)
                val data = result.data!!
                paneleditor.visibility= View.GONE
                progressBar.visibility= View.VISIBLE
                logTextView!!.post(Runnable {
                    try {
                        loadLogFile(data.data)
                    }
                    catch (e:Exception){
                        Log.e("Load File",e.message.toString())
                    }
                    runOnUiThread {
                        paneleditor.visibility= View.VISIBLE
                        progressBar.visibility= View.GONE
                    }
                })
//                val x=thread {
//
//                }
//                x.start()

                // Your code from onActivityResult goes here
            }
        }
        logTextView = findViewById(R.id.logTextView)
        val loadButton = findViewById<Button>(R.id.loadButton)
        loadButton.setOnClickListener {
            val chooseFile = Intent(Intent.ACTION_GET_CONTENT)
            chooseFile.addCategory(Intent.CATEGORY_OPENABLE)
            chooseFile.setType("text/plain") // Set the desired file type (e.g., text files)
            // Now filter the results to include only .log files
            chooseFile.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/plain", "application/octet-stream"))
            chooseFile.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false) // Allow selecting only one file
            val chooserIntent =Intent.createChooser(chooseFile, "Choose a file")
            mLauncher.launch(chooserIntent)
//            startActivityForResult(
//                Intent.createChooser(chooseFile, "Choose a file"),
//                picker_fileResultCode
//            )
            }
        loadSearchView()

    }
    private fun loadSearchView(){
        val mySearchView = findViewById<SearchView>(R.id.mySearchView)
        // Set up SearchView listeners or handle search queries
        mySearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Handle search submission
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                //
                startIndexFind=0
                // Change text color based on search query
                val fullText = logTextView?.text.toString()
                val highlightedText = newText?.let { query ->
                    val startIndex = fullText.indexOf(query, ignoreCase = true)
                    if (startIndex != -1) {
                        val endIndex = startIndex + query.length
                        val spannable = SpannableString(fullText)
                        spannable.setSpan(
                            ForegroundColorSpan(Color.RED),
                            startIndex,
                            endIndex,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        spannable
                    } else {
                        SpannableString(fullText)
                    }
                } ?: SpannableString(fullText)

                logTextView?.text = Editable.Factory.getInstance().newEditable(highlightedText)
                // Handle search text changes
                return false
            }
        })
        // Set up Next button click listener

        loadNextBtn(mySearchView)
    }
    fun loadNextBtn(mySearchView: SearchView) {
        val nextButton = findViewById<Button>(R.id.nextButton)
        nextButton.setOnClickListener {
            val currentQuery = mySearchView.query.toString()
            val fullText = logTextView?.text.toString()

            // Find the next occurrence of the search query
            startIndexFind = fullText.indexOf(currentQuery,startIndexFind, ignoreCase = true)
            if (startIndexFind != -1) {
                val endIndex = startIndexFind + currentQuery.length
                // Set the selection range using selectionStart and selectionEnd
                logTextView!!.setSelection(startIndexFind,endIndex)
            } else {
                // Handle the case when the search query is not found
                // (e.g., show a toast message or provide feedback to the user)
            }
        }
    }
    var startIndexFind=0
//    private val picker_fileResultCode = 123
//    @Deprecated("Deprecated in Java")
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        Log.i("select",requestCode.toString())
//        if (requestCode == picker_fileResultCode && resultCode == RESULT_OK) {
//            val selectedFileUri = data?.data
//            loadLogFile(selectedFileUri!!)
////            try {
////                val inputStream = contentResolver.openInputStream(selectedFileUri!!)
////                val reader = BufferedReader(InputStreamReader(inputStream))
////                val content = java.lang.StringBuilder()
////                var line: String?
////                while (reader.readLine().also { line = it } != null) {
////                    content.append(line).append("\n")
////                }
////                // Now 'content' contains the file content
////                // Display it in a TextView or process it further
////            } catch (e: IOException) {
////                e.printStackTrace()
////            }
//        }
//    }

    private fun loadLogFile(selectedFileUri: Uri?) {
        if (selectedFileUri != null) {
            // Proceed with opening the input stream
            val inputStream = contentResolver.openInputStream(selectedFileUri)
            val logContents = SpannableStringBuilder("")
            val reader = BufferedReader(InputStreamReader(inputStream))
            try {
                var line: String
                while (reader.readLine().also { line = it } != null) {
                    logContents.append(parseLogLine(line)).append("\n")
                }
                reader.close()
            } catch (e: NullPointerException) {
                Log.e("Null Line", e.message.toString())
                reader.close()
            } catch (e: IOException) {
                Log.e("Read file", e.message.toString())
                return
            }
            runOnUiThread{
                logTextView!!.text.clear()
                logTextView!!.text =Editable.Factory.getInstance().newEditable(logContents)
            }

        } else {
            // Handle the case when selectedFileUri is null
        }
    }

    private fun parseLogLine(logLine: String): SpannableString {
        // Extract priority level (E, W, I) and message
        val parts = logLine.split("\\s+".toRegex(), limit = 6).toTypedArray()

        if (logLine.isEmpty() ) {
            return SpannableString(logLine)
        }
        var key = logLine.substringBefore(":")
        var priority = ""
        var message = logLine.substringAfter(":")
        if (parts.size > 4) {
            key = parts[5].substringBefore(":")
            priority = parts[4]
            message = parts[5].substringAfter(":")
            priority= " $priority "
        }
        Log.i("",logLine)


        // Apply background colors based on priority
        val bgColor: Int = when (priority) {
            " E " -> Color.RED
            " D " -> Color.BLUE
            " W " -> Color.parseColor("#FFA500") // Orange
            " I " -> Color.BLACK
            " V " -> Color.GREEN // Green (for verbose)
            else -> Color.DKGRAY // Default color
        }

        // Create a SpannableString with the desired color
        val spannableMessage = SpannableString("$key$priority$message")
        spannableMessage.setSpan(BackgroundColorSpan(bgColor), key.length, key.length + priority.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableMessage.setSpan(ForegroundColorSpan(Color.WHITE), key.length, key.length + priority.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableMessage.setSpan(ForegroundColorSpan(Color.parseColor("#154734")/*dark green*/), key.length + priority.length, key.length + priority.length+message.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableMessage.setSpan(StyleSpan(Typeface.BOLD), key.length, key.length + priority.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        return spannableMessage
    }

//    fun fromHtml(html: String): String {
//        return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString()
//    }
//    private fun parseLogLine(logLine: String): String {
//        // Extract priority level (E, W, I) and message
//        val parts = logLine.split("\\s+".toRegex(), limit = 6).toTypedArray()
//        Log.i("log",logLine+"\n"+parts.count().toString())
//        if(logLine=="") return logLine
//        if(parts.count()<=4) return logLine
//        val key = parts[2]
//        val priority = parts[4]
//        val message = parts[5]
//
//        // Apply background colors based on priority
//        val bgColor: String = when (priority) {
//            "E" -> "#FF0000" // Red
//            "W" -> "#FFA500" // Orange
//            "I" -> "#000000" // Black
//            "V"->  "#00FF00"  //green
//            else -> "#00FF00" // Green (for other levels)
//        }
//        return "$key<font color=\"$bgColor\">$priority</font>" +
//                "<font color=\"#00FF00\">$message</font>"
//    }


}

