package com.example.calculator

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Switch
import androidx.appcompat.app.AppCompatDelegate

class SplashActivity : AppCompatActivity() {

    private lateinit var editor: SharedPreferences.Editor
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {

        sharedPreferences = getSharedPreferences("MODE", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()
        val nightMode:Boolean = sharedPreferences.getBoolean("nightMode", false)

        if (nightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val iv_image = findViewById<ImageView>(R.id.imageView)
        val i = Intent(this, MainActivity::class.java)

        iv_image.animate().apply {
            duration = 2000
            rotationY(360f)
        }.withEndAction {
            iv_image.animate().apply {
                duration = 500
                scaleY(1.5f)
                scaleX(1.5f)
            }.withEndAction {
                iv_image.animate().apply {
                    duration = 500
                    rotation(180f)
                    scaleY(-1.5f)
                    scaleX(-1.5f).withEndAction {
                        startActivity(i)
                        finish()
                    }.start()

                }

            }
        }

    }
}