package com.example.instagram.navigation

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.instagram.LoginActivity
import com.example.instagram.MainActivity
import com.example.instagram.R
import com.example.instagram.databinding.ActivityMainBinding
import com.example.instagram.databinding.FragmentDetailBinding
import com.example.instagram.databinding.FragmentUserBinding
import com.example.instagram.navigation.model.ContentDTO
import com.example.instagram.navigation.model.FollowDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserFragment : Fragment() {
    lateinit var binding : FragmentUserBinding
    var firestore : FirebaseFirestore? = null
    var uid : String? = null
    var auth : FirebaseAuth? = null
    var currentUserUid : String? = null
    companion object {
        var PICK_PROFILE_FROM_ALBUM = 10
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentUserBinding.inflate(inflater,container,false)
        uid = arguments?.getString("destinationUid")
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUserUid = auth?.currentUser?.uid

        if (uid == currentUserUid) {
            //My page
            binding.accountBtnFollowSignout.text = getString(R.string.signout)
            binding.accountBtnFollowSignout.setOnClickListener {
                activity?.finish()
                startActivity(Intent(activity, LoginActivity::class.java))
                auth?.signOut()
            }
        }else{
            //Other
            binding.accountBtnFollowSignout.text = getString(R.string.follow)
            var mainbinding = ActivityMainBinding.inflate(LayoutInflater.from(container!!.context),container,false)
            mainbinding.toolbarUsername.text = arguments?.getString("userId")
            mainbinding.toolbarUsername.setOnClickListener {
                mainbinding.bottomNavigation.selectedItemId = R.id.action_home
            }
            mainbinding.toolbarTitleImage.visibility = View.GONE
            mainbinding.toolbarUsername.visibility = View.VISIBLE
            mainbinding.toolbarBtnBack.visibility = View.VISIBLE

            binding.accountBtnFollowSignout.setOnClickListener {
                requestFollow()
            }
        }

        binding.accountRecyclerview?.adapter = UserFragmentRecyclerViewAdapter()
        binding.accountRecyclerview?.layoutManager = GridLayoutManager(activity, 3)

        binding.accountIvProfile.setOnClickListener {
            var photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            activity?.startActivityForResult(photoPickerIntent, PICK_PROFILE_FROM_ALBUM)
        }

        getProfileImage()
        getFollowerAndFollowing()
        return binding.root
    }

    fun getFollowerAndFollowing(){
        firestore?.collection("users")?.document(uid!!)?.addSnapshotListener{ documentSnapshot, firebaseFirestoreException ->
            if(documentSnapshot == null) return@addSnapshotListener

            var followDTO = documentSnapshot.toObject(FollowDTO::class.java)

            if(followDTO?.followingCount != null){
                binding.accountTvFollowingCount.text = followDTO?.followingCount?.toString()
            }
            if(followDTO?.followerCount != null){
                binding.accountTvFollwerCount.text = followDTO?.followerCount?.toString()
                if (followDTO?.followers?.containsKey(currentUserUid!!) == true){
                    binding.accountBtnFollowSignout.text = getString(R.string.follow_cancel)
                    binding.accountBtnFollowSignout.background?.setColorFilter(ContextCompat.getColor(requireActivity(),R.color.colorLightGray),PorterDuff.Mode.MULTIPLY)
                }else{
                    binding.accountBtnFollowSignout.text = getString(R.string.follow)
                    if(uid != currentUserUid){
                        binding.accountBtnFollowSignout?.background?.colorFilter = null
                    }
                }
            }
        }
    }

    fun requestFollow(){
        var tsDocFollowing = firestore?.collection("users")?.document(currentUserUid!!)
        firestore?.runTransaction { transition ->
            var followDTO = transition.get(tsDocFollowing!!).toObject(FollowDTO::class.java)

            if (followDTO == null) {
                followDTO = FollowDTO()
                followDTO!!.followingCount = 1
                followDTO!!.followers[uid!!] = true

                transition.set(tsDocFollowing,followDTO)
                return@runTransaction
            }

            if(followDTO.followings.containsKey(uid)){
                //팔로잉 제거
                followDTO?.followerCount = followDTO.followingCount - 1
                followDTO?.followers?.remove(uid)
            }else{
                //팔로잉 추가
                followDTO?.followingCount = followDTO.followingCount + 1
                followDTO?.followers!![uid!!] = true
            }
            transition.set(tsDocFollowing,followDTO)
            return@runTransaction
        }
        var tsDocFollower = firestore?.collection("users")?.document(uid!!)
        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollower!!).toObject(FollowDTO::class.java)
            if(followDTO == null){
                followDTO = FollowDTO()
                followDTO!!.followerCount = 1
                followDTO!!.followers[currentUserUid!!] = true

                transaction.set(tsDocFollower,followDTO!!)
                return@runTransaction
            }

            if(followDTO!!.followers.containsKey(currentUserUid)){
                //팔로워 취소
                followDTO!!.followerCount = followDTO!!.followerCount - 1
                followDTO!!.followers.remove(currentUserUid!!)
            }else{
                //팔로우 추가
                followDTO!!.followerCount = followDTO!!.followerCount + 1
                followDTO!!.followers[currentUserUid!!] = true
            }
            transaction.set(tsDocFollower,followDTO!!)
            return@runTransaction
        }
    }

    fun getProfileImage(){
        firestore?.collection("profileImages")?.document(uid!!)?.addSnapshotListener{ documentSnapshot, firebaseFirestoreException ->
            if (documentSnapshot == null) return@addSnapshotListener
            if (documentSnapshot.data != null){
                var url = documentSnapshot?.data!!["image"]
                Glide.with(requireActivity()).load(url).apply(RequestOptions().circleCrop()).into(binding.accountIvProfile!!)
            }
        }
    }

    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var contentDTOs : ArrayList<ContentDTO> = arrayListOf()

        init {
            firestore?.collection("images")?.whereEqualTo("uid", uid)?.addSnapshotListener { querySnapshot, firebaseFirestore ->
                if (querySnapshot == null) return@addSnapshotListener

                for(snapshot in querySnapshot.documents){
                    contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                }
                binding.accountTvPostCount.text = contentDTOs.size.toString()
                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RecyclerView.ViewHolder {
            var width = resources.displayMetrics.widthPixels / 3
            var imageview = ImageView(p0.context)
            imageview.layoutParams = LinearLayoutCompat.LayoutParams(width, width)
            return CustomViewHolder(imageview)
        }

        inner class CustomViewHolder(var imageview: ImageView) : RecyclerView.ViewHolder(imageview) {

        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {
            var imageview = (p0 as CustomViewHolder).imageview
            Glide.with(p0.itemView.context).load(contentDTOs[p1].imageUrl).apply(RequestOptions().centerCrop()).into(imageview)
        }

    }
}

