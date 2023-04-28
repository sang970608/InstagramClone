package com.example.instagram.navigation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.instagram.R
import com.example.instagram.navigation.model.ContentDTO
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import java.text.SimpleDateFormat
import java.util.Date

class AddPhotoActivity: AppCompatActivity() {
    var PICK_IMAGE_FROM_ALBUM = 0
    var storage : FirebaseStorage? = null
    var photoUri : Uri? = null
    var auth : FirebaseAuth? = null
    var firestore : FirebaseFirestore? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_photo)

        //Initiate storage
        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        var photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        startActivityForResult(photoPickerIntent, PICK_IMAGE_FROM_ALBUM)

        var addphoto_btn_upload = findViewById<Button>(R.id.addphoto_btn_upload)
        addphoto_btn_upload.setOnClickListener {
            contentUpload()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        var addphoto_image = findViewById<ImageView>(R.id.addphoto_image)

        if(requestCode == PICK_IMAGE_FROM_ALBUM) {
            if(resultCode == Activity.RESULT_OK){
                photoUri = data?.data
                addphoto_image.setImageURI(photoUri)
            }else{
                finish()
            }
        }
    }

    fun contentUpload(){
        var timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        var imageFileName = "IMAGE_" + timestamp + "_.png"
        var storageRef = storage?.reference?.child("images")?.child(imageFileName)

        storageRef?.putFile(photoUri!!)?.continueWithTask{
            task: Task<UploadTask.TaskSnapshot> ->
                return@continueWithTask storageRef.downloadUrl
        }?.addOnSuccessListener {
            uri ->
            var contentDTO = ContentDTO()
            val addphoto_edit_explain = findViewById<EditText>(R.id.addphoto_edit_explain)

            contentDTO.imageUrl = uri.toString()

            contentDTO.uid = auth?.currentUser?.uid

            contentDTO.userId = auth?.currentUser?.email

            contentDTO.explain = addphoto_edit_explain.text.toString()

            contentDTO.timestamp = System.currentTimeMillis()

            firestore?.collection("images")?.document()?.set(contentDTO)

            setResult(Activity.RESULT_OK)

            finish()
        }

//        storageRef?.putFile(photoUri!!)?.addOnSuccessListener {
//            storageRef.downloadUrl.addOnSuccessListener {
//                uri ->
//                var contentDTO = ContentDTO()
//                val addphoto_edit_explain = findViewById<EditText>(R.id.addphoto_edit_explain)
//
//                contentDTO.imageUrl = uri.toString()
//
//                contentDTO.uid = auth?.currentUser?.uid
//
//                contentDTO.userId = auth?.currentUser?.email
//
//                contentDTO.explain = addphoto_edit_explain.text.toString()
//
//                contentDTO.timestamp = System.currentTimeMillis()
//
//                firestore?.collection("images")?.document()?.set(contentDTO)
//
//                setResult(Activity.RESULT_OK)
//
//                finish()
//            }
//        }

    }
}