package com.xinke.edu.Appointment;


import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.icu.util.Calendar;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.release.alert.Alert;
import com.scwang.smart.refresh.footer.ClassicsFooter;
import com.scwang.smart.refresh.header.ClassicsHeader;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnLoadMoreListener;
import com.scwang.smart.refresh.layout.listener.OnRefreshListener;
import com.xinke.edu.Appointment.Adapter.ClassRoomAdapter;
import com.xinke.edu.Appointment.Adapter.CounselorAdapter;
import com.xinke.edu.Appointment.LoaringDialog.LoadingDialog;
import com.xinke.edu.Appointment.entity.Classrooms;
import com.xinke.edu.Appointment.entity.Result;
import com.xinke.edu.Appointment.net.RetrofitApi;
import com.xinke.edu.Appointment.token.AuthTokenInterceptor;
import com.xinke.edu.Appointment.token.SPUtils;
import com.xinke.edu.Appointment.token.TokenHeaderInterceptor;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;


/**
 * A simple {@link Fragment} subclass.
 */
public class BlankFragment1 extends Fragment implements ClassRoomAdapter.OnReservationSuccessListener {

    /*初始化下拉框*/
    private Spinner spinnerBuilding;
    private Spinner spinnerFloor;
    private Spinner spinnerPeriod;

    /*默认系统时间*/
    String formattedTime;

    /*初始化视图*/
    RecyclerView recyclerView;

    ClassRoomAdapter classGouAdapter;


    /*时间选择器*/
    private Button btnPickDate;
    private Calendar selectedDate;

    /*给后端传值类型*/
    String buildingStr;
    int floorStr;
    String periodStr;
    String timeStr;

    /*设置用户显示显示布局*/
    TextView tvSelectedBuilding;
    TextView tvSelectedFloor;
    TextView tvSelectedPeriod;
    TextView tvSelectedDate;

    /*token*/
    String settoken;

    /*实例化对象*/
    Classrooms classrooms;


    /*布局的id*/
    LinearLayout inquire, selectLayout;

    /**
     * 刷新数据
     */
    boolean blean;

    /*当天的日期*/
    String datetime;

