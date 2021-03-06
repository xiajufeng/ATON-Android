package com.juzix.wallet.component.ui.presenter;

import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.juzix.wallet.R;
import com.juzix.wallet.component.ui.base.BasePresenter;
import com.juzix.wallet.component.ui.contract.ImportObservedContract;
import com.juzix.wallet.component.ui.view.MainActivity;
import com.juzix.wallet.config.AppSettings;
import com.juzix.wallet.engine.NodeManager;
import com.juzix.wallet.engine.WalletManager;
import com.juzix.wallet.utils.CommonUtil;


public class ImportObservedPresenter extends BasePresenter<ImportObservedContract.View> implements ImportObservedContract.Presenter {

    public ImportObservedPresenter(ImportObservedContract.View view) {
        super(view);
    }

    @Override
    public void parseQRCode(String QRCode) {
        getView().showQRCode(QRCode);
    }

    @Override
    public void IsImportObservedWallet(String content) {
        if (!TextUtils.isEmpty(content)) {
            getView().enableImportObservedWallet(true);
        } else {
            getView().enableImportObservedWallet(false);
        }
    }

    @Override
    public void checkPaste() {
        String text = CommonUtil.getTextFromClipboard(getContext());
        if (isViewAttached()) {
            getView().enablePaste(!TextUtils.isEmpty(text));
        }
    }

    //验证钱包地址是否合法，完成导入
    @Override
    public void importWalletAddress(String walletAddress) {
        showLoadingDialog();
        int code = WalletManager.getInstance().importWalletAddress(walletAddress);
        switch (code) {
            case WalletManager.CODE_OK:
                mHandler.sendEmptyMessage(MSG_OK);
                break;
            case WalletManager.CODE_ERROR_INVALIA_ADDRESS:
                mHandler.sendEmptyMessage(MSG_INVALID_ADDRESS);
                break;
            case WalletManager.CODE_ERROR_WALLET_EXISTS:
                mHandler.sendEmptyMessage(MSG_WALLET_EXISTS);
                break;
            default:
                break;
        }
    }

    private static final int MSG_OK = 1;
    private static final int MSG_WALLET_EXISTS = -1;
    private static final int MSG_INVALID_ADDRESS = -2;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_OK:
                    dismissLoadingDialogImmediately();
                    AppSettings.getInstance().setWalletNameSequence(AppSettings.getInstance().getWalletNameSequence(NodeManager.getInstance().getChainId()) + 1);//钱包名称序号自增长
                    MainActivity.actionStart(currentActivity());
                    currentActivity().finish();
                    break;
                case MSG_INVALID_ADDRESS:
                    dismissLoadingDialogImmediately();
                    showLongToast(string(R.string.observed_invalid_address));
                    break;
                case MSG_WALLET_EXISTS:
                    dismissLoadingDialogImmediately();
                    showLongToast(string(R.string.walletExists));
                    break;
                default:
                    break;
            }
        }
    };
}
