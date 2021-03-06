package com.juzix.wallet.component.ui.presenter;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.view.View;

import com.juzhen.framework.util.LogUtils;
import com.juzix.wallet.R;
import com.juzix.wallet.component.ui.base.BaseActivity;
import com.juzix.wallet.component.ui.base.BasePresenter;
import com.juzix.wallet.component.ui.contract.ReceiveTransationContract;
import com.juzix.wallet.component.ui.dialog.BaseDialogFragment;
import com.juzix.wallet.component.ui.dialog.ShareDialogFragment;
import com.juzix.wallet.config.JZAppConfigure;
import com.juzix.wallet.config.JZDirType;
import com.juzix.wallet.engine.WalletManager;
import com.juzix.wallet.entity.ShareAppInfo;
import com.juzix.wallet.entity.Wallet;
import com.juzix.wallet.utils.AppUtil;
import com.juzix.wallet.utils.CommonUtil;
import com.juzix.wallet.utils.DensityUtil;
import com.juzix.wallet.utils.PhotoUtil;
import com.juzix.wallet.utils.QRCodeEncoder;
import com.juzix.wallet.utils.RxUtils;
import com.umeng.socialize.ShareAction;
import com.umeng.socialize.UMShareAPI;
import com.umeng.socialize.UMShareListener;
import com.umeng.socialize.bean.SHARE_MEDIA;
import com.umeng.socialize.media.UMImage;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;

/**
 * @author matrixelement
 */
public class ReceiveTransactionPresenter extends BasePresenter<ReceiveTransationContract.View> implements ReceiveTransationContract.Presenter {

    private Wallet walletEntity;
    private Bitmap mQRCodeBitmap;

    public ReceiveTransactionPresenter(ReceiveTransationContract.View view) {
        super(view);
    }

    @Override
    public void loadData() {
        walletEntity = WalletManager.getInstance().getSelectedWallet();
        if (isViewAttached() && walletEntity != null) {

            getView().setWalletInfo(walletEntity);

            Flowable.fromCallable(new Callable<Bitmap>() {

                @Override
                public Bitmap call() throws Exception {
                    String text = walletEntity.getPrefixAddress();
                    if (!TextUtils.isEmpty(text) && !text.toLowerCase().startsWith("0x")) {
                        text = "0x" + text;
                    }
                    return QRCodeEncoder.syncEncodeQRCode(text, DensityUtil.dp2px(getContext(), 250f));
                }
            }).compose(RxUtils.getFlowableSchedulerTransformer())
                    .subscribe(new Consumer<Bitmap>() {
                        @Override
                        public void accept(Bitmap bitmap) throws Exception {
                            mQRCodeBitmap = bitmap;
                            if (isViewAttached() && bitmap != null) {
                                getView().setWalletAddressQrCode(bitmap);
                            }
                        }
                    });
        }
    }

    private Bitmap screenShot(@NonNull final View decorView) {
        decorView.setDrawingCacheEnabled(true);
        decorView.buildDrawingCache();
        Bitmap bmp = decorView.getDrawingCache();
        Bitmap ret = Bitmap.createBitmap(bmp, 0, 0, decorView.getWidth(), decorView.getHeight());
        decorView.destroyDrawingCache();
        return ret;
    }

    @Override
    public void shareView() {
        if (mQRCodeBitmap == null || walletEntity == null) {
            return;
        }
        String text = walletEntity.getPrefixAddress();
        if (!TextUtils.isEmpty(text) && !text.toLowerCase().startsWith("0x")) {
            text = "0x" + text;
        }
        View shareView = getView().shareView(walletEntity.getName(), text, mQRCodeBitmap);
        final BaseActivity activity = currentActivity();
        JZAppConfigure.getInstance().getDir(activity, JZDirType.plat, new JZAppConfigure.DirCallback() {
            @Override
            public void callback(File dir) {
                if (dir != null) {
                    Bitmap shareBitmap = screenShot(shareView);
                    boolean saved = PhotoUtil.saveImageToAlbum(activity, dir, getImageName(), shareBitmap);
                    if (saved) {
                        showLongToast(R.string.save_image_tips);
                        List<ShareAppInfo> shareAppInfoList = Arrays.asList(ShareAppInfo.values());
                        if (!shareAppInfoList.isEmpty()) {
                            ShareDialogFragment.newInstance(new ArrayList<>(shareAppInfoList))
                                    .setOnShareItemClickListener(new ShareDialogFragment.OnShareItemClickListener() {
                                        @Override
                                        public void onShareItemClick(BaseDialogFragment dialogFragment, ShareAppInfo shareAppInfo) {
                                            dialogFragment.dismiss();
                                            shareAppInfo.share(currentActivity(), shareBitmap, new UMShareListener() {
                                                @Override
                                                public void onStart(SHARE_MEDIA share_media) {
                                                    showLoadingDialog();
                                                }

                                                @Override
                                                public void onResult(SHARE_MEDIA share_media) {
                                                    dismissLoadingDialogImmediately();
                                                    showLongToast(R.string.msg_share_success);
                                                }

                                                @Override
                                                public void onError(SHARE_MEDIA share_media, Throwable throwable) {
                                                    dismissLoadingDialogImmediately();
                                                    showLongToast(R.string.msg_share_failed);
                                                }

                                                @Override
                                                public void onCancel(SHARE_MEDIA share_media) {
                                                    dismissLoadingDialogImmediately();
                                                    showLongToast(R.string.msg_share_cancelled);
                                                }
                                            });
                                        }
                                    })
                                    .show(activity.getSupportFragmentManager(), "showShareDialogFragment");
                        }
                    } else {
                        showShortToast(R.string.save_image_failed_tips);
                    }
                }
            }
        });
    }

    @Override
    public void copy() {
        if (walletEntity == null) {
            return;
        }
        String text = walletEntity.getPrefixAddress();
        if (!TextUtils.isEmpty(text) && !text.toLowerCase().startsWith("0x")) {
            text = "0x" + text;
        }
        CommonUtil.copyTextToClipboard(getContext(), text);
    }

    private String getImageName() {
        return walletEntity.getPrefixAddress() + ".jpg";
    }
}
