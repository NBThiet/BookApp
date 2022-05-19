package com.example.doan

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.doan.databinding.ActivityPdfDetailBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.io.FileOutputStream

class PdfDetailActivity : AppCompatActivity() {
    //view binding
    private lateinit var binding:ActivityPdfDetailBinding
    private companion object{
        //TAG
        const val TAG = "BOOK_DETAILS_TAG"
    }
    //book id, get from intent
    private var bookId = ""
    //get from firebase
    private var bookTitle = ""
    private var bookUrl = ""
    private lateinit var progressDialog: ProgressDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //get book id from intent, will load book info using this bookId
        bookId = intent.getStringExtra("bookId")!!
        //init progress bar
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Vui lòng chờ")
        progressDialog.setCanceledOnTouchOutside(false)
        //increment book view count, whenever this page starts
        MyApplication.incrementBookViewCount(bookId)
        loadBookDetails()
        //handle back button click, go back
        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
        //handle click, open pdf view activity
        binding.readBookBtn.setOnClickListener {
            val intent = Intent(this, PdfViewActivity::class.java)
            intent.putExtra("bookId", bookId);
            startActivity(intent)
        }
        //handle click, download book/pdf
        binding.downloadBookBtn.setOnClickListener {
            //let's check WRITE_EXTERNAL_STORAGE permission first, if granted download book, if not granted request permission
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                    Log.d(TAG, "onCreate: STORAGE PERMISSION đã được cấp")
                    downloadBook()
                }else{
                    Log.d(TAG, "onCreate: STORAGE PERMISSION không được cấp, HÃY yêu cầu nó")
                    requestStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
        }
    }
    private val requestStoragePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){isGranted:Boolean ->
        //lets check if granted or not
        if (isGranted){
            Log.d(TAG, "onCreate: STORAGE PERMISSION được cấp")
            downloadBook()
        }else{
            Log.d(TAG, "onCreate: STORAGE PERMISSION bị từ chối")
            Toast.makeText(this, "PERMISSION bị từ chối", Toast.LENGTH_SHORT).show()
        }
    }
    private fun downloadBook(){
        Log.d(TAG, "downloadBook: Đang tải xuống sách")
        progressDialog.setMessage("Đang tải xuống sách")
        progressDialog.show()
        //lets download book from firebase storage using url
        val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(bookUrl)
        storageReference.getBytes(Constants.MAX_BYTES_PDF)
            .addOnSuccessListener {bytes ->
                Log.d(TAG, "downloadBook: Sách đã tải xuống")
                saveToDownloadsFolder(bytes)
            }
            .addOnFailureListener {e->
                progressDialog.dismiss()
                Log.d(TAG, "downloadBook: Không tải xuống được sách bởi vì ${e.message}")
                Toast.makeText(this, "Không tải xuống được sách bởi vì ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveToDownloadsFolder(bytes: ByteArray) {
        Log.d(TAG, "saveToDownloadsFolder: Lưu sách tải xuống")
        val nameWithExtention = "${System.currentTimeMillis()}.pdf"
        try {
            val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            downloadsFolder.mkdirs()
            val filePath = downloadsFolder.path +"/"+ nameWithExtention
            val out = FileOutputStream(filePath)
            out.write(bytes)
            out.close()
            Toast.makeText(this, "Lưu vào thư mục tải xuống", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "saveToDownloadsFolder: Lưu vào thư mục tải xuống")
            progressDialog.dismiss()
            incrementDownloadCount()
        }catch (e: Exception){
            progressDialog.dismiss()
            Log.d(TAG, "saveToDownloadsFolder: Không lưu được bởi vì ${e.message}")
            Toast.makeText(this, "Không lưu được bởi vì ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun incrementDownloadCount() {
        //increment downloads count to firebase db
        Log.d(TAG, "incrementDownloadCount: ")
        //step 1: get previous downloads count
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //get downloads count
                    var downloadsCount = "${snapshot.child("downloadsCount").value}"
                    Log.d(TAG, "onDataChange: Số lượt tải xuống hiện tại: $downloadsCount")
                    if (downloadsCount == "" || downloadsCount == "null"){
                        downloadsCount = "0"
                    }
                    //convert to long and increment 1
                    val newDownloadCount: Long = downloadsCount.toLong() + 1
                    Log.d(TAG, "onDataChange: Số lượt tải xuống mới: $newDownloadCount")
                    //setup data to update to db
                    val hashMap: HashMap<String, Any> = HashMap()
                    hashMap["downloadsCount"] = newDownloadCount
                    //step 2: update new incremented downloads count to db
                    val dbRef = FirebaseDatabase.getInstance().getReference("Books")
                    dbRef.child(bookId)
                        .updateChildren(hashMap)
                        .addOnSuccessListener {
                            Log.d(TAG, "onDataChange: Số lượt tải xuống đã tăng lên")
                        }
                        .addOnFailureListener { e->
                            Log.d(TAG, "onDataChange: Không thể tăng được bởi vì ${e.message}")
                        }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private fun loadBookDetails() {
        //books > bookId > details
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //get data
                    val categoryId = "${snapshot.child("categoryId").value}"
                    val description = "${snapshot.child("description").value}"
                    val downloadsCount = "${snapshot.child("downloadsCount").value}"
                    val timestamp = "${snapshot.child("timestamp").value}"
                    bookTitle = "${snapshot.child("title").value}"
                    val uid = "${snapshot.child("uid").value}"
                    bookUrl = "${snapshot.child("url").value}"
                    val viewsCount = "${snapshot.child("viewsCount").value}"
                    //format date
                    val date = MyApplication.formatTimeStamp(timestamp.toLong())
                    //load pdf category
                    MyApplication.loadCategory(categoryId, binding.categoryTv)
                    //load pdf thumbnail, pages count
                    MyApplication.loadPdfFromUrlSinglePage("$bookUrl", "$bookTitle", binding.pdfView, binding.progressBar, binding.pagesTv)
                    //load pdf size
                    MyApplication.loadPdfSize("$bookUrl", "$bookTitle", binding.sizeTv)
                    //set data
                    binding.titleTv.text = bookTitle
                    binding.descriptionTv.text = description
                    binding.viewsTv.text = viewsCount
                    binding.downloadsTv.text = downloadsCount
                    binding.dateTv.text = date
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }
}