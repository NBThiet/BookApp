package com.example.doan

import android.app.ProgressDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.doan.databinding.ActivityPdfEditBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class PdfEditActivity : AppCompatActivity() {
    //view binding
    private lateinit var binding: ActivityPdfEditBinding
    private companion object{
        private const val TAG = "PDF_EDIT_TAG"
    }
    //book id get from intent started from AdapterPdfAdmin
    private var bookId = ""
    //progress dialog
    private lateinit var progressDialog: ProgressDialog
    //arraylist to hold category titles
    private lateinit var categoryTitleArrayList:ArrayList<String>
    //arraylist to hold category ids
    private lateinit var categoryIdArrayList:ArrayList<String>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //get book id to edit the book info
        bookId = intent.getStringExtra("bookId")!!
        //setup progress dialog
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Vui lòng chờ")
        progressDialog.setCanceledOnTouchOutside(false)
        loadCategories()
        loadBookInfo()
        //handle click, go back
        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
        //handle click, pick category
        binding.categoryTv.setOnClickListener {
            categoryDialog()
        }
        //handle click, begin update
        binding.submitBtn.setOnClickListener {
            validateData()
        }
    }

    private fun loadBookInfo() {
        Log.d(TAG, "loadBookInfo: Đang tải thông tin sách")
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //get book info
                    selectedCategoryId = snapshot.child("categoryId").value.toString()
                    val description = snapshot.child("description").value.toString()
                    val title = snapshot.child("title").value.toString()
                    //set to views
                    binding.titleEt.setText(title)
                    binding.descriptionEt.setText(description)
                    //load book category info using categoryId
                    Log.d(TAG, "onDataChange: Đang tải thông tin thể loại sách")
                    val refBookCategory = FirebaseDatabase.getInstance().getReference("Categories")
                    refBookCategory.child(selectedCategoryId)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                //get category
                                val category = snapshot.child("category").value
                                //set to textview
                                binding.categoryTv.text = category.toString()
                            }

                            override fun onCancelled(error: DatabaseError) {

                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private var title = ""
    private var description = ""
    private fun validateData() {
        //get data
        title = binding.titleEt.text.toString().trim()
        description = binding.descriptionEt.text.toString().trim()
        //validate data
        if (title.isEmpty()){
            Toast.makeText(this, "Nhập tiêu đề", Toast.LENGTH_SHORT).show()
        }else if (description.isEmpty()){
            Toast.makeText(this, "Nhập mô tả", Toast.LENGTH_SHORT).show()
        }else if (selectedCategoryId.isEmpty()){
            Toast.makeText(this, "Chọn thể loại", Toast.LENGTH_SHORT).show()
        }else{
            updatePdf()
        }
    }

    private fun updatePdf() {
        Log.d(TAG, "updatePdf: Bắt đầu cập nhật thông tin PDF")
        //show progress
        progressDialog.setMessage("Cập nhật thông tin sách")
        progressDialog.show()
        //setup data to update to db, spellings of keys must be same as in firebase
        val hashMap = HashMap<String, Any>()
        hashMap["title"] = "$title"
        hashMap["description"] = "$description"
        hashMap["categoryId"] = "$selectedCategoryId"
        //start updating
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .updateChildren(hashMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Log.d(TAG, "updatePdf: Cập nhật thành công")
                Toast.makeText(this, "Cập nhật thành công", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e->
                Log.d(TAG, "updatePdf: Cập nhật thất bại bởi vì ${e.message}")
                progressDialog.dismiss()
                Toast.makeText(this, "Cập nhật thất bại bởi vì ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private var selectedCategoryId = ""
    private var selectedCategoryTitle = ""
    private fun categoryDialog() {
        /*show dialog to pick the category of pdf/book*/
        //make string array from arraylist of string
        val categoriesArray = arrayOfNulls<String>(categoryTitleArrayList.size)
        for (i in categoryTitleArrayList.indices){
            categoriesArray[i] = categoryTitleArrayList[i]
        }
        //alert dialog
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Chọn thể loại")
            .setItems(categoriesArray){dialog, position ->
                //handle click, save clicked category id and title
                selectedCategoryId = categoryIdArrayList[position]
                selectedCategoryTitle = categoryTitleArrayList[position]
                //set to textview
                binding.categoryTv.text = selectedCategoryTitle
            }
            .show() // show dialog
    }

    private fun loadCategories() {
        Log.d(TAG, "loadCategories: Tải các thể loại")
        categoryTitleArrayList = ArrayList()
        categoryIdArrayList = ArrayList()
        val ref = FirebaseDatabase.getInstance().getReference("Categories")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                //clear list before starting adding data into them
                categoryIdArrayList.clear()
                categoryTitleArrayList.clear()
                for (ds in snapshot.children){
                    val id = "${ds.child("id").value}"
                    val category = "${ds.child("category").value}"
                    categoryIdArrayList.add(id)
                    categoryTitleArrayList.add(category)
                    Log.d(TAG, "onDataChange: category id $id")
                    Log.d(TAG, "onDataChange: category $category")
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }
}