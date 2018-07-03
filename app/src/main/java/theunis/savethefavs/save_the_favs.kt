package theunis.savethefavs

import android.Manifest
import android.content.Context
import android.content.CursorLoader
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import kotlinx.android.synthetic.main.content_share_copy.*
import java.io.*

const val backup_dir = "/storage/emulated/0/DCIM_backup/"

class save_the_favs : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_save_the_favs)

    }
}

class receive_share : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle??) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share_copy)

        val intent = intent
        val action = intent.action
        val type = intent.type
        val r : Int = 0

        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            print("permission is not granted")
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    r)

        }
        val dir = File(backup_dir)
        if (!dir.exists()) {
            dir.mkdir()
        }

        if (Intent.ACTION_SEND_MULTIPLE == action && type != null) {
            val imageUris : ArrayList<Uri> = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
            if (imageUris != null) {
                progressBar.max = imageUris.size
                val realPaths = imageUris.map { uri -> getRealPathFromURI_API11to18(applicationContext, uri) }
                object : Thread() {
                    override fun run() {
                        for (path in realPaths) {
                            move_image(path!!, backup_dir)
                            try {
                                runOnUiThread { progressBar.progress = progressBar.progress + 1 }
                                Thread.sleep(100)
                            } catch (e: InterruptedException) {
                                e.printStackTrace()
                            }
                        }
                    }
                }.start()
            }
        }
    }

    private fun move_image(path: String, backup_dir: String) {
        val input_file = path?.substring(path?.lastIndexOf('/') + 1)
        val input_path = path?.substring(0, path?.lastIndexOf('/') + 1)
        moveFile(input_path!!, input_file!!, backup_dir)
    }
}

fun getRealPathFromURI_API11to18(context: Context, contentUri: Uri): String? {
    val proj = arrayOf(MediaStore.Images.Media.DATA)
    var result: String? = null

    val cursorLoader = CursorLoader(
            context,
            contentUri, proj, null, null, null)
    val cursor = cursorLoader.loadInBackground()

    if (cursor != null) {
        val column_index = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor!!.moveToFirst()
        result = cursor!!.getString(column_index)
    }
    return result
}


private fun moveFile(inputPath: String, inputFile: String, outputPath: String) {

    var inp: InputStream? = null
    var out: OutputStream? = null
    try {

        val dir = File(outputPath)
        if (!dir.exists()) {
            dir.mkdirs()
        }

        inp = FileInputStream(inputPath + inputFile)
        out = FileOutputStream(outputPath + inputFile)

        val buffer = ByteArray(1024)
        var read: Int
        read = inp!!.read(buffer)
        while ( read != -1) {
            out!!.write(buffer, 0, read)
            read = inp!!.read(buffer)
        }
        inp!!.close()
        inp = null

        // write the output file
        out!!.flush()
        out!!.close()
        out = null

        // delete the original file
        File(inputPath + inputFile).delete()


    } catch (fnfe1: FileNotFoundException) {
        Log.e("tag", fnfe1.message)
    } catch (e: Exception) {
        Log.e("tag", e.message)
    }

}

