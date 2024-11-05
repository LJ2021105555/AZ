package com.example.photoviewer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException

class MainActivity : ComponentActivity() {

    private var selectedImageUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // 启动图片选择器的按钮
                    val imageLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri: Uri? ->
                        selectedImageUri = uri
                    }

                    Button(onClick = {
                        imageLauncher.launch("image/*")
                    }) {
                        Text("选择图片")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 显示已选中的图片
                    selectedImageUri?.let { uri ->
                        Image(
                            painter = rememberAsyncImagePainter(
                                ImageRequest.Builder(LocalContext.current)
                                    .data(uri)
                                    .apply(block = {
                                        crossfade(true)
                                        transformations(CircleCropTransformation())
                                    })
                                    .build()
                            ),
                            contentDescription = null,
                            modifier = Modifier
                                .size(200.dp)
                                .padding(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 上传图片按钮
                    Button(onClick = {
                        selectedImageUri?.let { uri ->
                            uploadImage(uri)
                        }
                    }) {
                        Text("上传图片")
                    }
                }
            }
        }
    }

    private fun uploadImage(uri: Uri) {
        val context = this
        val contentResolver = context.contentResolver

        // Step 1: 将 Uri 转换为 File
        val inputStream = contentResolver.openInputStream(uri)
        val file = File(context.cacheDir, "upload_image.jpg")

        inputStream?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        // Step 2: 使用 OkHttp 创建请求
        val client = OkHttpClient()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",  // formData key 名称
                file.name,
                file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder()
            .url("\thttps://webhook.site/b7c3cd98-7ef3-4ef9-b87c-c44d61af9a8e") // 替换为你服务器的 URL
            .post(requestBody)
            .build()

        // Step 3: 执行网络请求
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(context, "上传失败：${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(context, "上传成功", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "上传失败：${response.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}
