package jd.hd.seller.hd_shop_sdk.app

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import jd.hd.hd_shop_sdk.HdShopWebViewFragment
import jd.hd.sdk.host.app.R
import jd.hd.hd_shop_sdk.interfaces.HdSdkWebViewListener
import kotlinx.android.synthetic.main.activity_main_fragment.*

class MainActivityFragment : FragmentActivity() {
    private var mFragment: HdShopWebViewFragment? = null
    private var tvTile: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_fragment)
        tvTile = findViewById(R.id.tvTitle)
        var url = intent.getStringExtra("EXTRA_KEY_URL")
        /*mFragment = HdShopWebViewFragment.newInstance(
            "",
            ""
        )*/

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.placeholder, mFragment!!)
            .commitAllowingStateLoss()

        mFragment?.let {
            it.setChangeTitleToActivityListener(object : HdSdkWebViewListener {
                override fun changeTitle(title: String) {
                    tvTile?.text = title
                }
            })
        }

        ivTitleBack.setOnClickListener {
            mFragment?.let {
                if (it.getCanGoBack()) {
                    it.goBack()
                } else {
                    finish()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        mFragment?.onActivityResult(requestCode, resultCode, data)
    }


}