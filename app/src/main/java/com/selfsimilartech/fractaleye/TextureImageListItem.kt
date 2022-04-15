package com.selfsimilartech.fractaleye

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.View
import androidx.cardview.widget.CardView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder
import kotlin.math.ceil


class TextureImageListItem (

        val id                  : Int = -1,
        val path                : String = ""

) : AbstractFlexibleItem<TextureImageListItem.TextureImageHolder>() {

    /**
     * When an item is equals to another?
     * Write your own concept of equals, mandatory to implement or use
     * default java implementation (return this == o;) if you don't have unique IDs!
     * This will be explained in the "Item interfaces" Wiki page.
     */
    override fun equals(other: Any?): Boolean {
        if (other is TextureImageListItem) {
            return (id != -1 && other.id != -1 && id == other.id) || (path != "" && other.path != "" && path == other.path)
        }
        return false
    }

    /**
     * You should implement also this method if equals() is implemented.
     * This method, if implemented, has several implications that Adapter handles better:
     * - The Hash, increases performance in big list during Update & Filter operations.
     * - You might want to activate stable ids via Constructor for RV, if your id
     * is unique (read more in the wiki page: "Setting Up Advanced") you will benefit
     * of the animations also if notifyDataSetChanged() is invoked.
     */
    override fun hashCode(): Int {
        return if (id != -1) id.hashCode() else path.hashCode()
    }

    /**
     * For the item type we need an int value: the layoutResID is sufficient.
     */
    override fun getLayoutRes(): Int {
        return R.layout.texture_image_list_item
    }

    /**
     * Delegates the creation of the ViewHolder to the user (AutoMap).
     * The inflated view is already provided as well as the Adapter.
     */
    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<*>?>?): TextureImageHolder {
        return TextureImageHolder(view, adapter)
    }

    /**
     * The Adapter and the Payload are provided to perform and get more specific
     * information.
     */
    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<*>?>, holder: TextureImageHolder,
                                position: Int,
                                payloads: List<Any>) {

        if (id == R.drawable.texture_image_add) {

            holder.image?.setImageResource(id)
            holder.image?.showGradient = !Settings.goldEnabled

        } else {

            if (id != -1) holder.image?.setImageResource(id)
            else {

                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                var inputStream = holder.itemView.context.openFileInput(path)
                BitmapFactory.decodeStream(inputStream, null, options)
                inputStream.close()
                val sampleSize = ceil(options.outWidth/240.0).toInt()

                Log.d("TEX IMAGE ITEM", "sample size: $sampleSize")

                inputStream = holder.itemView.context.openFileInput(path)
                holder.image?.setImageBitmap(
                        BitmapFactory.decodeStream(inputStream, null,
                                BitmapFactory.Options().apply {
                                    inPreferredConfig = Bitmap.Config.RGB_565
                                    inSampleSize = sampleSize
                                }
                        )
                )
                inputStream.close()
                holder.image?.setBackgroundColor(holder.itemView.resources.getColor(R.color.transparent, null))

            }
        }

    }


    /**
     * The ViewHolder used by this item.
     * Extending from FlexibleViewHolder is recommended especially when you will use
     * more advanced features.
     */
    class TextureImageHolder(view: View, adapter: FlexibleAdapter<*>?) : FlexibleViewHolder(view, adapter) {

        override fun getActivationElevation(): Float {
            return 20f
        }

        var card  : CardView?  = view.findViewById( R.id.textureImageCard )
        var image : GradientImageView? = view.findViewById( R.id.textureImage     )

    }

}