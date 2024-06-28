package com.example.memo.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.memo.databinding.ItemListBinding
import com.example.memo.model.MemoModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

interface RvInterface{
    fun onItemClicked(position: Int)
}
class MemoAdapter(private val list: ArrayList<MemoModel>, private val onDeleteClickListener: (MemoModel) -> Unit, private val onClickItem: RvInterface): RecyclerView.Adapter<MemoAdapter.MemoViewHolder>() {
    class MemoViewHolder(private val binding: ItemListBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(memo: MemoModel, onDeleteClickListener: (MemoModel) -> Unit) {
            binding.txtToDoList.text = memo.memo
            binding.txtPriority.text = "優先度: ${memo.priority}★"
            binding.btnDelete.setOnClickListener {
                onDeleteClickListener(memo)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemoViewHolder {
        val binding = ItemListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MemoViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: MemoViewHolder, position: Int) {
        val memo = list[position]
        holder.bind(memo, onDeleteClickListener)
        holder.itemView.setOnClickListener {
            onClickItem.onItemClicked(position)
        }
    }
}