package com.juzix.wallet.component.ui.presenter;

import android.util.Log;

import com.juzix.wallet.app.CustomObserver;
import com.juzix.wallet.component.ui.base.BasePresenter;
import com.juzix.wallet.component.ui.contract.AssetsContract;
import com.juzix.wallet.component.ui.dialog.InputWalletPasswordDialogFragment;
import com.juzix.wallet.component.ui.view.BackupMnemonicPhraseActivity;
import com.juzix.wallet.engine.WalletManager;
import com.juzix.wallet.entity.Wallet;
import com.juzix.wallet.utils.RxUtils;
import com.trello.rxlifecycle2.android.FragmentEvent;

import org.web3j.crypto.Credentials;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import io.reactivex.functions.Consumer;

public class AssetsPresenter extends BasePresenter<AssetsContract.View> implements AssetsContract.Presenter {

    private List<Wallet> mWalletList;

    public AssetsPresenter(AssetsContract.View view) {
        super(view);
        mWalletList = WalletManager.getInstance().getWalletList();
    }

    @Override
    public void fetchWalletList() {
        if (isViewAttached()) {
            show();
        }
    }

    @Override
    public List<Wallet> getRecycleViewDataSource() {
        return mWalletList;
    }

    @Override
    public void clickRecycleViewItem(Wallet walletEntity) {
        WalletManager.getInstance().setSelectedWallet(walletEntity);
        getView().showWalletList(walletEntity);
        getView().showWalletInfo(walletEntity);
        getView().setArgument(walletEntity);
    }

    @Override
    public void backupWallet() {
        Wallet walletEntity = WalletManager.getInstance().getSelectedWallet();
        InputWalletPasswordDialogFragment.newInstance(walletEntity).setOnWalletCorrectListener(new InputWalletPasswordDialogFragment.OnWalletCorrectListener() {
            @Override
            public void onCorrect(Credentials credentials, String password) {
                BackupMnemonicPhraseActivity.actionStart(getContext(), password, walletEntity, 1);
            }
        }).show(currentActivity().getSupportFragmentManager(), "inputPassword");
    }

    @Override
    public void fetchWalletsBalance() {

        WalletManager.getInstance().getAccountBalance()
                .compose(bindUntilEvent(FragmentEvent.STOP))
                .compose(RxUtils.getSchedulerTransformer())
                .subscribe(new CustomObserver<BigDecimal>() {
                    @Override
                    public void accept(BigDecimal balance) {
                        if (isViewAttached()) {
                            getView().showTotalBalance(balance.toPlainString());
                            Wallet wallet = WalletManager.getInstance().getSelectedWallet();
                            if (wallet != null) {
                                getView().showFreeBalance(wallet.getFreeBalance());
                                getView().showLockBalance(wallet.getLockBalance());
                            }
                            getView().finishRefresh();
                        }
                    }

                    @Override
                    public void accept(Throwable throwable) {
                        super.accept(throwable);
                        if (isViewAttached()) {
                            getView().finishRefresh();
                        }
                    }
                });
    }


    private boolean isSelected(Wallet selectedWallet) {
        if (selectedWallet == null) {
            return false;
        }
        for (int i = 0; i < mWalletList.size(); i++) {
            if (mWalletList.get(i) == selectedWallet) {
                return true;
            }
        }
        return false;
    }

    private Wallet getSelectedWallet() {
        for (int i = 0; i < mWalletList.size(); i++) {
            return mWalletList.get(i);
        }
        return null;
    }

    private void show() {
        Log.e("AssetsFragment", "钱包列表是否为空：" + (mWalletList == null || mWalletList.isEmpty()));
        mWalletList = WalletManager.getInstance().getWalletList();
        if (mWalletList.isEmpty()) {
            getView().showTotalBalance("0");
            getView().showContent(true);
            return;
        }
        Collections.sort(mWalletList);
        Wallet walletEntity = WalletManager.getInstance().getSelectedWallet();
        if (!isSelected(walletEntity)) {
            //挑选一个当前选中的钱包
            walletEntity = getSelectedWallet();
            WalletManager.getInstance().setSelectedWallet(walletEntity);
            getView().setArgument(walletEntity);
        }
        getView().showWalletList(walletEntity);
        getView().showWalletInfo(walletEntity);
        getView().showContent(false);
    }
}
