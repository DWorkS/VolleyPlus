package com.volley.demo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.RxRequest;
import com.volley.demo.util.MyClass;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.android.volley.Request.Method.GET;


public class ExampleRxGsonRequest extends AppCompatActivity {

    private TextView mTvResult;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gson_request);

        mTvResult = (TextView) findViewById(R.id.tv_result);

        Button btnSimpleRequest = (Button) findViewById(R.id.btn_gson_request);
        btnSimpleRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Single.create(RxRequest.<MyClass>newRequest(ExampleRxGsonRequest.this)
                        .setRequestType(MyClass.class)
                        .setUrl("http://validate.jsontest.com/?json={'key':'value'}")
                        .setRequestMethod(GET)
                        .build())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new SingleObserver() {
                            @Override
                            public void onSubscribe(Disposable d) {
                                compositeDisposable.add(d);
                            }

                            @Override
                            public void onSuccess(Object response) {
                                MyClass myClass = (MyClass) response;
                                mTvResult.setText(Long.toString(myClass.mNanoseconds));
                            }

                            @Override
                            public void onError(Throwable e) {

                            }
                        });


            }
        });
    }

    @Override
    protected void onStop() {
        compositeDisposable.clear();
        super.onStop();
    }
}
