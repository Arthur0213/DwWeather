package com.dw.dwweather.util;

public interface HttpCallbackListener {

	void onFinish(String response);

	void onError(Exception e);

}
