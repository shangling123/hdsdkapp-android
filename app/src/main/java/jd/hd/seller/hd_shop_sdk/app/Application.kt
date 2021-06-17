package jd.hd.seller.hd_shop_sdk.app

import androidx.multidex.MultiDexApplication
import jd.hd.hd_shop_sdk.HdShopSdk

class Application : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        HdShopSdk.init(applicationContext, AppConfig.venderId)
    }
}