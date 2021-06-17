package jd.hd.seller.hd_shop_sdk.app.entity


class IsvLoginEntity {
    var code = "0"
    var msg: String? = null
    var cookieDTO: CookieDTO? = null

    class CookieDTO {
        var cookie_key: String? = null
        var cookie_value: String? = null
    }
}