package com.rayhahah.library.core

import com.google.gson.reflect.TypeToken
import com.rayhahah.library.delegate.LambdaDelegate
import com.rayhahah.library.http.HttpFile
import com.rayhahah.library.http.HttpHeader
import com.rayhahah.library.http.TYPE
import com.rayhahah.library.parser.JsonParser
import com.rayhahah.library.parser.Parser
import com.rayhahah.library.service.RequestManager
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import okhttp3.Call
import okhttp3.OkHttpClient
import java.io.File
import java.util.*
import kotlin.collections.HashMap

/**
 * ┌───┐ ┌───┬───┬───┬───┐ ┌───┬───┬───┬───┐ ┌───┬───┬───┬───┐ ┌───┬───┬───┐
 * │Esc│ │ F1│ F2│ F3│ F4│ │ F5│ F6│ F7│ F8│ │ F9│F10│F11│F12│ │P/S│S L│P/B│ ┌┐    ┌┐    ┌┐
 * └───┘ └───┴───┴───┴───┘ └───┴───┴───┴───┘ └───┴───┴───┴───┘ └───┴───┴───┘ └┘    └┘    └┘
 * ┌──┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───────┐┌───┬───┬───┐┌───┬───┬───┬───┐
 * │~`│! 1│@ 2│# 3│$ 4│% 5│^ 6│& 7│* 8│( 9│) 0│_ -│+ =│ BacSp ││Ins│Hom│PUp││N L│ / │ * │ - │
 * ├──┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─────┤├───┼───┼───┤├───┼───┼───┼───┤
 * │Tab │ Q │ W │ E │ R │ T │ Y │ U │ I │ O │ P │{ [│} ]│ | \ ││Del│End│PDn││ 7 │ 8 │ 9 │   │
 * ├────┴┬──┴┬──┴┬──┴┬──┴┬──┴┬──┴┬──┴┬──┴┬──┴┬──┴┬──┴┬──┴─────┤└───┴───┴───┘├───┼───┼───┤ + │
 * │Caps │ A │ S │ D │ F │ G │ H │ J │ K │ L │: ;│" '│ Enter  │             │ 4 │ 5 │ 6 │   │
 * ├─────┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴────────┤    ┌───┐    ├───┼───┼───┼───┤
 * │Shift  │ Z │ X │ C │ V │ B │ N │ M │< ,│> .│? /│  Shift   │    │ ↑ │    │ 1 │ 2 │ 3 │   │
 * ├────┬──┴─┬─┴──┬┴───┴───┴───┴───┴───┴──┬┴───┼───┴┬────┬────┤┌───┼───┼───┐├───┴───┼───┤ E││
 * │Ctrl│Ray │Alt │         Space         │ Alt│code│fuck│Ctrl││ ← │ ↓ │ → ││   0   │ . │←─┘│
 * └────┴────┴────┴───────────────────────┴────┴────┴────┴────┘└───┴───┴───┘└───────┴───┴───┘
 *
 * @author Rayhahah
 * @blog http://rayhahah.com
 * @time 2018/2/28
 * @tips 这个类是Object的子类
 * @fuction
 */
class EasyHttp {

    var client: OkHttpClient? = null
    var baseUrl: String? = null
    var src: String = ""
    var type: String? = null

    var json: String? = null
    val params = Params()
    val downloadParams = Download()
     val head = HttpHeader(HashMap<String, String>())
    var header: HttpHeader.() -> Unit by LambdaDelegate<HttpHeader>(head)
    var data: Params.() -> Unit by LambdaDelegate<Params>(params)
    var download: Download.() -> Unit by LambdaDelegate<Download>(downloadParams)
    var parser: Parser? = null

    /**
     * 异步加载数据
     */
    inline fun <reified T : Any> go(noinline success: (data: T) -> Unit) {
        go<T>(success, { call, exception -> }, { value, total -> })
    }

    inline fun <reified T : Any> get(noinline success: (data: T) -> Unit) {
        get<T>(success, { call, exception -> }, { value, total -> })
    }

    inline fun <reified T : Any> get(noinline success: (data: T) -> Unit,
                                     noinline failed: (call: Call, exception: Exception) -> Unit,
                                     noinline progress: (value: Float, total: Long) -> Unit) {
        type = TYPE.METHOD_GET
        go<T>(success, { call, exception -> }, { value, total -> })
    }


    inline fun <reified T : Any> post(noinline success: (data: T) -> Unit) {
        post<T>(success, { call, exception -> }, { value, total -> })
    }

    inline fun <reified T : Any> post(noinline success: (data: T) -> Unit,
                                      noinline failed: (call: Call, exception: Exception) -> Unit,
                                      noinline progress: (value: Float, total: Long) -> Unit) {
        type = TYPE.METHOD_POST
        go<T>(success, { call, exception -> }, { value, total -> })
    }


