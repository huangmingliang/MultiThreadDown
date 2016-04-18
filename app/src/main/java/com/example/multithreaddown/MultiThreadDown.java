package com.example.multithreaddown;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

/**
 * Description:
 * <br/>site: <a href="http://www.crazyit.org">crazyit.org</a>
 * <br/>Copyright (C), 2001-2014, Yeeku.H.Lee
 * <br/>This program is protected by copyright laws.
 * <br/>Program Name:
 * <br/>Date:
 * @author  Yeeku.H.Lee kongyeeku@163.com
 * @version  1.0
 */
public class MultiThreadDown extends Activity
{
	EditText url;
	EditText target;
	Button downBn;
	ProgressBar bar;
	DownUtil downUtil;
	private int mDownStatus;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		// ��ȡ��������е��������ؼ�
		url = (EditText) findViewById(R.id.url);
		target = (EditText) findViewById(R.id.target);
		downBn = (Button) findViewById(R.id.down);
		bar = (ProgressBar) findViewById(R.id.bar);
		// ����һ��Handler����
		final Handler handler = new Handler()
		{
			@Override
			public void handleMessage(Message msg)
			{
				if (msg.what == 0x123)
				{
					bar.setProgress(mDownStatus);
				}
			}
		};
		downBn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				// ��ʼ��DownUtil�������һ������ָ���߳���
				downUtil = new DownUtil(url.getText().toString(),
					target.getText().toString(), 6);
				new Thread()
				{
					@Override
					public void run()
					{
						try
						{
							// ��ʼ����
							downUtil.download();
						}
						catch (Exception e)
						{
							e.printStackTrace();
						}
						// ����ÿ����Ȼ�ȡһ��ϵͳ����ɽ��
						final Timer timer = new Timer();
						timer.schedule(new TimerTask()
						{
							@Override
							public void run()
							{
								// ��ȡ�����������ɱ���
								double completeRate = downUtil.getCompleteRate();
								mDownStatus = (int) (completeRate * 100);
								// ������Ϣ֪ͨ������½����
								handler.sendEmptyMessage(0x123);
								// ������ȫ��ȡ���������
								if (mDownStatus >= 100)
								{
									timer.cancel();
								}
							}
						}, 0, 100);
					}
				}.start();
			}
		});
	}
}