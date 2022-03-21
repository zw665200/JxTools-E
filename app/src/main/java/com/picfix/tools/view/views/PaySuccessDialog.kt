package com.picfix.tools.view.views

import android.app.Activity
import android.app.Dialog
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import com.picfix.tools.R
import com.picfix.tools.bean.FileBean
import com.picfix.tools.callback.DialogCallback
import com.picfix.tools.utils.AppUtil


class PaySuccessDialog(private val activity: Activity, callback: DialogCallback) : Dialog(activity, R.style.app_dialog) {
    private lateinit var agree: TextView
    private lateinit var layout: FrameLayout
    private var mCallback = callback

    init {
        initVew()
    }

    private fun initVew() {
        val dialogContent = LayoutInflater.from(activity).inflate(R.layout.d_pay_success, null)
        setContentView(dialogContent)
        setCancelable(false)

        agree = findViewById(R.id.success_ok)
        layout = findViewById(R.id.fl_pay)

        agree.setOnClickListener {
            mCallback.onSuccess(FileBean("", "", "", 0))
        }

    }


    override fun show() {
        window!!.decorView.setPadding(0, 0, 0, 0)
        window!!.attributes = window!!.attributes.apply {
            gravity = Gravity.CENTER
            width = AppUtil.getScreenWidth(context) * 3 / 4
            height = WindowManager.LayoutParams.WRAP_CONTENT
        }
        super.show()
    }
}