    /**
     * 异步加载数据
     */
    inline fun <reified T : Any> go(noinline success: (data: T) -> Unit,
                                    noinline failed: (call: Call, exception: Exception) -> Unit,
                                    noinline progress: (value: Float, total: Long) -> Unit) {
        initParser<T>()
        val url = initUrl()
        initType()
        initHeader()
        when (type) {
            TYPE.METHOD_GET ->
                RequestManager.goGet<T>(client, url, head, params.data, parser, success, failed, progress)
            TYPE.METHOD_POST ->
                if (json != null) {
                    RequestManager.goJson<T>(client, url, head, type!!, json!!, parser, success, failed, progress)
                } else {
                    val paramsType = params.getType()
                    when (paramsType) {
                        Params.SINGLE_FILE -> {
                            val fileMap = HashMap<String, HttpFile>()
                            val fileList = ArrayList<File>()
                            fileList.add(params.files.DEFAULT!!)
                            fileMap.put("", HttpFile(Files.FILE_TYPE_FILE, fileList))
                            RequestManager.goFile<T>(client, url, head, params.data, fileMap, parser, success, failed, progress)
                        }
                        Params.LIST_FILE -> RequestManager.goFile<T>(client, url, head, params.data, params.files.data, parser, success, failed, progress)
                        else -> RequestManager.goForm<T>(client, url, head, type!!, params.data, parser, success, failed, progress)
                    }
                }
            TYPE.METHOD_PUT,
            TYPE.METHOD_DELETE ->
                if (json != null) {
                    RequestManager.goJson<T>(client, url, head, type!!, json!!, parser, success, failed, progress)
                } else {
                    RequestManager.goForm<T>(client, url, head, type!!, params.data, parser, success, failed, progress)
                }
        }
    }


    /**
     * 异步加载数据
     */
    fun download(success: (data: File) -> Unit) {
        download(success, { call, exception -> }, { value, total -> })
    }


    /**
     * 异步加载数据
     */
    fun download(success: (data: File) -> Unit
                 , failed: (call: Call, exception: Exception) -> Unit
                 , progress: (value: Float, total: Long) -> Unit) {
        val url = initUrl()
        RequestManager.goDownload<File>(client, url, head, downloadParams.fileDir, downloadParams.fileName, success, failed, progress)
    }


    inline fun <reified T> rx(noinline progress: (value: Float, total: Long) -> Unit = { v, t -> }): Observable<T> {
        initParser<T>()
        val url = initUrl()
        initType()
        initHeader()
        return when (type) {
            TYPE.METHOD_GET ->
                RequestManager.rxGet<T>(client, url, head, params.data, parser, progress)
            TYPE.METHOD_POST ->
                if (json != null) {
                    RequestManager.rxJson<T>(client, baseUrl + src, head, type!!, json!!, parser, progress)
                } else {
                    val paramsType = params.getType()
                    when (paramsType) {
                        Params.SINGLE_FILE -> {
                            val fileMap = HashMap<String, HttpFile>()
                            val fileList = ArrayList<File>()
                            fileList.add(params.files.DEFAULT!!)
                            fileMap.put("", HttpFile(Files.FILE_TYPE_FILE, fileList))
                            RequestManager.rxFile<T>(client, url, head, params.data, fileMap, parser, progress)
                        }
                        Params.LIST_FILE -> RequestManager.rxFile<T>(client, url, head, params.data, params.files.data, parser, progress)
                        else -> RequestManager.rxForm<T>(client, baseUrl + src, head, type!!, params.data, parser, progress)
                    }
                }
            TYPE.METHOD_PUT,
            TYPE.METHOD_DELETE ->
                if (json != null) {
                    RequestManager.rxJson<T>(client, url, head, type!!, json!!, parser, progress)
                } else {
                    RequestManager.rxForm<T>(client, url, head, type!!, params.data, parser, progress)
                }
            else -> {
                Observable.create<T> { emitter: ObservableEmitter<T> ->
                    emitter.onError(throw Throwable("Please Enter Right Type"))
                }
            }
        }
    }

    /**
     * 初始化请求类型
     */
    fun initType() {
        if (type == null) {
            type = RequestManager.type
        }
    }


    /**
     * 初始化请求头
     */
    fun initHeader() {
        val currentData = head.currentData()
        RequestManager.header.keys
                .filter { head[it] == null }
                .forEach {
                    val value: String? = RequestManager.header[it]
                    currentData.put(it, value!!)
                }
        head.updateData(currentData)
    }


    /**
     * 初始化baseUrl
     */
     fun initUrl(): String {
        return if (baseUrl != null) {
            baseUrl + src
        } else {
            return if (RequestManager.baseUrl != null) {
                RequestManager.baseUrl + src
            } else {
                throw Exception("Must have the baseUrl!!")
            }
        }
    }

    /**
     * 初始化数据解析类
     */
    inline fun <reified T> initParser() {
        if (parser == null) {
            parser = if (RequestManager.parser != null) {
                RequestManager.parser
            } else {
                val type = object : TypeToken<T>() {}.rawType
                JsonParser<T>(type, T::class.java.name)
            }
        }
    }

}
