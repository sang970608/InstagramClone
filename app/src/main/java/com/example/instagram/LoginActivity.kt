package com.example.instagram

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import java.util.Arrays

class LoginActivity: AppCompatActivity() {
    var auth : FirebaseAuth? = null
    var googleSignInClient : GoogleSignInClient? = null
    var GOOGLE_LOGIN_CODE = 9001
    var callbackManager : CallbackManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        var loginbutton = findViewById<Button>(R.id.email_login_button)
        var googlebutton = findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.google_sign_in_button)
        var facebookbutton = findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.facebook_sign_in_button)

        loginbutton.setOnClickListener {
            signinAndSignup()
        }

        googlebutton.setOnClickListener {
            googleLogin()
        }

        facebookbutton.setOnClickListener {
            facebookLogin()
        }

        var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        callbackManager = CallbackManager.Factory.create()
    }

    fun facebookLogin(){
        LoginManager.getInstance()
            .logInWithReadPermissions(this, Arrays.asList("public_profile", "email"))

        LoginManager.getInstance()
            .registerCallback(callbackManager, object : FacebookCallback<LoginResult>{

                override fun onSuccess(result: LoginResult) {
                    handleFacebookAccessToken(result?.accessToken)
                }

                override fun onCancel() {

                }

                override fun onError(error: FacebookException) {

                }
            })
    }

    fun handleFacebookAccessToken(token : AccessToken?){
        var credential = FacebookAuthProvider.getCredential(token?.token!!)
        auth?.signInWithCredential(credential)
            ?.addOnCompleteListener {
                    task ->
                if(task.isSuccessful){
                    moveMainPage(task.result?.user)
                }else{
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager?.onActivityResult(requestCode, resultCode,data)

        if (requestCode == GOOGLE_LOGIN_CODE) {
            var result = Auth.GoogleSignInApi.getSignInResultFromIntent(data!!)

            if (result != null) {
                if (result.isSuccess){
                    var account = result.signInAccount
                    firebaseAuthWithGoogle(account)
                }
            }
        }
    }

    fun firebaseAuthWithGoogle(account : GoogleSignInAccount?){
        var credential = GoogleAuthProvider.getCredential(account?.idToken, null)

        auth?.signInWithCredential(credential)
            ?.addOnCompleteListener {
                task ->
                if(task.isSuccessful){
                    moveMainPage(task.result?.user)
                }else{
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onStart() {
        super.onStart()
        moveMainPage(auth?.currentUser)
    }

    fun googleLogin(){
        var signInIntent = googleSignInClient?.signInIntent

        if (signInIntent != null) {
            startActivityForResult(signInIntent, GOOGLE_LOGIN_CODE)
        }
    }

    fun signinAndSignup(){
        var email = findViewById<EditText>(R.id.email_edittext)
        var password = findViewById<EditText>(R.id.password_edittext)

        auth?.createUserWithEmailAndPassword(email.text.toString(), password.text.toString())
            ?.addOnCompleteListener {
                task ->
                    if(task.isSuccessful){
                        moveMainPage(task.result?.user)
                    }else if (task.exception?.message.isNullOrEmpty()){
                        Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                    }else {
                        signinEmail()
                    }
        }
    }

    fun signinEmail(){
        var email = findViewById<EditText>(R.id.email_edittext)
        var password = findViewById<EditText>(R.id.password_edittext)

        auth?.signInWithEmailAndPassword(email.text.toString(), password.text.toString())
            ?.addOnCompleteListener {
                task ->
                if(task.isSuccessful){
                    moveMainPage(task.result?.user)
                }else{
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                }
            }
    }

    fun moveMainPage(user:FirebaseUser?){
        if (user != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}