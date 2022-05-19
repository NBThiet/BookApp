package com.example.doan

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.databinding.RowCategoryBinding
import com.google.firebase.database.FirebaseDatabase

class AdapterCategory :RecyclerView.Adapter<AdapterCategory.HolderCategory>, Filterable{
    private val context: Context
    public var categoryArrayList: ArrayList<ModelCategory>
    private var filterList: ArrayList<ModelCategory>
    private var filter: FilterCategory? = null
    private lateinit var binding: RowCategoryBinding

    //constructor
    constructor(context: Context, categoryArrayList: ArrayList<ModelCategory>) {
        this.context = context
        this.categoryArrayList = categoryArrayList
        this.filterList = categoryArrayList
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderCategory {
        //inflate/binding row_category.xml
        binding = RowCategoryBinding.inflate(LayoutInflater.from(context), parent, false)
        return HolderCategory(binding.root)
    }

    override fun onBindViewHolder(holder: HolderCategory, position: Int) {
        /*-----Get data, set data, handle clicks etc-----*/
        //get data
        val model = categoryArrayList[position]
        val id = model.id
        val category = model.category
        val uid = model.uid
        val timestamp = model.timestamp
        //set data
        holder.categoryTv.text = category
        //handle click, delete category
        holder.deleteBtn.setOnClickListener {
            //confirm before delete
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Xóa")
                .setMessage("Bạn có chắc chắn muốn xóa thể loại này không?")
                .setPositiveButton("Xác nhận"){a, d->
                    Toast.makeText(context, "Đang xóa", Toast.LENGTH_SHORT).show()
                    deleteCategory(model, holder)
                }
                .setNegativeButton("Hủy bỏ"){a, d->
                    a.dismiss()
                }
                .show()
        }
        //handle click, start pdf list admin activity, also pas pdf id, title
        holder.itemView.setOnClickListener {
            val intent = Intent(context, PdfListAdminActivity::class.java)
            intent.putExtra("categoryId", id)
            intent.putExtra("category", category)
            context.startActivity(intent)
        }
    }
    private fun deleteCategory(model: ModelCategory, holder: HolderCategory) {
        //get id of category to delete
        val id = model.id
        //firebase db > categories > categoryId
        val ref = FirebaseDatabase.getInstance().getReference("Categories")
        ref.child(id)
            .removeValue()
            .addOnSuccessListener {
                Toast.makeText(context, "Đã xóa", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e->
                Toast.makeText(context, "Không thể xóa bởi vì ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    override fun getItemCount(): Int {
        return categoryArrayList.size //number of items in list
    }
    //ViewHolder class to hold/init UI views for row_category.xml
    inner class HolderCategory(itemView: View): RecyclerView.ViewHolder(itemView){
        //init ui views
        var categoryTv:TextView = binding.categoryTv
        var deleteBtn:ImageButton = binding.deleteBtn
    }

    override fun getFilter(): Filter {
        if(filter == null){
            filter = FilterCategory(filterList, this)
        }
        return filter as FilterCategory
    }
}