package com.picfix.tools.view.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.CountDownTimer
import android.view.View
import android.widget.*
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.picfix.tools.R
import com.picfix.tools.adapter.DataAdapter
import com.picfix.tools.bean.FileBean
import com.picfix.tools.bean.Price
import com.picfix.tools.bean.Resource
import com.picfix.tools.callback.DialogCallback
import com.picfix.tools.callback.PayCallback
import com.picfix.tools.config.Constant
import com.picfix.tools.controller.ImageManager
import com.picfix.tools.controller.LogReportManager
import com.picfix.tools.controller.PayManager
import com.picfix.tools.controller.WxManager
import com.picfix.tools.http.loader.OrderDetailLoader
import com.picfix.tools.http.response.ResponseTransformer
import com.picfix.tools.http.schedulers.SchedulerProvider
import com.picfix.tools.utils.AppUtil
import com.picfix.tools.utils.JLog
import com.picfix.tools.utils.ToastUtil
import com.picfix.tools.view.base.BaseActivity
import com.picfix.tools.view.views.AutoTextView
import com.picfix.tools.view.views.PaySuccessDialog
import com.picfix.tools.view.views.QuitDialog
import com.tencent.mmkv.MMKV
import kotlinx.android.synthetic.main.heart_small.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FixPayActivity : BaseActivity() {
    private lateinit var back: ImageView
    private lateinit var pay: Button
    private lateinit var userAgreement: AppCompatCheckBox
    private lateinit var wechatPay: AppCompatCheckBox
    private lateinit var aliPay: AppCompatCheckBox
    private lateinit var titleName: TextView

    private lateinit var firstLayout: FrameLayout
    private lateinit var secondLayout: FrameLayout
    private lateinit var thirdLayout: FrameLayout
    private lateinit var wxPayLayout: LinearLayout

    private lateinit var firstPriceView: TextView
    private lateinit var secondPriceView: TextView
    private lateinit var thirdPriceView: TextView

    private lateinit var discount: TextView
    private lateinit var discountWx: TextView
    private lateinit var menuBox: RecyclerView

    private var currentServiceId = 0
    private var firstServiceId = 0
    private var secondServiceId = 0
    private var thirdServiceId = 0
    private var checkItem = 0

    private var lastClickTime: Long = 0L

    private var mPrice = 0f
    private var firstPrice = 0f
    private var secondPrice = 0f
    private var thirdPrice = 0f
    private var serverTimes = 0

    private lateinit var firstDailyPrice: TextView
    private lateinit var secondDailyPrice: TextView
    private lateinit var thirdDailyPrice: TextView
    private lateinit var times: TextView

    private lateinit var counter: TextView
    private lateinit var counterTimer: CountDownTimer
    private lateinit var timer: CountDownTimer
    private lateinit var customerAgreement: TextView
    private lateinit var notice: AutoTextView

    private var remindTime = 15 * 60 * 1000L
    private var kv: MMKV? = MMKV.defaultMMKV()
    private var orderSn = ""
    private var startPay = false

    override fun setLayout(): Int {
        return R.layout.a_fix_pay
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun initView() {
        back = findViewById(R.id.iv_back)
        pay = findViewById(R.id.do_pay)
        wechatPay = findViewById(R.id.do_wechat_pay)
        aliPay = findViewById(R.id.do_alipay_pay)
        titleName = findViewById(R.id.pay_content)

        firstPriceView = findViewById(R.id.price)
        secondPriceView = findViewById(R.id.price2)
        thirdPriceView = findViewById(R.id.price3)
        times = findViewById(R.id.times)

        counter = findViewById(R.id.counter)
        notice = findViewById(R.id.tv_notice)
        customerAgreement = findViewById(R.id.customer_agreement)
        userAgreement = findViewById(R.id.user_agreement)
        discount = findViewById(R.id.discount)
        discountWx = findViewById(R.id.discount_wx)
        menuBox = findViewById(R.id.menu_box)

        firstLayout = findViewById(R.id.ll_1)
        secondLayout = findViewById(R.id.ll_2)
        thirdLayout = findViewById(R.id.ll_3)
        wxPayLayout = findViewById(R.id.layout_wx_pay)

        back.setOnClickListener { onBackPressed() }
        pay.setOnClickListener { checkPay(this) }
        firstLayout.setOnClickListener { chooseMenu(1) }
        secondLayout.setOnClickListener { chooseMenu(2) }
        thirdLayout.setOnClickListener { chooseMenu(3) }
        customerAgreement.setOnClickListener { toAgreementPage() }
        menuBox.setOnTouchListener { _, _ ->
            firstLayout.performClick()
            false
        }

        //??????????????????
        firstDailyPrice = findViewById(R.id.price_per_day)
        secondDailyPrice = findViewById(R.id.price_per_day2)
        thirdDailyPrice = findViewById(R.id.price_per_day3)


        //??????????????????
        wechatPay.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                aliPay.isChecked = false
            }
        }

        //?????????????????????
        aliPay.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                wechatPay.isChecked = false
            }
        }


        initNotice()
        initCounter()

    }

    override fun onResume() {
        super.onResume()
        if (startPay) {
            checkPayResult()
        }
    }

    override fun initData() {

        getServicePrice()
        loadMenuBox()
        chooseMenu(1)

    }

    private fun initNotice() {
        timer = object : CountDownTimer(4000 * 1000L, 4000) {
            override fun onFinish() {

            }

            override fun onTick(millisUntilFinished: Long) {
                val str = WxManager.getInstance(this@FixPayActivity).getRecoveryUser()
                notice.setText(str, Color.GRAY)
            }
        }

        timer.start()
    }

    private fun initCounter() {
        val result = kv?.decodeLong("fix_pay_counter")
        remindTime = if (result == 0L) 15 * 60 * 1000L else result!!

        counterTimer = object : CountDownTimer(remindTime, 100 / 6L) {
            override fun onFinish() {
                val text = AppUtil.timeStamp2Date("0", "mm:ss:SS")
                counter.text = text
                kv?.encode("pay_counter", 15 * 60 * 1000L)
            }

            override fun onTick(millisUntilFinished: Long) {
                val text = AppUtil.timeStamp2Date(millisUntilFinished.toString(), "mm:ss:SS")
                counter.text = text
                remindTime = millisUntilFinished
            }
        }
    }


    private fun loadMenuBox() {
        val list = arrayListOf<Resource>()
        list.add(Resource("4", R.drawable.iv_definition, "????????????"))
        list.add(Resource("1", R.drawable.iv_watermark, "????????????"))
        list.add(Resource("5", R.drawable.iv_cartoon, "????????????"))
        list.add(Resource("6", R.drawable.iv_face_merge, "????????????"))
        list.add(Resource("2", R.drawable.iv_matting, "????????????"))
        list.add(Resource("3", R.drawable.iv_colour, "????????????"))
        list.add(Resource("2", R.drawable.iv_format_trans, "????????????"))
        list.add(Resource("2", R.drawable.iv_zip, "????????????"))
        list.add(Resource("5", R.drawable.iv_contrast, "????????????"))
        list.add(Resource("5", R.drawable.iv_resize, "????????????"))
        list.add(Resource("5", R.drawable.iv_defogging, "????????????"))
        list.add(Resource("5", R.drawable.iv_style_trans, "????????????"))
        list.add(Resource("2", R.drawable.iv_age_trans, "????????????"))
        list.add(Resource("2", R.drawable.iv_gender_trans, "????????????"))
        list.add(Resource("2", R.drawable.iv_morph, "????????????"))
        list.add(Resource("2", R.drawable.iv_stretch, "????????????"))


        val mAdapter = DataAdapter.Builder<Resource>()
            .setData(list)
            .setLayoutId(R.layout.heart_small)
            .addBindView { itemView, itemData ->
                Glide.with(this).load(itemData.icon).into(itemView.iv_icon)
                itemView.tv_name.text = itemData.name
            }
            .create()

        menuBox.layoutManager = GridLayoutManager(this, 4)
        menuBox.adapter = mAdapter
        mAdapter.notifyItemRangeInserted(0, list.size)
    }


    private fun chooseMenu(index: Int) {
        when (index) {
            1 -> {
                firstLayout.background = ResourcesCompat.getDrawable(resources, R.drawable.background_gradient_stroke, null)
                secondLayout.background = ResourcesCompat.getDrawable(resources, R.drawable.pay_background_nomal, null)
                thirdLayout.background = ResourcesCompat.getDrawable(resources, R.drawable.pay_background_nomal, null)
                checkItem = 1
            }

            2 -> {
                firstLayout.background = ResourcesCompat.getDrawable(resources, R.drawable.pay_background_nomal, null)
                secondLayout.background = ResourcesCompat.getDrawable(resources, R.drawable.background_gradient_stroke, null)
                thirdLayout.background = ResourcesCompat.getDrawable(resources, R.drawable.pay_background_nomal, null)
                checkItem = 2
            }

            3 -> {
                firstLayout.background = ResourcesCompat.getDrawable(resources, R.drawable.pay_background_nomal, null)
                secondLayout.background = ResourcesCompat.getDrawable(resources, R.drawable.pay_background_nomal, null)
                thirdLayout.background = ResourcesCompat.getDrawable(resources, R.drawable.background_gradient_stroke, null)
                checkItem = 3
            }
        }

    }


    private fun toAgreementPage() {
        val intent = Intent()
        intent.setClass(this, AgreementActivity::class.java)
        startActivity(intent)
    }

    @SuppressLint("SetTextI18n")
    private fun getServicePrice() {

        PayManager.getInstance().getServiceList(this) {
            val packDetails = arrayListOf<Price>()
            for (child in it) {
                if (child.server_code == Constant.PHOTO_FIX) {
                    packDetails.add(child)
                }

                if (child.server_code == Constant.PHOTO_FIX_TIMES) {
                    packDetails.add(child)
                }
            }

            for (child in packDetails) {
                if (child.expire_type == "2") {
                    firstServiceId = child.id
                    firstPrice = child.sale_price.toFloat()
                    currentServiceId = firstServiceId
                }

                if (child.expire_type == "1") {
                    secondServiceId = child.id
                    secondPrice = child.sale_price.toFloat()
                }

                if (child.server_code == Constant.PHOTO_FIX_TIMES) {
                    thirdServiceId = child.id
                    thirdPrice = child.sale_price.toFloat()
                    serverTimes = child.server_times
                }
            }

            //????????????
            changeDescription()
        }
    }


    @SuppressLint("SetTextI18n")
    private fun changeDescription() {
        pay.visibility = View.VISIBLE

        firstPriceView.text = String.format("%.0f", firstPrice) + "?????????"
        secondPriceView.text = String.format("%.0f", secondPrice) + "?????????"
        thirdPriceView.text = String.format("%.1f", thirdPrice) + "????????????"

        times.text = "${serverTimes}??? | ${String.format("%.1f", thirdPrice)}???"

        firstDailyPrice.text = "0.01???/???"
        secondDailyPrice.text = String.format("%.2f", secondPrice / 365) + "???/???"
        thirdDailyPrice.text = String.format("%.2f", thirdPrice / serverTimes) + "???/???"

        counterTimer.start()

    }


    private fun checkPay(c: Activity) {
        if (!userAgreement.isChecked) {
            ToastUtil.show(this, "????????????????????????????????????")
            return
        }

        if (!wechatPay.isChecked && !aliPay.isChecked) {
            ToastUtil.show(this, "?????????????????????")
            return
        }

        if (lastClickTime == 0L) {
            lastClickTime = System.currentTimeMillis()
        } else if (System.currentTimeMillis() - lastClickTime < 2 * 1000) {
            ToastUtil.showShort(c, "???????????????????????????")
            return
        }

        lastClickTime = System.currentTimeMillis()

        when (checkItem) {
            1 -> {
                currentServiceId = firstServiceId
                mPrice = firstPrice
            }
            2 -> {
                currentServiceId = secondServiceId
                mPrice = secondPrice
            }
            3 -> {
                currentServiceId = thirdServiceId
                mPrice = thirdPrice
            }
            0 -> return
        }

        if (wechatPay.isChecked) {
            startPay = true
            doPay(c, 2)
        } else {
            startPay = false
            doPay(c, 1)
        }
    }

    /**
     *  index = 0???????????? 1??????????????? 2????????????
     */
    private fun doPay(c: Activity, index: Int) {
        when (index) {
            0 -> PayManager.getInstance().doFastPay(c, currentServiceId, object : PayCallback {
                override fun success() {
                }

                override fun progress(orderId: String) {
                    orderSn = orderId
                }

                override fun failed(msg: String) {
                    launch(Dispatchers.Main) {
                        ToastUtil.showShort(c, msg)
                    }
                }
            })

            1 -> PayManager.getInstance().doAliPay(c, currentServiceId, object : PayCallback {
                override fun success() {
                    launch(Dispatchers.Main) {

                        if (AppUtil.getChannelId() == Constant.CHANNEL_OPPO) {
                            launch(Dispatchers.IO) {
                                ImageManager.reportToOPPO(this@FixPayActivity, System.currentTimeMillis(), 7, (mPrice * 100).toInt())
                            }
                        }

                        ToastUtil.showShort(c, "????????????")

                        LogReportManager.logReport("???????????????", "????????????${mPrice}", LogReportManager.LogType.ORDER)

                        openPaySuccessDialog()
                    }
                }

                override fun progress(orderId: String) {
                    orderSn = orderId
                }

                override fun failed(msg: String) {
                    launch(Dispatchers.Main) {
                        ToastUtil.showShort(c, msg)
                    }
                }
            })

            2 -> PayManager.getInstance().doWechatPay(c, currentServiceId, object : PayCallback {
                override fun success() {
                }

                override fun progress(orderId: String) {
                    JLog.i("orderId = $orderId")
                    orderSn = orderId
                }

                override fun failed(msg: String) {
                }
            })
        }

    }

    private fun checkPayResult() {
        JLog.i("orderSn = $orderSn")
        if (orderSn == "") return
        launch(Dispatchers.IO) {
            OrderDetailLoader.getOrderStatus(orderSn)
                .compose(ResponseTransformer.handleResult())
                .compose(SchedulerProvider.getInstance().applySchedulers())
                .subscribe({
                    JLog.i("order_sn = ${it.order_sn}")
                    if (it.order_sn != orderSn) {
                        return@subscribe
                    }

                    when (it.status) {
                        "1" -> {

                            if (AppUtil.getChannelId() == Constant.CHANNEL_OPPO) {
                                launch(Dispatchers.IO) {
                                    ImageManager.reportToOPPO(this@FixPayActivity, System.currentTimeMillis(), 7, (mPrice * 100).toInt())
                                }
                            }

                            openPaySuccessDialog()

                            //??????????????????
                            ToastUtil.showShort(this@FixPayActivity, "????????????")

                            LogReportManager.logReport("???????????????", "????????????${mPrice}", LogReportManager.LogType.ORDER)
                        }

                        else -> {
                            ToastUtil.show(this@FixPayActivity, "?????????")
                        }
                    }

                }, {
                    ToastUtil.show(this@FixPayActivity, "????????????????????????")
                })
        }
    }

    private fun openPaySuccessDialog() {
        PaySuccessDialog(this@FixPayActivity, object : DialogCallback {
            override fun onSuccess(file: FileBean) {
                setResult(0x100)
                finish()
            }

            override fun onCancel() {
            }
        }).show()
    }


    override fun onBackPressed() {
        QuitDialog(this, getString(R.string.quite_title), object : DialogCallback {
            override fun onSuccess(file: FileBean) {
                finish()
            }

            override fun onCancel() {
            }
        }).show()
    }

    override fun onDestroy() {
        super.onDestroy()

        timer.cancel()
        counterTimer.cancel()

        if (kv != null && remindTime != 0L) {
            kv?.encode("fix_pay_counter", remindTime)
        }
    }


}