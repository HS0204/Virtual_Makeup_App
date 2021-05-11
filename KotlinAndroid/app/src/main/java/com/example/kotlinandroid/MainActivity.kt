package com.example.kotlinandroid

import android.annotation.SuppressLint
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
import android.widget.SeekBar
import androidx.core.content.FileProvider
import com.example.kotlinandroid.Retrofit.*
import com.example.kotlinandroid.Utils.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
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
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.lang.reflect.Type
import java.net.URISyntaxException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), IUploadCallback {

    lateinit var upService: IUploadAPI
    lateinit var downService : IDownloadAPI
    lateinit var upParamsService : IUploadParamsAPI


    var selectedUri: Uri? = null
    lateinit var dialog: ProgressDialog

    private val IMAGE_CAPTURE_REQUEST: Int = 1 // 사진 촬영 요청 코드, val은 불변
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

        // 저장소 권한 허가
        Dexter.withActivity(this)
            .withPermissions(android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
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

        // 메이크업 시트
        BottomSheetBehavior.from(sheet).apply {
            peekHeight = 270 // 보이는 정도
            this.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        // 메이크업 강도 조절 바
        volumeSeekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                volume.text = progress.toString()
                volume.visibility = View.VISIBLE
                uploadStrong(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                makeUpFace()
            }

        })

        // 이벤트
        btn_gallery.setOnClickListener { chooseFile() } // 갤러리에서 사진 선택
        btn_makeup.setOnClickListener { downloadFile() } // 서버에서 메이크업된 사진 수신
        btn_capture.setOnClickListener { takeCapture() } // 사진 촬영

        // 메이크업
        lip.setOnClickListener {
            volumeSeekBar.visibility = View.VISIBLE
            uploadParameter(247, 40, 57, 33, 2)
        }
        cheek.setOnClickListener {
            volumeSeekBar.visibility = View.VISIBLE
            uploadParameter(247, 40, 57, 100, 3)
        }
        shadow.setOnClickListener {
            volumeSeekBar.visibility = View.VISIBLE
            uploadParameter(247, 40, 57, 66, 1)
        }
        """
        pupli.setOnClickListener {
            volumeSeekBar.visibility = View.VISIBLE
            uploadParameter(255, 0, 51, 66, 1)
            uploadFile()
        }
        """
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
                file = File(Common?.getFilePath(this, selectedUri!!))
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
                                // image_view.setImageURI(selectedUri)
                                // Log.d("결과", "성공 : //{response.raw()}")
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
        downService.downloadFile("http://192.168.1.102:5000/static/Output.jpg/")
                .enqueue(object : Callback<ResponseBody>{
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        var inputS : InputStream = response.body()!!.byteStream()
                        var bmp : Bitmap = BitmapFactory.decodeStream(inputS)
                        // Log.d("결과", "성공 : ${response.raw()}")
                        image_view.setImageBitmap(bmp)
                        //savePhoto(bmp)
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
                        //Log.d("결과", "성공 : ${response.raw()}")
                        //Toast.makeText(this@MainActivity, "파라메타 전달에 성공하였습니다.", Toast.LENGTH_SHORT).show()
                    }

                    override fun onFailure(call: Call<parameter>, t: Throwable) {
                        //Toast.makeText(this@MainActivity, t.message, Toast.LENGTH_SHORT).show()
                    }
                })
    }

    // 메이크업 강도 전달
    private fun uploadStrong(strong:Int){
        upParamsService.uploadStrong(strong).enqueue(object : Callback<strong>{
            override fun onResponse(call: Call<strong>, response: Response<strong>) {
                //Log.d("결과", "성공 : ${response.raw()}")
                //Toast.makeText(this@MainActivity, "파라메타 전달에 성공하였습니다.", Toast.LENGTH_SHORT).show()
            }

            override fun onFailure(call: Call<strong>, t: Throwable) {
                //Toast.makeText(this@MainActivity, t.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    // 실질적인 메이크업 요청
    private fun makeUpFace(){
        val strong = 1
        upParamsService.makeUpFace(strong).enqueue(object : Callback<strong>{
            override fun onResponse(call: Call<strong>, response: Response<strong>) {
                Toast.makeText(this@MainActivity, "메이크업 버튼을 눌러주세요.", Toast.LENGTH_SHORT).show()
            }

            override fun onFailure(call: Call<strong>, t: Throwable) {

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
                            "com.example.KotlinAndroid.Fileprovider",
                            it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, IMAGE_CAPTURE_REQUEST) // ForResult를 쓰면 서브 액티비티에서 돌아오는 결과값을 main에서 받을 수 있음
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
                    if (selectedUri != null && !selectedUri!!.path!!.isEmpty()){
                        image_view.setImageURI(selectedUri)
                        uploadFile()
                    }

                }
        }

        // 카메라에서 이미지를 성공적으로 가져왔다면
        if(requestCode == IMAGE_CAPTURE_REQUEST && resultCode == Activity.RESULT_OK) {
            val bitmap: Bitmap
            val file = File(curPhotoPath) // 현재 사진이 저장된 값
            val filepath = Uri.fromFile(file) // 현재 사진이 저장된 경로
            selectedUri = filepath

            if (Build.VERSION.SDK_INT < 28) {
                bitmap = MediaStore.Images.Media.getBitmap(contentResolver, filepath)
                image_view.setImageBitmap(bitmap)
                uploadFile()
            } else {
                val decode = ImageDecoder.createSource(
                        this.contentResolver,
                        filepath
                )
                bitmap = ImageDecoder.decodeBitmap(decode)
                image_view.setImageBitmap(bitmap)
                uploadFile()
            }
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

    // 갤러리에 저장
    @SuppressLint("SimpleDateFormat")
    private fun savePhoto(bitmap: Bitmap) {
        val folderPath = Environment.getExternalStorageDirectory().absolutePath + "/Pictures/" // Pictures 폴더에 저장하기 위한 경로 선언
        val timestamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date()) // 저장 시간에 따른 파일명 설정
        val fileName = "${timestamp}.jpeg"
        val folder = File(folderPath)

        // 선언한 경로의 폴더가 존재하지 않을 경우
        if(!folder.isDirectory) {
            folder.mkdirs()
        }

        // 실질적인 저장처리
        val out = FileOutputStream(folderPath + fileName)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        //Toast.makeText(this,"갤러리에 사진이 저장되었습니다", Toast.LENGTH_SHORT).show()
    }

    override fun onProgressupdate(percent: Int) {

    }


}
