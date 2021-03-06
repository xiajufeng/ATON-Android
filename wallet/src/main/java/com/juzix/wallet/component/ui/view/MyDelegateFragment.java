package com.juzix.wallet.component.ui.view;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jakewharton.rxbinding2.view.RxView;
import com.juzhen.framework.util.NumberParserUtils;
import com.juzix.wallet.App;
import com.juzix.wallet.R;
import com.juzix.wallet.app.Constants;
import com.juzix.wallet.app.CustomObserver;
import com.juzix.wallet.component.adapter.MyDelegateAdapter;
import com.juzix.wallet.component.ui.base.MVPBaseFragment;
import com.juzix.wallet.component.ui.contract.MyDelegateContract;
import com.juzix.wallet.component.ui.dialog.BaseDialogFragment;
import com.juzix.wallet.component.ui.dialog.CommonGuideDialogFragment;
import com.juzix.wallet.component.ui.presenter.MyDelegatePresenter;
import com.juzix.wallet.component.widget.CustomRefreshHeader;
import com.juzix.wallet.config.AppSettings;
import com.juzix.wallet.entity.DelegateInfo;
import com.juzix.wallet.entity.GuideType;
import com.juzix.wallet.entity.WebType;
import com.juzix.wallet.event.Event;
import com.juzix.wallet.event.EventPublisher;
import com.juzix.wallet.utils.AmountUtil;
import com.juzix.wallet.utils.BigDecimalUtil;
import com.juzix.wallet.utils.LanguageUtil;
import com.juzix.wallet.utils.RxUtils;
import com.juzix.wallet.utils.StringUtil;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.listener.OnRefreshListener;
import com.umeng.analytics.MobclickAgent;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.Flowable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;


/**
 * 我的委托页面
 */

public class MyDelegateFragment extends MVPBaseFragment<MyDelegatePresenter> implements MyDelegateContract.View {

    private Unbinder unbinder;

    @BindView(R.id.refreshLayout)
    SmartRefreshLayout refreshLayout;
    @BindView(R.id.iv_total_delegate)
    ImageView iconDelegate;
    @BindView(R.id.tv_total_delegate)
    TextView tv_total_delegate;
    @BindView(R.id.tv_delegate_record)
    TextView tv_delegate_record;
    @BindView(R.id.ll_no_data)
    LinearLayout ll_no_data;
    @BindView(R.id.list_delegate)
    RecyclerView list_delegate;
    @BindView(R.id.ll_problem)
    LinearLayout ll_problem;
    @BindView(R.id.ll_tutorial)
    LinearLayout ll_tutorial;
    @BindView(R.id.ll_guide)
    LinearLayout ll_guide;
    @BindView(R.id.tv_no_data_tips)
    TextView tv_no_data_tips;

    private MyDelegateAdapter mMyDelegateAdapter;
    private LinearLayoutManager layoutManager;
    List<DelegateInfo> datalist;

    @Override
    protected MyDelegatePresenter createPresenter() {
        return new MyDelegatePresenter(this);
    }

    @Override
    protected void onFragmentPageStart() {
        refreshLayout.autoRefresh();
    }

