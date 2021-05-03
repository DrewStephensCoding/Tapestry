package com.example.tapestry.ui.home

import android.graphics.drawable.ScaleDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import com.example.tapestry.R
import com.example.tapestry.objects.WallImage
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade


class ImageAdapter(private val dimensions: IntArray, private val lowRes: Boolean) :
    RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    var wallImages = ArrayList<WallImage>()
    private val width: Int = dimensions[0]
    private val height: Int = dimensions[1]


    fun updateWallImages(updatedList: ArrayList<WallImage>) {
        wallImages = updatedList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.card_image, parent, false)
        return ImageViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.image.setImageBitmap(null)
        holder.image.setImageDrawable(null)
        val currentImage = wallImages[position]
        val placeholder = ContextCompat.getDrawable(holder.itemView.context, R.drawable.ic_android)
        val errorDraw = ScaleDrawable(placeholder, 0, (width / 2).toFloat(), (height / 4).toFloat())
        val requestOptions = RequestOptions()
        requestOptions.apply {
            diskCacheStrategy(DiskCacheStrategy.NONE)
            skipMemoryCache(true)
            signature(ObjectKey(System.currentTimeMillis()))
            centerCrop()
            withCrossFade()
            placeholder(errorDraw)
            error(errorDraw)
        }

        //call this to clear previous requests
        Glide.with(holder.image).clear(holder.image)
        holder.image.setImageBitmap(null)

        Glide.with(holder.image.context)
            .applyDefaultRequestOptions(requestOptions)
            .load(if (lowRes) currentImage.previewUrl else currentImage.imgUrl).into(holder.image)
        Log.e("Load Image: ", currentImage.imgUrl)

    }

    override fun getItemCount(): Int {
        return wallImages.size
    }

    fun getWallImage(position: Int): WallImage {
        return if (position >= wallImages.size) {
            wallImages[wallImages.size - 1]
        } else {
            wallImages[position]
        }
    }

    override fun getItemId(position: Int): Long {
        return wallImages.size.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    inner class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.image_holder)
    }
}
