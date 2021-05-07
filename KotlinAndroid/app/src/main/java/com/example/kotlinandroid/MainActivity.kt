package com.example.kotlinandroid

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.kotlinandroid.Retrofit.IDownloadAPI
import com.example.kotlinandroid.Retrofit.IUploadAPI
import com.example.kotlinandroid.Retrofit.RetrofitClient_Down
import com.example.kotlinandroid.Retrofit.RetrofitClient_Up
import com.example.kotlinandroid.Utils.Common
import com.example.kotlinandroid.Utils.IUploadCallback
import com.example.kotlinandroid.Utils.ProgressRequestBody
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.lang.StringBuilder
import java.net.URISyntaxException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

class MainActivity : AppCompatActivity(), IUploadCallback {

    lateinit var makeupSeletLayout: RelativeLayout // add--> baselayout
    lateinit var btn_makeupSelect: Button // add--> makeupSelect button

    lateinit var upService: IUploadAPI
    lateinit var downService : IDownloadAPI


    var selectedUri: Uri? = null
    lateinit var dialog: ProgressDialog
    val REQUSET_IMAGE_CAPTURE = 1 // 사진 촬영 요청 코드, val은 불변

    companion object {
        private val PICK_FILE_REQUEST: Int = 1000
    }

    private val apiUpload: IUploadAPI
        get() = RetrofitClient_Up.client.create(IUploadAPI::class.java)
    private val apiDownload: IDownloadAPI
        get() = RetrofitClient_Down.client.create(IUploadAPI::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        title = "테스트중"

        makeupSeletLayout = findViewById<RelativeLayout>(R.id.baselayout) as RelativeLayout
        btn_makeupSelect = findViewById<Button>(R.id.btn_makeupSelect) as Button
        registerForContextMenu(btn_makeupSelect)


        // 저장소 권한 허가
        Dexter.withActivity(this)
            .withPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse?) {

                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest?,
                    token: PermissionToken?
                ) {

                }

                override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                    Toast.makeText(this@MainActivity, "권한을 허가 받아야 합니다.", Toast.LENGTH_LONG).show()
                }
            }).check()

        // API
        upService = apiUpload
        downService = apiDownload

        // 이벤트
        btn_gallery.setOnClickListener { chooseFile() } // 갤러리에서 사진 선택
        btn_upload.setOnClickListener { uploadFile() } // 서버로 사진 전송
        //btn_capture.setOnClickListener { takeCapture() } // 사진 촬영
    }

    // 갤러리에서 이미지 선택
    private fun chooseFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_FILE_REQUEST)
    }

    // 서버로 이미지 업로드
    private fun uploadFile() {
        if (selectedUri != null) {
            var file: File? = null
            try {
                file = File(Common.getFilePath(this, selectedUri!!))
            } catch (e: URISyntaxException) {
                e.printStackTrace()
            }
            if (file != null) {
                val requestBody = ProgressRequestBody(file, this)
                val body = MultipartBody.Part.createFormData("image", file.name, requestBody)

                Thread(Runnable {
                    upService.uploadFile(body)
                        .enqueue(object : Callback<String> {
                            override fun onResponse(call: Call<String>, response: Response<String>) {
                                image_view.setImageURI(selectedUri)
                                Toast.makeText(this@MainActivity, "사진 전송에 성공하였습니다.", Toast.LENGTH_SHORT).show()
                            }

                            override fun onFailure(call: Call<String>, t: Throwable) {
                                Toast.makeText(this@MainActivity, t.message, Toast.LENGTH_SHORT).show()
                            }

                        })
                }).start()
            } else {
                Toast.makeText(this@MainActivity, "이 이미지를 업로드 할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 서버에서 이미지 다운로드
    private fun downloadFile(){
        downService.downloadFile("http://192.168.1.100:5000/static/Output.jpg/")
                .enqueue(object : Callback<ResponseBody>{
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                var inputS : InputStream = response.body()!!.byteStream()
                var bmp : Bitmap = BitmapFactory.decodeStream(inputS)
                image_view.setImageBitmap(bmp)
                Toast.makeText(this@MainActivity, "메이크업에 성공하였습니다.", Toast.LENGTH_SHORT).show()
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(this@MainActivity, t.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    // image_view에 선택한 사진 뿌리기
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PICK_FILE_REQUEST) {
                if (data != null) {
                    selectedUri = data.data
                    if (selectedUri != null && !selectedUri!!.path!!.isEmpty())
                        image_view.setImageURI(selectedUri)
                }
            }
        }
    }

    override fun onProgressupdate(percent: Int) {

    }

    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)

        var mInflater = this.menuInflater
        if (v === btn_makeupSelect) {
            menu!!.setHeaderTitle("배경색 변경")
            mInflater.inflate(R.menu.menu1, menu)
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.itemcheekRed -> {
                uploadFile()
                downloadFile()
                return true
            }

            R.id.itemcheekOrange -> {
                downloadFile()
                return true
            }

            R.id.itemcheekPink -> {
                // code insert
                return true
            }

        }
        return false
    }


}