package jd.hd.seller.hd_shop_sdk.app

interface NetworkRequestCallbackListener {
    fun onResponse(requestType: String, responseJson: String)

    fun onFailure(requestType: String, errMsg: String)
}