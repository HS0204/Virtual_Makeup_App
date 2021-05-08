package com.example.kotlinandroid

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.*
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.Toast
import android.util.Log
import androidx.core.content.FileProvider
import com.example.kotlinandroid.Retrofit.*
import com.example.kotlinandroid.Utils.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.*
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.lang.reflect.Type
import java.net.URISyntaxException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), IUploadCallback {

    lateinit var makeupSeletLayout: RelativeLayout // add--> baselayout
    lateinit var btn_makeupSelect: Button // add--> makeupSelect button

    lateinit var upService: IUploadAPI
    lateinit var downService : IDownloadAPI
    lateinit var upParamsService : IUploadParamsAPI


    var selectedUri: Uri? = null
    lateinit var dialog: ProgressDialog

    private val REQUEST_IMAGE_CAPTURE: Int = 1 // 사진 촬영 요청 코드, val은 불변
    private val PICK_FILE_REQUEST: Int = 1000 // 갤러리 오픈 요청 코드
    lateinit var curPhotoPath: String // 사진 경로, var은 변할 수 있고, lateinit var은 나중에 정할 수 있음

    // 인터페이스 생성
    private val apiUpload: IUploadAPI
        get() = RetrofitClient_Up.client.create(IUploadAPI::class.java)
    private val apiDownload: IDownloadAPI
        get() = RetrofitClient_Down.client.create(IDownloadAPI::class.java)
    private val apiParameter: IUploadParamsAPI
        get() = RetrofitClient_Parameter.client.create(IUploadParamsAPI::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        title = "테스트중"

        makeupSeletLayout = findViewById<RelativeLayout>(R.id.baselayout) as RelativeLayout
        btn_makeupSelect = findViewById<Button>(R.id.btn_makeupSelect) as Button
        registerForContextMenu(btn_makeupSelect)


        // 저장소 권한 허가
        Dexter.withActivity(this)
            .withPermissions(android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.CAMERA)
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport) {

                    }
                    override fun onPermissionRationaleShouldBeShown( permissions: List<PermissionRequest>, token: PermissionToken ) {

                    }
            }).check()

        // API
        upService = apiUpload
        downService = apiDownload
        upParamsService = apiParameter

        // 이벤트
        btn_gallery.setOnClickListener { chooseFile() } // 갤러리에서 사진 선택
        btn_makeup.setOnClickListener { downloadFile() } // 서버에서 메이크업된 사진 수신
        btn_capture.setOnClickListener { takeCapture() } // 사진 촬영
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
                                Log.d("급나피곤해>??", "성공 : ${response.raw()}")
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
                        Log.d("우짤램>??", "성공 : ${response.raw()}")
                        image_view.setImageBitmap(bmp)
                        Toast.makeText(this@MainActivity, "메이크업에 성공하였습니다.", Toast.LENGTH_SHORT).show()
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Toast.makeText(this@MainActivity, t.message, Toast.LENGTH_SHORT).show()
                    }
        })
    }

    // 파라메타 전달
    private fun uploadParameter(r:Int, g:Int, b:Int, size:Int, index:Int){
        val input = HashMap<String, Int>()
        input["rColor"] = r
        input["gColor"] = g
        input["bColor"] = b
        input["size"] = size
        input["index"] = index
        upParamsService.uploadParameter(input).enqueue(object : Callback<parameter>{
                    override fun onResponse(call: Call<parameter>, response: Response<parameter>) {
                        //Log.d("여기 JSON이어야 하믄데", "성공 : ${response.raw()}")
                        //Log.d("여기 컬레겟해", "성공 : ${response.body()?.getRC()}")
                        //Toast.makeText(this@MainActivity, "파라메타 전달에 성공하였습니다.", Toast.LENGTH_SHORT).show()
                    }

                    override fun onFailure(call: Call<parameter>, t: Throwable) {
                        //Toast.makeText(this@MainActivity, t.message, Toast.LENGTH_SHORT).show()
                    }
                })
    }

    // 카메라 촬영
    private fun takeCapture() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also{
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    null
                }
                photoFile?. also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                            this,
                            "com.example.kotlinandroid.fileprovider",
                            it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE) // ForResult를 쓰면 서브 액티비티에서 돌아오는 결과값을 main에서 받을 수 있음
                }
            }
        }
    }

    // 이미지 파일 생성
    private fun createImageFile(): File? {
        val timestamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date()) // 저장 시간에 따른 파일명 설정
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES) // 파일 경로
        return File.createTempFile("JPEG_${timestamp}_", ".jpg", storageDir)
                .apply { curPhotoPath = absolutePath} // 절대 경로에 저장 but 이미지뷰에 세팅하기 위한 임시(temp) 저장
    }

    // image_view에 선택한 사진 뿌리기
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // 갤러리에서 이미지를 성공적으로 가져왔다면
        if (requestCode == PICK_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    selectedUri = data.data
                    if (selectedUri != null && !selectedUri!!.path!!.isEmpty())
                        image_view.setImageURI(selectedUri)
                }
        }

        // 카메라에서 이미지를 성공적으로 가져왔다면
        if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val bitmap: Bitmap
            val file = File(curPhotoPath) // 현재 사진이 저장된 값
            val filepath = Uri.fromFile(file) // 현재 사진이 저장된 경로

            // 버전에 따라 이미지 가져오는 방식이 다르기 때문에 설정
            if (Build.VERSION.SDK_INT < 28) {
                bitmap = MediaStore.Images.Media.getBitmap(contentResolver, filepath)
                image_view.setImageBitmap(imgRotate(bitmap))
            } else {
                val decode = ImageDecoder.createSource(
                        this.contentResolver,
                        filepath
                )
                bitmap = ImageDecoder.decodeBitmap(decode)
                image_view.setImageBitmap(imgRotate(bitmap))
            }

            //savePhoto(bitmap)
            //uploadPhoto(filepath)
        }

    }

    // 이미지 회전
    private fun imgRotate(bitmap: Bitmap): Bitmap? {
        val width = bitmap.width
        val height = bitmap.height

        val matrix = Matrix()
        matrix.postRotate(90F)

        val resizeBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
        bitmap.recycle()

        return resizeBitmap
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
            R.id.itemshadowRed -> {
                uploadParameter(255, 0, 51, 45, 1)
                uploadFile()
                return true
            }
            R.id.itemshadowOrange -> {
                uploadParameter(255, 102, 0, 45, 1)
                uploadFile()
                return true
            }
            R.id.itemshadowPink -> {
                uploadParameter(255, 102, 153, 45, 1)
                uploadFile()
                return true
            }

            R.id.itemlibRed -> {
                uploadParameter(255, 0, 51, 11, 2)
                uploadFile()
                return true
            }
            R.id.itemlibOrange -> {
                uploadParameter(255, 102, 0, 11, 2)
                uploadFile()
                return true
            }
            R.id.itemlibPink -> {
                uploadParameter(255, 102, 153, 11, 2)
                uploadFile()
                return true
            }

            R.id.itemcheekRed -> {
                uploadParameter(255, 0, 51, 80, 3)
                uploadFile()
                return true
            }
            R.id.itemcheekOrange -> {
                uploadParameter(255, 102, 0, 80, 3)
                uploadFile()
                return true
            }
            R.id.itemcheekPink -> {
                uploadParameter(255, 102, 153, 80, 3)
                uploadFile()
                return true
            }

            R.id.itempupilRed -> {
                uploadParameter(255, 0, 51, 11, 4)
                uploadFile()
                return true
            }
            R.id.itempupilOrange -> {
                uploadParameter(255, 102, 0, 11, 4)
                uploadFile()
                return true
            }
            R.id.itempupilPink -> {
                uploadParameter(255, 102, 153, 11, 4)
                uploadFile()
                return true
            }
        }
        return false
    }


}
