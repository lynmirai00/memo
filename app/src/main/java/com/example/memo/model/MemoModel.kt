package com.example.memo.model

class MemoModel(
    var memo: String? = null,
    var priority: Int? = null,
    var id: String? = null,
    val userEmail: String? = null  // Thêm trường này
)