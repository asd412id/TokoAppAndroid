package com.asd412id.tokoappscanner

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.webkit.URLUtil
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.zxing.Result
import me.dm7.barcodescanner.zxing.ZXingScannerView

class ScanActivity : AppCompatActivity(), ZXingScannerView.ResultHandler {
    private var mScannerView: ZXingScannerView? = null
    lateinit var preferences: SharedPreferences
    lateinit var dbuilder: AlertDialog.Builder
    lateinit var alert: AlertDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = getSharedPreferences("configs", MODE_PRIVATE)
        if (ContextCompat.checkSelfPermission(this@ScanActivity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this@ScanActivity, arrayOf(Manifest.permission.CAMERA), 123)
        }
        dbuilder = AlertDialog.Builder(this)
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
            }
            dbuilder.apply {
                setTitle("Alamat server berhasil diubah")
                setMessage(code)
                setPositiveButton("TUTUP") { dialog, _ ->
                    dialog.dismiss()
                    mScannerView!!.resumeCameraPreview(this@ScanActivity)
                }
            }

            alert = dbuilder.create()
            alert.show()
        }else{
            val queue = Volley.newRequestQueue(this)
            val request = object: JsonObjectRequest(Method.POST, preferences.getString("url",""), null, { response ->
                dbuilder.apply {
                    setTitle("Kode Barang")
                    setMessage(response.getString("kode"))
                    setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                        mScannerView!!.resumeCameraPreview(this@ScanActivity)
                    }
                }

                alert = dbuilder.create()
                alert.show()
            }, {
                dbuilder.apply {
                    setTitle("Kesalahan")
                    setMessage("Tidak dapat terhubung ke server")
                    setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                        mScannerView!!.resumeCameraPreview(this@ScanActivity)
                    }
                }

                alert = dbuilder.create()
                alert.show()
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