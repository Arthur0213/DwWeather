package com.dw.dwweather.activity;

import java.util.ArrayList;
import java.util.List;

import com.dw.dwweather.db.DwWeatherDB;
import com.dw.dwweather.model.City;
import com.dw.dwweather.model.County;
import com.dw.dwweather.model.Province;
import com.dw.dwweather.util.HttpCallbackListener;
import com.dw.dwweather.util.HttpUtil;
import com.dw.dwweather.util.Utility;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ChooseAreaActivity extends Activity {
	public static final int LEVEL_PROVINCE = 0;
	public static final int LEVEL_CITY = 1;
	public static final int LEVEL_COUNTY = 2;

	private ProgressDialog progressDialog;
	private TextView titleText;
	private ListView listView;
	private ArrayAdapter<String> adapter;
	private DwWeatherDB dwWeatherDB;
	private List<String> dataList = new ArrayList<String>();

	private List<Province> provinceList;
	private List<City> cityList;
	private List<County> countyList;

	private Province selectedProvince;
	private City selectedCity;

	private int currentLevel;
	
	private boolean isFromWeatherActivity;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		isFromWeatherActivity=getIntent().getBooleanExtra("from_weather_activity", false);
		
		SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(this);
		//已经选择了城市而且不是从weatherActivity跳转过来，才会直接跳转到Weather、Activity
		if(prefs.getBoolean("city_selected", false)&&!isFromWeatherActivity){
			Intent intent=new Intent(this, WeatherActivity.class);
			startActivity(intent);
			finish();
			return;
		}
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.choose_area);
		listView = (ListView) findViewById(R.id.list_view);
		titleText = (TextView) findViewById(R.id.title_text);
		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, dataList);
		listView.setAdapter(adapter);
		
		dwWeatherDB=DwWeatherDB.getInstance(this);
		
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int index,
					long arg3) {
				// TODO Auto-generated met
				if (currentLevel == LEVEL_PROVINCE) {
					selectedProvince = provinceList.get(index);
					queryCities();
				} else if (currentLevel == LEVEL_CITY) {
					selectedCity = cityList.get(index);
					queryCounties();
				}else if(currentLevel == LEVEL_COUNTY){
					String countyCode=countyList.get(index).getCountyCode();
					Intent intent=new Intent(ChooseAreaActivity.this, WeatherActivity.class);
					intent.putExtra("county_code", countyCode);
					startActivity(intent);
					finish();
				}
			}
		});
		queryProvinces();//加载省级数据
	}

	/**
	 * 查询全国所有的省份，优先从数据库查询，如果数据库上没有查询到再去服务器上查询
	 */
	private void queryProvinces() {
		// TODO Auto-generated method stub
		System.out.println("进入queryProvince方法");
		provinceList = dwWeatherDB.loadProvinces();
		System.out.println(provinceList.size()+"----------");
		if (provinceList.size() > 0) {
			dataList.clear();
			for (Province province : provinceList) {
				dataList.add(province.getProvinceName());
				System.out.println(dataList.toString());
			}
			adapter.notifyDataSetChanged();// 刷新
			listView.setSelection(0);
			titleText.setText("中国");
			currentLevel = LEVEL_PROVINCE;
		} else {
			queryFromServer(null, "province");
			System.out.println("服务器查询方法执行完毕");
		}
		System.out.println("queryProvince方法执行完毕");
	}

	/**
	 * 查询全国所有的市，优先从数据库查询，如果数据库上没有查询到再去服务器上查询
	 */

	private void queryCities() {
		// TODO Auto-generated method stub
		cityList = dwWeatherDB.loadCities(selectedProvince.getId());
		if (cityList.size() > 0) {
			dataList.clear();
			for (City city : cityList) {
				dataList.add(city.getCityName());
			}
			adapter.notifyDataSetChanged();// 刷新
			listView.setSelection(0);
			titleText.setText(selectedProvince.getProvinceName());
			currentLevel = LEVEL_CITY;
		} else {
			queryFromServer(selectedProvince.getProvinceCode(), "city");
		}

	}

	private void queryCounties() {
		// TODO Auto-generated method stub
		countyList = dwWeatherDB.loadCounties(selectedCity.getId());
		if (countyList.size() > 0) {
			dataList.clear();
			for (County county : countyList) {
				dataList.add(county.getCountyName());
			}
			adapter.notifyDataSetChanged();// 刷新
			listView.setSelection(0);
			titleText.setText(selectedProvince.getProvinceName());
			currentLevel = LEVEL_CITY;
		} else {
			queryFromServer(selectedCity.getCityCode(), "county");
		}
	}

	/**
	 * 根据传入的代号和类型从服务器上查询市县数据
	 * 
	 * @param object
	 * @param string
	 */
	private void queryFromServer(final String code, final String type) {
		// TODO Auto-generated method stub
		String address;
		if (!TextUtils.isEmpty(code)) {
			address = "http://www.weather.com.cn/data/list3/city" + code
					+ ".xml";
		} else {
			address = "http://www.weather.com.cn/data/list3/city.xml";
		}
		showProgressDialog();

		HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {

			@Override
			public void onFinish(String response) {
				// TODO Auto-generated method stub
				boolean result = false;
				if ("province".equals(type)) {
					result = Utility.handleProvincesResponse(dwWeatherDB,
							response);
				} else if ("city".equals(type)) {
					result = Utility.handleCitiesResponse(dwWeatherDB,
							response, selectedProvince.getId());
				} else if ("county".equals(type)) {
					result = Utility.handleCountiesResponse(dwWeatherDB,
							response, selectedCity.getId());
				}
				if (result) {
					// 通过runOnUiThread方法回到主线程处理逻辑
					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							// TODO Auto-generated method stub
							closeProgressDialog();
							if ("province".equals(type)) {
								queryProvinces();
							} else if ("city".equals(type)) {
								queryCities();
							} else if ("county".equals(type)) {
								queryCounties();
							}
						}
					});
				}
			}

			@Override
			public void onError(Exception e) {
				// TODO Auto-generated method stub
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						// TODO Auto-generated method stub
						closeProgressDialog();
						Toast.makeText(ChooseAreaActivity.this, "加载失败！",
								Toast.LENGTH_SHORT).show();
					}
				});
			}
		});
	}

	/**
	 * 显示加载进度
	 */
	private void showProgressDialog() {
		// TODO Auto-generated method stub
		if (progressDialog == null) {
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("正在玩命加载...");
			progressDialog.setCanceledOnTouchOutside(false);
		}
		progressDialog.show();

	}

	private void closeProgressDialog() {
		if (progressDialog != null) {
			progressDialog.dismiss();
		}
	}

	/**
	 * 根据当前的级别来判断是返回市列表、省列表还是直接退出
	 */
	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		if (currentLevel == LEVEL_COUNTY) {
			queryCities();
		} else if (currentLevel == LEVEL_CITY) {
			queryProvinces();
		} else {
			if(isFromWeatherActivity){
				Intent intent=new Intent(this, WeatherActivity.class);
				startActivity(intent);
			}
			finish();
		}
	}
}
