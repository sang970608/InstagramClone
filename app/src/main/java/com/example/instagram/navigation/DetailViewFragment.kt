package com.example.instagram.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.instagram.R
import com.example.instagram.databinding.FragmentDetailBinding
import com.example.instagram.databinding.ItemDetailBinding
import com.example.instagram.navigation.model.ContentDTO
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DetailViewFragment : Fragment() {
    var firestore : FirebaseFirestore? = null
    var uid : String? = null
    lateinit var binding : FragmentDetailBinding
    lateinit var itembinding : ItemDetailBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDetailBinding.inflate(inflater,container,false)
        firestore = FirebaseFirestore.getInstance()
        uid = FirebaseAuth.getInstance().currentUser?.uid

        binding.detailviewfragmentRecyclerview.adapter = DetailViewRecyclerViewAdapter()
        binding.detailviewfragmentRecyclerview.layoutManager = LinearLayoutManager(activity)

        return binding.root
    }

    inner class DetailViewRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var contentDTOs : ArrayList<ContentDTO> = arrayListOf()
        var contentUidList : ArrayList<String> = arrayListOf()

        init {
            firestore?.collection("images")?.orderBy("timestamp")?.addSnapshotListener{ querySnapshot, firebaseFirestoreException ->
                contentDTOs.clear()
                contentUidList.clear()
                if(querySnapshot == null) return@addSnapshotListener
                for(snapshot in querySnapshot!!.documents){
                    var item = snapshot.toObject(ContentDTO::class.java)
                    contentDTOs.add(item!!)
                    contentUidList.add(snapshot.id)
                }
                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RecyclerView.ViewHolder {
            var view = LayoutInflater.from(p0.context).inflate(R.layout.item_detail, p0, false)
            itembinding = ItemDetailBinding.inflate(LayoutInflater.from(p0.context), p0,false)
            return CustomViewHolder(view)
        }

        inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view)

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {
            var viewholder = (p0 as CustomViewHolder).itemView

            itembinding.detailviewitemProfileTextview.text = contentDTOs!![p1].userId

            Glide.with(p0.itemView.context).load(contentDTOs!![p1].imageUrl).into(itembinding.detailviewitemImageviewContent)

            itembinding.detailviewitemExplainTextview.text = contentDTOs!![p1].explain

            itembinding.detailviewitemFavoritecounterTextview.text = "Likes " + contentDTOs!![p1].favoriteCount

            Glide.with(p0.itemView.context).load(contentDTOs!![p1].imageUrl).into(itembinding.detailviewitemProfileImage)

            itembinding.detailviewitemFavoriteImageview.setOnClickListener {
                favoriteEvent(p1)
            }

            if(contentDTOs!![p1].favorites.containsKey(uid)){
                itembinding.detailviewitemFavoriteImageview.setImageResource(R.drawable.ic_favorite)
            }else{
                itembinding.detailviewitemFavoriteImageview.setImageResource(R.drawable.ic_favorite_border)
            }

            itembinding.detailviewitemProfileImage.setOnClickListener {
                var fragment = UserFragment()
                var bundle = Bundle()
                bundle.putString("destinationUid", contentDTOs[p1].uid)
                bundle.putString("userId", contentDTOs[p1].uid)
                fragment.arguments = bundle
                activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.main_content,fragment)?.commit()
            }
        }

        fun favoriteEvent(position : Int){
            var tsDoc = firestore?.collection("images")?.document(contentUidList[position])
            firestore?.runTransaction{ transaction ->
                var uid = FirebaseAuth.getInstance().currentUser?.uid
                var contentDTO = transaction.get(tsDoc!!).toObject(ContentDTO::class.java)

                if(contentDTO!!.favorites.containsKey(uid)){
                    contentDTO.favoriteCount = contentDTO.favoriteCount - 1
                    contentDTO.favorites.remove(uid)

                }else{
                    contentDTO.favoriteCount = contentDTO.favoriteCount + 1
                    contentDTO.favorites[uid!!] = true
                }
                transaction.set(tsDoc,contentDTO)
            }
        }

    }
}