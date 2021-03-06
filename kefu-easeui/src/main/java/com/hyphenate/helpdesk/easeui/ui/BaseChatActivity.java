package com.hyphenate.helpdesk.easeui.ui;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.WindowManager;

import com.hyphenate.chat.ChatClient;
import com.hyphenate.helpdesk.R;
import com.hyphenate.helpdesk.easeui.util.Config;

public class BaseChatActivity extends BaseActivity {
    protected ChatFragment chatFragment = null;
    protected String toChatUsername = null;

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        setContentView(R.layout.hd_activity_chat);
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        //IM服务号
        assert bundle != null;
        toChatUsername = bundle.getString(Config.EXTRA_SERVICE_IM_NUMBER);
        chatFragment = new ChatFragment();

        //是否开启机器人客服
        int robot = intent.getIntExtra("robot", 0);
        chatFragment.robotShow = robot != 1;

        //是否付费
        boolean isPay = intent.getBooleanExtra("isPay", false);
        if (isPay) {
            chatFragment.isPay = true;
        }

        // 传入参数
        chatFragment.setArguments(intent.getExtras());
        getSupportFragmentManager().beginTransaction().add(R.id.container, chatFragment).commit();
        ChatClient.getInstance().chatManager().bindChat(toChatUsername);

        //防止截屏录屏
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }

    @Override
    public void onBackPressed() {
        chatFragment.onBackPressed();
        super.onBackPressed();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // 点击notification bar进入聊天页面，保证只有一个聊天页面
        String username = intent.getStringExtra(Config.EXTRA_SERVICE_IM_NUMBER);
        if (toChatUsername.equals(username))
            super.onNewIntent(intent);
        else {
            finish();
            startActivity(intent);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}
