package com.android.example.cameraxapp_extention

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.android.example.cameraxapp_extention.databinding.ChooseImageAndUpBinding
import com.bumptech.glide.load.resource.bitmap.TransformationUtils.rotateImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.IOException

class NextPageActivity : AppCompatActivity() {
    private lateinit var viewBinding: ChooseImageAndUpBinding
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>

    private lateinit var selected_View: ImageView
    private lateinit var registrated_View: ImageView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ChooseImageAndUpBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        resultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data: Intent? = result.data

                    /*        data?.data?.also { uri ->
                                try {
                                    val inputStream = contentResolver?.openInputStream(uri)
                                    val image = BitmapFactory.decodeStream(inputStream)
                                    selected_View = findViewById<ImageView>(R.id.selected_View)
                                    selected_View.setImageBitmap(image)
                                } catch (e: Exception) {
                                    Toast.makeText(this, "エラーが発生しました", Toast.LENGTH_LONG).show()
                                }
                            }  */

                    data?.data?.also { uri ->
                        try {
                            val inputStream = contentResolver?.openInputStream(uri)
                            val image = BitmapFactory.decodeStream(inputStream)
                            val exifInterface = ExifInterface(inputStream!!)
                            inputStream?.close()

                            val orientation = exifInterface.getAttributeInt(
                                ExifInterface.TAG_ORIENTATION,
                                ExifInterface.ORIENTATION_UNDEFINED
                            )
                            Log.d("Orientation_org", orientation.toString())

                            val rotatedBitmap = when (orientation) {
                                ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(image, 90)
                                ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(image, 180)
                                ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(image, 270)
                                else -> image
                            }

                            selected_View = findViewById<ImageView>(R.id.selected_View)
                            selected_View.setImageBitmap(rotatedBitmap)

                        } catch (e: IOException) {
                            Toast.makeText(this, "エラーが発生しました", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }

        //ボタン押下時の処理
        viewBinding.chooseImageButton.setOnClickListener { chooseImage() }
        viewBinding.cameraButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        viewBinding.uploadButton.setOnClickListener { uploadImage() }
        viewBinding.rotateButton.setOnClickListener {
            // 現在ImageViewに表示されているBitmapを取得
            val imageView = findViewById<ImageView>(R.id.selected_View)
            imageView.drawable?.let { drawable ->
                // DrawableからBitmapを取得
                val bitmap = (drawable as BitmapDrawable).bitmap
                // Bitmapを90度回転
                val rotatedBitmap = rotateImage(bitmap, 90f)
                // 回転したBitmapをImageViewにセット
                imageView.setImageBitmap(rotatedBitmap)
            }
        }
        }
    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    //画像選択
    private fun chooseImage() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        resultLauncher.launch(intent)
    }

    //READ_REQUEST_CODEの定義
    companion object {
        private const val READ_REQUEST_CODE: Int = 42
    }

    //写真が選択された後の動き
    //resultLauncherの定義の内容へ移動
    /*
    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)

    }　*/

    //選択画像をリネームしてアップロードする。準備中。
    private fun uploadImage() {
 
}
