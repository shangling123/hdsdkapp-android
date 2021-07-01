package jd.hd.seller.hd_shop_sdk.app.ui.login

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import jd.hd.sdk.host.app.R
import jd.hd.hd_shop_sdk.entity.EventMessage
import jd.hd.hd_shop_sdk.entity.EventMsgLogInfo
import jd.hd.seller.hd_shop_sdk.app.AppConfig
import jd.hd.seller.hd_shop_sdk.app.NetworkRequestCallbackListener
import jd.hd.seller.hd_shop_sdk.app.NetworkRequestType
import jd.hd.seller.hd_shop_sdk.app.entity.IsvLoginEntity
import jd.hd.seller.hd_shop_sdk.app.entity.OpenLoginEntity
import kotlinx.android.synthetic.main.activity_login.*
import okhttp3.*
import org.greenrobot.eventbus.EventBus
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class LoginActivity : Activity(), View.OnClickListener, NetworkRequestCallbackListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)
        login.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.login -> {
                loading.visibility = View.VISIBLE
                isvLogin(username.text.toString(), password.text.toString(), this)
            }
        }
    }

    private fun isvLogin(
            userName: String,
            password: String,
            isvNetworkRequestCallbackListener: NetworkRequestCallbackListener
    ) {
        val client = OkHttpClient()
        var jsonObject = JSONObject()
        jsonObject.put("userName", userName)
        jsonObject.put("passport", password)
        val body: RequestBody = FormBody.create(
                MediaType.parse("application/json"),
                jsonObject.toString()
        )
        val request: Request = Request.Builder()
                .url("https://mock-isv.jd.com/appShop/login")
                .post(body)
                .build()
        val call = client.newCall(request)
        call.enqueue(object : Callback {
            @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
            override fun onFailure(call: Call, e: IOException) {
                if (!isDestroyed) {
                    isvNetworkRequestCallbackListener.onFailure(
                            NetworkRequestType.HD_SHOP_SDK_NETWORK_REQUEST_TYPE_ISV_LOGIN,
                            e.message.toString()
                    )
                }
            }

            @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                if (!isDestroyed) {
                    val res = response.body()!!.string()
                    isvNetworkRequestCallbackListener.onResponse(
                            NetworkRequestType.HD_SHOP_SDK_NETWORK_REQUEST_TYPE_ISV_LOGIN,
                            res
                    )
                }
            }
        })
    }

    private fun openLogin(
            venderId: String,
            userName: String,
            networkRequestCallbackListener: NetworkRequestCallbackListener
    ) {
        val client = OkHttpClient()
        var jsonObject = JSONObject()
        jsonObject.put("venderId", venderId)
        jsonObject.put("userName", userName)

        val body: RequestBody = FormBody.create(
                MediaType.parse("application/json"),
                jsonObject.toString()
        )
        val request: Request = Request.Builder()
                .url("https://mock-isv.jd.com/appShop/openLogin")
                .post(body)
                .build()
        val call = client.newCall(request)
        call.enqueue(object : Callback {
            @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
            override fun onFailure(call: Call, e: IOException) {
                if (!isDestroyed) {
                    networkRequestCallbackListener.onFailure(
                            NetworkRequestType.HD_SHOP_SDK_NETWORK_REQUEST_TYPE_ISV_OPEN_LOGIN,
                            e.message.toString()
                    )
                }
            }

            @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                if (!isDestroyed) {
                    val res = response.body()!!.string()
                    networkRequestCallbackListener.onResponse(
                            NetworkRequestType.HD_SHOP_SDK_NETWORK_REQUEST_TYPE_ISV_OPEN_LOGIN,
                            res
                    )
                }
            }
        })
    }

    override fun onResponse(requestType: String, responseJson: String) {
        when (requestType) {
            NetworkRequestType.HD_SHOP_SDK_NETWORK_REQUEST_TYPE_ISV_LOGIN -> {
                try {
                    val isvLoginEntity = Gson().fromJson(responseJson, IsvLoginEntity::class.java)
                    if ("0" == isvLoginEntity.code) {
                        isvLoginEntity.cookieDTO?.cookie_value?.let {
                            AppConfig.userName = it
                            openLogin(AppConfig.venderId, AppConfig.userName, this)
                        }
                    } else {
                        runOnUiThread {
                            loading.visibility = View.GONE
                            //自主登录失败
                            Toast.makeText(
                                    this,
                                    "自主登录失败：${isvLoginEntity.msg}",
                                    Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } catch (var9: JSONException) {
                    runOnUiThread {
                        loading.visibility = View.GONE
                    }
                    var9.printStackTrace()
                }
            }

            NetworkRequestType.HD_SHOP_SDK_NETWORK_REQUEST_TYPE_ISV_OPEN_LOGIN -> {
                runOnUiThread {
                    loading.visibility = View.GONE
                }

                try {
                    val openLoginEntity = Gson().fromJson(responseJson, OpenLoginEntity::class.java)
                    openLoginEntity?.cookieDTO?.cookie_key?.let { itCookieKey ->
                        openLoginEntity.cookieDTO?.cookie_value?.let { itCookieValue ->
                            if (itCookieKey.isNotEmpty() && itCookieValue.isNotEmpty()) {
                                EventBus.getDefault()
                                        .post(
                                                EventMessage(
                                                        EventMessage.HD_SHOP_SDK_EVENT_MSG_TYPE_LOGIN_SUCCESS,
                                                        EventMsgLogInfo(itCookieKey, itCookieValue)
                                                )
                                        )
                            }
                        }
                    }

                } catch (var9: JSONException) {
                    var9.printStackTrace()
                }

                finish()
            }
        }
    }

    override fun onFailure(requestType: String, errMsg: String) {
        when (requestType) {
            NetworkRequestType.HD_SHOP_SDK_NETWORK_REQUEST_TYPE_ISV_LOGIN -> {
                loading.visibility = View.GONE
                Toast.makeText(this, "登录失败", Toast.LENGTH_SHORT).show()
            }
            NetworkRequestType.HD_SHOP_SDK_NETWORK_REQUEST_TYPE_ISV_OPEN_LOGIN -> {
                loading.visibility = View.GONE
                Toast.makeText(this, "账号打通失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
