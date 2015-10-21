package com.example.testonly;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 * Created by sunxiaodi on 15/10/8.
 */
public class BeforeShowVideo extends Activity {

	private Button aliyun;
	private Button guoshui;
	private Button guoshui2;
	private Button hls;
	private Button rtsp;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.before_layout);
		aliyun = (Button) findViewById(R.id.aliyun);
		aliyun.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (true) {
					Intent in = new Intent(BeforeShowVideo.this,
							UniversalVideoViewActivity.class);
					in.putExtra("which", "1");
					in.putExtra("title", "阿里云视频");
					in.putExtra("url",
							"http://121.42.47.92/nsrxt_vedio/sds/A105000.mp4");
					startActivity(in);
				}

			}
		});
		guoshui = (Button) findViewById(R.id.guoshui);
		guoshui.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (true) {
					Intent in = new Intent(BeforeShowVideo.this,
							UniversalVideoViewActivity.class);
					in.putExtra("which", "2");
					in.putExtra("title", "国税局视频-域名");
					in.putExtra(
							"url",
							"http://www.qd-n-tax.gov.cn:8001/masvod/public/2015/09/23/20150923_14ff7c87c1b_r1030_1200k.mp4");
					startActivity(in);
				}

			}
		});

		guoshui2 = (Button) findViewById(R.id.guoshui2);
		guoshui2.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (true) {
					Intent in = new Intent(BeforeShowVideo.this,
							UniversalVideoViewActivity.class);
					in.putExtra("which", "2");
					in.putExtra("title", "国税局视频-IP");
					in.putExtra(
							"url",
							"http://222.173.123.246:8001/masvod/public/2015/09/23/20150923_14ff7c87c1b_r1030_1200k.mp4");
					startActivity(in);
				}

			}
		});

		hls = (Button) findViewById(R.id.hls);
		hls.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (true) {
					Intent in = new Intent(BeforeShowVideo.this,
							UniversalVideoViewActivity.class);
					in.putExtra("which", "2");
					in.putExtra("title", "苹果HLS协议视频");
					in.putExtra("url",
							"http://devimages.apple.com/iphone/samples/bipbop/bipbopall.m3u8");
					startActivity(in);
				}

			}
		});

		rtsp = (Button) findViewById(R.id.rtsp);
		rtsp.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (true) {
					Intent in = new Intent(BeforeShowVideo.this,
							UniversalVideoViewActivity.class);
					in.putExtra("which", "3");
					in.putExtra("title", "拱北口岸珠海过澳门大厅");
					in.putExtra("url",
							"rtsp://218.204.223.237:554/live/1/66251FC11353191F/e7ooqwcfbqjoo80j.sdp");
					startActivity(in);
				}

			}
		});
	}
}
