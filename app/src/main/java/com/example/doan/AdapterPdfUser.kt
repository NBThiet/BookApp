package com.example.doan

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.databinding.RowPdfUserBinding

class AdapterPdfUser : RecyclerView.Adapter<AdapterPdfUser.HolderPdfUser>, Filterable{
    //context, get using constructor
    private var context: Context
    //arraylist to hold pdfs, get using constructor
    public var pdfArrayList: ArrayList<ModelPdf>
    //arraylist to hold filtered pdfs
    public var filteredList: ArrayList<ModelPdf>
    //viewBinding row_pdf_user.xml => RowPdfUserBinding
    private lateinit var binding: RowPdfUserBinding
    private var filter: FilterPdfUser? = null
    constructor(context: Context, pdfArrayList: ArrayList<ModelPdf>) {
        this.context = context
        this.pdfArrayList = pdfArrayList
        this.filteredList = pdfArrayList
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderPdfUser {
        //inflate/bind layout row_pdf_user.xml
        binding = RowPdfUserBinding.inflate(LayoutInflater.from(context), parent, false)
        return HolderPdfUser(binding.root)
    }

    override fun onBindViewHolder(holder: HolderPdfUser, position: Int) {
        /*get data, set data, handle click etc*/
        //get data
        val model = pdfArrayList[position]
        val bookId = model.id
        val categoryId = model.categoryId
        val title = model.title
        val description = model.description
        val uid = model.uid
        val url = model.url
        val timestamp = model.timestamp
        //convert time
        val date = MyApplication.formatTimeStamp(timestamp)
        //set data
        holder.titleTv.text = title
        holder.descriptionTv.text = description
        holder.dateTv.text = date
        MyApplication.loadPdfFromUrlSinglePage(url, title, holder.pdfView, holder.progressBar, null)
        MyApplication.loadCategory(categoryId, holder.categoryTv)
        MyApplication.loadPdfSize(url, title, holder.sizeTv)
        //handle click, open pdf details page
        holder.itemView.setOnClickListener {
            //pass book id in intent, that will be used to get pdf info
            val intent = Intent(context, PdfDetailActivity::class.java)
            intent.putExtra("bookId", bookId)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return pdfArrayList.size //return list size/number of records
    }
    override fun getFilter(): Filter {
        if (filter == null){
            filter = FilterPdfUser(filteredList, this)
        }
        return filter as FilterPdfUser
    }

    /*ViewHolder class row_pdf_user.xml*/
    inner class HolderPdfUser(itemView: View): RecyclerView.ViewHolder(itemView){
        //init UI components of row_pdf_user.xml
        var pdfView = binding.pdfView
        var progressBar = binding.progressBar
        var titleTv = binding.titleTv
        var descriptionTv = binding.descriptionTv
        var categoryTv = binding.categoryTv
        var sizeTv = binding.sizeTv
        var dateTv = binding.dateTv
    }



}