package com.asd412id.tokoappscanner

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.URLUtil
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.zxing.Result
import me.dm7.barcodescanner.zxing.ZXingScannerView
import org.json.JSONObject

class ScanActivity : AppCompatActivity(), ZXingScannerView.ResultHandler {
    private var mScannerView: ZXingScannerView? = null
    lateinit var preferences: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = getSharedPreferences("configs", MODE_PRIVATE)
        if (ContextCompat.checkSelfPermission(this@ScanActivity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this@ScanActivity, arrayOf(Manifest.permission.CAMERA), 123)
        }

        mScannerView = ZXingScannerView(this)
        setContentView(mScannerView)
    }

    public override fun onResume() {
        super.onResume()
        mScannerView!!.setResultHandler(this)
        mScannerView!!.startCamera()
    }

    public override fun onPause() {
        super.onPause()
        mScannerView!!.stopCamera()
    }

    override fun handleResult(rawResult: Result) {
        val code = rawResult.toString()
        if (URLUtil.isValidUrl(code)){
            with(preferences.edit()){
                putString("url",code)
                apply()
                Toast.makeText(this@ScanActivity,"Alamat server berhasil diubah ke $code",Toast.LENGTH_SHORT).show()
            }
            mScannerView!!.resumeCameraPreview(this@ScanActivity)
        }else{
            Log.i("ANU",code)
            val queue = Volley.newRequestQueue(this)
            val request = object: JsonObjectRequest(Method.POST, preferences.getString("url",""), null, { response ->
                Toast.makeText(this,"Kode: ${response.getString("kode")}",Toast.LENGTH_SHORT).show()
                mScannerView!!.resumeCameraPreview(this@ScanActivity)
            }, {error ->
                Toast.makeText(this,"Tidak dapat terhubung ke server",Toast.LENGTH_LONG).show()
                mScannerView!!.resumeCameraPreview(this@ScanActivity)
            }){
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    headers["kode"] = code
                    return headers
                }
            }

            request.retryPolicy = DefaultRetryPolicy(
                DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
                0,
                1f
            )

            queue.add(request)
        }
    }
}