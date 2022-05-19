package com.example.doan

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import com.example.doan.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {
    //view binding
    private lateinit var binding:ActivityRegisterBinding
    //firebase auth
    private lateinit var firebaseAuth: FirebaseAuth
    //progress dialog
    private lateinit var progressDialog: ProgressDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //init firebase auth
        firebaseAuth = FirebaseAuth.getInstance()
        //init progress dialog, will show while creating account | Register user
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Vui lòng chờ")
        progressDialog.setCanceledOnTouchOutside(false)
        //handle back button click
        binding.backBtn.setOnClickListener {
            onBackPressed() //goto previous screen
        }
        //handle click, begin register
        binding.registerBtn.setOnClickListener {
            /* Steps
            * 1) Input data
            * 2) Validate data
            * 3) Create account - firebase auth
            * 4) save user info - firebase realtime database */
            validateData()
        }
    }
    private var name = ""
    private var email = ""
    private var password = ""
    private fun validateData() {
        //1) Input data
        name = binding.nameEt.text.toString().trim()
        email = binding.emailEt.text.toString().trim()
        password = binding.passwordEt.text.toString().trim()
        var cPassword = binding.cPasswordEt.text.toString().trim()
        //2) Validate data
        if (name.isEmpty()) {
            //empty name
            Toast.makeText(this, "Nhập tên của bạn", Toast.LENGTH_SHORT).show()
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            //email không hợp lệ
            Toast.makeText(this, "Email không hợp lệ", Toast.LENGTH_SHORT).show()
        } else if (password.isEmpty()) {
            //empty password
            Toast.makeText(this, "Nhập mật khẩu", Toast.LENGTH_SHORT).show()
        } else if (cPassword.isEmpty()) {
            //empty confirm password
            Toast.makeText(this, "Xác nhận mật khẩu", Toast.LENGTH_SHORT).show()
        } else if (password != cPassword) {
            Toast.makeText(this, "Mật khẩu không khớp", Toast.LENGTH_SHORT).show()
        } else {
            createUserAccount()
        }
    }

    private fun createUserAccount() {
        //3) Create account - firebase auth
        //show progress
        progressDialog.setMessage("Tạo tài khoản")
        progressDialog.show()
        //create user in firebase auth
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                //tài khoản đã được tạo
                updateUserInfo()
            }
            .addOnFailureListener { e->
                //không tạo được tài khoản
                progressDialog.dismiss()
                Toast.makeText(this, "Không tạo được tài khoản bởi vì ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUserInfo() {
        //4) save user info - firebase realtime database
        progressDialog.setMessage("Lưu thông tin người dùng")
        //timestamp
        val timestamp = System.currentTimeMillis()
        //get current user uid, since user is registered so we can get it now
        val uid = firebaseAuth.uid
        //setup data to add in db
        val hashMap: HashMap<String, Any?> = HashMap()
        hashMap["uid"] = uid
        hashMap["email"] = email
        hashMap["name"] = name
        hashMap["profile"] = "" //add empty, will do in profile edit
        hashMap["userType"] = "user" //possible values are user/admin, will change value to admin manually on firebase db
        hashMap["timestamp"] = timestamp
        //set data to db
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(uid!!)
            .setValue(hashMap)
            .addOnSuccessListener {
                //user info saved, open user
                progressDialog.dismiss()
                Toast.makeText(this, "Tài khoản đã được tạo", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@RegisterActivity, DashboardUserActivity::class.java))
                finish()
            }
            .addOnFailureListener { e->
                //failing adding data to db
                progressDialog.dismiss()
                Toast.makeText(this, "Không lưu được thông tin người dùng bởi vì ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}