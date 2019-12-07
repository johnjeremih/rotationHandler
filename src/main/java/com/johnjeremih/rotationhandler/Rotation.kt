package com.johnjeremih.rotationhandler

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.bumptech.glide.load.resource.bitmap.TransformationUtils
import java.io.ByteArrayOutputStream
import java.io.IOException

object Rotation {

        //Handling image rotation
        @Throws(IOException::class)
        fun handleRotation(
            context: Context?,
            selectedImage: Uri?
        ): Bitmap? {
            val MAX_HEIGHT = 1024
            val MAX_WIDTH = 1024
            // First decode with inJustDecodeBounds=true to check dimensions
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            var imageStream =
                context!!.contentResolver.openInputStream(selectedImage!!)
            BitmapFactory.decodeStream(imageStream, null, options)
            imageStream!!.close()
            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, MAX_WIDTH, MAX_HEIGHT)
            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false
            imageStream = context.contentResolver.openInputStream(selectedImage)
            var img = BitmapFactory.decodeStream(imageStream, null, options)
            img = rotateImageIfRequired(context, img!!, selectedImage)
            return img
        }

        //Minimizing image size
        private fun calculateInSampleSize(
            options: BitmapFactory.Options,
            reqWidth: Int,
            reqHeight: Int
        ): Int { // Raw height and width of image
            val height = options.outHeight
            val width = options.outWidth
            var inSampleSize = 1
            if (height > reqHeight || width > reqWidth) { // Calculate ratios of height and width to requested height and width
                val heightRatio =
                    Math.round(height.toFloat() / reqHeight.toFloat())
                val widthRatio =
                    Math.round(width.toFloat() / reqWidth.toFloat())
                // Choose the smallest ratio as inSampleSize value, this will guarantee a final image
// with both dimensions larger than or equal to the requested height and width.
                inSampleSize = if (heightRatio < widthRatio) heightRatio else widthRatio
                // This offers some additional logic in case the image has a strange
// aspect ratio. For example, a panorama may have a much larger
// width than height. In these cases the total pixels might still
// end up being too large to fit comfortably in memory, so we should
// be more aggressive with sample down the image (=larger inSampleSize).
                val totalPixels = width * height.toFloat()
                // Anything more than 2x the requested pixels we'll sample down further
                val totalReqPixelsCap = reqWidth * reqHeight * 2.toFloat()
                while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
                    inSampleSize++
                }
            }
            return inSampleSize
        }


        //Rotate the image if it is required
        @Throws(IOException::class)
        private fun rotateImageIfRequired(
            context: Context,
            img: Bitmap,
            selectedImage: Uri
        ): Bitmap? {
            val input =
                context.contentResolver.openInputStream(selectedImage)
            val ei: ExifInterface
            ei = ExifInterface(input!!)
            val orientation =
                ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            return when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> TransformationUtils.rotateImage(img, 90)
                ExifInterface.ORIENTATION_ROTATE_180 -> TransformationUtils.rotateImage(img, 180)
                ExifInterface.ORIENTATION_ROTATE_270 -> TransformationUtils.rotateImage(img, 270)
                else -> img
            }
        }

        //Reducing the Quality of the image by converting it into bytes
        fun qualityReducer(bitmap: Bitmap, maxSize: Int): ByteArray? {
            val resizedBitmap = getResizedBitmap(bitmap, maxSize)
            val byteArrayOutputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
            return byteArrayOutputStream.toByteArray()
        }

        fun getResizedBitmap(image: Bitmap, maxSize: Int): Bitmap {
            var width = image.width
            var height = image.height
            val bitmapRatio = width.toFloat() / height.toFloat()
            if (bitmapRatio > 1) {
                width = maxSize
                height = (width / bitmapRatio).toInt()
            } else {
                height = maxSize
                width = (height * bitmapRatio).toInt()
            }
            return Bitmap.createScaledBitmap(image, width, height, true)
        }


}