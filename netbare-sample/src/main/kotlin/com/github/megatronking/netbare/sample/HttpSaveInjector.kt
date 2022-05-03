package com.github.megatronking.netbare.sample

import android.util.Log
import com.blankj.utilcode.util.FileIOUtils
import com.blankj.utilcode.util.SDCardUtils
import com.github.megatronking.netbare.NetBareUtils
import com.github.megatronking.netbare.http.HttpBody
import com.github.megatronking.netbare.http.HttpRequest
import com.github.megatronking.netbare.http.HttpResponse
import com.github.megatronking.netbare.injector.InjectorCallback
import com.github.megatronking.netbare.injector.SimpleHttpInjector
import com.github.megatronking.netbare.io.HttpBodyInputStream
import java.util.zip.GZIPInputStream

class HttpSaveInjector : SimpleHttpInjector() {

    companion object {
        const val TAG = "HttpSaveInjector"
        val FILE_SUFFIX = Regex.fromLiteral("[a-zA-Z]{2,10}")
    }

    override fun sniffResponse(response: HttpResponse): Boolean {
        return true;
    }

    override fun sniffRequest(request: HttpRequest): Boolean {
        return true;
    }

    override fun onRequestInject(request: HttpRequest, body: HttpBody, callback: InjectorCallback) {
        super.onRequestInject(request, body, callback)
        request.requestHeaders()["accept-encoding"] = listOf("gzip, deflate")
    }

    override fun onResponseInject(response: HttpResponse, body: HttpBody, callback: InjectorCallback) {
        super.onResponseInject(response, body, callback)
        var sb = StringBuffer(NetBareUtils.LINE_END);
        sb.append(response.method().name + " " + response.url() + NetBareUtils.LINE_END);
        for ((k, v) in response.requestHeaders()) {
            sb.append(k + ": " + v.joinToString(";") + NetBareUtils.LINE_END);
        }
        sb.append(NetBareUtils.LINE_END);
        sb.append("response:" + NetBareUtils.LINE_END);
        for ((k, v) in response.responseHeaders()) {
            sb.append(k + ": " + v.joinToString(";") + NetBareUtils.LINE_END);
        }
        try {
            var path = getSavePath(response);
            FileIOUtils.writeFileFromString("$path.header", sb.toString())
            var steam = HttpBodyInputStream(body);
            if (response.responseHeader("content-encoding").contains("gzip")) {
                FileIOUtils.writeFileFromIS(path, GZIPInputStream(steam), true);
            } else {
                FileIOUtils.writeFileFromIS(path, steam, true);
            }
        } catch (t: Throwable) {
            Log.e(TAG, "onResponseInject error", t);
        }
    }

    private fun getSavePath(response: HttpResponse): String {
        var reqPath = response.path();
        // 获取参数后缀
        var suffix = "";
        var dotLoc = reqPath.lastIndexOf('.');
        if (dotLoc > 0) {
            var temp = reqPath.substring(dotLoc);
            if (temp.matches(FILE_SUFFIX)) {
                suffix = temp;
            }
        }
        // 去除参数
        if (reqPath.contains('?')) {
            reqPath = reqPath.substring(0, reqPath.indexOf('?'));
        }
        // 组装文件名
        if (!reqPath.contains('.') && suffix.isNotBlank()) {
            reqPath = "$reqPath.$suffix";
        }
        if (reqPath.isBlank() || reqPath.equals("/")) {
            reqPath = "index.html"
        }
        dotLoc = reqPath.lastIndexOf('.');
        if (dotLoc > 0) {
            reqPath = reqPath.substring(0, dotLoc) + "_" + response.time() + reqPath.substring(dotLoc)
        } else {
            reqPath += response.time();
        }
        // 全路径
        var savePath = String.format("%s/Capture/%s/%s", SDCardUtils.getSDCardPathByEnvironment(), response.host(), reqPath);
        return savePath;
    }


}
