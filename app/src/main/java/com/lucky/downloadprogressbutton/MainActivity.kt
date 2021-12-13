package com.lucky.downloadprogressbutton

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    var isRunning = true
    var progress = 0.0f
    lateinit var downloadProgressButton: DownloadProgressButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        downloadProgressButton = findViewById<DownloadProgressButton>(R.id.download)
        downloadProgressButton.isEnablePause = true
        downloadProgressButton.onDownLoadClickListener =
            object : DownloadProgressButton.OnDownLoadClickListener {
                override fun clickDownload() {
                    showToast("clickDownload")
                    isRunning = true
                    startRunning()
                }

                override fun clickPause() {
                    isRunning = false
                    showToast("clickPause")
                }

                override fun clickResume() {
                    isRunning = true
                    startRunning()
                    showToast("clickResume")
                }

                override fun clickFinish() {
                    isRunning = false
                    showToast("clickFinish")
                }

            }
    }

    fun startRunning(){
        Thread(Runnable {
            while (isRunning) {
                runOnUiThread(
                    Runnable {
                        progress += 0.1f
                        downloadProgressButton.progress = progress
                    }
                )
                Thread.sleep(100)
            }
        }).start()
    }


    fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}