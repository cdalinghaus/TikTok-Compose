package com.puskal.loginwithemailphone

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.puskal.core.base.BaseViewModel
import com.puskal.loginwithemailphone.tabs.AuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Created by Puskal Khadka on 3/28/2023.
 */

@HiltViewModel
class LoginWithEmailPhoneViewModel @Inject constructor(
) : BaseViewModel<ViewState, LoginEmailPhoneEvent>() {
    private val _settledPage = MutableStateFlow<Int?>(null)
    val settledPage = _settledPage.asStateFlow()

    private val _phoneNumber =
        MutableStateFlow<Pair<String, String?>>(Pair("", null))  //Pair(value,errorMsg)
    val phoneNumber = _phoneNumber.asStateFlow()

    private val _dialCode = MutableStateFlow<Pair<String, String?>>(Pair("Np +977", null))
    val dialCode = _dialCode.asStateFlow()

    private val _email = MutableStateFlow<Pair<String, String?>>(Pair("", null))
    val email = _email.asStateFlow()

    private val _username = MutableStateFlow<Pair<String, String?>>(Pair("", null))
    val username = _username.asStateFlow()

    private val _password = MutableStateFlow<Pair<String, String?>>(Pair("", null))
    val password = _password.asStateFlow()


    override fun onTriggerEvent(event: LoginEmailPhoneEvent) {
        when (event) {
            is LoginEmailPhoneEvent.EventPageChange -> _settledPage.value = event.settledPage
            is LoginEmailPhoneEvent.OnChangeEmailEntry -> _email.value =
                _email.value.copy(first = event.newValue)
            is LoginEmailPhoneEvent.OnChangePhoneNumber -> _phoneNumber.value =
                _phoneNumber.value.copy(first = event.newValue)

            is LoginEmailPhoneEvent.OnChangeUsernameEntry -> _username.value =
                _username.value.copy(first = event.newValue)

            is LoginEmailPhoneEvent.OnChangePasswordEntry -> _password.value =
                _password.value.copy(first = event.newValue)
        }


    }

    val _isUserExists = mutableStateOf(true)
    val isUserExists: State<Boolean> = _isUserExists

    fun checkUserExists(username: String, authService: AuthService) {
        viewModelScope.launch {
            try {
                val response = authService.checkUserExists(username)
                _isUserExists.value = false
                //Log.d("AUTH", "RESPONSE CODE" + response.code().toString())

                //_isUserExists.value = response.isSuccessful && response.body() == true
                //Log.d("AUTH", "USER CHECK" + _isUserExists.toString())
            } catch (e: Exception) {
                // Handle exception
                _isUserExists.value = true
            }
        }
    }


}