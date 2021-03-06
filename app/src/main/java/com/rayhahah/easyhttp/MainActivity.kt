package com.rayhahah.easyhttp

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import com.rayhahah.easyhttp.Util.FileUtils
import com.rayhahah.library.core.EClient
import com.rayhahah.library.core.EHttp
import com.rayhahah.library.core.Files
import com.rayhahah.library.http.TYPE
import com.sembozdemir.permissionskt.askPermissions
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.Call
import java.io.File
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    var maxCacheSize = 10 * 1024 * 1024
    val CODE_CHOOSE_PHOTO = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mInitClient.setOnClickListener {
            /**
             * 构建OkHttpClient
             * 使用这种方式构建的话，会直接配置成默认使用的OkHttpClient
             */
            EClient {
                //配置默认的baseUrl
                baseUrl = "http://mall.rayhahah.com/"
                //配置默认的请求类型
                type = TYPE.METHOD_POST
                timeUnit = TimeUnit.SECONDS
                connectTimeout = 10
                readTimeout = 10
                writeTimeout = 10
                interceptors()
                networkInterceptors()
                retryOnConnectionFailure = true
                cache = null
                //配置默认的解析器
                parser = null
                //配置全局通用的请求头
                header = {
                    "custom_head"("rayhahah")
                }
            }
        }

        /**
         * 普通Get请求
         */
        mGet.setOnClickListener {
            request(TYPE.METHOD_GET, "test", "1234567") { data: User ->
                mTvTest.setText(data.data.username)
            }

            /**
             * 下面是一种用于简单的Get请求
             */
//            var params = HashMap<String, String>()
//            EGet("url", params).go(success = {
//
//            }, failed = { call, exception ->
//            }, progress = { value, total ->
//
//            })

        }

        /**
         * 普通POST请求
         */
        mPost.setOnClickListener {
            EHttp {
                baseUrl = "http://mall.rayhahah.com/"
                src = "user/login.do"
                type = TYPE.METHOD_POST
                data = {
                    "username"("test")
                    "password"("1234567")
                }
                header = {
                    "cache-Control"("no-cache")
                }

            }.go<String> { data: String ->
                //直接返回json数据
                mTvTest.setText("data=$data")
            }

            /**
             * 下面一种是用于简单的Post请求
             */
//            var params = HashMap<String, String>()
//            EPost("url", params).go(success = {
//
//            }, failed = { call, exception ->
//            }, progress = { value, total ->
//
//            })
        }

        /**
         * 普通PUT请求
         */
        mPut.setOnClickListener {
            request(TYPE.METHOD_PUT, "admin", "1234567") { data: User ->
            }
        }

        /**
         * 普通DELETE请求
         */
        mDelete.setOnClickListener {
            request(TYPE.METHOD_DELETE, "admin", "1234567") { data: User ->
            }
        }

        /**
         * 上传单个文件请求
         */
        mPostFile.setOnClickListener {
            val context = this
            askPermissions(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) {
                onGranted {
                    val chooseIntent = Intent(Intent.ACTION_GET_CONTENT)
                    chooseIntent.type = "image/*"
                    context.startActivityForResult(chooseIntent, CODE_CHOOSE_PHOTO)
                }
            }
        }

        /**
         * 上传Json数据
         */
        mJson.setOnClickListener {
            EHttp {
                baseUrl = "http://mall.rayhahah.com/"
                src = "user/login.do"
                type = TYPE.METHOD_POST
                json = ""
                header = {
                    "cache-Control"("no-cache")
                }

            }.go { data: String ->

            }
        }


        /**
         * 下载文件数据
         */
        mDownload.setOnClickListener {
            EHttp {
                baseUrl = "http://thing.rayhahah.com/version/EasySport_1.1.4.apk"
                download = {
                    fileDir = FileUtils.getRootFilePath() + "EasyHttp/images"
                    fileName = "test.apk"
                }
            }.download({ data: File ->
                data.log()

            }, { call: Call, exception: Exception ->


            }, { value: Float, total: Long ->
                value.log()
                total.log()
            })

            /**
             *下面这种是简化了的下载请求
             */
//            EDownload("http://thing.rayhahah.com/version/EasySport_1.1.4.apk",
//                    FileUtils.getRootFilePath() + "EasyHttp/images",
//                    "test.apk",
//                    success = {
//
//                    },
//                    fail = { call: Call, e: Exception ->
//
//                    }, progress = { value, total ->
//            })

        }

        /**
         * RxJava的形式处理网络请求
         */
        mRx.setOnClickListener {
            rxRequest(TYPE.METHOD_POST, "test", "1234567")
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { t: String ->
                        t.log()
                        mTvTest.setText(t)

                    }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            CODE_CHOOSE_PHOTO -> if (data != null) {
                var path = FileUtils.getPathFromUri(this, data.data)
                val file = FileUtils.getFileByPath(path)
                requestFile("test", "1234567", file, { data: String ->
                    data.log()
                    mTvTest.setText(data)
                }, { call: Call, e: Exception ->
                    e.log()

                }, { value, total ->
                    value.log()
                    total.log()
                })
            }
            else -> {
            }
        }
    }


    fun request(method: String, username: String, password: String, success: (data: User) -> Unit) {
        EHttp {
            baseUrl = "http://mall.rayhahah.com/"
            src = "user/login.do"
            type = method
            data = {
                "username"(username)
                "password"(password)
            }
            header = {
                "cache-Control"("no-cache")
            }

        }.go<User>(success)
    }

    fun rxRequest(method: String, username: String, password: String): Observable<String> {
        return EHttp {
            baseUrl = "http://mall.rayhahah.com/"
            src = "user/login.do"
            type = method
            data = {
                "username"(username)
                "password"(password)
            }
            header = {
                "cache-Control"("no-cache")
            }

        }.rx(progress = { value, total -> })
    }


    fun requestFile(username: String, password: String, cover: File,
                    success: (data: String) -> Unit,
                    fail: (call: Call, e: Exception) -> Unit,
                    progress: (value: Float, total: Long) -> Unit) {
        EHttp {
            baseUrl = "http://mall.rayhahah.com/"
            src = "easysport/user/update_cover.do"
            type = TYPE.METHOD_POST
            data = {
                "username"(username)
                "password"(password)
                file = {
                    "upload_file"(Files.FILE_TYPE_MULTIPART, cover)
//                    val fileList = ArrayList<File>()
//                    fileList.add(File("1.txt"))
//                    fileList.add(File("2.txt"))
//                    fileList.add(File("3.txt"))
//                    "upload"(HttpFile(Files.FILE_TYPE_MULTIPART, fileList))
                }
            }
            header = {
                "cache-Control"("no-cache")
            }

        }.go(success, fail, progress)

    }

    /**
     * 获取缓存目录
     */
    fun getCachePathStr(): File {
        return if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            externalCacheDir
        } else {
            cacheDir
        }
    }
}
