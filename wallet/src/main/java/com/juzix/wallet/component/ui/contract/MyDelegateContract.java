package com.juzix.wallet.component.ui.contract;

import com.juzix.wallet.component.ui.base.IPresenter;
import com.juzix.wallet.component.ui.base.IView;
import com.juzix.wallet.entity.DelegateInfo;

import java.util.List;

public class MyDelegateContract {

    public interface View extends IView {
        void showMyDelegateData(List<DelegateInfo> list);

        void showMyDelegateDataFailed();


    }

    public interface Presenter extends IPresenter<View> {

        //加载数据
        void loadMyDelegateData();


    }


}
