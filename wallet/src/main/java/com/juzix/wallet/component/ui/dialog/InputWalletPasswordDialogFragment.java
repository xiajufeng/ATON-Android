package com.juzix.wallet.component.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.juzhen.framework.network.SchedulersTransformer;
import com.juzix.wallet.R;
import com.juzix.wallet.app.Constants;
import com.juzix.wallet.app.LoadingTransformer;
import com.juzix.wallet.component.ui.base.BaseActivity;
import com.juzix.wallet.component.widget.CustomUnderlineEditText;
import com.juzix.wallet.component.widget.ShadowButton;
import com.juzix.wallet.component.widget.ShadowDrawable;
import com.juzix.wallet.entity.Wallet;
import com.juzix.wallet.utils.DensityUtil;
import com.juzix.wallet.utils.JZWalletUtil;
import com.juzix.wallet.utils.RxUtils;

import org.web3j.crypto.Credentials;

import java.util.concurrent.Callable;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;

/**
 * @author matrixelement
 */
public class InputWalletPasswordDialogFragment extends BaseDialogFragment {

    @BindView(R.id.text_wallet_name)
    TextView textWalletName;
    @BindView(R.id.et_password)
    CustomUnderlineEditText etPassword;
    @BindView(R.id.tv_password_error)
    TextView tvPasswordError;
    @BindView(R.id.button_confirm)
    ShadowButton buttonConfirm;
    @BindView(R.id.text_cancel)
    TextView textCancel;
    @BindView(R.id.layout_content)
    LinearLayout layoutContent;

    private Unbinder unbinder;
    private OnWalletCorrectListener mCorrectListener;
    private OnWalletPasswordCorrectListener mListener;

    public static InputWalletPasswordDialogFragment newInstance(Wallet walletEntity) {
        InputWalletPasswordDialogFragment dialogFragment = new InputWalletPasswordDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(Constants.Bundle.BUNDLE_WALLET, walletEntity);
        dialogFragment.setArguments(bundle);
        return dialogFragment;
    }

    public InputWalletPasswordDialogFragment setOnWalletPasswordCorrectListener(OnWalletPasswordCorrectListener mListener) {
        this.mListener = mListener;
        return this;
    }

    public InputWalletPasswordDialogFragment setOnWalletCorrectListener(OnWalletCorrectListener mListener) {
        this.mCorrectListener = mListener;
        return this;
    }

    @Override
    protected Dialog onCreateDialog(Dialog baseDialog) {
        View contentView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_fragment_input_wallet_password, null);
        baseDialog.setContentView(contentView);
        setFullWidthEnable(true);
        setGravity(Gravity.CENTER);
        unbinder = ButterKnife.bind(this, contentView);
        initViews();
        return baseDialog;
    }

    private void initViews() {

        Wallet walletEntity = getArguments().getParcelable(Constants.Bundle.BUNDLE_WALLET);

        ShadowDrawable.setShadowDrawable(layoutContent,
                ContextCompat.getColor(context, R.color.color_ffffff),
                DensityUtil.dp2px(context, 6f),
                ContextCompat.getColor(context, R.color.color_33616161)
                , DensityUtil.dp2px(context, 10f),
                0,
                DensityUtil.dp2px(context, 2f));

        textWalletName.setText(walletEntity.getName());

        RxTextView.textChanges(etPassword)
                .compose(bindToLifecycle())
                .subscribe(new Consumer<CharSequence>() {
                    @Override
                    public void accept(CharSequence charSequence) throws Exception {
                        buttonConfirm.setEnabled(charSequence.toString().trim().length() >= 6);
                    }
                });

        RxView.clicks(textCancel)
                .compose(bindToLifecycle())
                .compose(RxUtils.getClickTransformer())
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(Object object) throws Exception {
                        dismiss();
                    }
                });

        RxView.clicks(buttonConfirm)
                .compose(bindToLifecycle())
                .compose(RxUtils.getClickTransformer())
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(Object object) throws Exception {
                        Single.fromCallable(new Callable<Credentials>() {
                            @Override
                            public Credentials call() throws Exception {
                                return JZWalletUtil.getCredentials(getPassword(), walletEntity.getKey());
                            }
                        })
                                .compose(bindToLifecycle())
                                .compose(new SchedulersTransformer())
                                .compose(LoadingTransformer.bindToSingleLifecycle((BaseActivity) getActivity()))
                                .subscribe(new Consumer<Credentials>() {
                                    @Override
                                    public void accept(Credentials credentials) throws Exception {
                                        if (credentials != null) {
                                            if (mListener != null) {
                                                dismiss();
                                                mListener.onWalletPasswordCorrect(credentials);
                                            }
                                            if (mCorrectListener != null) {
                                                dismiss();
                                                mCorrectListener.onCorrect(credentials, getPassword());
                                            }
                                        } else {
                                            tvPasswordError.setVisibility(View.VISIBLE);
                                            etPassword.setStatus(CustomUnderlineEditText.Status.ERROR);
                                        }
                                    }
                                }, new Consumer<Throwable>() {
                                    @Override
                                    public void accept(Throwable throwable) throws Exception {
                                        tvPasswordError.setVisibility(View.VISIBLE);
                                        etPassword.setStatus(CustomUnderlineEditText.Status.ERROR);
                                    }
                                });
                    }
                });

        showSoftInput(etPassword);
    }

    private String getPassword() {
        return etPassword.getText().toString().trim();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnWalletPasswordCorrectListener) {
            mListener = (OnWalletPasswordCorrectListener) context;
        }
        if (context instanceof OnWalletCorrectListener) {
            mCorrectListener = (OnWalletCorrectListener) context;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (unbinder != null) {
            unbinder.unbind();
        }
    }

    public interface OnWalletCorrectListener {

        void onCorrect(Credentials credentials, String password);
    }

    public interface OnWalletPasswordCorrectListener {

        void onWalletPasswordCorrect(Credentials credentials);
    }
}
