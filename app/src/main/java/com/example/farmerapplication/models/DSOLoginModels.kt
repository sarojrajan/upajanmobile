package com.example.farmerapplication.models

data class DSOLoginRequest(
    val login_id: String,
    val password: String,
    val usertype_id: Int = 4   // always 4 for DSO
)

data class DSOLoginResponse(
    val statusCode: Int?,
    val message: String?,
    val user_id: Int?,
    val usertype_id: Int?,
    val mobile_no: String?,
    val email_id: String?,
    val officer_name: String?,
    val token: String?
)