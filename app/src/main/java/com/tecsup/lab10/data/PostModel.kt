package com.tecsup.lab10.data

import com.google.gson.annotations.SerializedName

data class PostModel(
    @SerializedName("id")
    var id: Int,
    @SerializedName("userId")
    var userId: Int,
    @SerializedName("title")
    var title: String,
    @SerializedName("body")
    var body: String
)