package com.example.demo

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.demo.ui.theme.DemoTheme
import com.google.android.gms.ads.MobileAds
import dalvik.system.DexClassLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.lang.Exception

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DemoTheme {
                // A surface container using the 'background' color from the theme
                Row {
                    Button(onClick = {
                    }, modifier = Modifier.wrapContentSize()) {
                        Text("copy", modifier = Modifier.wrapContentSize())
                    }
                    Button(onClick = {
                    }, modifier = Modifier.wrapContentSize()) {
                        Text("read", modifier = Modifier.wrapContentSize())
                    }
                }
            }


        }
    }

    fun initSDK(){
        MobileAds.initialize(this){

        }
    }

    fun showToast(content:String){
        GlobalScope.launch(Dispatchers.Main) {
            Toast.makeText(this@MainActivity,content,Toast.LENGTH_SHORT).show()
        }
    }

    @Composable
    fun Greeting(name: String, modifier: Modifier = Modifier) {
        Text(
            text = "Hello $name!",
            modifier = modifier
        )
    }

    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        DemoTheme {
            Greeting("Android")
        }
    }
}