    @Override
    protected View onCreateFragmentPage(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_delegate, container, false);
        unbinder = ButterKnife.bind(this, view);
        EventPublisher.getInstance().register(this);
        initViews();
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initClickListener();
    }

    private void initClickListener() {

        RxView.clicks(tv_delegate_record)
                .compose(RxUtils.bindToLifecycle(this))
                .compose(RxUtils.getClickTransformer())
                .subscribe(new CustomObserver<Object>() {
                    @Override
                    public void accept(Object o) {
                        DelegateRecordActivity.actionStart(getContext());
                    }
                });
        RxView.clicks(ll_problem).compose(RxUtils.bindToLifecycle(this))
                .compose(RxUtils.getClickTransformer())
                .subscribe(new CustomObserver<Object>() {
                    @Override
                    public void accept(Object o) {
                        CommonHybridActivity.actionStart(getContext(), getResources().getString(R.string.web_url_common_delegate_problem), WebType.WEB_TYPE_COMMON);
                    }
                });

        RxView.clicks(ll_tutorial).compose(RxUtils.bindToLifecycle(this))
                .compose(RxUtils.getClickTransformer())
                .subscribe(new CustomObserver<Object>() {
                    @Override
                    public void accept(Object o) {
                        CommonHybridActivity.actionStart(getContext(), getResources().getString(R.string.web_url_tutorial), WebType.WEB_TYPE_COMMON);
                    }
                });

        RxView.clicks(ll_no_data).compose(RxUtils.bindToLifecycle(this))
                .compose(RxUtils.getClickTransformer())
                .subscribe(new CustomObserver<Object>() {
                    @Override
                    public void accept(Object o) {
                        //没数据，跳转到验证节点页面
                        DelegateFragment delegateFragment = (DelegateFragment) MyDelegateFragment.this.getParentFragment();
                        delegateFragment.setFragment2Fragment(new DelegateFragment.Fragment2Fragment() {
                            @Override
                            public void gotoFragment(ViewPager viewPager) {
                                viewPager.setCurrentItem(1);
                            }
                        });
                        delegateFragment.forSkip();
                    }
                });

    }


    private void initViews() {
        initColor();
        initRefreshView();
        layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mMyDelegateAdapter = new MyDelegateAdapter(datalist);
        list_delegate.setLayoutManager(layoutManager);
        list_delegate.setAdapter(mMyDelegateAdapter);

        list_delegate.setEnabled(true);
        mMyDelegateAdapter.setOnItemClickListener(new MyDelegateAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(DelegateInfo delegateInfo) {
                DelegateDetailActivity.actionStart(getContext(), delegateInfo);
            }
        });
        initGuide();
    }

    private void initGuide() {
        if (!AppSettings.getInstance().getMyDelegateBoolean()) {
            CommonGuideDialogFragment.newInstance(GuideType.DELEGATE_LIST).setOnDissmissListener(new BaseDialogFragment.OnDissmissListener() {
                @Override
                public void onDismiss() {
                    AppSettings.getInstance().setMyDelegateBoolean(true);
                }
            }).show(getActivity().getSupportFragmentManager(), "showGuideDialogFragment");
        }
    }

    private void initColor() {
        String str = null;
        if (Locale.CHINESE.getLanguage().equals(LanguageUtil.getLocale(App.getContext()).getLanguage())) {
            str = "<font color='#61646E'>查看验证节点，</font>"
                    + "<font color= '#105CFE'>参与委托</font>";
        } else {
            str = "<font color='#61646E'>View all validators,</font>"
                    + "<font color= '#105CFE'>Delegate</font>";
        }

        tv_no_data_tips.setText(Html.fromHtml(str));
    }

    private void initRefreshView() {
        refreshLayout.setRefreshHeader(new CustomRefreshHeader(getContext()));
        refreshLayout.setEnableLoadMore(false);//禁止上拉加载更多
        refreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(@NonNull RefreshLayout refreshLayout) {
                mPresenter.loadMyDelegateData();
            }
        });
    }


    @Override
    public void showMyDelegateData(List<DelegateInfo> list) {
        if (list != null && list.size() > 0) {
            ll_no_data.setVisibility(View.GONE);
        } else {
            ll_no_data.setVisibility(View.VISIBLE);
        }
        mMyDelegateAdapter.notifyDataChanged(list);
        showTotal(list);
        refreshLayout.finishRefresh();
    }

    private void showTotal(List<DelegateInfo> list) {

        BigDecimal totalDelegateAmount = getTotalDelegateAmount(list);

        tv_total_delegate.setText(totalDelegateAmount.compareTo(BigDecimal.ZERO) > 0 ? getString(R.string.amount_with_unit, StringUtil.formatBalance(AmountUtil.convertVonToLat(totalDelegateAmount.toPlainString(), 8), false)) : "— —");
    }

    private BigDecimal getTotalDelegateAmount(List<DelegateInfo> list) {

        return Flowable.fromIterable(list)
                .map(new Function<DelegateInfo, String>() {
                    @Override
                    public String apply(DelegateInfo delegateInfo) throws Exception {
                        return delegateInfo.getDelegated();
                    }
                })
                .map(new Function<String, BigDecimal>() {
                    @Override
                    public BigDecimal apply(String s) throws Exception {
                        return BigDecimalUtil.toBigDecimal(s);
                    }
                })
                .reduce(new BiFunction<BigDecimal, BigDecimal, BigDecimal>() {
                    @Override
                    public BigDecimal apply(BigDecimal bigDecimal, BigDecimal bigDecimal2) throws Exception {
                        return bigDecimal.add(bigDecimal2);
                    }
                })
                .defaultIfEmpty(BigDecimal.ZERO)
                .onErrorReturnItem(BigDecimal.ZERO)
                .blockingGet();
    }


    @Override
    public void showMyDelegateDataFailed() {
        ll_no_data.setVisibility(View.VISIBLE);
        tv_total_delegate.setText("--");
        refreshLayout.finishRefresh();
//        mMyDelegateAdapter.notifyDataSetChanged();
        mMyDelegateAdapter.notifyDataChanged(null);
    }


    //接收event事件然后刷新
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refreshMyDelegate(Event.UpdateDelegateTabEvent tabEvent) {
        if (AppSettings.getInstance().getMydelegateTab()) {
            refreshLayout.autoRefresh();
        }

    }

    //接收tab切换到当前页面的时候，刷新
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refreshTabChange(Event.UpdateTabChangeEvent event) {
        refreshLayout.autoRefresh();
    }

    @Override
    public void onTabHidden() {
        super.onTabHidden();
        MobclickAgent.onPageEnd(Constants.UMPages.MY_DELEGATION);
    }

    @Override
    public void onTabShown() {
        super.onTabShown();
        MobclickAgent.onPageStart(Constants.UMPages.MY_DELEGATION);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        if (isVisibleToUser) {
            AppSettings.getInstance().setMydelegateTab(true);
        } else {
            AppSettings.getInstance().setMydelegateTab(false);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (unbinder != null) {
            unbinder.unbind();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventPublisher.getInstance().unRegister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onShowMyDelegateGuideEvent(Event.MyDelegateGuide event) {
        if (!AppSettings.getInstance().getMyDelegateBoolean()) {
            CommonGuideDialogFragment.newInstance(GuideType.DELEGATE_LIST).setOnDissmissListener(new BaseDialogFragment.OnDissmissListener() {
                @Override
                public void onDismiss() {
                    AppSettings.getInstance().setMyDelegateBoolean(true);
                }
            }).show(getActivity().getSupportFragmentManager(), "showGuideDialogFragment");
        }
    }
}
