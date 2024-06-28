package com.example.memo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.memo.adapter.MemoAdapter
import com.example.memo.adapter.RvInterface
import com.example.memo.databinding.ActivityMainBinding
import com.example.memo.databinding.AddItemBinding
import com.example.memo.databinding.UpdateItemBinding
import com.example.memo.model.MemoModel
import com.firebase.ui.auth.AuthUI
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class MainActivity : AppCompatActivity(), RvInterface {
    private lateinit var binding: ActivityMainBinding
    private lateinit var bindingAddItem: AddItemBinding
    private lateinit var bindingUpdateItem: UpdateItemBinding
    private lateinit var listMemo: ArrayList<MemoModel>
    private lateinit var adapter: MemoAdapter
    private val TAG = "memo"
    private var email: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        bindingAddItem = AddItemBinding.inflate(layoutInflater)
        bindingUpdateItem = UpdateItemBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 前画面からユーザ情報を受け取る
        val myIntent = intent;
        val user = myIntent.getStringExtra("userName")
        email = myIntent.getStringExtra("email")
        binding.txtName.text = user
        binding.txtEmail.text = email

        val db = Firebase.firestore
        //
        listMemo = arrayListOf<MemoModel>()

        //
        binding.rvToDoList.layoutManager = LinearLayoutManager(this)
        //lam the nao de hien thi toan bo du lieu da nhap trong cloud firebase
        db.collection("memos").whereEqualTo("userEmail", email).get().addOnSuccessListener { result ->
            for (document in result) {
                val memo = document.toObject(MemoModel::class.java)
                memo.id = document.id
                listMemo.add(memo)
            }
            adapter = MemoAdapter(listMemo, { deletedMemo ->
                // Xóa dữ liệu từ Firebase
                db.collection("memos")
                    .document(deletedMemo.id.toString()) // Giả sử MemoModel có một thuộc tính id
                    .delete().addOnSuccessListener {
                        Log.d(TAG, "DocumentSnapshot successfully deleted!")
                    }.addOnFailureListener { e ->
                        Log.w(TAG, "Error deleting document", e)
                    }

                // Cập nhật danh sách và thông báo sự thay đổi
                listMemo.remove(deletedMemo)
                adapter.notifyDataSetChanged()
            }, this)
            binding.rvToDoList.adapter = adapter
        }.addOnFailureListener { e ->
            Log.w(TAG, "Error getting documents: ", e)
        }

        //
        binding.btnAdd.setOnClickListener {
            openAddDialog()
        }

        binding.btnLogout.setOnClickListener {
            AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener {
                    // ...
                    val nextIntent = Intent(this, LoginActivity::class.java)
                    startActivity(nextIntent)
                }
        }

    }

    private fun openAddDialog() {
        val mDialog = AlertDialog.Builder(this)
        val mDialogView = bindingAddItem.root
        // Kiểm tra xem view đã có parent chưa
        if (mDialogView.parent != null) {
            (mDialogView.parent as ViewGroup).removeView(mDialogView)
        }
        mDialog.setView(mDialogView)
        mDialog.setTitle("Add memo")
        val ad = mDialog.create()
        ad.show()
        //
        var priority = 1
        bindingAddItem.btnPriority1.setOnClickListener {
            selectPriorityAdd(1)
            priority = 1
        }
        bindingAddItem.btnPriority2.setOnClickListener {
            selectPriorityAdd(2)
            priority = 2
        }
        bindingAddItem.btnPriority3.setOnClickListener {
            selectPriorityAdd(3)
            priority = 3
        }
        bindingAddItem.btnPriority4.setOnClickListener {
            selectPriorityAdd(4)
            priority = 4
        }
        bindingAddItem.btnPriority5.setOnClickListener {
            selectPriorityAdd(5)
            priority = 5
        }

        //
        bindingAddItem.btnCancel.setOnClickListener {
            ad.dismiss()
        }
        bindingAddItem.btnSubmit?.setOnClickListener {
            val db = Firebase.firestore
            val memo = bindingAddItem.edtAddToDoList.text.toString()
            val userEmail = email; // Lấy ID người dùng từ Firebase Authentication
//            val memo = "nhi"
            Log.d(TAG, "ten cua memo la   $memo")

            val meMo = hashMapOf(
                "memo" to memo,
                "priority" to priority,
                "userEmail" to userEmail  // Liên kết ghi chú với ID người dùng
            )
            db.collection("memos").add(meMo).addOnSuccessListener { document ->
                Log.d(TAG, "addes with id ${document.id}")
                bindingAddItem.edtAddToDoList.setText("")
                bindingAddItem.btnPriority1.setImageResource(R.drawable.ic_action_star_border_black)
                bindingAddItem.btnPriority2.setImageResource(R.drawable.ic_action_star_border_black)
                bindingAddItem.btnPriority3.setImageResource(R.drawable.ic_action_star_border_black)
                bindingAddItem.btnPriority4.setImageResource(R.drawable.ic_action_star_border_black)
                bindingAddItem.btnPriority5.setImageResource(R.drawable.ic_action_star_border_black)
                val newMemo = MemoModel(memo, priority, document.id)
                listMemo.add(newMemo)
                adapter.notifyDataSetChanged()
            }.addOnFailureListener { e ->
                Log.w(TAG, "Error: $e")
            }
            Toast.makeText(this, "Add successfully", Toast.LENGTH_SHORT).show()
            ad.dismiss()
        }
    }

    override fun onItemClicked(position: Int) {
        val clickedMemo = listMemo[position]

        // Hiển thị Dialog cập nhật
        val uDialog = AlertDialog.Builder(this)
        val uDialogView = bindingUpdateItem.root
        if (uDialogView.parent != null) {
            (uDialogView.parent as ViewGroup).removeView(uDialogView)
        }
        uDialog.setView(uDialogView)
        uDialog.setTitle("Update memo")

        // Hiển thị dữ liệu hiện tại trong EditText và chọn priority
        bindingUpdateItem.edtUpdateToDoList.setText(clickedMemo.memo)
        var uPriority = clickedMemo.priority
        selectPriority(uPriority)

        // Hiển thị Dialog
        val ad = uDialog.create()
        ad.show()

        // Gán sự kiện click cho các nút priority
        bindingUpdateItem.btnPriority1.setOnClickListener {
            selectPriority(1)
            uPriority = 1
        }
        bindingUpdateItem.btnPriority2.setOnClickListener {
            selectPriority(2)
            uPriority = 2
        }
        bindingUpdateItem.btnPriority3.setOnClickListener {
            selectPriority(3)
            uPriority = 3
        }
        bindingUpdateItem.btnPriority4.setOnClickListener {
            selectPriority(4)
            uPriority = 4
        }
        bindingUpdateItem.btnPriority5.setOnClickListener {
            selectPriority(5)
            uPriority = 5
        }

        // Gán sự kiện click cho nút Update
        bindingUpdateItem.btnUpdate.setOnClickListener {
            updateMemo(position, clickedMemo.id.toString(), uPriority)
            ad.dismiss()
        }
    }

    private fun selectPriority(selectedPriority: Int?) {
        // Hủy chọn tất cả các nút priority
        val priorityButtons = arrayOf(
            bindingUpdateItem.btnPriority1,
            bindingUpdateItem.btnPriority2,
            bindingUpdateItem.btnPriority3,
            bindingUpdateItem.btnPriority4,
            bindingUpdateItem.btnPriority5
        )
        priorityButtons.forEachIndexed { index, button ->
//            button.setImageResource(if (index + 1 == selectedPriority) R.drawable.ic_action_star_black else R.drawable.ic_action_star_border_black)
            if ((index + 1) <= selectedPriority!!) {
                for (i in (index + 1)..selectedPriority!!) {
                    button.setImageResource(R.drawable.ic_action_star_black)
                }
            } else {
                button.setImageResource(R.drawable.ic_action_star_border_black)
            }
        }
    }

    private fun selectPriorityAdd(selectedPriority: Int?) {
        // Hủy chọn tất cả các nút priority
        val priorityButtons = arrayOf(
            bindingAddItem.btnPriority1,
            bindingAddItem.btnPriority2,
            bindingAddItem.btnPriority3,
            bindingAddItem.btnPriority4,
            bindingAddItem.btnPriority5
        )
        priorityButtons.forEachIndexed { index, button ->
//            button.setImageResource(if (index + 1 == selectedPriority) R.drawable.ic_action_star_black else R.drawable.ic_action_star_border_black)
            if ((index + 1) <= selectedPriority!!) {
                for (i in (index + 1)..selectedPriority!!) {
                    button.setImageResource(R.drawable.ic_action_star_black)
                }
            } else {
                button.setImageResource(R.drawable.ic_action_star_border_black)
            }
        }
    }

    private fun updateMemo(position: Int, memoId: String, memoPriority: Int?) {
        val db = Firebase.firestore
        val updatedMemo = bindingUpdateItem.edtUpdateToDoList.text.toString()

        // Kiểm tra xem có sự thay đổi không
        if (updatedMemo != listMemo[position].memo || memoPriority != listMemo[position].priority) {
            val updateData = hashMapOf(
                "memo" to updatedMemo, "priority" to memoPriority
            )

            db.collection("memos").document(memoId).update(updateData as Map<String, Any>)
                .addOnSuccessListener {
                    Log.d(TAG, "DocumentSnapshot successfully updated!")
                    // Cập nhật dữ liệu trong danh sách
                    listMemo[position].memo = updatedMemo
                    listMemo[position].priority = memoPriority
                    adapter.notifyItemChanged(position)
                }.addOnFailureListener { e ->
                    Log.w(TAG, "Error updating document", e)
                }
        }
    }
}