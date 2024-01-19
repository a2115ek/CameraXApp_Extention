package com.android.example.cameraxapp_extention

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.android.example.cameraxapp_extention.databinding.ChooseImageAndUpBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.TransformationUtils.rotateImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File
import java.io.IOException
import java.util.concurrent.CountDownLatch

class NextPageActivity : AppCompatActivity() {
    private lateinit var viewBinding: ChooseImageAndUpBinding
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>

    private lateinit var selected_View: ImageView
    private lateinit var registrated_View: ImageView

    //Firebaseの変数設定
    private var storage = FirebaseStorage.getInstance()


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
        // Intentから画像のURIを取得
        val imageUri = intent.getStringExtra("image_uri")?.let { Uri.parse(it) }
        if (imageUri != null) {
            // URIから画像をImageViewに表示
            selected_View = findViewById<ImageView>(R.id.selected_View)
            selected_View.setImageURI(imageUri)
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

       //deleteImage()
        previewStorageImage()
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

    //選択画像をリネームしてアップロードする。アップロード先に先にあるファイルは削除する。（準備中）
    private fun uploadImage() {
        //Firebaseへ匿名ログイン ※アプリ起動時に一度だけ実施すればよい
        FirebaseAuth.getInstance().signInAnonymously()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    val user = FirebaseAuth.getInstance().currentUser
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(NextPageActivity.TAG, "signInAnonymously:failure", task.exception)
                }
            }
        //ContentResolverを使ってファイルパスの取得
        fun getRealPathFromUri(contentUri: Uri, context: Context): String? {
            var cursor: Cursor? = null
            try {
                // コンテントURIから取得する列を指定
                val proj = arrayOf(MediaStore.Images.Media.DATA)
                cursor = context.contentResolver.query(contentUri, proj, null, null, null)
                val columnIndex = cursor?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                cursor?.moveToFirst()
                return columnIndex?.let { cursor?.getString(it) }
            } finally {
                cursor?.close()
            }
        }

        //intentに設定している画像のuriを取得
        val imageUri_now: Uri? = intent.getStringExtra("imageUri_now")?.let { Uri.parse(it) }
        Log.d(TAG, "val imageUri_now")
        if (imageUri_now != null) {
            //imageUri_nowを使用して変数photoFileを設定する
            imageUri_now?.let { uri ->
                val realPath = getRealPathFromUri(uri, this)
                Log.d(TAG, "val realPath")
                realPath?.let { path ->
                    val photoFile = File(path)
                    Log.d(TAG, "val photoFile")
                    val ref = storage.reference.child("images/${photoFile.name}")
                    Log.d(TAG, "val ref")
            ref.putFile(Uri.fromFile(photoFile))
                .addOnSuccessListener {
                    // Handle successful upload
                    val ok_msg = "Photo upload succeeded"
                    Toast.makeText(baseContext, ok_msg, Toast.LENGTH_SHORT).show()
                    Log.d(NextPageActivity.TAG, ok_msg)
                }
                .addOnFailureListener {
                    // Handle failed upload
                    val ng_msg = "Photo upload failed"
                    Toast.makeText(baseContext, ng_msg, Toast.LENGTH_SHORT).show()
                    Log.d(NextPageActivity.TAG, ng_msg)
                    }
                }
            }
        }else{
            Log.d(TAG, "imageUri_now = null")
        }
    }

    //Firebase Storage上の対象ディレクトリのファイルを全削除する
    private fun deleteImage() {
        //imagesディレクトリのリファレンスを取得
        val imagesRef = storage.reference.child("images")
        imagesRef.listAll().addOnSuccessListener { listResult ->
            for (fileRef in listResult.items) {
                // 各ファイルを削除
                fileRef.delete().addOnSuccessListener {
                    // ファイルの削除に成功
                    Log.d(TAG, "File deleted successfully: ${fileRef.path}")
                }.addOnFailureListener {
                    // ファイルの削除に失敗
                    Log.e(TAG, "Error while deleting the file: ${fileRef.path}")
                }
            }
        }
    }

    //Firebase Storage上の最新のタイムスタンプの画像を表示する
    private fun previewStorageImage() {
        //imagesディレクトリのリファレンスを取得
        val imagesRef = storage.reference.child("images")
        //imagesディレクトリ内のファイルのリストを取得
        imagesRef.listAll().addOnSuccessListener { listResult ->
            Log.d(TAG, "imagesRef.listAll()成功: ")
            val items = listResult.items
            findLatestImage(items)
            Log.d(TAG, "findLatestImage(items)")
        }.addOnFailureListener { exception ->
            Log.e(TAG, "imagesRef.listAll()失敗: ", exception)
        }
    }

    //最終更新時刻（updatedTimeMillis）を比較
    private fun findLatestImage(items: List<StorageReference>) {
        var latestTimestamp = Long.MIN_VALUE
        var latestImageRef: StorageReference? = null

        // 同期処理を排除し、最新の画像が見つかったら直接UIを更新
        for (item in items) {
            item.metadata.addOnSuccessListener { metadata ->
                Log.d(TAG, "item.metadata 成功: ")
                val updatedTime = metadata.updatedTimeMillis
                if (updatedTime > latestTimestamp) {
                    Log.d(TAG, "updatedTime > latestTimestamp")
                    latestTimestamp = updatedTime
                    latestImageRef = item
                    // 最新の画像が更新されたら、その画像をロード
                    loadImageIntoView(latestImageRef)
                    Log.d(TAG, "loadImageIntoView(latestImageRef)")
                }else{
                    Log.d(TAG, "updatedTime > latestTimestamp でエラー: ")
                }
            }.addOnFailureListener {exception ->
                Log.e(TAG, "item.metadata 失敗: ", exception)
            }
        }
    }

    fun loadImageIntoView(imageRef: StorageReference?) {
        imageRef?.downloadUrl?.addOnSuccessListener { uri ->
            val localFile = File.createTempFile("images", "jpg")
            imageRef.getFile(localFile).addOnSuccessListener {
                Log.d(TAG, "imageRef.getFile(localFile) 成功: ")
                // ダウンロード成功
                val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
                runOnUiThread {
                    // UIスレッドで画像をセット
                    val registratedView: ImageView = findViewById(R.id.registrated_View)
                    registratedView.setImageBitmap(bitmap)
                    Log.d(TAG, "registratedView.setImageBitmap(bitmap)")
                }
                Log.d(TAG, "runOnUiThread ")
            }.addOnFailureListener {exception ->
                Log.e(TAG, "imageRef.getFile(localFile) 失敗: ", exception)
            }
        }
    }

    companion object {
        private const val TAG = "CameraXApp_NextPageActivity"
    }
}