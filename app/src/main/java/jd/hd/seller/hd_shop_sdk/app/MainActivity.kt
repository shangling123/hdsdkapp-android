package jd.hd.seller.hd_shop_sdk.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import jd.hd.hd_shop_sdk.HdShopSdk
import jd.hd.hd_shop_sdk.app.R.layout
import jd.hd.hd_shop_sdk.entity.EventMessage
import jd.hd.hd_shop_sdk.entity.EventMsgLogInfo
import jd.hd.seller.hd_shop_sdk.app.entity.OpenLoginEntity
import jd.hd.seller.hd_shop_sdk.app.ui.login.LoginActivity
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException


class MainActivity : Activity(), NetworkRequestCallbackListener {
    var mContext: Context? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout.activity_main)
        EventBus.getDefault().register(this)

        mContext = this
        btnOpenShopHomePage.setOnClickListener {
            if (etVenderId.text.toString().isEmpty()) {
                Toast.makeText(this, "请输入VenderId", Toast.LENGTH_SHORT).show()
            } else {
                if (etVenderId.text.toString() != AppConfig.venderId) {
                    AppConfig.venderId = etVenderId.text.toString()
                    HdShopSdk.updateVenderId(AppConfig.venderId)
                }
                HdShopSdk.openShopHomePage(this)
            }
        }

        btnOpenProductDetailsPage.setOnClickListener {
            if (etVenderId.text.toString().isEmpty()) {
                Toast.makeText(this, "请输入店铺Id", Toast.LENGTH_SHORT).show()
            } else {
                if (etVenderId.text.toString() != AppConfig.venderId) {
                    AppConfig.venderId = etVenderId.text.toString()
                    HdShopSdk.updateVenderId(AppConfig.venderId)
                }
                if (etSkuId.text.toString().isNotEmpty()) {
                    HdShopSdk.openProductDetailsPage(this, etSkuId.text.toString())
                } else {
                    Toast.makeText(this, "请输入要跳转的sku", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnOutLogin.setOnClickListener {
            isvOutLogin(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public fun handleEvent(event: EventMessage) {
        when (event.type) {
            EventMessage.HD_SHOP_SDK_EVENT_MSG_TYPE_SHARE_DATA -> {
                Toast.makeText(
                    this,
                    "宿主APP收到需要分享的广播，分享内容如下：\r\n ${event.message?.shareData}",
                    Toast.LENGTH_LONG
                ).show()
            }
            EventMessage.HD_SHOP_SDK_EVENT_MSG_TYPE_LOGIN -> {
                if (rbCompletionLogin.isChecked) {
                    //TODO 该值应该时/appShop/login 接口中返回的cookie的value，此处用于模拟宿主APP已登录状态，故硬编码
                    AppConfig.userName = "selling-appA"
                    openLogin(AppConfig.venderId, AppConfig.userName, this)
                } else {
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                }
            }
            EventMessage.HD_SHOP_SDK_EVENT_MSG_SEL_PIC -> {
                Toast.makeText(this, "宿主APP收到需要打开相册的通知", Toast.LENGTH_LONG).show()
                openFileChooseProcess()
            }
        }
    }

    private fun openFileChooseProcess() {
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.addCategory(Intent.CATEGORY_OPENABLE)
        i.type = "image/*"
        startActivityForResult(
            Intent.createChooser(i, "test"),
            2002
        )
    }

    private fun openLogin(
        venderId: String,
        userName: String,
        networkRequestCallbackListener: NetworkRequestCallbackListener
    ) {
        val client = OkHttpClient()
        val jsonObject = JSONObject()
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

    private fun isvOutLogin(outNetworkRequestCallbackListener: NetworkRequestCallbackListener) {
        val client = OkHttpClient()
        val jsonObject = JSONObject()
        jsonObject.put("userName", "selling-appA")

        val body: RequestBody = FormBody.create(
            MediaType.parse("application/json"),
            jsonObject.toString()
        )
        val request: Request = Request.Builder()
            .url("https://mock-isv.jd.com/appShop/logout")
            .post(body)
            .build()
        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                outNetworkRequestCallbackListener.onFailure(
                    NetworkRequestType.HD_SHOP_SDK_NETWORK_REQUEST_TYPE_ISV_LOGOUT,
                    e.message.toString()
                )
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                val res = response.body()!!.string()
                outNetworkRequestCallbackListener.onResponse(
                    NetworkRequestType.HD_SHOP_SDK_NETWORK_REQUEST_TYPE_ISV_LOGOUT,
                    res
                )
            }
        })
    }

    private fun outOpenLogin(outNetworkRequestCallbackListener: NetworkRequestCallbackListener) {
        val client = OkHttpClient()
        val jsonObject = JSONObject()
        jsonObject.put("sessionInfo", HdShopSdk.getSessionInfo())

        val body: RequestBody = FormBody.create(
            MediaType.parse("application/json"),
            jsonObject.toString()
        )
        val request: Request = Request.Builder()
            .url("https://mock-isv.jd.com/appShop/openLogout")
            .post(body)
            .build()
        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                outNetworkRequestCallbackListener.onFailure(
                    NetworkRequestType.HD_SHOP_SDK_NETWORK_REQUEST_TYPE_ISV_OPEN_LOGOUT,
                    e.message.toString()
                )
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                val res = response.body()!!.string()
                outNetworkRequestCallbackListener.onResponse(
                    NetworkRequestType.HD_SHOP_SDK_NETWORK_REQUEST_TYPE_ISV_OPEN_LOGOUT,
                    res
                )
            }
        })
    }

    override fun onResponse(requestType: String, responseJson: String) {
        when (requestType) {
            NetworkRequestType.HD_SHOP_SDK_NETWORK_REQUEST_TYPE_ISV_LOGOUT -> {
                outOpenLogin(this)
            }

            NetworkRequestType.HD_SHOP_SDK_NETWORK_REQUEST_TYPE_ISV_OPEN_LOGOUT -> {
                HdShopSdk.loginOut()
                runOnUiThread {
                    Toast.makeText(this, "登出登录成功", Toast.LENGTH_SHORT).show()
                }
            }
            NetworkRequestType.HD_SHOP_SDK_NETWORK_REQUEST_TYPE_ISV_OPEN_LOGIN -> {
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
                runOnUiThread {
                    Toast.makeText(this, "宿主APP静默登录成功", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onFailure(requestType: String, errMsg: String) {
        when (requestType) {
            NetworkRequestType.HD_SHOP_SDK_NETWORK_REQUEST_TYPE_ISV_LOGOUT -> {
                runOnUiThread {
                    Toast.makeText(this, "ISV登出失败", Toast.LENGTH_SHORT).show()
                }
            }
            NetworkRequestType.HD_SHOP_SDK_NETWORK_REQUEST_TYPE_ISV_OPEN_LOGOUT -> {
                runOnUiThread {
                    Toast.makeText(this, "OpenLogin登出失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