    public BlankFragment1() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_blank_fragment1, null, false);

        /*下拉刷新样式*/
        RefreshLayout refreshLayout = (RefreshLayout) view.findViewById(R.id.refreshLayout);
        refreshLayout.setRefreshHeader(new ClassicsHeader(getContext()));
        refreshLayout.setRefreshFooter(new ClassicsFooter(getContext()));
        refreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(RefreshLayout refreshlayout) {
                refreshlayout.finishRefresh(2000/*,false*/);//传入false表示刷新失败
                GetData();

            }
        });
        refreshLayout.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore(RefreshLayout refreshlayout) {
                boolean boole = GetData();
                if (boole) {
                } else {
                }
                refreshlayout.finishLoadMore(boole);//传入false表示加载失败

            }
        });



        /*获取token*/
        settoken = (String) SPUtils.get(getContext(), "token", "");

        //获取三个下拉框的id
        spinnerBuilding = view.findViewById(R.id.spinnerBuilding);
        spinnerFloor = view.findViewById(R.id.spinnerFloor);
        spinnerPeriod = view.findViewById(R.id.spinnerPeriod);

        /*获取用户选择信息的id*/
        tvSelectedBuilding = view.findViewById(R.id.tvSelectedBuilding);
        tvSelectedFloor = view.findViewById(R.id.tvSelectedFloor);
        tvSelectedPeriod = view.findViewById(R.id.tvSelectedPeriod);
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate);


        /*实例化适配器*/
        recyclerView = view.findViewById(R.id.recyclerView);

        /*把布局划分成左右两边的代码，后面的2就是分成几份*/
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 1));

        /*帮助类实例化*/
        classGouAdapter = new ClassRoomAdapter(new ArrayList<>());
        classGouAdapter.setContext(getContext());

        /*把实例化的帮助类类传入布局*/
        recyclerView.setAdapter(classGouAdapter);


        // 初始化 Spinner  获取下拉框的数据
        initSpinners();




        /*点击查询空闲教室*/
        inquire = view.findViewById(R.id.inquire);
        inquire.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GetData();
            }
        });

        /*点击查看我的预约*/
        selectLayout = view.findViewById(R.id.selectLayout);
        selectLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), MyreservationActivity.class);
                startActivity(intent);
            }
        });





        /*点击获取时间选择器的方法*/
        btnPickDate = view.findViewById(R.id.btnPickDate);
        btnPickDate.setText(datetime);

        btnPickDate.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);

        // 设置按钮点击事件，弹出日期选择器
        btnPickDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDatePickerDialog();
            }
        });


        //调用监听器回调
        setupAdapter();

        return view;

    }

    //设置监听器
    private void setupAdapter() {
        classGouAdapter = new ClassRoomAdapter(new ArrayList<>());
        classGouAdapter.setOnReservationSuccessListener(this);
        classGouAdapter.setContext(getContext());
        recyclerView.setAdapter(classGouAdapter);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 1));
    }


    /*默认加载下拉选择器的方法*/
    private void initSpinners() {

        /*获取当天日期*/
        datetime = CurrentTime();
        tvSelectedDate.setText("日期:" + datetime);


        // 设置教学楼名称 Spinner
        ArrayAdapter<CharSequence> buildingAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.building_names, android.R.layout.simple_spinner_item);
        buildingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBuilding.setAdapter(buildingAdapter);

        // 设置楼层 Spinner
        ArrayAdapter<CharSequence> floorAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.floor_numbers, android.R.layout.simple_spinner_item);
        floorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFloor.setAdapter(floorAdapter);


        // 设置节数 Spinner
        ArrayAdapter<CharSequence> periodAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.period_numbers, android.R.layout.simple_spinner_item);
        periodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPeriod.setAdapter(periodAdapter);


        // 设置选择教学楼 Spinner 的选项选择事件
        spinnerBuilding.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedBuilding = parent.getItemAtPosition(position).toString();

                buildingStr = selectedBuilding;
                tvSelectedBuilding.setText("楼名: " + buildingStr);

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // 设置选择教室楼层 Spinner 的选项选择事件
        spinnerFloor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // 获取选中的楼层
                String selectedFloorString = parent.getItemAtPosition(position).toString();
                String floor = selectedFloorString;
                floorStr = Integer.parseInt(floor);

                tvSelectedFloor.setText("楼层: " + floorStr);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // 设置选择预约节数 Spinner 的选项选择事件
        spinnerPeriod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // 获取选中的预约节数
                String selectedPeriodString = parent.getItemAtPosition(position).toString();
                periodStr = selectedPeriodString;
                tvSelectedPeriod.setText("节数: " + periodStr);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });


    }


    /*时间选择器的方法*/
    private void showDatePickerDialog() {
        // 获取当前日期
        Calendar selectedDate = Calendar.getInstance();

        int year = selectedDate.get(Calendar.YEAR);
        int month = selectedDate.get(Calendar.MONTH);
        int day = selectedDate.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        // 更新选定的日期
                        selectedDate.set(Calendar.YEAR, year);
                        selectedDate.set(Calendar.MONTH, month);
                        selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                        // 获取的日期文本用一个字符串保存起来
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        String formattedDate = sdf.format(selectedDate.getTime());
                        timeStr = formattedDate;
                        tvSelectedDate.setText("日期:" + timeStr);
                        btnPickDate.setText(timeStr);


                    }
                }, year, month, day);

        // 显示日期选择器对话框
        datePickerDialog.show();
    }

    /*okhttp拦截器*/
    private OkHttpClient.Builder getClient() {
        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        httpClientBuilder.connectTimeout(15, TimeUnit.SECONDS);

        //token拦截器
        httpClientBuilder.addNetworkInterceptor(new TokenHeaderInterceptor(getContext()));

        // 添加状态码拦截器
        httpClientBuilder.addInterceptor(new AuthTokenInterceptor(getContext()));

        return httpClientBuilder;
    }


    /*获取系统当前时间*/
    public String CurrentTime() {
        // 获取当前时间
        Date currentTime = new Date();

        // 创建SimpleDateFormat对象，指定日期时间格式
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // 格式化当前时间
        formattedTime = sdf.format(currentTime);

        return formattedTime;
    }


    @Override
    public void onReservationSuccess() {
        //重新请求数据
        GetData();
    }


    /*后端获取数据的方法*/
    public boolean GetData() {
        /*获取网络*/
        Retrofit retrofit = new Retrofit
                .Builder()
                .client(getClient().build())
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(RetrofitApi.BaseUrl)
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();



        /*实例化*/
        RetrofitApi retrofitApi = retrofit.create(RetrofitApi.class);

        classrooms = new Classrooms();
        classrooms.setBuilding(buildingStr);

        classrooms.setFloor(floorStr);

        classrooms.setPeriod(periodStr);
        if (timeStr == null) {
            classrooms.setTime(datetime);
        } else {
            classrooms.setTime(timeStr);
        }


        retrofitApi.queryclassroom(classrooms, settoken)
                .observeOn(Schedulers.io())
                .timeout(10, TimeUnit.SECONDS) // 设置超时时间为10秒
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Result<List<Classrooms>>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        //加载动画
                        LoadingDialog.getInstance(getContext()).show();
                    }

                    @Override
                    public void onNext(@NonNull Result<List<Classrooms>> listResult) {
                        List<Classrooms> data = listResult.getData();
                        if (data != null && data.size() != 0) {


                            /*一共获取到的数据*/
                            int size = data.size();

                            // 获取第一条数据
                            Classrooms firstClassroom = data.get(0);


                            SPUtils.put(getContext(), "buildingStr", buildingStr);
                            SPUtils.put(getContext(), "floorStr", floorStr);

                            // 如果用户选择了时间，则保存用户选择的时间，否则保存当前日期
                            if (timeStr != null) {
                                SPUtils.put(getContext(), "timeStr", timeStr);
                            } else {
                                SPUtils.put(getContext(), "datetime", formattedTime);
                            }
                            /*保存要预约的节数*/
                            SPUtils.put(getContext(), "periodStr", periodStr);


                            // 更新适配器的数据集合
                            classGouAdapter.replaceData(listResult.getData());

                            Toast.makeText(getContext(), "查询成功", Toast.LENGTH_SHORT).show();

                            LoadingDialog.getInstance(getContext()).hide();//隐藏加载窗
                        } else {
                            new Alert(getContext())
                                    .builder(Alert.Type.PROGRESS)
                                    .setProgressText("没有检索到符合你查询的教室，请重新选择吧，轻触重试！")
                                    .show();
                            blean = false;
                            return;
                        }


                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        e.printStackTrace();
                        // 发生错误时的操作
                        if (e instanceof TimeoutException) {
                            // 请求超时
                            LoadingDialog.getInstance(getContext()).hide();//隐藏
                            requireActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getContext(), "服务器请求超时", Toast.LENGTH_SHORT).show();
                                    blean = false;
                                }
                            });
                        } else {
                            // 其他服务器错误
                            Log.e("onError", e.toString());
                            requireActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getContext(), "网络不给力噢，请检查你的网络设置~", Toast.LENGTH_SHORT).show();
                                    blean = false;
                                }
                            });
                        }
                    }

                    @Override
                    public void onComplete() {
                        LoadingDialog.getInstance(getContext()).hide();//隐藏
                    }
                });

        Log.d("blean", blean + "");
        return blean;

    }

}


